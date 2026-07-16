package com.jprcoder.valnarratorbackend;

import com.google.gson.*;
import com.jprcoder.valnarratorgui.ValNarratorController;
import dev.mccue.jlayer.decoder.JavaLayerException;
import dev.mccue.jlayer.player.FactoryRegistry;
import dev.mccue.jlayer.player.advanced.AdvancedPlayer;
import dev.mccue.jlayer.player.advanced.PlaybackEvent;
import dev.mccue.jlayer.player.advanced.PlaybackListener;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.zip.DataFormatException;

import static com.jprcoder.valnarratorbackend.VoiceType.fromString;
import static com.jprcoder.valnarratorbackend.ZlibCompression.decodeBase64AndInflate;
import static com.jprcoder.valnarratorgui.Main.CONFIG_DIR;
import static com.jprcoder.valnarratorgui.ValNarratorApplication.showAlert;

public class VoiceGenerator {
    private static final Logger logger = LoggerFactory.getLogger(VoiceGenerator.class);
    private static final VoiceGenerator singleton;
    private static final String CONFIG_FILE = "config.json";
    private static final int CONFIG_VERSION = 2;
    private static final int defaultKeyEvent = KeyEvent.VK_V;
    // Hard ceiling on how long the push-to-talk key is held for one narration, as a safety net
    // against the audio detector never reporting silence.
    private static final int MAX_PTT_HOLD_MS = 30_000;
    private static final ArrayList<String> normalVoices, neuralVoices;
    private static final List<String> inbuiltVoices;
    private static final InbuiltVoiceSynthesizer synthesizer = new InbuiltVoiceSynthesizer();
    private static final AgentVoiceSynthesizer agentSynthesizer = new AgentVoiceSynthesizer();
    private static final PlaybackDetector playbackDetector = new PlaybackDetector();

    private static boolean isTeamKeyEnabled = true;
    private static boolean syncValorantSettingsToggle = true;
    private static boolean isSystemMicStreamed = false;
    private static boolean isPrivateMessagesEnabled = false;
    private static String currentVoice = "Matthew";
    private static EnumSet<Source> currentSource = Source.fromString("SELF");
    private static VoiceType currentVoiceType = VoiceType.STANDARD;
    private static short currentVoiceRate = 100;
    private static RiotClientDetails riotClientDetails;
    private static String accessToken;
    private static EntitlementsTokenResponse entitlement;

    static {
        try {
            singleton = new VoiceGenerator();
        } catch (AWTException | IOException e) {
            throw new RuntimeException(e);
        }
        normalVoices = new ArrayList<>();
        neuralVoices = new ArrayList<>();
        neuralVoices.add("Kajal");
        inbuiltVoices = synthesizer.getAvailableVoices();
    }

    private final Robot robot = new Robot();
    private int keyEvent;

    private VoiceGenerator() throws AWTException, IOException {
        this.keyEvent = defaultKeyEvent;
        loadConfig();
        keybindChange(keyEvent);

        Platform.runLater(() -> ValNarratorController.getLatestInstance().teamChatButton.setSelected(isTeamKeyEnabled));
        Platform.runLater(() -> ValNarratorController.getLatestInstance().valorantSettings
                .setSelected(syncValorantSettingsToggle));
        Platform.runLater(() -> ValNarratorController.getLatestInstance().sources.getSelectionModel()
                .select(Source.toString(currentSource)));
        Platform.runLater(() -> {
            ValNarratorController.getLatestInstance().micButton.setSelected(isSystemMicStreamed);
            ValNarratorController.getLatestInstance().toggleMic();
        });
        Platform.runLater(() -> {
            ValNarratorController.getLatestInstance().privateChatButton.setSelected(isPrivateMessagesEnabled);
            ValNarratorController.getLatestInstance().togglePrivateMessages();
        });

        String soundVolumeView = String.format("%s/ValorantNarrator/SoundVolumeView.exe",
                System.getenv("ProgramFiles").replace("\\", "/"));
        long pid = ProcessHandle.current().pid();
        ProcessUtil.runDetached(soundVolumeView, "/SetAppDefault", "CABLE Input", "all", String.valueOf(pid));
        ProcessUtil.runDetached(soundVolumeView, "/SetPlaybackThroughDevice", "CABLE Output",
                "Default Playback Device");
        ProcessUtil.runDetached(soundVolumeView, "/SetListenToThisDevice", "CABLE Output", "1");
        ProcessUtil.runDetached(soundVolumeView, "/unmute", "CABLE Output");
        logger.debug("Configured VB-Audio CABLE routing.");

        LockFileHandler lockFileHandler = new LockFileHandler();
        entitlement = ChatDataHandler.getInstance().getAPIHandler().getEntitlement(lockFileHandler);
        accessToken = entitlement.accessToken();
        riotClientDetails = ChatDataHandler.getInstance().getAPIHandler().getRiotClientDetails(lockFileHandler);
    }

