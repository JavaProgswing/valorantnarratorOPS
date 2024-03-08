package com.jprcoder.valnarratorbackend;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jprcoder.valnarratorgui.ValNarratorController;
import dev.mccue.jlayer.decoder.JavaLayerException;
import dev.mccue.jlayer.player.FactoryRegistry;
import dev.mccue.jlayer.player.advanced.AdvancedPlayer;
import dev.mccue.jlayer.player.jlp;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.polly.PollyClient;
import software.amazon.awssdk.services.polly.model.Engine;
import software.amazon.awssdk.services.polly.model.OutputFormat;
import software.amazon.awssdk.services.polly.model.SynthesizeSpeechRequest;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static com.jprcoder.valnarratorgui.Main.CONFIG_DIR;
import static com.jprcoder.valnarratorgui.ValNarratorApplication.showInformation;

public class VoiceGenerator {
    private static final Logger logger = LoggerFactory.getLogger(VoiceGenerator.class);
    private static final VoiceGenerator singleton;
    private static final String CONFIG_FILE = "config.json";
    private static final int defaultKeyEvent = KeyEvent.VK_END;
    private static final ResponseProcess isVoiceActive;
    private static final GsonBuilder builder;
    private static final ArrayList<String> normalVoices, neuralVoices;
    private static boolean isSpeaking = false, isValorantVoice = false, isTeamKeyDisabled = false, isStartupPromptRequired = true;
    private static String currentVoice = "Matthew";

    static {
        try {
            singleton = new VoiceGenerator();
        } catch (AWTException | IOException e) {
            throw new RuntimeException(e);
        }
        normalVoices = new ArrayList<>();
        neuralVoices = new ArrayList<>();
        neuralVoices.add("Kajal");
        builder = new GsonBuilder();
        builder.setPrettyPrinting();
        isVoiceActive = new ResponseProcess();
    }

    private final Robot robot = new Robot();
    private PollyClient polly;
    private int keyEvent;

    private VoiceGenerator() throws AWTException, IOException {
        this.keyEvent = defaultKeyEvent;
        loadConfig();
        keybindChange(keyEvent);
        if (isStartupPromptRequired) {
            Platform.runLater(() -> showInformation("START-UP Instructions", String.format("In valorant settings AUDIO>VOICE CHAT, Set Input device to CABLE Output (VB-Audio Virtual Cable) and set Party Voice Activation Mode to Automatic, set Team Voice Chat KeyBind to %s to be able to hear voice comms(Use windows on-screen keyboard if necessary). Also set your preferred sound output/input in windows sound settings.", VoiceGenerator.getInstance().getCurrentKeybind())));
            isStartupPromptRequired = false;
            saveConfig();
        }
        Platform.runLater(() -> ValNarratorController.getLatestInstance().teamChatButton.setSelected(isTeamKeyDisabled));
        CompletableFuture.runAsync(() -> {
            while (isVoiceActive.isRunning()) {
                if (!ValNarratorController.getLatestInstance().getVoicesVisibility() && !ValNarratorController.getLatestInstance().isSelectingKeybind() && isTeamKeyDisabled) {
                    robot.keyPress(keyEvent);
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                }
                robot.keyRelease(keyEvent);
                try {
                    Thread.sleep(5);
                } catch (InterruptedException ignored) {
                }
            }
            robot.keyRelease(keyEvent);
        });
    }

    public static void generateSingleton() {
    }

    public static boolean isValorantVoice() {
        return isValorantVoice;
    }

    public static void setValorantVoice() {
        isValorantVoice = true;
    }

