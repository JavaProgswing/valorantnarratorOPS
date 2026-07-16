package com.jprcoder.valnarratorbackend;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import static com.jprcoder.valnarratorbackend.APIHandler.downloadAgentVoice;

public class AgentVoiceSynthesizer {

    private static final Logger logger = LoggerFactory.getLogger(AgentVoiceSynthesizer.class);

    // Matches tqdm bars, [INFO] logs, or numbered logs like [1/3]
    private static final Pattern PROGRESS_PATTERN = Pattern.compile("^(\\s*\\d+%\\|.*?(ETA|<).*|\\[INFO].*|\\[\\d+/\\d+].*)");

    // Local voice-server call timeouts. Connect is fast (loopback); read is generous because XTTS
    // synthesis can take several seconds, but bounded so a hung server never blocks the (single)
    // narration thread indefinitely.
    private static final int VOICE_CONNECT_TIMEOUT_MS = 3_000;
    private static final int VOICE_READ_TIMEOUT_MS = 60_000;

    public volatile String downloadProgress = "";
    private Process voiceServerProcess = null;
    private volatile boolean initialized = false;
    private volatile boolean hasError = false;

    /**
     * UPDATE-ONLY constructor - used only during the auto-updater.
     * Does NOT start the Python voice server, only downloads updates.
     */
    public AgentVoiceSynthesizer() {
        try {
            checkForUpdates((percent, downloaded, total) -> {
                String msg;
                if (total > 0) {
                    msg = String.format("Updating agent voices...\n%.2f%% (%,d / %,d bytes)", percent, downloaded, total);
                } else {
                    msg = String.format("Updating agent voices...\nDownloaded %,d bytes", downloaded);
                }
                downloadProgress = msg;
            });
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to check/download updates: ", e);
        }
    }

    // ----------------- VERSION & UPDATE CHECK -----------------

    public static boolean compareVersions(String localVersion, String remoteVersion) {
        if (localVersion == null) return true;

        logger.debug("Comparing versions - Local: {}, Remote: {}", localVersion, remoteVersion);
        try {
            return Float.parseFloat(remoteVersion) > Float.parseFloat(localVersion);
        } catch (NumberFormatException e) {
            return true;
        }
    }

    public static String getFileVersion(String exePath) {
        try {
            Process p = new ProcessBuilder("powershell", "(Get-Item '" + exePath + "').VersionInfo.FileVersion").start();

            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String version = br.readLine();
                if (version != null) {
                    version = version.trim();
                    int firstDotIndex = version.indexOf('.') != -1 ? version.indexOf('.') + 1 : version.length();
                    int secondDotIndex = version.indexOf('.', firstDotIndex) != -1 ? version.indexOf('.', firstDotIndex) : version.length();
                    return version.substring(0, secondDotIndex);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to read agent voice file version: {}", e.getMessage());
        }
        return null;
    }

    public static boolean isNewerVersionAvailable() {
        String exePath = System.getenv("LOCALAPPDATA").replace("\\", "/") + "/ValorantNarrator/valorantNarrator-agentVoices.exe";

        String localVersion = getFileVersion(exePath);
        if (localVersion == null) return true;

        VersionInfo remote;
        try {
            remote = APIHandler.fetchVersionInfo();
        } catch (InterruptedException e) {
            logger.error("Failed to fetch version info: ", e);
            return false;
        }

        return compareVersions(localVersion, String.valueOf(remote.agent_version()));
    }

    public static void checkForUpdates(ProgressCallback callback) throws IOException, InterruptedException {
        logger.info("Checking for updates...");
        if (isNewerVersionAvailable()) {
            downloadAgentVoice(callback);
        }
    }

    /**
     * Best-effort recursive delete; used to wipe the redirected numba cache so a corrupt entry from
     * a previously killed run cannot resurface. Never throws - a failure here is non-fatal.
     */
    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) return;
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) deleteRecursively(child);
        }
        if (!file.delete()) {
            file.deleteOnExit();
        }
    }

    /**
     * Builds the {@code /speak} request body with Gson so any character in {@code agent}/{@code text}
     * - quotes, backslashes, newlines - is properly escaped. A hand-formatted string would emit
     * invalid JSON for such input and the server would reject or mis-parse it, silently dropping
     * that line.
     */
    static String buildSpeakPayload(String agent, String text) {
        JsonObject payloadJson = new JsonObject();
        payloadJson.addProperty("agent", agent);
        payloadJson.addProperty("text", text);
        return payloadJson.toString();
    }

    // ----------------- STATUS / AGENT LISTING -----------------

    public boolean isInitialized() {
        return initialized;
    }

    public String getStatus() {
        if (hasError) return "An error occurred while starting the voice server.";
        if (initialized) return "Voice server ready.";
        if (!downloadProgress.isEmpty()) return "Initializing voice server...\n" + downloadProgress;
        return "Waiting for model to initialize...";
    }

    // ----------------- GET VOICE FROM TTS SERVER -----------------

    public void initialize() {
        if (voiceServerProcess != null) {
            logger.warn("Voice server process is already running.");
            return;
        }

        try {
            String localAppData = System.getenv("LOCALAPPDATA").replace("\\", "/");
            String exePath = localAppData + "/ValorantNarrator/valorantNarrator-agentVoices.exe";

            ProcessBuilder builder = new ProcessBuilder(exePath);
            builder.redirectErrorStream(true);
            // Redirect numba's on-disk cache to a dedicated writable dir so it never reads the
            // corrupt/zeroed cache bundled inside the frozen exe (which crashes librosa's import
            // with "_pickle.UnpicklingError: invalid load key, '\x00'" and kills the voice server).
            // numba's UserProvidedCacheLocator (driven by NUMBA_CACHE_DIR) takes precedence over the
            // in-bundle cache, so this bypasses it. Wipe the dir first to self-heal a prior partial
            // write. Fixes agent voices without re-releasing the Python exe.
            File numbaCache = new File(localAppData + "/ValorantNarrator/numba_cache");
            deleteRecursively(numbaCache);
            if (numbaCache.mkdirs() || numbaCache.isDirectory()) {
                builder.environment().put("NUMBA_CACHE_DIR", numbaCache.getAbsolutePath());
            }
            voiceServerProcess = builder.start();

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(voiceServerProcess.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        logger.debug("[VoiceServer] {}", line);

                        if (line.contains("Application startup complete")) {
                            initialized = true;
                            logger.info("Voice server initialized successfully.");
                        } else if (PROGRESS_PATTERN.matcher(line).find()) {
                            downloadProgress = line;
                        }
                    }
                } catch (IOException e) {
                    logger.error("Error reading TTS server output: ", e);
                    hasError = true;
                }
            });

        } catch (IOException e) {
            logger.error("Failed to start valorantNarrator-agentVoices.exe: ", e);
            hasError = true;
        }
    }

    public InputStream getAgentVoice(String agent, String text) throws IOException {
        if (!initialized) throw new IllegalStateException("Voice server not initialized yet.");
        logger.debug("Requesting voice for agent: {}, text: {}", agent, text);

        URL url = URI.create("http://127.0.0.1:5005/speak").toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(VOICE_CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(VOICE_READ_TIMEOUT_MS);

        byte[] payload = buildSpeakPayload(agent, text).getBytes(StandardCharsets.UTF_8);

        try {
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload);
            }
            int code = conn.getResponseCode();
            if (code != 200) {
                throw new IOException("HTTP " + code + " from voice server.");
            }
            return conn.getInputStream();
        } catch (IOException e) {
            conn.disconnect();
            throw e;
        }
    }
}