    public static void initializeAgentSynthesizer() {
        agentSynthesizer.initialize();
    }

    public static RiotClientDetails getRiotClientDetails() {
        return riotClientDetails;
    }

    public static String getAccessToken() {
        return accessToken;
    }

    public static EntitlementsTokenResponse getEntitlement() {
        return entitlement;
    }

    public static boolean isSyncValorantSettingsEnabled() {
        return syncValorantSettingsToggle;
    }

    public static void generateSingleton() {
        // Initializes the static block
    }

    public static List<String> getInbuiltVoices() {
        return inbuiltVoices;
    }

    public static String getCurrentVoice() {
        return currentVoice;
    }

    private static String filterVoiceName(String voiceName) {
        int comma = voiceName.lastIndexOf(',');
        return comma < 0 ? voiceName : voiceName.substring(0, comma);
    }

    public static void setCurrentRate(final short rate) {
        currentVoiceRate = rate;
    }

    public static boolean setCurrentVoice(final String voice) {
        final VoiceType voiceType = fromString(voice);
        if (voiceType == VoiceType.AGENT) {
            if (!agentSynthesizer.isInitialized()) {
                ValNarratorController.getLatestInstance().revertVoiceSelection();
                showAlert("Agent Voice", agentSynthesizer.getStatus());
                return false;
            }
        } else {
            if (ChatDataHandler.getInstance().getProperties().isQuotaExhausted() && voiceType == VoiceType.STANDARD) {
                ValNarratorController.getLatestInstance().revertVoiceSelection();
                showAlert("Quota Exhausted",
                        "Your quota has been exhausted, please wait for the next refresh to continue using this voice!");
                return false;
            }
        }
        currentVoice = filterVoiceName(voice);
        currentVoiceType = voiceType;
        logger.debug(String.format("(%s)Set voice to: %s", currentVoiceType, currentVoice));
        return true;
    }

    public static VoiceGenerator getInstance() {
        return singleton;
    }

    private static JsonObject getActionKey(int value, String keyEventName) {
        JsonObject actionKey = new JsonObject();
        actionKey.addProperty("alt", false);
        actionKey.addProperty("bindIndex", value);
        actionKey.addProperty("characterName", "None");
        actionKey.addProperty("cmd", false);
        actionKey.addProperty("ctrl", false);
        actionKey.addProperty("name", "VOICE_TeamPTTAction");
        actionKey.addProperty("key", keyEventName);
        actionKey.addProperty("shift", false);
        actionKey.addProperty("tapHoldType", "None");
        return actionKey;
    }

    private String getSettingsEventName(int code) {
        return KeyEvent.getKeyText(code);
    }

