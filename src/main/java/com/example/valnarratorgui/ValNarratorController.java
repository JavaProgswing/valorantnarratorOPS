package com.example.valnarratorgui;

import com.example.valnarratorbackend.ChatHandler;
import com.example.valnarratorbackend.CustomFormatter;
import com.example.valnarratorbackend.Message;
import com.example.valnarratorbackend.VoiceGenerator;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.jfoenix.controls.JFXToggleButton;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;

import java.awt.*;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

interface XMPPEventDispatcher {
    void dispatchError(XMPPError error) throws InterruptedException, ExecutionException, IOException;

    void dispatchEvent(XMPPEvent event);
}

interface InputCallback {
    void onInput(String line);
}

record XMPPError(String reason, int code) {
    public String toString() {
        return String.format("(%d) %s", code, reason);
    }
}

record XMPPEvent(String type, long time, String data) {
    public String toString() {
        return String.format("(%s) %s", type, data);
    }
}

public class ValNarratorController implements XMPPEventDispatcher {
    private static final CustomFormatter logger = new CustomFormatter(ValNarratorController.class);
    private static final Gson gson = new Gson();
    public static ValNarratorController latest_instance;
    private static String previousSelection = "Matthew, male";
    @FXML
    public TextField keybindTextField;
    @FXML
    public JFXToggleButton micButton;
    @FXML
    public JFXToggleButton privateChatButton;
    @FXML
    public JFXToggleButton teamChatButton;
    @FXML
    public ComboBox<String> voices, sources;
    @FXML
    public Label progressLoginLabel;
    @FXML
    public Label premiumDateLabel;
    @FXML
    public Label userIDLabel;
    @FXML
    public Label accountLabel;
    @FXML
    public Label quotaLabel;
    @FXML
    public Label messagesSentLabel;
    @FXML
    public Label charactersNarratedLabel;
    @FXML
    public Label windowTitle;
    @FXML
    public Label premiumWindowTitle;
    @FXML
    public Label keybindText;
    @FXML
    public Button subscribeButton;
    @FXML
    public ProgressBar quotaBar, progressLogin;
    @FXML
    public ImageView btnPower, btnInfo, btnUser, btnSettings;
    @FXML
    public AnchorPane panelLogin, panelUser, panelSettings, panelInfo, topBar;
    private boolean isLoading = true;
    private AnchorPane lastAnchorPane;

    private boolean isVoicesVisible = false;

    private boolean selectingKeybind = false;

    public ValNarratorController() {
        latest_instance = this;
    }

    private static ArrayList<String> getMBoardSerial() throws IOException {
        ArrayList<String> MBoardSerial = new ArrayList<>();
        Runtime runtime = Runtime.getRuntime();
        Process process;
        try {
            process = runtime.exec(new String[]{"wmic", "baseboard", "get", "serialnumber"});
        } catch (IOException ignored) {
            return MBoardSerial;
        }

        OutputStream os = process.getOutputStream();
        InputStream is = process.getInputStream();

        try (is) {
            try {
                os.close();
            } catch (IOException ignored) {
            }
            Scanner sc = new Scanner(is);
            sc.nextLine();
            while (sc.hasNext()) {
                String serial = sc.nextLine().trim().replace("\n", "");
                if (serial.isEmpty()) continue;
                MBoardSerial.add(serial);
            }
        }
        return MBoardSerial;
    }

    private static ArrayList<String> getDiskSerial() throws IOException {
        ArrayList<String> diskSerial = new ArrayList<>();
        Runtime runtime = Runtime.getRuntime();
        Process process;
        try {
            process = runtime.exec(new String[]{"wmic", "diskdrive", "get", "serialnumber"});
        } catch (IOException ignored) {
            return diskSerial;
        }

        OutputStream os = process.getOutputStream();
        InputStream is = process.getInputStream();

        try (is) {
            try {
                os.close();
            } catch (IOException ignored) {
            }
            Scanner sc = new Scanner(is);
            sc.nextLine();
            while (sc.hasNext()) {
                String serial = sc.nextLine().trim().replace("\n", "");
                if (serial.isEmpty()) continue;
                diskSerial.add(serial);
            }
        }
        return diskSerial;
    }