    public static void setNormalVoice() {
        isValorantVoice = false;
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

    public static void setCurrentVoice(String voice) {
        currentVoice = voice;
    }

    public static VoiceGenerator getInstance() {
        return singleton;
    }

    public void toggleTeamKey() throws IOException {
        isTeamKeyDisabled = !isTeamKeyDisabled;
        saveConfig();
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

    public int getKeyEvent() {
        return keyEvent;
    }

    public synchronized void setKeyEvent(int keyEvent) throws IOException {
        ValNarratorController.getLatestInstance().keybindTextField.setDisable(true);
        this.keyEvent = keyEvent;
        saveConfig();
        logger.info("Set keybind to: " + getCurrentKeybind());
        try {
            isVoiceActive.setFinished();
        } catch (Exception ignored) {
        }
        isVoiceActive.reset();
        ValNarratorController.getLatestInstance().keybindTextField.setDisable(false);
        CompletableFuture.runAsync(() -> {
            while (isVoiceActive.isRunning()) {
                if (!ValNarratorController.getLatestInstance().getVoicesVisibility() && !ValNarratorController.getLatestInstance().isSelectingKeybind() && isTeamKeyDisabled) {
                    robot.keyPress(keyEvent);
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                }
                robot.keyRelease(keyEvent);
                try {
                    Thread.sleep(5);
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
        jsonObject.addProperty("isTeamKeyDisabled", isTeamKeyDisabled);
        jsonObject.addProperty("isStartupPromptRequired", isStartupPromptRequired);
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
                    isTeamKeyDisabled = jsonObject.get("isTeamKeyDisabled").getAsBoolean();
                }
                if (jsonObject.has("isStartupPromptRequired")) {
                    isStartupPromptRequired = jsonObject.get("isStartupPromptRequired").getAsBoolean();
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

    private void saveConfig() throws IOException {
        File configDir = new File(CONFIG_DIR);
        if (!configDir.exists()) {
            Files.createDirectories(Paths.get(CONFIG_DIR));
        }

        File configFile = new File(configDir, CONFIG_FILE);

        try (FileWriter writer = new FileWriter(configFile)) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("keyEvent", keyEvent);
            jsonObject.addProperty("isTeamKeyDisabled", isTeamKeyDisabled);
            jsonObject.addProperty("isStartupPromptRequired", isStartupPromptRequired);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(jsonObject, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private InputStream synthesize(String text) {
        SynthesizeSpeechRequest synthReq;
        if ((ChatDataHandler.getInstance().isPremium() && !normalVoices.contains(currentVoice)) || neuralVoices.contains(currentVoice))
            synthReq = SynthesizeSpeechRequest.builder().text(text).voiceId(currentVoice).engine(Engine.NEURAL).outputFormat(OutputFormat.MP3).build();
        else
            synthReq = SynthesizeSpeechRequest.builder().text(text).voiceId(currentVoice).engine(Engine.STANDARD).outputFormat(OutputFormat.MP3).build();
        return polly.synthesizeSpeech(synthReq);
    }


    public void speakVoice(String text, String id, String key, String sessionToken) throws IOException {
        boolean isTextOverflowed = Math.min(text.length(), 71) != text.length();
        polly = PollyClient.builder().region(Region.AP_SOUTH_1).credentialsProvider(StaticCredentialsProvider.create(AwsSessionCredentials.create(id, key, sessionToken))).build();
        if (isValorantVoice) {
            final String audioUrl = ChatDataHandler.getInstance().getAPIHandler().speakPremiumVoice(getCurrentVoice(), text);
            if (audioUrl == null) {
                return;
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
                try {
                    jlp.createInstance(new String[]{"-url", audioUrl}).play();
                } catch (JavaLayerException e) {
                    logger.warn("Exception while playing agent voice!");
                }

                if (isTextOverflowed)
                    try (InputStream speechStream1 = Objects.requireNonNull(VoiceGenerator.class.getResource("speechEnd.mp3")).openStream()) {
                        AdvancedPlayer player1 = new AdvancedPlayer(speechStream1, FactoryRegistry.systemRegistry().createAudioDevice());
                        player1.play();
                    } catch (JavaLayerException | IOException e) {
                        throw new RuntimeException(e);
                    }
                try {
                    finishedSpeaking();
                    rp.setFinished();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            return;
        }
        InputStream speechStream = synthesize(text);
        AdvancedPlayer player;
        try {
            player = new AdvancedPlayer(speechStream, FactoryRegistry.systemRegistry().createAudioDevice());
        } catch (JavaLayerException e) {
            throw new RuntimeException(e);
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
            try {
                player.play();
            } catch (JavaLayerException e) {
                throw new RuntimeException(e);
            }
            if (isTextOverflowed)
                try (InputStream speechStream1 = Objects.requireNonNull(VoiceGenerator.class.getResource("speechEnd.mp3")).openStream()) {
                    AdvancedPlayer player1 = new AdvancedPlayer(speechStream1, FactoryRegistry.systemRegistry().createAudioDevice());
                    player1.play();
                } catch (JavaLayerException | IOException e) {
                    throw new RuntimeException(e);
                }
            try {
                finishedSpeaking();
                rp.setFinished();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}