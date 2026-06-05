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

    /** Display-mode values emitted by the sidecar's {@code display} diagnostic (stderr). */
    public static final String DISPLAY_FULLSCREEN = "fullscreen";
    public static final String DISPLAY_OK = "ok";

    private final Supplier<String> selfNameSupplier;
    private final Consumer<Message> sink;
    private volatile Consumer<String> displayModeListener;

    private Process process;
    private volatile boolean running;

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

    /**
     * Launches the sidecar and starts the reader threads. No-op if the executable
     * is missing (the app still runs with whisper-only narration).
     *
     * @throws IOException if the process cannot be started
     */
    public void start() throws IOException {
        final String exePath = Paths.get(System.getenv("ProgramFiles"), "ValorantNarrator", "ocr", "valorantNarrator-ocr.exe").toString();
        File exe = new File(exePath);
        if (!exe.isFile()) {
            logger.warn("OCR chat sidecar not found at '{}' - in-game team/all/party narration disabled.", exePath);
            return;
        }

        process = new ProcessBuilder(exePath).redirectErrorStream(false).start();
        running = true;
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "ocr-chat-shutdown"));
        logger.info("Started OCR chat sidecar: {}", exePath);

        startDaemon("ocr-chat-stdout", this::readStdout);
        startDaemon("ocr-chat-stderr", this::readStderr);
    }

    /**
     * Stops the sidecar and reader threads.
     */
    public void stop() {
        running = false;
        if (process != null) {
            process.destroy();
            process = null;
        }
    }

    private void startDaemon(String name, Runnable body) {
        Thread t = new Thread(body, name);
        t.setDaemon(true);
        t.start();
    }

    private void readStdout() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while (running && (line = reader.readLine()) != null) {
                handleLine(line);
            }
        } catch (IOException e) {
            if (running) logger.warn("OCR sidecar stdout closed: {}", e.getMessage());
        }
    }

    private void readStderr() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while (running && (line = reader.readLine()) != null) {
                handleDiag(line);
            }
        } catch (IOException ignored) {
            // sidecar exited; nothing to do.
        }
    }

    /** Handles a sidecar stderr diagnostic line: routes "display" events, debug-logs the rest. */
    private void handleDiag(String line) {
        String trimmed = line.trim();
        if (trimmed.startsWith("{")) {
            try {
                JsonObject json = JsonParser.parseString(trimmed).getAsJsonObject();
                if ("display".equals(optString(json, "type"))) {
                    String mode = optString(json, "mode");
                    Consumer<String> listener = displayModeListener;
                    if (listener != null && mode != null) listener.accept(mode);
                    return;
                }
            } catch (RuntimeException ignored) {
                // not JSON we care about; fall through to debug-log
            }
        }
        logger.debug("OCR sidecar: {}", line);
    }

    private void handleLine(String line) {
        line = line.trim();
        if (line.isEmpty() || line.charAt(0) != '{') return;
        try {
            JsonObject json = JsonParser.parseString(line).getAsJsonObject();
            if (!"chat".equals(optString(json, "type"))) return;

            String body = optString(json, "body");
            String name = optString(json, "name");
            if (body == null || body.isEmpty() || name == null || name.isEmpty()) return;

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
            boolean own = self != null && self.equalsIgnoreCase(name);
            if ("TO".equalsIgnoreCase(direction)) {
                own = true;
            } else if ("FROM".equalsIgnoreCase(direction)) {
                own = false;
            }

            Message message = new Message(type, name, body, own);
            logger.debug("OCR chat: {}", message);
            sink.accept(message);
        } catch (RuntimeException e) {
            logger.debug("Ignoring unparseable OCR line '{}': {}", line, e.getMessage());
        }
    }
}