    private static ArrayList<String> getGPUSerial() throws IOException {
        ArrayList<String> gpuSerial = new ArrayList<>();
        Runtime runtime = Runtime.getRuntime();
        Process process;
        try {
            process = runtime.exec(new String[]{"wmic", "PATH", "Win32_VideoController", "GET", "PNPDeviceID"});
        } catch (IOException ignored) {
            return gpuSerial;
        }

        OutputStream os = process.getOutputStream();
        InputStream is = process.getInputStream();

        try (is) {
            try {
                os.close();
            } catch (IOException ignored) {
            }
            Scanner sc = new Scanner(is);
            sc.nextLine();
            while (sc.hasNext()) {
                String serial = sc.nextLine().trim().replace("\n", "");
                if (serial.isEmpty()) continue;
                gpuSerial.add(serial.substring(serial.indexOf('\\') + 1));
            }
        }
        return gpuSerial;
    }

    private static String getCPUSerial() throws IOException {
        String cpuSerial = null;
        Runtime runtime = Runtime.getRuntime();
        Process process;
        try {
            process = runtime.exec(new String[]{"wmic", "cpu", "get", "ProcessorId"});
        } catch (IOException ignored) {
            return null;
        }

        OutputStream os = process.getOutputStream();
        InputStream is = process.getInputStream();

        try (is) {
            try {
                os.close();
            } catch (IOException ignored) {
            }
            Scanner sc = new Scanner(is);
            while (sc.hasNext()) {
                String next = sc.next();
                if ("ProcessorId".equals(next)) {
                    cpuSerial = sc.next().trim();
                    break;
                }
            }
        }
        return cpuSerial;
    }

    public static String getSerialNumber() throws IOException {
        String cpuSerial = getCPUSerial();
        ArrayList<String> gpuSerial = getGPUSerial();
        ArrayList<String> hddSerial = getDiskSerial();
        ArrayList<String> mBoardSerial = getMBoardSerial();
        return String.valueOf(Objects.hash(cpuSerial, gpuSerial, hddSerial, mBoardSerial));
    }

    public boolean isLoading() {
        return isLoading;
    }

