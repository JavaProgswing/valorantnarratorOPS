package com.jprcoder.valnarratorgui;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.jfoenix.controls.JFXToggleButton;
import com.jprcoder.valnarratorbackend.ChatDataHandler;
import com.jprcoder.valnarratorbackend.Message;
import com.jprcoder.valnarratorbackend.MessageQuota;
import com.jprcoder.valnarratorbackend.VoiceGenerator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.jprcoder.valnarratorgui.ValNarratorApplication.showAlert;

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
    private static final Logger logger = LoggerFactory.getLogger(ValNarratorController.class);
    private static ValNarratorController latestInstance;
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
    private String previousSelection = "Matthew, male";
    private boolean isLoading = true;
    private AnchorPane lastAnchorPane;

    private boolean isVoicesVisible = false;

    private boolean selectingKeybind = false;

    public ValNarratorController() {
        latestInstance = this;
    }

    public static ValNarratorController getLatestInstance() {
        return latestInstance;
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
                            Gson gson = new Gson();
                            JsonObject json = gson.fromJson(line, JsonObject.class);
                            if (json.has("error")) {
                                ValNarratorController.getLatestInstance().dispatchError(new XMPPError(json.get("reason").getAsString(), json.get("code").getAsInt()));
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
                                        ChatDataHandler.getInstance().getProperties().setSelfID(jid);
                                    }
                                    for (Character ch : xml.toCharArray())
                                        incomingBuffer.append(ch);
                                    ValNarratorController.getLatestInstance().dispatchEvent(event);
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
                if (ValNarratorController.getLatestInstance().progressLogin.getProgress() >= 1)
                    Platform.runLater(() -> ValNarratorController.getLatestInstance().progressLogin.setProgress(0));
                Platform.runLater(() -> ValNarratorController.getLatestInstance().progressLogin.setProgress(ValNarratorController.getLatestInstance().progressLogin.getProgress() + 0.025));
            }
            Platform.runLater(() -> {
                ValNarratorController.getLatestInstance().progressLogin.setProgress(1);
                ValNarratorController.getLatestInstance().panelInfo.setVisible(false);
                ValNarratorController.getLatestInstance().panelSettings.setVisible(false);
                ValNarratorController.getLatestInstance().panelLogin.setVisible(false);
                ValNarratorController.getLatestInstance().panelUser.setVisible(true);
            });
            ChatDataHandler.generateSingleton();
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

    public void keybindChange(javafx.scene.input.KeyEvent event) throws IOException {
        if (!selectingKeybind) return;

        KeyCode keyCode = event.getCode();
        String keyText = keyCode.getName();

        if (keyText != null && !keyText.isEmpty()) {
            Platform.runLater(() -> keybindTextField.setText(keyText));
        }

        VoiceGenerator.getInstance().setKeyEvent(keyCode.getCode());
        Platform.runLater(() -> keybindText.setText("Pick a keybind for team mic, currently set to " + keyText));
    }

    public void revertVoiceSelection() {
        voices.getSelectionModel().select(previousSelection);
    }

    public void updateRequestQuota(MessageQuota mq) {
        if (mq.remainingQuota() <= 0) {
            Platform.runLater(() -> {
                ValNarratorController.getLatestInstance().quotaLabel.setText("Quota Exhausted!");
                ValNarratorController.getLatestInstance().quotaLabel.setTextFill(Color.RED);
                ValNarratorController.getLatestInstance().quotaBar.setProgress(0.0);
                ValNarratorController.getLatestInstance().setPremiumDateLabel("No");
            });
        } else {
            if (mq.isPremium()) {
                Platform.runLater(() -> {
                    ValNarratorController.getLatestInstance().windowTitle.setVisible(false);
                    ValNarratorController.getLatestInstance().premiumWindowTitle.setVisible(true);
                    ValNarratorController.getLatestInstance().subscribeButton.setVisible(false);
                    ValNarratorController.getLatestInstance().setPremiumDateLabel(String.format("Valid till %s", mq.premiumTill()));
                });
            } else {
                Platform.runLater(() -> {
                    ValNarratorController.getLatestInstance().quotaLabel.setText(mq.remainingQuota() + "/" + ChatDataHandler.getInstance().getProperties().getQuotaLimit());
                    ValNarratorController.getLatestInstance().quotaBar.setProgress((double) mq.remainingQuota() / ChatDataHandler.getInstance().getProperties().getQuotaLimit());
                    ValNarratorController.getLatestInstance().setPremiumDateLabel("No");
                });
            }
        }
    }

    public void browseSubscription() throws IOException, InterruptedException {
        logger.info("Opening subscription page...");
        String subscriptionURL = ChatDataHandler.getInstance().getAPIHandler().getSubscriptionURL();
        if (subscriptionURL != null) java.awt.Desktop.getDesktop().browse(java.net.URI.create(subscriptionURL));
        else showAlert("Error!", "Failed to open subscription page, please try again later.");
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
            if (ChatDataHandler.getInstance().getProperties().toggleState()) {
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
                ChatDataHandler.getInstance().getProperties().setSelfEnabled();
                ChatDataHandler.getInstance().getProperties().setPartyDisabled();
                ChatDataHandler.getInstance().getProperties().setTeamDisabled();
                ChatDataHandler.getInstance().getProperties().setAllDisabled();
                logger.info("Toggled Self Narration ON");
            }
            case "PARTY" -> {
                ChatDataHandler.getInstance().getProperties().setSelfDisabled();
                ChatDataHandler.getInstance().getProperties().setPartyEnabled();
                ChatDataHandler.getInstance().getProperties().setTeamDisabled();
                ChatDataHandler.getInstance().getProperties().setAllDisabled();
                logger.info("Toggled Party Narration ON");
            }
            case "TEAM" -> {
                ChatDataHandler.getInstance().getProperties().setSelfDisabled();
                ChatDataHandler.getInstance().getProperties().setPartyDisabled();
                ChatDataHandler.getInstance().getProperties().setTeamEnabled();
                ChatDataHandler.getInstance().getProperties().setAllDisabled();
                logger.info("Toggled Team Narration ON");
            }
            case "SELF+PARTY" -> {
                ChatDataHandler.getInstance().getProperties().setSelfEnabled();
                ChatDataHandler.getInstance().getProperties().setPartyEnabled();
                ChatDataHandler.getInstance().getProperties().setTeamDisabled();
                ChatDataHandler.getInstance().getProperties().setAllDisabled();
                logger.info("Toggled Self and Party Narration ON");
            }
            case "SELF+TEAM" -> {
                ChatDataHandler.getInstance().getProperties().setSelfEnabled();
                ChatDataHandler.getInstance().getProperties().setTeamEnabled();
                ChatDataHandler.getInstance().getProperties().setPartyDisabled();
                ChatDataHandler.getInstance().getProperties().setAllDisabled();
                logger.info("Toggled Self and Team Narration ON");
            }
            case "PARTY+TEAM" -> {
                ChatDataHandler.getInstance().getProperties().setPartyEnabled();
                ChatDataHandler.getInstance().getProperties().setTeamEnabled();
                ChatDataHandler.getInstance().getProperties().setAllDisabled();
                ChatDataHandler.getInstance().getProperties().setSelfEnabled();
                logger.info("Toggled Party and Team Narration ON");
            }
            case "PARTY+TEAM+ALL" -> {
                ChatDataHandler.getInstance().getProperties().setPartyEnabled();
                ChatDataHandler.getInstance().getProperties().setTeamEnabled();
                ChatDataHandler.getInstance().getProperties().setAllEnabled();
                ChatDataHandler.getInstance().getProperties().setSelfDisabled();
                logger.info("Toggled Party, Team and All Narration ON");
            }
            case "SELF+PARTY+TEAM" -> {
                ChatDataHandler.getInstance().getProperties().setPartyEnabled();
                ChatDataHandler.getInstance().getProperties().setTeamEnabled();
                ChatDataHandler.getInstance().getProperties().setAllDisabled();
                ChatDataHandler.getInstance().getProperties().setSelfEnabled();
                logger.info("Toggled Self, Party and Team Narration ON");
            }
            case "SELF+PARTY+ALL" -> {
                ChatDataHandler.getInstance().getProperties().setPartyEnabled();
                ChatDataHandler.getInstance().getProperties().setTeamDisabled();
                ChatDataHandler.getInstance().getProperties().setAllEnabled();
                ChatDataHandler.getInstance().getProperties().setSelfEnabled();
                logger.info("Toggled Self, Party and Team Narration ON");
            }
            case "SELF+PARTY+TEAM+ALL" -> {
                ChatDataHandler.getInstance().getProperties().setPartyEnabled();
                ChatDataHandler.getInstance().getProperties().setTeamEnabled();
                ChatDataHandler.getInstance().getProperties().setAllEnabled();
                ChatDataHandler.getInstance().getProperties().setSelfEnabled();
                logger.info("Toggled Self, Party, Team and All Narration ON");
            }
        }
    }

    public void selectVoice() {
        final String rawVoiceId = voices.getValue();
        boolean isValorant = rawVoiceId.contains("VALORANT");
        final String voiceId = rawVoiceId.substring(0, rawVoiceId.indexOf(","));
        if (isValorant) {
            if (!ChatDataHandler.getInstance().isPremium()) {
                revertVoiceSelection();
                showAlert("Premium Required", "Valorant voices are available with premium, please subscribe to premium to continue using this voice! You can subscribe in the info tab.");
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

    public void toggleTeamChat() throws IOException {
        VoiceGenerator.getInstance().toggleTeamKey();
    }

    public void togglePrivateMessages() {
        if (privateChatButton.isSelected()) {
            ChatDataHandler.getInstance().getProperties().setPrivateEnabled();
            logger.info("Toggled Private Messages ON");
        } else {
            ChatDataHandler.getInstance().getProperties().setPrivateDisabled();
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
            Platform.runLater(() -> ValNarratorController.getLatestInstance().progressLoginLabel.setText("Riot client opened."));
        } else if (event.data().contains("_xmpp_session1")) {
            Platform.runLater(() -> ValNarratorController.getLatestInstance().progressLoginLabel.setText("Valorant opened."));
            isLoading = false;
        }
    }

}

class IncomingBufferHandler extends XMLBuffer implements InputCallback {
    @Override
    public void onInput(String line) {
        if (!ValNarratorController.getLatestInstance().isLoading() && ChatDataHandler.getInstance().getProperties().getState()) {
            return;
        }

        if (line.startsWith("<message")) {
            Message msg = new Message(line);
            ChatDataHandler.getInstance().message(msg);
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