    /**
     * Pushes the app's team push-to-talk keybind into Valorant's cloud settings and
     * points Valorant's voice-capture device at the VB-Audio CABLE output, so
     * generated
     * narration is transmitted on the team voice channel. Rewrites the local
     * {@code RiotUserSettings.ini} and re-uploads the encoded settings blob.
     */
    public void syncValorantPlayerSettings() throws IOException, DataFormatException, InterruptedException {
        final String encodedSettings = ChatDataHandler.getInstance().getAPIHandler()
                .getEncodedPlayerSettings(accessToken, riotClientDetails.version());
        final String decodedSettingsJson = decodeBase64AndInflate(encodedSettings);
        final JsonObject settingsJson = JsonParser.parseString(decodedSettingsJson).getAsJsonObject();
        JsonArray actionMappings = settingsJson.getAsJsonArray("actionMappings");
        final String keyEventName = getSettingsEventName(keyEvent);
        String prefKeyEventName = null;
        for (int i = 0; i < actionMappings.size(); i++) {
            JsonObject actionMapping = actionMappings.get(i).getAsJsonObject();
            String name = actionMapping.get("name").getAsString();
            if (name.equals("VOICE_TeamPTTAction")) {
                logger.debug("Found existing VOICE_TeamPTTAction keybind: {}", actionMapping);
                if (actionMapping.get("bindIndex").getAsInt() == 0) {
                    prefKeyEventName = actionMapping.get("key").getAsString();
                }
                actionMappings.remove(i);
                i--;
            }
        }

        JsonObject appKeybind = getActionKey(1, keyEventName);
        logger.debug("App keybind: {}", appKeybind);
        actionMappings.add(appKeybind);

        if (prefKeyEventName != null) {
            JsonObject userPreferredKeybind = getActionKey(0, prefKeyEventName);
            logger.debug("User preferred keybind: {}", userPreferredKeybind);
            actionMappings.add(userPreferredKeybind);
        }

        JsonArray boolSettings = settingsJson.getAsJsonArray("boolSettings");
        for (int i = 0; i < boolSettings.size(); i++) {
            JsonObject boolSetting = boolSettings.get(i).getAsJsonObject();
            String settingEnum = boolSetting.get("settingEnum").getAsString();
            if (settingEnum.equals("EAresBoolSettingName::PushToTalkEnabled")) {
                boolSettings.remove(i);
                i--;
            }
        }
        logger.debug(String.valueOf(settingsJson));
        ChatDataHandler.getInstance().getAPIHandler().setEncodedPlayerSettings(accessToken, riotClientDetails.version(),
                ZlibCompression.deflateAndBase64Encode(settingsJson.toString()));

        String fileLocation = String.format("%s/ValorantNarrator/SoundVolumeView.exe",
                System.getenv("ProgramFiles").replace("\\", "/"));
        Process process = ProcessUtil.start(fileLocation, "/GetColumnValue",
                "VB-Audio Virtual Cable\\Device\\CABLE Output\\Capture", "Item ID");
        final String id;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            id = reader.readLine().split("}\\.\\{")[1].replace("}", "");
        }
        logger.debug("SETTING Input Device ID: {}", id);
        process.waitFor();
        Path path = Paths.get(System.getenv("LocalAppData"), "VALORANT", "Saved", "Config",
                String.format("%s-%s", riotClientDetails.subject_id(), riotClientDetails.subject_deployment()),
                "Windows", "RiotUserSettings.ini");
        String configData = Files.readString(path, StandardCharsets.UTF_8);
        String[] data = configData.split("\n");
        boolean deviceCaptureOverridden = false;
        final String voiceCaptureData = String.format("%s\"{%s}\"", "EAresStringSettingName::VoiceDeviceCaptureHandle=",
                id);
        for (int i = 0; i < data.length; i++) {
            if (data[i].startsWith("EAresStringSettingName::VoiceDeviceCaptureHandle=")) {
                data[i] = voiceCaptureData;
                deviceCaptureOverridden = true;
            }
        }
        try (FileWriter writer = new FileWriter(String.valueOf(path), StandardCharsets.UTF_8)) {
            writer.write(String.join("\n", data));
            if (!deviceCaptureOverridden) {
                writer.write("\n" + voiceCaptureData);
            }
        }
    }

    public boolean syncValorantSettingsToggle() throws IOException {
        syncValorantSettingsToggle = !syncValorantSettingsToggle;
        saveConfig();
        return syncValorantSettingsToggle;
    }

    public boolean toggleTeamKey() throws IOException {
        isTeamKeyEnabled = !isTeamKeyEnabled;
        saveConfig();
        return isTeamKeyEnabled;
    }

    private void keybindChange(int newKeyEvent) {
        String keyText = getKeyName(newKeyEvent);
        if (!keyText.isEmpty()) {
            Platform.runLater(() -> ValNarratorController.getLatestInstance().keybindTextField.setText(keyText));
        }
        Platform.runLater(
                () -> ValNarratorController.getLatestInstance().keybindText.setText("Team voice key: " + keyText));
    }

    public String getKeyName(int code) {
        return KeyEvent.getKeyText(code);
    }

    private void pressKey() {
        logger.debug(String.format("Pressing key: %s", getKeyName(keyEvent)));
        robot.keyPress(keyEvent);
    }

    private void releaseKey() {
        logger.debug(String.format("Releasing key: %s", getKeyName(keyEvent)));
        robot.keyRelease(keyEvent);
    }

    public synchronized void setKeyEvent(int newKeyEvent) throws IOException {
        ValNarratorController.getLatestInstance().keybindTextField.setDisable(true);
        this.keyEvent = newKeyEvent;
        saveConfig();

        logger.debug("Set keybind to: {}", getKeyName(newKeyEvent));
        ValNarratorController.getLatestInstance().keybindTextField.setDisable(false);
    }

    /**
     * Loads {@code config.json}, tolerating older layouts. Any keys missing from an
     * older file simply keep their defaults, and the file is rewritten in the
     * current
     * format afterwards so upgrades are seamless (backwards compatible).
     */
    private void loadConfig() throws IOException {
        File configFile = new File(CONFIG_DIR, CONFIG_FILE);

        if (!configFile.exists()) {
            Files.createDirectories(Paths.get(CONFIG_DIR));
            saveConfig(); // write defaults (including built-in full-forms)
            return;
        }

        try (FileReader reader = new FileReader(configFile, StandardCharsets.UTF_8)) {
            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
            if (jsonObject.has("keyEvent")) {
                keyEvent = jsonObject.get("keyEvent").getAsInt();
            }
            if (jsonObject.has("isTeamKeyDisabled")) {
                isTeamKeyEnabled = jsonObject.get("isTeamKeyDisabled").getAsBoolean();
            }
            if (jsonObject.has("syncValorantSettingsToggle")) {
                syncValorantSettingsToggle = jsonObject.get("syncValorantSettingsToggle").getAsBoolean();
            }
            if (jsonObject.has("sources")) {
                currentSource = Source.fromString(jsonObject.get("sources").getAsString());
            }
            if (jsonObject.has("isSystemMicStreamed")) {
                isSystemMicStreamed = jsonObject.get("isSystemMicStreamed").getAsBoolean();
            }
            if (jsonObject.has("isPrivateMessagesEnabled")) {
                isPrivateMessagesEnabled = jsonObject.get("isPrivateMessagesEnabled").getAsBoolean();
            }
            if (jsonObject.has("fullForms") && jsonObject.get("fullForms").isJsonObject()) {
                Map<String, String> loaded = new LinkedHashMap<>();
                for (Map.Entry<String, JsonElement> e : jsonObject.getAsJsonObject("fullForms").entrySet()) {
                    if (e.getValue() != null && e.getValue().isJsonPrimitive()) {
                        loaded.put(e.getKey(), e.getValue().getAsString());
                    }
                }
                if (!loaded.isEmpty())
                    ChatUtilityHandler.setFullForms(loaded);
            }
            // Older configs without "fullForms" keep the built-in defaults and gain
            // the key on the rewrite below.
        } catch (Exception e) {
            logger.warn("Failed to parse config.json, keeping defaults: {}", e.getMessage());
        }
        saveConfig();
    }

    public void loadCurrentSource(final String sourceName) {
        currentSource = Source.fromString(sourceName.replace("+", "_"));
    }

    public void saveConfig() throws IOException {
        File configDir = new File(CONFIG_DIR);
        if (!configDir.exists()) {
            Files.createDirectories(Paths.get(CONFIG_DIR));
        }

        File configFile = new File(configDir, CONFIG_FILE);
        try (FileWriter writer = new FileWriter(configFile, StandardCharsets.UTF_8)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(buildConfigJson(), writer);
        } catch (IOException e) {
            logger.error("Failed to write config.json: {}", e.getMessage());
            throw e;
        }
    }

    private JsonObject buildConfigJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("configVersion", CONFIG_VERSION);
        jsonObject.addProperty("keyEvent", keyEvent);
        jsonObject.addProperty("isTeamKeyDisabled", isTeamKeyEnabled);
        jsonObject.addProperty("syncValorantSettingsToggle", syncValorantSettingsToggle);
        jsonObject.addProperty("sources", Source.toString(currentSource));
        jsonObject.addProperty("isSystemMicStreamed", isSystemMicStreamed);
        jsonObject.addProperty("isPrivateMessagesEnabled", isPrivateMessagesEnabled);

        JsonObject fullForms = new JsonObject();
        ChatUtilityHandler.getFullForms().forEach(fullForms::addProperty);
        jsonObject.add("fullForms", fullForms);
        return jsonObject;
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void closeQuietly(Closeable c) {
        if (c == null) return;
        try {
            c.close();
        } catch (IOException ignored) {
            // best-effort cleanup
        }
    }

    /**
     * Wait until detector sees audio or timeout. Polls with a short sleep rather than a busy spin
     * so it does not peg a CPU core while waiting.
     */
    private boolean waitForAudioToStart(int timeoutMs) {
        long start = System.currentTimeMillis();
        while (!playbackDetector.isPlaying()) {
            if (System.currentTimeMillis() - start >= timeoutMs)
                return false;
            sleepQuietly(3);
        }
        return true;
    }

    /**
     * Wait until the detector sees silence, but never longer than {@code maxHoldMs} so a detector
     * that stays (wrongly) above threshold - e.g. steady background noise on the CABLE line -
     * cannot pin the push-to-talk key open and block the narration thread indefinitely.
     */
    private void waitUntilDetectorSilent(int maxHoldMs) {
        long start = System.currentTimeMillis();
        while (playbackDetector.isPlaying()) {
            if (System.currentTimeMillis() - start >= maxHoldMs) {
                logger.warn("Audio still detected after {} ms; releasing key anyway.", maxHoldMs);
                return;
            }
            sleepQuietly(3);
        }
    }

    private void handleAudioLifecycle(Runnable ttsTask) {
        if (isTeamKeyEnabled) {
            pressKey();
        }
        if (ttsTask != null) {
            ttsTask.run();
        }

        CompletableFuture.runAsync(() -> {
            waitForAudioToStart(3000);

            try {
                waitUntilDetectorSilent(MAX_PTT_HOLD_MS);
            } finally {
                if (isTeamKeyEnabled) {
                    releaseKey();
                }
            }
        });
    }

    /**
     * Synthesizes and plays {@code text} with the currently selected voice, routing
     * to
     * the agent voice server, the inbuilt Windows synthesizer, or AWS Polly
     * (standard or
     * neural) depending on the active {@link VoiceType}. Serialized on this
     * instance so
     * narrations play one at a time.
     *
     * @return the voice type used paired with the originating HTTP response
     * (response is
     * {@code null} for the inbuilt synthesizer, which plays directly).
     */
    public Map.Entry<VoiceType, HttpResponse<?>> speakVoice(String text)
            throws IOException, QuotaExhaustedException, OutdatedVersioningException {
        final long start = System.currentTimeMillis();
        synchronized (this) {
            logger.info("({} ms)Narrating: '{}' using ({}) {}", System.currentTimeMillis() - start, text,
                    currentVoiceType, getCurrentVoice());

            final VoiceType voiceType = currentVoiceType;
            InputStream speechStream;
            HttpResponse<?> httpResp = null;

            try {
                switch (voiceType) {

                    case AGENT:
                        speechStream = agentSynthesizer.getAgentVoice(currentVoice, text);
                        break;

                    case INBUILT:
                        CompletableFuture.runAsync(() -> handleAudioLifecycle(
                                () -> synthesizer.speakInbuiltVoice(currentVoice, text, currentVoiceRate)));
                        return new AbstractMap.SimpleEntry<>(voiceType, null);

                    case STANDARD:
                    default:
                        boolean isNeural = (ChatDataHandler.getInstance().isPremium()
                                && !normalVoices.contains(currentVoice)) || neuralVoices.contains(currentVoice);

                        AbstractMap.Entry<HttpResponse<InputStream>, InputStream> resp = ChatDataHandler.getInstance()
                                .getAPIHandler().speakVoice(text, currentVoiceRate, getCurrentVoice(),
                                        isNeural ? VoiceEngineType.NEURAL : VoiceEngineType.STANDARD,
                                        System.getProperty("aws.accessKeyId"), System.getProperty("aws.secretKey"),
                                        System.getProperty("aws.sessionToken"));

                        httpResp = resp.getKey();
                        speechStream = resp.getValue();
                        break;
                }
            } catch (IOException e) {
                logger.error("Error preparing TTS stream for voice type {}: {}", voiceType, e.getMessage());
                return new AbstractMap.SimpleEntry<>(voiceType, null);
            }

            AdvancedPlayer player;
            try {
                player = new AdvancedPlayer(speechStream, FactoryRegistry.systemRegistry().createAudioDevice());
            } catch (JavaLayerException e) {
                logger.warn("Failed to initialize player: {}", e.getMessage());
                closeQuietly(speechStream);
                return new AbstractMap.SimpleEntry<>(voiceType, httpResp);
            }

            player.setPlayBackListener(new CustomPlaybackListener());

            try {
                player.play();
            } catch (JavaLayerException e) {
                logger.warn("Playback exception: {}", e.getMessage());
                // Playback may have died after the key was pressed (playbackStarted) but before
                // playbackFinished released it - release defensively so the mic key never sticks.
                if (isTeamKeyEnabled) {
                    releaseKey();
                }
            } finally {
                player.close();
                closeQuietly(speechStream);
            }

            return new AbstractMap.SimpleEntry<>(voiceType, httpResp);
        }

    }

    class CustomPlaybackListener extends PlaybackListener {

        @Override
        public void playbackStarted(PlaybackEvent evt) {
            if (isTeamKeyEnabled)
                pressKey();
        }

        @Override
        public void playbackFinished(PlaybackEvent evt) {
            if (isTeamKeyEnabled)
                releaseKey();
        }
    }
}