    public void initialize() {
        logger.info("Initializing application...");
        lastAnchorPane = panelUser;
        try {
            String fileLocation = "SoundVolumeView.exe";
            long pid = ProcessHandle.current().pid();
            String command = fileLocation + " /SetAppDefault \"CABLE Input\" all " + pid;
            Runtime.getRuntime().exec(command);
            command = fileLocation + " /SetPlaybackThroughDevice \"CABLE Output\" \"Default Playback Device\"";
            Runtime.getRuntime().exec(command);
            command = fileLocation + " /SetListenToThisDevice \"CABLE Output\" 1";
            Runtime.getRuntime().exec(command);
        } catch (IOException ignored) {
        }
        try {
            String command = "taskkill /F /IM RiotClientServices.exe";
            ProcessBuilder processBuilder = new ProcessBuilder(command.split("\\s+"));
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            int code = process.waitFor();
            if (code == 0) {
                logger.info("Closed riot client!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            String command = "taskkill /F /IM VALORANT-Win64-Shipping.exe";
            ProcessBuilder processBuilder = new ProcessBuilder(command.split("\\s+"));
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            int code = process.waitFor();
            if (code == 0) {
                logger.info("Closed valorant!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            String command = "taskkill /F /IM node.exe";
            ProcessBuilder processBuilder = new ProcessBuilder(command.split("\\s+"));
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            int code = process.waitFor();
            if (code == 0) {
                logger.info("Closed node!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            String appPath = System.getProperty("user.dir");

            String nodeScriptPath = appPath + "/xmpp";

            ProcessBuilder processBuilder = new ProcessBuilder("node", ".");
            processBuilder.directory(Paths.get(nodeScriptPath).toFile());
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            new Thread(() -> {
                final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                final IncomingBufferHandler incomingBuffer = new IncomingBufferHandler();
                try {
                    while ((line = reader.readLine()) != null) {
                        try {
                            JsonObject json = gson.fromJson(line, JsonObject.class);
                            if (json.has("error")) {
                                ValNarratorController.latest_instance.dispatchError(new XMPPError(json.get("reason").getAsString(), json.get("code").getAsInt()));
                            } else {
                                XMPPEvent event = new XMPPEvent(json.get("type").getAsString(), json.get("time").getAsLong(), json.get("data").getAsString());
                                if (event.type().equals("incoming")) {
                                    final String xml = event.data();
                                    Pattern idPattern = Pattern.compile("id='(.*?)'");
                                    Matcher idMatcher = idPattern.matcher(xml.replace("\"", "'"));
                                    String id = idMatcher.find() ? idMatcher.group(1) : null;
                                    Pattern jidPattern = Pattern.compile("<jid>(.*?)</jid>");
                                    Matcher jidMatcher = jidPattern.matcher(xml);
                                    String jid = jidMatcher.find() ? jidMatcher.group(1).split("@")[0] : null;
                                    if (id != null && id.equals("_xmpp_bind1")) {
                                        ChatHandler.setSelfID(jid);
                                    }
                                    for (Character ch : xml.toCharArray())
                                        incomingBuffer.append(ch);
                                    ValNarratorController.latest_instance.dispatchEvent(event);
                                } else {
                                    final String xml = event.data();
                                    Pattern idPattern = Pattern.compile("id='(.*?)'");
                                    Matcher idMatcher = idPattern.matcher(xml.replace("\"", "'"));
                                    String id = idMatcher.find() ? idMatcher.group(1) : null;
                                    if (id != null && id.startsWith("join_muc_")) {
                                        Pattern toPattern = Pattern.compile("to='(.*?)'");
                                        Matcher toMatcher = toPattern.matcher(xml);
                                        String to = toMatcher.find() ? toMatcher.group(1) : "";
                                        String type = to.split("@")[1].split("\\.")[0];
                                        if (type.equals("ares-parties")) {
                                            ChatHandler.setPartyID(to.split("/")[0]);
                                        }
                                    }
                                }
                            }
                        } catch (JsonSyntaxException | NullPointerException | InterruptedException |
                                 ExecutionException | IOException ignored) {
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
            logger.info("Node started!");
        } catch (IOException e) {
            e.printStackTrace();
        }
        CompletableFuture.runAsync(() -> {
            while (isLoading) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (ValNarratorController.latest_instance.progressLogin.getProgress() >= 1)
                    Platform.runLater(() -> ValNarratorController.latest_instance.progressLogin.setProgress(0));
                Platform.runLater(() -> ValNarratorController.latest_instance.progressLogin.setProgress(ValNarratorController.latest_instance.progressLogin.getProgress() + 0.025));
            }
            Platform.runLater(() -> {
                ValNarratorController.latest_instance.progressLogin.setProgress(1);
                ValNarratorController.latest_instance.panelInfo.setVisible(false);
                ValNarratorController.latest_instance.panelSettings.setVisible(false);
                ValNarratorController.latest_instance.panelLogin.setVisible(false);
                ValNarratorController.latest_instance.panelUser.setVisible(true);
            });
            new ChatHandler();
            VoiceGenerator.generateSingleton();
        });
    }

    public void showValorantSettingsDemo() {
        final Alert newAlert = new Alert(Alert.AlertType.INFORMATION);
        newAlert.setHeaderText("Toggle the settings highlighted in red.");
        newAlert.getDialogPane().setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        ImageView imageView = new ImageView(System.getProperty("user.dir") + "//tutorial.png");
        newAlert.setGraphic(imageView);
        newAlert.show();
    }

    public void keybindChange(javafx.scene.input.KeyEvent event) {
        if (!selectingKeybind) return;

        KeyCode keyCode = event.getCode();
        String keyText = keyCode.getName();

        if (keyText != null && !keyText.isEmpty()) {
            Platform.runLater(() -> keybindTextField.setText(keyText));
        }

        VoiceGenerator.getInstance().setKeyEvent(keyCode.getCode());
        Platform.runLater(() -> keybindText.setText("Pick a keybind for team mic, currently set to " + keyText));
    }

    public void browseSubscription() throws IOException {
        logger.info("Opening subscription page...");
        HttpClient client = HttpClient.newHttpClient();
        String url = Main.getProperties().getProperty("paymentLink");
        String jsonPayload = String.format("{\n" + "  \"plan_id\": \"%s\",\n" + "  \"quantity\": \"1\",\n" + "  \"custom_id\": \"" + getSerialNumber() + "\",\n" + "   \"application_context\": {\n" + "     \"shipping_preference\": \"NO_SHIPPING\",\n" + "     \"return_url\": \"%s/payment_return\",\n" + "     \"cancel_url\": \"%s/payment_cancel\"\n" + "   }\n" + "}", Main.getProperties().getProperty("paymentPlanID"), url, url);

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).POST(HttpRequest.BodyPublishers.ofString(jsonPayload)).setHeader("content-type", "application/json").build();

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String subscriptionURL = response.headers().firstValue("subscriptionURL").get();
                java.awt.Desktop.getDesktop().browse(java.net.URI.create(subscriptionURL));
            } else {
                logger.error("Premium Request failed with code: " + response.statusCode());
                logger.error(response.body());
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setMessagesSent(long number) {
        messagesSentLabel.setText(Long.toString(number));
    }

    public void setCharactersNarrated(long number) {
        charactersNarratedLabel.setText(Long.toString(number));
    }

    public void setAccountLabel(String accountName) {
        accountLabel.setText(accountName);
    }

    public void setPremiumDateLabel(String date) {
        premiumDateLabel.setText(date);
    }

    public void setUserIDLabel(String userID) {
        userIDLabel.setText(userID);
    }

    public void showVoices() {
        isVoicesVisible = true;
    }

    public void hideVoices() {
        isVoicesVisible = false;
    }

    public boolean getVoicesVisibility() {
        return isVoicesVisible;
    }

    public void enterKeybind() {
        selectingKeybind = true;
    }

    public void exitKeybind() {
        selectingKeybind = false;
    }

    public boolean isSelectingKeybind() {
        return selectingKeybind;
    }

    public void handleButtonAction(MouseEvent event) {
        if (isLoading) {
            return;
        }
        if (event.getTarget() == btnPower) {
            if (ChatHandler.toggleState()) {
                if (panelInfo.isVisible()) lastAnchorPane = panelInfo;
                else if (panelUser.isVisible()) lastAnchorPane = panelUser;
                else if (panelSettings.isVisible()) lastAnchorPane = panelSettings;
                panelInfo.setVisible(false);
                panelSettings.setVisible(false);
                panelUser.setVisible(false);
            } else {
                lastAnchorPane.setVisible(true);
            }
        }

        if (event.getTarget() == btnInfo) {
            panelInfo.setVisible(true);
            panelSettings.setVisible(false);
            panelUser.setVisible(false);
        } else if (event.getTarget() == btnUser) {
            panelInfo.setVisible(false);
            panelSettings.setVisible(false);
            panelUser.setVisible(true);
        } else if (event.getTarget() == btnSettings) {
            panelInfo.setVisible(false);
            panelSettings.setVisible(true);
            panelUser.setVisible(false);
        }
    }

    public void selectSource() {
        final String rawSource = sources.getValue();
        switch (rawSource) {
            case "SELF" -> {
                ChatHandler.setSelfEnabled();
                ChatHandler.setPartyDisabled();
                ChatHandler.setTeamDisabled();
                ChatHandler.setAllDisabled();
                logger.info("Toggled Self Narration ON");
            }
            case "PARTY" -> {
                ChatHandler.setSelfDisabled();
                ChatHandler.setPartyEnabled();
                ChatHandler.setTeamDisabled();
                ChatHandler.setAllDisabled();
                logger.info("Toggled Party Narration ON");
            }
            case "TEAM" -> {
                ChatHandler.setSelfDisabled();
                ChatHandler.setPartyDisabled();
                ChatHandler.setTeamEnabled();
                ChatHandler.setAllDisabled();
                logger.info("Toggled Team Narration ON");
            }
            case "SELF+PARTY" -> {
                ChatHandler.setSelfEnabled();
                ChatHandler.setPartyEnabled();
                ChatHandler.setTeamDisabled();
                ChatHandler.setAllDisabled();
                logger.info("Toggled Self and Party Narration ON");
            }
            case "SELF+TEAM" -> {
                ChatHandler.setSelfEnabled();
                ChatHandler.setTeamEnabled();
                ChatHandler.setPartyDisabled();
                ChatHandler.setAllDisabled();
                logger.info("Toggled Self and Team Narration ON");
            }
            case "PARTY+TEAM" -> {
                ChatHandler.setPartyEnabled();
                ChatHandler.setTeamEnabled();
                ChatHandler.setAllDisabled();
                ChatHandler.setSelfEnabled();
                logger.info("Toggled Party and Team Narration ON");
            }
            case "PARTY+TEAM+ALL" -> {
                ChatHandler.setPartyEnabled();
                ChatHandler.setTeamEnabled();
                ChatHandler.setAllEnabled();
                ChatHandler.setSelfDisabled();
                logger.info("Toggled Party, Team and All Narration ON");
            }
            case "SELF+PARTY+TEAM" -> {
                ChatHandler.setPartyEnabled();
                ChatHandler.setTeamEnabled();
                ChatHandler.setAllDisabled();
                ChatHandler.setSelfEnabled();
                logger.info("Toggled Self, Party and Team Narration ON");
            }
            case "SELF+PARTY+ALL" -> {
                ChatHandler.setPartyEnabled();
                ChatHandler.setTeamDisabled();
                ChatHandler.setAllEnabled();
                ChatHandler.setSelfEnabled();
                logger.info("Toggled Self, Party and Team Narration ON");
            }
            case "SELF+PARTY+TEAM+ALL" -> {
                ChatHandler.setPartyEnabled();
                ChatHandler.setTeamEnabled();
                ChatHandler.setAllEnabled();
                ChatHandler.setSelfEnabled();
                logger.info("Toggled Self, Party, Team and All Narration ON");
            }
        }
    }

    public void selectVoice() {
        final String rawVoiceId = voices.getValue();
        boolean isValorant = rawVoiceId.contains("VALORANT");
        final String voiceId = rawVoiceId.substring(0, rawVoiceId.indexOf(","));
        if (isValorant) {
            if (!ChatHandler.isPremium) {
                voices.getSelectionModel().select(previousSelection);
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setHeaderText("Premium Required");
                alert.setContentText("Valorant voices are available with premium, please subscribe to premium to continue using this voice! You can subscribe in the info tab.");
                alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                Toolkit.getDefaultToolkit().beep();
                alert.show();
                return;
            }
            previousSelection = rawVoiceId;
            VoiceGenerator.setCurrentVoice(voiceId);
            VoiceGenerator.setValorantVoice();
            logger.info("(VALORANT)Set voice to: " + voiceId);
        } else {
            previousSelection = rawVoiceId;
            VoiceGenerator.setCurrentVoice(voiceId);
            VoiceGenerator.setNormalVoice();
            logger.info("Set voice to: " + voiceId);
        }
    }

    public void toggleTeamChat() {
        VoiceGenerator.getInstance().toggleTeamKey();
    }

    public void togglePrivateMessages() {
        if (privateChatButton.isSelected()) {
            ChatHandler.setPrivateEnabled();
            logger.info("Toggled Private Messages ON");
        } else {
            ChatHandler.setPrivateDisabled();
            logger.info("Toggled Private Messages OFF");
        }
    }

    public void toggleMic() {
        if (micButton.isSelected()) {
            try {
                String fileLocation = "SoundVolumeView.exe";
                String command = fileLocation + " /SetPlaybackThroughDevice \"DefaultCaptureDevice\" \"CABLE Input\"";
                Runtime.getRuntime().exec(command);
                command = fileLocation + " /SetListenToThisDevice \"DefaultCaptureDevice\" 1";
                Runtime.getRuntime().exec(command);
            } catch (IOException ignored) {
            }
            logger.info("Toggled Mic ON");

        } else {
            try {
                String fileLocation = "SoundVolumeView.exe";
                String command = fileLocation + " /SetPlaybackThroughDevice \"DefaultCaptureDevice\" \"CABLE Input\"";
                Runtime.getRuntime().exec(command);
                command = fileLocation + " /SetListenToThisDevice \"DefaultCaptureDevice\" 0";
                Runtime.getRuntime().exec(command);
            } catch (IOException ignored) {
            }
            logger.info("Toggled Mic OFF");
        }
    }

    @Override
    public void dispatchError(XMPPError error) throws InterruptedException, ExecutionException, IOException {
        logger.error(String.valueOf(error));
    }

    @Override
    public void dispatchEvent(XMPPEvent event) {
        if (event.data().contains("<?xml version='1.0'?>")) {
            Platform.runLater(() -> ValNarratorController.latest_instance.progressLoginLabel.setText("Riot client opened."));
        } else if (event.data().contains("_xmpp_session1")) {
            Platform.runLater(() -> ValNarratorController.latest_instance.progressLoginLabel.setText("Valorant opened."));
            isLoading = false;
        }
    }

}

class IncomingBufferHandler extends XMLBuffer implements InputCallback {
    private static final CustomFormatter logger = new CustomFormatter(IncomingBufferHandler.class);

    @Override
    public void onInput(String line) {
        if (!ValNarratorController.latest_instance.isLoading() && ChatHandler.getState()) {
            return;
        }

        if (line.startsWith("<message")) {
            Message msg = new Message(line);
            ChatHandler.message(msg);
        }
    }

    public void append(int character) {
        if (bufferIndex == buffer.length) {
            buffer = Arrays.copyOf(buffer, buffer.length * 2);
        }
        buffer[bufferIndex++] = (byte) character;

        if (character == '>') check();
    }

    void check() {
        String buffered = new String(buffer, 0, bufferIndex, StandardCharsets.UTF_8);
        if (buffered.startsWith("</")) {
            reset();
        } else if (buffered.startsWith("<?")) {
            if (breakpoint == null && buffered.endsWith("?>")) {
                int first = buffered.indexOf('?');
                int second = buffered.indexOf('?', first + 1);
                breakpoint = buffered.substring(first, second).split(" ")[1] + ">";
            }
            if (breakpoint != null) {
                if (buffered.endsWith(breakpoint)) {
                    dispatch(buffered);
                }
            }
        } else if (buffered.startsWith("<")) {
            if (breakpoint == null && buffered.endsWith("/>")) {
                breakpoint = "/>";
            } else if (breakpoint == null && buffered.endsWith(">")) {
                int spaceIndex = buffered.indexOf(' ');
                if (spaceIndex == -1) {
                    int first = buffered.indexOf('>');
                    breakpoint = "</" + buffered.substring(1, first) + ">";
                } else {
                    breakpoint = "</" + buffered.substring(1, spaceIndex) + ">";
                }
            }
            if (breakpoint != null) {
                if (buffered.endsWith(breakpoint)) {
                    dispatch(buffered);
                }
            }
        }
    }

    void dispatch(String line) {
        onInput(line);
        reset();
    }

    void reset() {
        this.bufferIndex = 0;
        this.breakpoint = null;
        this.buffer = new byte[1024];
    }
}


abstract class XMLBuffer {
    byte[] buffer = new byte[1024];
    int bufferIndex = 0;
    String breakpoint;

    abstract void append(int character);

    abstract void check();

    abstract void dispatch(String line);

    abstract void reset();

}