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
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.zip.DataFormatException;

import static com.jprcoder.valnarratorbackend.VoiceType.fromString;
import static com.jprcoder.valnarratorbackend.ZlibCompression.decodeBase64AndInflate;
import static com.jprcoder.valnarratorgui.Main.CONFIG_DIR;
import static com.jprcoder.valnarratorgui.ValNarratorApplication.showAlert;

enum Sources {
    SELF, PARTY, TEAM, SELF_PARTY, SELF_TEAM, PARTY_TEAM, PARTY_TEAM_ALL, SELF_PARTY_TEAM, SELF_TEAM_ALL, SELF_PARTY_TEAM_ALL;

    public static Sources fromString(String text) {
        for (Sources value : Sources.values()) {
            if (value.name().equalsIgnoreCase(text)) {
                return value;
            }
        }
        throw new IllegalArgumentException(String.format("No constant with text %s found in enum Sources!", text));
    }
}

public class VoiceGenerator {
    private static final Logger logger = LoggerFactory.getLogger(VoiceGenerator.class);
    private static final VoiceGenerator singleton;
    private static final String CONFIG_FILE = "config.json";
    private static final int defaultKeyEvent = KeyEvent.VK_V;
    private static final GsonBuilder builder;
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
    private static Sources currentSource = Sources.SELF;
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
        builder = new GsonBuilder();
        builder.setPrettyPrinting();
    }

    private final Robot robot = new Robot();
    private int keyEvent;

    private VoiceGenerator() throws AWTException, IOException {
        this.keyEvent = defaultKeyEvent;
        loadConfig();
        keybindChange(keyEvent);

        Platform.runLater(() -> ValNarratorController.getLatestInstance().teamChatButton.setSelected(isTeamKeyEnabled));
        Platform.runLater(() -> ValNarratorController.getLatestInstance().valorantSettings.setSelected(syncValorantSettingsToggle));
        Platform.runLater(() -> ValNarratorController.getLatestInstance().sources.getSelectionModel().select(currentSource.name().replace("_", "+")));
        Platform.runLater(() -> {
            ValNarratorController.getLatestInstance().micButton.setSelected(isSystemMicStreamed);
            ValNarratorController.getLatestInstance().toggleMic();
        });
        Platform.runLater(() -> {
            ValNarratorController.getLatestInstance().privateChatButton.setSelected(isPrivateMessagesEnabled);
            ValNarratorController.getLatestInstance().togglePrivateMessages();
        });

        try {
            String fileLocation = String.format("%s/ValorantNarrator/SoundVolumeView.exe", System.getenv("ProgramFiles").replace("\\", "/"));
            long pid = ProcessHandle.current().pid();
            String command = fileLocation + " /SetAppDefault \"CABLE Input\" all " + pid;
            long start = System.currentTimeMillis();
            Runtime.getRuntime().exec(command);
            logger.debug(String.format("(%d ms)Successfully set the app's output to VB-Audio CABLE Input.", (System.currentTimeMillis() - start)));
            command = fileLocation + " /SetPlaybackThroughDevice \"CABLE Output\" \"Default Playback Device\"";
            start = System.currentTimeMillis();
            Runtime.getRuntime().exec(command);
            logger.debug(String.format("(%d ms)Added a listen-in into the VB-Audio CABLE Output to default playback device.", (System.currentTimeMillis() - start)));
            command = fileLocation + " /SetListenToThisDevice \"CABLE Output\" 1";
            start = System.currentTimeMillis();
            Runtime.getRuntime().exec(command);
            logger.debug(String.format("(%d ms)Successfully set the listen-in to true on VB-Audio CABLE Output.", (System.currentTimeMillis() - start)));
            command = fileLocation + " /unmute \"CABLE Output\"";
            start = System.currentTimeMillis();
            Runtime.getRuntime().exec(command);
            logger.debug(String.format("(%d ms)Successfully unmuted the VB-Audio CABLE Output.", (System.currentTimeMillis() - start)));
        } catch (IOException e) {
            logger.error(String.format("SoundVolumeView.exe generated an error: %s", e));
            e.printStackTrace();
        }
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
    }

    public static List<String> getInbuiltVoices() {
        return inbuiltVoices;
    }


    public static String getCurrentVoice() {
        return currentVoice;
    }

    private static String filterVoiceName(String voiceName) {
        return voiceName.substring(0, voiceName.lastIndexOf(','));
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
                showAlert("Quota Exhausted", "Your quota has been exhausted, please wait for the next refresh to continue using this voice!");
                return false;
            }
        }
        currentVoice = filterVoiceName(voice);
        currentVoiceType = voiceType;
        logger.info(String.format("(%s)Set voice to: %s", currentVoiceType, currentVoice));
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

    private String getSettingsEventName(int keyEvent) {
        return KeyEvent.getKeyText(keyEvent);
    }

    public void syncValorantPlayerSettings() throws IOException, DataFormatException, InterruptedException {
        final String encodedSettings = ChatDataHandler.getInstance().getAPIHandler().getEncodedPlayerSettings(accessToken, riotClientDetails.version());
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
        ChatDataHandler.getInstance().getAPIHandler().setEncodedPlayerSettings(accessToken, riotClientDetails.version(), ZlibCompression.deflateAndBase64Encode(settingsJson.toString()));

        String fileLocation = String.format("%s/ValorantNarrator/SoundVolumeView.exe", System.getenv("ProgramFiles").replace("\\", "/"));
        Process process = Runtime.getRuntime().exec(String.format("%s /GetColumnValue \"VB-Audio Virtual Cable\\Device\\CABLE Output\\Capture\" \"Item ID\"", fileLocation));
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        final String id = reader.readLine().split("}\\.\\{")[1].replace("}", "");
        logger.info("SETTING Input Device ID: {}", id);
        process.waitFor();
        Path path = Paths.get(System.getenv("LocalAppData"), "VALORANT", "Saved", "Config", String.format("%s-%s", riotClientDetails.subject_id(), riotClientDetails.subject_deployment()), "Windows", "RiotUserSettings.ini");
        String configData = Files.readString(path, StandardCharsets.UTF_8);
        String[] data = configData.split("\n");
        boolean deviceCaptureOverridden = false;
        final String voiceCaptureData = String.format("%s\"{%s}\"", "EAresStringSettingName::VoiceDeviceCaptureHandle=", id);
        for (int i = 0; i < data.length; i++) {
            if (data[i].startsWith("EAresStringSettingName::VoiceDeviceCaptureHandle=")) {
                data[i] = voiceCaptureData;
                deviceCaptureOverridden = true;
            }
        }
        try (FileWriter writer = new FileWriter(String.valueOf(path))) {
            writer.write(String.join("\n", data));
            if (!deviceCaptureOverridden) writer.write(voiceCaptureData);
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

    private void keybindChange(int keyEvent) {
        String keyText = getKeyName(keyEvent);
        if (!keyText.isEmpty()) {
            Platform.runLater(() -> ValNarratorController.getLatestInstance().keybindTextField.setText(keyText));
        }
        Platform.runLater(() -> ValNarratorController.getLatestInstance().keybindText.setText("Pick a keybind for team mic, currently set to " + keyText));
    }

    public String getKeyName(int keyEvent) {
        return KeyEvent.getKeyText(keyEvent);
    }

    private void pressKey() {
        logger.info(String.format("Pressing key: %s", getKeyName(keyEvent)));
        robot.keyPress(keyEvent);
    }

    private void releaseKey() {
        logger.info(String.format("Releasing key: %s", getKeyName(keyEvent)));
        robot.keyRelease(keyEvent);
    }

    public synchronized void setKeyEvent(int keyEvent) throws IOException {
        ValNarratorController.getLatestInstance().keybindTextField.setDisable(true);
        this.keyEvent = keyEvent;
        saveConfig();

        logger.info("Set keybind to: {}", getKeyName(keyEvent));
        ValNarratorController.getLatestInstance().keybindTextField.setDisable(false);
    }

    private void createDefaultConfig() {
        File configFile = new File(CONFIG_DIR, CONFIG_FILE);
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("keyEvent", defaultKeyEvent);
        jsonObject.addProperty("isTeamKeyDisabled", isTeamKeyEnabled);
        jsonObject.addProperty("syncValorantSettingsToggle", syncValorantSettingsToggle);
        jsonObject.addProperty("sources", currentSource.name());
        jsonObject.addProperty("isSystemMicStreamed", isSystemMicStreamed);
        jsonObject.addProperty("isPrivateMessagesEnabled", isPrivateMessagesEnabled);

        try (FileWriter writer = new FileWriter(configFile)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(jsonObject, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadConfig() throws IOException {
        File configFile = new File(CONFIG_DIR, CONFIG_FILE);

        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
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
                    currentSource = Sources.fromString(jsonObject.get("sources").getAsString());
                }
                if (jsonObject.has("isSystemMicStreamed")) {
                    isSystemMicStreamed = jsonObject.get("isSystemMicStreamed").getAsBoolean();
                }
                if (jsonObject.has("isPrivateMessagesEnabled")) {
                    isPrivateMessagesEnabled = jsonObject.get("isPrivateMessagesEnabled").getAsBoolean();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            saveConfig();
        } else {
            Files.createDirectories(Paths.get(CONFIG_DIR));
            createDefaultConfig();
        }
    }

    public void loadCurrentSource(final String sourceName) {
        currentSource = Sources.fromString(sourceName.replace("+", "_"));
    }

    public void saveConfig() throws IOException {
        File configDir = new File(CONFIG_DIR);
        if (!configDir.exists()) {
            Files.createDirectories(Paths.get(CONFIG_DIR));
        }

        File configFile = new File(configDir, CONFIG_FILE);

        try (FileWriter writer = new FileWriter(configFile)) {
            JsonObject jsonObject = getActionKey();

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(jsonObject, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private JsonObject getActionKey() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("keyEvent", keyEvent);
        jsonObject.addProperty("isTeamKeyDisabled", isTeamKeyEnabled);
        jsonObject.addProperty("syncValorantSettingsToggle", syncValorantSettingsToggle);
        jsonObject.addProperty("sources", currentSource.name());
        jsonObject.addProperty("isSystemMicStreamed", isSystemMicStreamed);
        jsonObject.addProperty("isPrivateMessagesEnabled", isPrivateMessagesEnabled);
        return jsonObject;
    }

    /**
     * Wait until detector sees audio or timeout
     */
    private boolean waitForAudioToStart(int timeoutMs) {
        long start = System.currentTimeMillis();
        if (playbackDetector.isPlaying()) return true;

        while (!playbackDetector.isPlaying()) {
            if (System.currentTimeMillis() - start >= timeoutMs) return false;
            Thread.onSpinWait();
        }
        return true;
    }

    /**
     * Wait until detector sees silence
     */
    private void waitUntilDetectorSilent() {
        while (playbackDetector.isPlaying()) {
            Thread.onSpinWait();
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
                waitUntilDetectorSilent();
            } finally {
                if (isTeamKeyEnabled) {
                    releaseKey();
                }
            }
        });
    }

    public Map.Entry<VoiceType, HttpResponse<?>> speakVoice(String text) throws IOException, QuotaExhaustedException, OutdatedVersioningException {
        final long start = System.currentTimeMillis();
        synchronized (this) {
            logger.info("({} ms)Narrating: '{}' using ({}) {}", System.currentTimeMillis() - start, text, currentVoiceType, getCurrentVoice());

            final VoiceType voiceType = currentVoiceType;
            InputStream speechStream;
            HttpResponse<?> httpResp = null;

            try {
                switch (voiceType) {

                    case AGENT:
                        speechStream = agentSynthesizer.getAgentVoice(currentVoice, text);
                        break;

                    case INBUILT:
                        CompletableFuture.runAsync(() -> handleAudioLifecycle(() -> synthesizer.speakInbuiltVoice(currentVoice, text, currentVoiceRate)));
                        return new AbstractMap.SimpleEntry<>(voiceType, null);

                    case STANDARD:
                    default:
                        boolean isNeural = (ChatDataHandler.getInstance().isPremium() && !normalVoices.contains(currentVoice)) || neuralVoices.contains(currentVoice);

                        AbstractMap.Entry<HttpResponse<InputStream>, InputStream> resp = ChatDataHandler.getInstance().getAPIHandler().speakVoice(text, currentVoiceRate, getCurrentVoice(), isNeural ? VoiceEngineType.NEURAL : VoiceEngineType.STANDARD, System.getProperty("aws.accessKeyId"), System.getProperty("aws.secretKey"), System.getProperty("aws.sessionToken"));

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
                return new AbstractMap.SimpleEntry<>(voiceType, httpResp);
            }

            player.setPlayBackListener(new CustomPlaybackListener());

            try {
                player.play();
            } catch (JavaLayerException e) {
                logger.warn("Playback exception: {}", e.getMessage());
            }

            return new AbstractMap.SimpleEntry<>(voiceType, httpResp);
        }

    }

    class CustomPlaybackListener extends PlaybackListener {

        @Override
        public void playbackStarted(PlaybackEvent evt) {
            if (isTeamKeyEnabled) pressKey();
        }

        @Override
        public void playbackFinished(PlaybackEvent evt) {
            if (isTeamKeyEnabled) releaseKey();
        }
    }
}