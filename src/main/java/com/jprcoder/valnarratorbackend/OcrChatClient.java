package com.jprcoder.valnarratorbackend;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Bridges the native screen-OCR chat sidecar ({@code valorantNarrator-ocr.exe}) into the
 * narration pipeline. It is the app's sole chat source.
 * <p>
 * In-game chat cannot be read from the network (Riot pins the chat TLS and the game
 * process is Vanguard-protected), so the sidecar captures the on-screen chat box with
 * Windows.Graphics.Capture, OCRs it, and prints each new line as a JSON object on stdout:
 * <pre>{@code {"type":"chat","source":"ocr","channel":"TEAM","name":"Bob","body":"rotate a","direction":null}}</pre>
 * {@code channel} is TEAM / ALL / PARTY / WHISPER; {@code direction} (TO / FROM, whispers
 * only) overrides own-message detection. This client launches the sidecar (resolved from
 * {@code CONFIG_DIR/ocr/}), reads each line, turns it into a {@link Message} and hands it to
 * the supplied sink (normally {@code ChatDataHandler.getInstance()::message}).
 */
public final class OcrChatClient {
    private static final Logger logger = LoggerFactory.getLogger(OcrChatClient.class);

    /**
     * Display-mode values emitted by the sidecar's {@code display} diagnostic (stderr).
     */
    public static final String DISPLAY_FULLSCREEN = "fullscreen";
    public static final String DISPLAY_OK = "ok";

    // Sidecar restart backoff: if the process dies, relaunch it (chat would otherwise go silent
    // until the whole app restarts). Back off when it dies quickly so a persistently-failing exe
    // does not spin in a hot relaunch loop.
    private static final long OCR_RESTART_MIN_UPTIME_MS = 5_000;
    private static final long OCR_RESTART_BASE_DELAY_MS = 1_000;
    private static final long OCR_RESTART_MAX_DELAY_MS = 30_000;

    private final Supplier<String> selfNameSupplier;
    private final Consumer<Message> sink;
    private volatile Consumer<String> displayModeListener;
    private final Object restartLock = new Object();
    private volatile boolean running;
    private volatile Process process;
    private volatile String exePath;
    private volatile long processStartedAt;
    private int consecutiveQuickFailures = 0;

    /**
     * @param selfNameSupplier supplies the local player's display name for
     *                         own-message detection (may return {@code null})
     * @param sink             receives every parsed in-game chat message
     */
    public OcrChatClient(Supplier<String> selfNameSupplier, Consumer<Message> sink) {
        this.selfNameSupplier = selfNameSupplier;
        this.sink = sink;
    }

    /**
     * Sets a listener for the sidecar's capture display-mode signal: {@link #DISPLAY_FULLSCREEN}
     * when Valorant looks to be in exclusive fullscreen (capture stays blank, so chat cannot be
     * read), {@link #DISPLAY_OK} when normal capture resumes. Used to prompt the user to switch to
     * Borderless.
     * <p>
     * <b>Threading:</b> the listener is invoked on the sidecar's background stderr-reader thread.
     * A listener that touches UI must marshal onto its UI thread (e.g. {@code Platform.runLater}).
     */
    public void setDisplayModeListener(Consumer<String> listener) {
        this.displayModeListener = listener;
    }

    private static String optString(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : null;
    }

    private static String safeExitValue(Process p) {
        try {
            return String.valueOf(p.exitValue());
        } catch (IllegalThreadStateException e) {
            return "still-running";
        }
    }

    static Message parseChatLine(String line, Supplier<String> selfNameSupplier) {
        if (line == null) return null;
        line = line.trim();
        if (line.isEmpty() || line.charAt(0) != '{') return null;

        JsonObject json = JsonParser.parseString(line).getAsJsonObject();
        if (!"chat".equals(optString(json, "type"))) return null;

        String body = optString(json, "body");
        String name = optString(json, "name");
        if (body == null || body.isEmpty() || name == null || name.isEmpty()) return null;

        String channel = optString(json, "channel");
        String direction = optString(json, "direction");

        MessageType type;
        if ("ALL".equals(channel)) {
            type = MessageType.ALL;
        } else if ("PARTY".equals(channel)) {
            type = MessageType.PARTY;
        } else if ("WHISPER".equals(channel)) {
            type = MessageType.WHISPER;
        } else {
            type = MessageType.TEAM;
        }

        String self = selfNameSupplier == null ? null : selfNameSupplier.get();
        boolean own = namesMatch(self, name);
        if ("TO".equalsIgnoreCase(direction)) {
            own = true;
        } else if ("FROM".equalsIgnoreCase(direction)) {
            own = false;
        }

        return new Message(type, name, body, own);
    }

    /**
     * True if an OCR-read sender name is the local player's own name, tolerant of light OCR jitter.
     * Exact (case-insensitive) match, or a single character edit for names long enough that one
     * substitution cannot realistically collide two different players (>= 5 chars). Without this,
     * a single mis-read letter in the player's own name (e.g. "FRANKLN" -> "FRANKLY") flips
     * own-message detection and the line is re-routed to a possibly-disabled channel and dropped.
     */
    static boolean namesMatch(String self, String name) {
        if (self == null || name == null) return false;
        String a = self.trim().toLowerCase(java.util.Locale.ROOT);
        String b = name.trim().toLowerCase(java.util.Locale.ROOT);
        if (a.isEmpty() || b.isEmpty()) return false;
        if (a.equals(b)) return true;
        if (a.length() < 5 || Math.abs(a.length() - b.length()) > 1) return false;
        return withinOneEdit(a, b);
    }

