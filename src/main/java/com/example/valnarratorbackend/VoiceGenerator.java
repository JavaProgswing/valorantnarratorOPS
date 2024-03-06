package com.example.valnarratorbackend;

import com.example.valnarratorencryption.Signature;
import com.example.valnarratorgui.Main;
import com.example.valnarratorgui.ValNarratorController;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.mccue.jlayer.decoder.JavaLayerException;
import dev.mccue.jlayer.player.FactoryRegistry;
import dev.mccue.jlayer.player.advanced.AdvancedPlayer;
import dev.mccue.jlayer.player.jlp;
import javafx.application.Platform;
import javafx.scene.control.Alert;
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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static com.example.valnarratorencryption.SignatureValidator.generateSignature;
import static com.example.valnarratorgui.Main.CONFIG_DIR;

public class VoiceGenerator {
    private static final CustomFormatter logger = new CustomFormatter(VoiceGenerator.class);
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
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setHeaderText("START-UP Instructions");
                alert.setContentText(String.format("In valorant settings AUDIO>VOICE CHAT, Set Input device to CABLE Output (VB-Audio Virtual Cable) and set Party Voice Activation Mode to Automatic, set Team Voice Chat KeyBind to %s to be able to hear voice comms(Use windows on-screen keyboard if necessary). Also set your preferred sound output/input in windows sound settings.", VoiceGenerator.getInstance().getCurrentKeybind()));
                alert.getDialogPane().setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
                Toolkit.getDefaultToolkit().beep();
                alert.showAndWait();
            });
            isStartupPromptRequired = false;
            saveConfig();
        }
        Platform.runLater(() -> ValNarratorController.latest_instance.teamChatButton.setSelected(isTeamKeyDisabled));
        CompletableFuture.runAsync(() -> {
            while (isVoiceActive.isRunning()) {
                if (!ValNarratorController.latest_instance.getVoicesVisibility() && !ValNarratorController.latest_instance.isSelectingKeybind() && isTeamKeyDisabled) {
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

    public void toggleTeamKey() {
        isTeamKeyDisabled = !isTeamKeyDisabled;
        saveConfig();
    }

    private void keybindChange(int keyEvent) {
        String keyText = KeyEvent.getKeyText(keyEvent);
        if (!keyText.isEmpty()) {
            Platform.runLater(() -> ValNarratorController.latest_instance.keybindTextField.setText(keyText));
        }
        Platform.runLater(() -> ValNarratorController.latest_instance.keybindText.setText("Pick a keybind for team mic, currently set to " + keyText));
    }

    public String getCurrentKeybind() {
        return KeyEvent.getKeyText(keyEvent);
    }

    public int getKeyEvent() {
        return keyEvent;
    }

    public synchronized void setKeyEvent(int keyEvent) {
        ValNarratorController.latest_instance.keybindTextField.setDisable(true);
        this.keyEvent = keyEvent;
        saveConfig();
        logger.info("Set keybind to: " + getCurrentKeybind());
        try {
            isVoiceActive.setFinished();
        } catch (Exception ignored) {
            logger.warn("Error while setting voice keybind.");
        }
        isVoiceActive.reset();
        ValNarratorController.latest_instance.keybindTextField.setDisable(false);
        CompletableFuture.runAsync(() -> {
            while (isVoiceActive.isRunning()) {
                if (!ValNarratorController.latest_instance.getVoicesVisibility() && !ValNarratorController.latest_instance.isSelectingKeybind() && isTeamKeyDisabled) {
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
        jsonObject.addProperty("isStartupPromptRequired", true);
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

    private void saveConfig() {
        File configDir = new File(CONFIG_DIR);
        if (!configDir.exists()) {
            configDir.mkdirs();
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
        if ((ChatHandler.isPremium && !normalVoices.contains(currentVoice)) || neuralVoices.contains(currentVoice))
            synthReq = SynthesizeSpeechRequest.builder().text(text).voiceId(currentVoice).engine(Engine.NEURAL).outputFormat(OutputFormat.MP3).build();
        else
            synthReq = SynthesizeSpeechRequest.builder().text(text).voiceId(currentVoice).engine(Engine.STANDARD).outputFormat(OutputFormat.MP3).build();
        return polly.synthesizeSpeech(synthReq);
    }


    public void speakVoice(String text, String id, String key, String sessionToken) throws IOException {
        boolean isTextOverflowed = Math.min(text.length(), 71) != text.length();
        polly = PollyClient.builder().region(Region.AP_SOUTH_1).credentialsProvider(StaticCredentialsProvider.create(AwsSessionCredentials.create(id, key, sessionToken))).build();
        if (isValorantVoice) {
            HttpClient client = HttpClient.newHttpClient();
            Signature sign = generateSignature();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://api.valnarrator.tech/getPremiumVoice?hwid=" + Main.serialNumber + "&version=" + com.example.valnarratorgui.Main.currentVersion)).setHeader("Authorization", sign.signature()).setHeader("epochTimeElapsed", String.valueOf(sign.epochTime())).POST(HttpRequest.BodyPublishers.ofString(String.format("""
                    {
                      "emotion": "Neutral",
                      "name": "%s",
                      "text": "%s",
                      "speed": 1
                    }""", getCurrentVoice(), text))).setHeader("content-type", "application/json").build();
            HttpResponse<String> response = null;
            try {
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (InterruptedException e) {
                logger.warn("Exception while sending agent voice request!");
            }
            final String audioUrl = response.body();
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
                    } catch (Exception ignored) {
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
                } catch (Exception ignored) {
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