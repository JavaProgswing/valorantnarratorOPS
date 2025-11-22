package com.jprcoder.valnarratorbackend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import static com.jprcoder.valnarratorbackend.APIHandler.downloadAgentVoice;

public class AgentVoiceSynthesizer {

    private static final Logger logger = LoggerFactory.getLogger(AgentVoiceSynthesizer.class);

    // Matches tqdm bars, [INFO] logs, or numbered logs like [1/3]
    private static final Pattern PROGRESS_PATTERN = Pattern.compile(
            "^(\\s*\\d+%\\|.*?(ETA|<).*|\\[INFO].*|\\[\\d+/\\d+].*)"
    );

    public volatile String downloadProgress = "";
    private Process voiceServerProcess = null;
    private volatile boolean initialized = false;
    private volatile boolean hasError = false;

    /**
     * UPDATE-ONLY constructor — used only during the auto-updater.
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

    public void initialize(){
        if(voiceServerProcess != null){
            logger.warn("Voice server process is already running.");
            return;
        }

        try {
            String exePath = System.getenv("LOCALAPPDATA").replace("\\", "/")
                    + "/ValorantNarrator/valorantNarrator-agentVoices.exe";

            ProcessBuilder builder = new ProcessBuilder(exePath);
            builder.redirectErrorStream(true);
            voiceServerProcess = builder.start();

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(voiceServerProcess.getInputStream(), StandardCharsets.UTF_8)
                )) {
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
            Process p = new ProcessBuilder(
                    "powershell",
                    "(Get-Item '" + exePath + "').VersionInfo.FileVersion"
            ).start();

            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String version = br.readLine();
                if (version != null) return version.trim();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean isNewerVersionAvailable() {
        String exePath = System.getenv("LOCALAPPDATA").replace("\\", "/")
                + "/ValorantNarrator/valorantNarrator-agentVoices.exe";

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

    public static void checkForUpdates(ProgressCallback callback)
            throws IOException, InterruptedException {
        logger.info("Checking for updates...");
        if (isNewerVersionAvailable()) {
            downloadAgentVoice(callback);
        }
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

    public InputStream getAgentVoice(String agent, String text) throws IOException {
        if (!initialized) throw new IllegalStateException("Voice server not initialized yet.");
        logger.debug("Requesting voice for agent: {}, text: {}", agent, text);

        URL url = new URL("http://127.0.0.1:5005/speak");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");

        String payload = String.format("{\"agent\":\"%s\",\"text\":\"%s\"}", agent, text);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }

        if (conn.getResponseCode() != 200)
            throw new IOException("HTTP " + conn.getResponseCode() + " from voice server.");

        return conn.getInputStream();
    }
}