    private void startDaemon(String name, Runnable body) {
        Thread t = new Thread(body, name);
        t.setDaemon(true);
        t.start();
    }

    /**
     * True when {@code a} and {@code b} are within a single edit (substitution, insertion or
     * deletion). Assumes their lengths differ by at most one (callers guarantee this).
     */
    private static boolean withinOneEdit(String a, String b) {
        if (a.length() == b.length()) {
            int diff = 0;
            for (int i = 0; i < a.length(); i++)
                if (a.charAt(i) != b.charAt(i) && ++diff > 1) return false;
            return true;
        }
        // one longer than the other: check b can be formed from a by a single insertion
        String longer = a.length() > b.length() ? a : b;
        String shorter = a.length() > b.length() ? b : a;
        int i = 0, j = 0;
        boolean skipped = false;
        while (i < longer.length() && j < shorter.length()) {
            if (longer.charAt(i) == shorter.charAt(j)) {
                i++;
                j++;
            } else {
                if (skipped) return false;
                skipped = true;
                i++; // consume the extra char in the longer string
            }
        }
        return true;
    }

    /**
     * Launches the sidecar and starts the reader threads. No-op if the executable
     * is missing (the app still runs with whisper-only narration).
     *
     * @throws IOException if the process cannot be started
     */
    public void start() throws IOException {
        exePath = Paths.get(System.getenv("ProgramFiles"), "ValorantNarrator", "ocr", "valorantNarrator-ocr.exe").toString();
        File exe = new File(exePath);
        if (!exe.isFile()) {
            logger.warn("OCR chat sidecar not found at '{}' - in-game team/all/party narration disabled.", exePath);
            return;
        }

        running = true;
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "ocr-chat-shutdown"));
        launch();
    }

    /**
     * Starts (or restarts) the sidecar process and its reader threads. Each reader is bound to the
     * specific {@link Process} it was started for, so a stale reader from a dead process never
     * touches its replacement.
     */
    private void launch() throws IOException {
        Process p = new ProcessBuilder(exePath).redirectErrorStream(false).start();
        process = p;
        processStartedAt = System.currentTimeMillis();
        logger.info("Started OCR chat sidecar: {}", exePath);

        startDaemon("ocr-chat-stdout", () -> readStdout(p));
        startDaemon("ocr-chat-stderr", () -> readStderr(p));
    }

    /**
     * Stops the sidecar and reader threads, and prevents any further auto-restart.
     */
    public void stop() {
        running = false;
        Process p = process;
        if (p != null) {
            p.destroy();
            process = null;
        }
    }

    static String parseDisplayModeDiagnostic(String line) {
        if (line == null) return null;
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.charAt(0) != '{') return null;
        try {
            JsonObject json = JsonParser.parseString(trimmed).getAsJsonObject();
            if (!"display".equals(optString(json, "type"))) return null;
            return optString(json, "mode");
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void readStdout(Process p) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while (running && (line = reader.readLine()) != null) {
                handleLine(line);
            }
        } catch (IOException e) {
            if (running) logger.warn("OCR sidecar stdout closed: {}", e.getMessage());
        }
        if (running) maybeRestart(p);
    }

    private void readStderr(Process p) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while (running && (line = reader.readLine()) != null) {
                handleDiag(line);
            }
        } catch (IOException ignored) {
            // sidecar exited; the stdout reader owns restart, so nothing to do here.
        }
    }

    /**
     * Relaunches the sidecar after it dies, with exponential backoff on rapid repeated failures.
     * Only the stdout reader calls this, and only for the current process, so a single death yields
     * a single restart.
     */
    private void maybeRestart(Process dead) {
        synchronized (restartLock) {
            if (!running || process != dead) return;

            long uptime = System.currentTimeMillis() - processStartedAt;
            if (uptime < OCR_RESTART_MIN_UPTIME_MS) {
                consecutiveQuickFailures++;
            } else {
                consecutiveQuickFailures = 0;
            }
            long delay = Math.min(OCR_RESTART_MAX_DELAY_MS,
                    OCR_RESTART_BASE_DELAY_MS * (1L << Math.min(consecutiveQuickFailures, 5)));
            logger.warn("OCR sidecar exited (exit={}); restarting in {} ms.", safeExitValue(dead), delay);

            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            if (!running) return;
            try {
                launch();
            } catch (IOException e) {
                logger.error("Failed to restart OCR sidecar: {}", e.getMessage());
            }
        }
    }

    /**
     * Handles a sidecar stderr diagnostic line: routes "display" events, debug-logs the rest.
     */
    private void handleDiag(String line) {
        String mode = parseDisplayModeDiagnostic(line);
        if (mode != null) {
            Consumer<String> listener = displayModeListener;
            if (listener != null) listener.accept(mode);
            return;
        }
        logger.debug("OCR sidecar: {}", line);
    }

    private void handleLine(String line) {
        try {
            Message message = parseChatLine(line, selfNameSupplier);
            if (message == null) return;
            logger.debug("OCR chat: {}", message);
            sink.accept(message);
        } catch (RuntimeException e) {
            logger.debug("Ignoring unparseable OCR line '{}': {}", line, e.getMessage());
        }
    }
}
