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
import java.net.URL;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
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
    private static final ResponseProcess isVoiceActive;
    private static final GsonBuilder builder;
    private static final ArrayList<String> normalVoices, neuralVoices;
    private static final List<String> inbuiltVoices;
    private static final ConnectionHandler connectionHandler;
    private static final InbuiltVoiceSynthesizer synthesizer = new InbuiltVoiceSynthesizer();
    private static boolean isSpeaking = false;
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
        try {
            connectionHandler = new ConnectionHandler();
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        normalVoices = new ArrayList<>();
        neuralVoices = new ArrayList<>();
        neuralVoices.add("Kajal");
        inbuiltVoices = synthesizer.getAvailableVoices();
        builder = new GsonBuilder();
        builder.setPrettyPrinting();
        isVoiceActive = new ResponseProcess();
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

        CompletableFuture.runAsync(() -> {
            robot.keyPress(keyEvent);

            while (isVoiceActive.isRunning()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
            }
            robot.keyRelease(keyEvent);
        });
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

    private static boolean isAlreadySpeaking() {
        return isSpeaking;
    }

    private static void startedSpeaking() {
        isSpeaking = true;
    }

    private static void finishedSpeaking() {
        isSpeaking = false;
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
        if (voiceType == VoiceType.PREMIUM) {
            if (!ChatDataHandler.getInstance().isPremium()) {
                ValNarratorController.getLatestInstance().revertVoiceSelection();
                showAlert("Premium Required", "Valorant voices are available with premium, please subscribe to premium to continue using this voice! You can subscribe in the info tab.");
                return false;
            }
            ValNarratorController.getLatestInstance().disableRateSlider();
        } else {
            ValNarratorController.getLatestInstance().enableRateSlider();
        }
        currentVoice = filterVoiceName(voice);
        currentVoiceType = voiceType;
        logger.info(String.format("(%s)Set voice to: %s", currentVoiceType, currentVoice));
        return true;
    }

    public static VoiceGenerator getInstance() {
        return singleton;
    }

    private static JsonObject getJsonObject(int value, String keyEventName) {
        JsonObject newObjectAtIndex0 = new JsonObject();

        newObjectAtIndex0.addProperty("alt", false);
        newObjectAtIndex0.addProperty("bindIndex", value);
        newObjectAtIndex0.addProperty("characterName", "None");
        newObjectAtIndex0.addProperty("cmd", false);
        newObjectAtIndex0.addProperty("ctrl", false);
        newObjectAtIndex0.addProperty("name", "VOICE_TeamPTTAction");
        newObjectAtIndex0.addProperty("key", keyEventName);
        newObjectAtIndex0.addProperty("shift", false);
        newObjectAtIndex0.addProperty("tapHoldType", "None");
        return newObjectAtIndex0;
    }

    public void syncValorantPlayerSettings() throws IOException, DataFormatException, InterruptedException {
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
        } catch (IOException e) {
            logger.error(String.format("SoundVolumeView.exe generated an error: %s", (Object) e.getStackTrace()));
        }
        LockFileHandler lockFileHandler = new LockFileHandler();
        APIHandler apiHandler = new APIHandler(connectionHandler);
        entitlement = apiHandler.getEntitlement(lockFileHandler);
        accessToken = entitlement.accessToken();
        riotClientDetails = apiHandler.getRiotClientDetails(lockFileHandler);
        final String encodedSettings = apiHandler.getEncodedPlayerSettings(accessToken, riotClientDetails.version());
        final String decodedSettingsJson = decodeBase64AndInflate(encodedSettings);
        final JsonObject settingsJson = JsonParser.parseString(decodedSettingsJson).getAsJsonObject();
        JsonArray actionMappings = settingsJson.getAsJsonArray("actionMappings");
        final String keyEventName = KeyEvent.getKeyText(keyEvent);
        int totalKeyCount = 0, copyBindIndex = -1;
        final boolean hasHiddenDefaultKey;
        for (int i = 0; i < actionMappings.size(); i++) {
            JsonObject actionMapping = actionMappings.get(i).getAsJsonObject();
            String name = actionMapping.get("name").getAsString();
            if (name.equals("VOICE_TeamPTTAction")) {
                copyBindIndex = actionMapping.get("bindIndex").getAsInt();
                actionMappings.remove(i);
                i--;
                totalKeyCount++;
            }
        }
        hasHiddenDefaultKey = (totalKeyCount == 1 && copyBindIndex == 1);

        JsonObject newObjectAtIndex0;
        if (hasHiddenDefaultKey) {
            newObjectAtIndex0 = new JsonObject();
            JsonObject newObjectAtIndex1 = getJsonObject(1, "V");

            newObjectAtIndex1.addProperty("alt", false);
            newObjectAtIndex1.addProperty("bindIndex", 0);
            newObjectAtIndex1.addProperty("characterName", "None");
            newObjectAtIndex1.addProperty("cmd", false);
            newObjectAtIndex1.addProperty("ctrl", false);
            newObjectAtIndex1.addProperty("name", "VOICE_TeamPTTAction");
            newObjectAtIndex1.addProperty("key", keyEventName);
            newObjectAtIndex1.addProperty("shift", false);
            newObjectAtIndex1.addProperty("tapHoldType", "None");

            actionMappings.add(newObjectAtIndex1);
        } else {
            newObjectAtIndex0 = getJsonObject(0, keyEventName);
        }
        actionMappings.add(newObjectAtIndex0);
        JsonArray boolSettings = settingsJson.getAsJsonArray("boolSettings");
        for (int i = 0; i < boolSettings.size(); i++) {
            JsonObject boolSetting = boolSettings.get(i).getAsJsonObject();
            String settingEnum = boolSetting.get("settingEnum").getAsString();
            if (settingEnum.equals("EAresBoolSettingName::PushToTalkEnabled")) {
                boolSettings.remove(i);
                i--;
            }
        }
        apiHandler.setEncodedPlayerSettings(accessToken, riotClientDetails.version(), ZlibCompression.deflateAndBase64Encode(settingsJson.toString()));

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
        String keyText = KeyEvent.getKeyText(keyEvent);
        if (!keyText.isEmpty()) {
            Platform.runLater(() -> ValNarratorController.getLatestInstance().keybindTextField.setText(keyText));
        }
        Platform.runLater(() -> ValNarratorController.getLatestInstance().keybindText.setText("Pick a keybind for team mic, currently set to " + keyText));
    }

    public String getCurrentKeybind() {
        return KeyEvent.getKeyText(keyEvent);
    }

    public synchronized void setKeyEvent(int keyEvent) throws IOException {
        ValNarratorController.getLatestInstance().keybindTextField.setDisable(true);
        this.keyEvent = keyEvent;
        saveConfig();
        logger.info("Set keybind to: {}", getCurrentKeybind());
        try {
            isVoiceActive.setFinished();
        } catch (Exception ignored) {
        }
        isVoiceActive.reset();
        ValNarratorController.getLatestInstance().keybindTextField.setDisable(false);
        CompletableFuture.runAsync(() -> {
            robot.keyPress(keyEvent);

            while (isVoiceActive.isRunning()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
            }
            robot.keyRelease(keyEvent);
        });
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
            JsonObject jsonObject = getJsonObject();

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(jsonObject, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private JsonObject getJsonObject() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("keyEvent", keyEvent);
        jsonObject.addProperty("isTeamKeyDisabled", isTeamKeyEnabled);
        jsonObject.addProperty("syncValorantSettingsToggle", syncValorantSettingsToggle);
        jsonObject.addProperty("sources", currentSource.name());
        jsonObject.addProperty("isSystemMicStreamed", isSystemMicStreamed);
        jsonObject.addProperty("isPrivateMessagesEnabled", isPrivateMessagesEnabled);
        return jsonObject;
    }

    public Map.Entry<VoiceType, HttpResponse<?>> speakVoice(String text) throws IOException, QuotaExhaustedException, OutdatedVersioningException {
        boolean isTextOverflowed = Math.min(text.length(), 71) != text.length();

        logger.info(String.format("(%s)Narrating message: '%s' with (%s)%s's voice", (isTextOverflowed) ? "-" : "+", text, currentVoiceType, getCurrentVoice()));
        final VoiceType voiceType = currentVoiceType;
        if (voiceType == VoiceType.PREMIUM) {
            final Map.Entry<HttpResponse<String>, String> response = ChatDataHandler.getInstance().getAPIHandler().speakPremiumVoice(getCurrentVoice(), text);
            final String audioUrl = response.getValue();
            if (audioUrl == null) {
                return new AbstractMap.SimpleEntry<>(voiceType, response.getKey());
            }

            if (isAlreadySpeaking()) {
                while (isAlreadySpeaking()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else startedSpeaking();
            ResponseProcess rp = new ResponseProcess();
            CompletableFuture.runAsync(() -> {
                logger.debug("Using agent voice: {}", currentVoice);
                long start = System.currentTimeMillis();
                try {
                    URL url = new URL(audioUrl);
                    InputStream fin = url.openStream();
                    BufferedInputStream bin = new BufferedInputStream(fin);
                    AdvancedPlayer player = new AdvancedPlayer(bin, FactoryRegistry.systemRegistry().createAudioDevice());
                    player.setPlayBackListener(new CustomPlaybackListener());
                    player.play();
                } catch (JavaLayerException e) {
                    logger.warn("Exception while playing agent voice!");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                logger.debug("Finished speaking in {}ms", System.currentTimeMillis() - start);
                try {
                    finishedSpeaking();
                    rp.setFinished();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            return new AbstractMap.SimpleEntry<>(voiceType, response.getKey());
        } else if (voiceType == VoiceType.INBUILT) {
            logger.debug("Using inbuilt voice: {}", currentVoice);
            long start = System.currentTimeMillis();
            if (isTeamKeyEnabled) {
                robot.keyRelease(keyEvent);
                robot.keyPress(keyEvent);
            }
            synthesizer.speakInbuiltVoice(currentVoice, text, currentVoiceRate);
            logger.debug("Finished speaking in {}ms", System.currentTimeMillis() - start);
            return new AbstractMap.SimpleEntry<>(voiceType, null);
        }

        String id = System.getProperty("aws.accessKeyId"), key = System.getProperty("aws.secretKey"), sessionToken = System.getProperty("aws.sessionToken");
        InputStream speechStream;
        final AbstractMap.Entry<HttpResponse<InputStream>, InputStream> response;
        if ((ChatDataHandler.getInstance().isPremium() && !normalVoices.contains(currentVoice)) || neuralVoices.contains(currentVoice)) {
            response = ChatDataHandler.getInstance().getAPIHandler().speakVoice(text, currentVoiceRate, getCurrentVoice(), VoiceEngineType.NEURAL, id, key, sessionToken);
        } else {
            response = ChatDataHandler.getInstance().getAPIHandler().speakVoice(text, currentVoiceRate, getCurrentVoice(), VoiceEngineType.STANDARD, id, key, sessionToken);
        }
        speechStream = response.getValue();
        AdvancedPlayer player;
        try {
            player = new AdvancedPlayer(speechStream, FactoryRegistry.systemRegistry().createAudioDevice());
        } catch (JavaLayerException e) {
            logger.warn("Exception while initializing normal voice!");
            return new AbstractMap.SimpleEntry<>(voiceType, response.getKey());
        }
        player.setPlayBackListener(new CustomPlaybackListener());
        if (isAlreadySpeaking()) {
            while (isAlreadySpeaking()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } else startedSpeaking();
        ResponseProcess rp = new ResponseProcess();
        CompletableFuture.runAsync(() -> {
            logger.debug("Using standard voice: {}", currentVoice);
            long start = System.currentTimeMillis();
            try {
                player.play();
            } catch (JavaLayerException e) {
                logger.warn("Exception while playing normal voice!");
            }
            logger.debug("Finished speaking in {}ms", System.currentTimeMillis() - start);
            try {
                finishedSpeaking();
                rp.setFinished();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return new AbstractMap.SimpleEntry<>(voiceType, response.getKey());
    }

    class CustomPlaybackListener extends PlaybackListener {

        @Override
        public void playbackStarted(PlaybackEvent evt) {
            if (isTeamKeyEnabled) robot.keyPress(keyEvent);
        }

        @Override
        public void playbackFinished(PlaybackEvent evt) {
            if (isTeamKeyEnabled) robot.keyRelease(keyEvent);
        }
    }
}