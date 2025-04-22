package com.jprcoder.valnarratorgui;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.jfoenix.controls.JFXToggleButton;
import com.jprcoder.valnarratorbackend.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;

import static com.jprcoder.valnarratorbackend.RiotUtilityHandler.isValorantRunning;
import static com.jprcoder.valnarratorgui.ValNarratorApplication.*;

interface XMPPEventDispatcher {
    void dispatchError(XMPPError error) throws InterruptedException, ExecutionException, IOException;

    void dispatchEvent(XMPPEvent event);
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

//TODO: Seperate a new class named ValNarratorProperties for utility/property methods.
public class ValNarratorController implements XMPPEventDispatcher {
    private static final Logger logger = LoggerFactory.getLogger(ValNarratorController.class);
    private static ValNarratorController latestInstance;
    @FXML
    public Slider rateSlider;
    @FXML
    public TextField keybindTextField;
    @FXML
    public JFXToggleButton micButton;
    @FXML
    public JFXToggleButton valorantSettings;
    @FXML
    public JFXToggleButton privateChatButton;
    @FXML
    public JFXToggleButton teamChatButton;
    @FXML
    public ComboBox<String> voices, sources, addIgnoredPlayer, removeIgnoredPlayer;
    @FXML
    public Label progressLoginLabel;
    @FXML
    public Label premiumDateLabel;
    @FXML
    public Label userIDLabel;
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
    public Button voiceSettingsSync;
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
        logger.info("Initializing app.");
        lastAnchorPane = panelUser;
        long start = System.currentTimeMillis(), appStart = System.currentTimeMillis();
        try {
            String command = "taskkill /F /IM valorantNarrator-xmpp.exe";
            ProcessBuilder processBuilder = new ProcessBuilder(command.split("\\s+"));
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            int code = process.waitFor();
            if (code == 0) {
                logger.info("Forcefully closed XMPP!");
            }
        } catch (Exception e) {
            logger.error(String.format("Killing XMPP generated an error: %s", (Object) e.getStackTrace()));
        }

        try {
            String command = "taskkill /F /IM RiotClientServices.exe";
            ProcessBuilder processBuilder = new ProcessBuilder(command.split("\\s+"));
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            int code = process.waitFor();
            if (code == 0) {
                logger.info("Forcefully closed riot client!");
            }
        } catch (Exception e) {
            logger.error(String.format("Killing Riot-Client generated an error: %s", (Object) e.getStackTrace()));
        }

        try {
            String command = "taskkill /F /IM VALORANT-Win64-Shipping.exe";
            ProcessBuilder processBuilder = new ProcessBuilder(command.split("\\s+"));
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            int code = process.waitFor();
            if (code == 0) {
                logger.info("Forcefully closed valorant!");
            }
        } catch (Exception e) {
            logger.error(String.format("Killing Valorant generated an error: %s", (Object) e.getStackTrace()));
        }

        logger.info(String.format("Closed XMPP, riot-client, valorant in %d ms.", (System.currentTimeMillis() - start)));
        try {
            String fileLocation = String.format("%s/ValorantNarrator/SoundVolumeView.exe", System.getenv("ProgramFiles").replace("\\", "/"));
            long pid = ProcessHandle.current().pid();
            String command = fileLocation + " /SetAppDefault \"CABLE Input\" all " + pid;
            start = System.currentTimeMillis();
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
        logger.info("Initialized app's sound output.");
        logger.info("Initializing xmpp-node.");
        try {
            final String xmppPath = String.format("%s/ValorantNarrator/valorantNarrator-xmpp.exe", System.getenv("ProgramFiles").replace("\\", "/"));
            ProcessBuilder processBuilder = new ProcessBuilder(xmppPath);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            BufferedWriter processWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

            CompletableFuture.runAsync(() -> {
                try (ServerSocket serverSocket = new ServerSocket(35480)) {
                    logger.info("Listening on localhost:35480 for input to Xmpp-Node");
                    while (true) {
                        Socket clientSocket = serverSocket.accept();
                        BufferedReader socketReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        String line;
                        while ((line = socketReader.readLine()) != null) {
                            line = line.replace(">", "").replace("<", "");
                            logger.debug("Received from socket: " + line);

                            final String xml = ChatDataHandler.getInstance().getSentMessageXML(line);
                            logger.debug("Sending to xmpp-node: " + xml);
                            processWriter.write(xml);
                            processWriter.newLine();
                            processWriter.flush();
                        }
                        clientSocket.close();
                    }
                } catch (IOException e) {
                    logger.error("Socket server failed: ", e);
                }
            });
            CompletableFuture.runAsync(() -> {
                final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while (true) {
                    try {
                        if ((line = reader.readLine()) == null) break;
                    } catch (IOException e) {
                        logger.error(String.format("Reading Xmpp-Node's output generated an error: %s", (Object) e.getStackTrace()));
                        throw new RuntimeException(e);
                    }

                    try {
                        Gson gson = new Gson();
                        JsonObject json = gson.fromJson(line, JsonObject.class);
                        if (json.get("type").getAsString().equals("error")) {
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
                                if (ChatDataHandler.getInstance().getProperties().getSelfID() == null && id != null && id.equals("_xmpp_bind1")) {
                                    logger.debug(String.format("Set Self ID to %s", jid));
                                    ChatDataHandler.getInstance().getProperties().setSelfID(jid);
                                }
                                ValNarratorController.getLatestInstance().dispatchEvent(event);
                                if (!ValNarratorController.getLatestInstance().isLoading() && ChatDataHandler.getInstance().getProperties().isDisabled()) {
                                    continue;
                                }

                                if (xml.startsWith("<message")) {
                                    Message msg = new Message(xml);
                                    logger.info(String.format("Received message: %s", msg));
                                    ChatDataHandler.getInstance().message(msg);
                                    //<message id="1745268795434:1" to="e0da59fd-266d-4022-9dab-16e189dda809@ares-parties.jp1.pvp.net" type="groupchat"><body>yo teaaaam</body></message>
                                    //<message id="1745268831981:2" to="e0da59fd-266d-4022-9dab-16e189dda809@ares-parties.jp1.pvp.net" type="groupchat"><body>yo teaaaam</body></message>
                                }
                            } else {
                                final String xml = event.data();
                                if (xml.startsWith("<message")) {
                                    logger.debug(String.format("Sent: %s", xml));
                                    ChatDataHandler.getInstance().getProperties().incrementSelfMessagesSent();
                                }

                                if (xml.startsWith("<presence")) {
                                    logger.debug(String.format("Sent presence: %s", xml));

                                    Pattern idPattern = Pattern.compile("id='(.*?)'");
                                    Matcher idMatcher = idPattern.matcher(xml.replace("\"", "'"));
                                    String id = idMatcher.find() ? idMatcher.group(1) : null;

                                    Pattern toPattern = Pattern.compile("to='(.*?)'");
                                    Matcher toMatcher = toPattern.matcher(xml.replace("\"", "'"));
                                    String to = toMatcher.find() ? toMatcher.group(1) : null;
                                    if (id != null && id.startsWith("join_muc_") && to != null) {
                                        ChatDataHandler.getInstance().getProperties().setMucID(to.substring(0, to.indexOf('/')));
                                    }
                                }

                                if (event.type().equals("close-riot") || event.type().equals("close-valorant")) {
                                    ValNarratorApplication.showAlert("Warning", "Valorant or Riot client has been closed, application will close in 5 seconds.");
                                    Thread.sleep(5000);
                                    System.exit(0);
                                }
                            }

                        }
                    } catch (JsonSyntaxException | NullPointerException | InterruptedException | ExecutionException |
                             IOException ignored) {
                    }
                }
            });
        } catch (IOException e) {
            logger.error("Running Xmpp-Node generated an error: ");
            e.printStackTrace();
        }
        logger.info(String.format("Initialization completed in %d ms.", System.currentTimeMillis() - appStart));

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
            new VoiceTokenHandler(ChatDataHandler.getInstance().getAPIHandler()).startRefreshToken();
            VoiceGenerator.generateSingleton();
            Platform.runLater(() -> {
                ArrayList<String> inbuiltVoiceNames = new ArrayList<>();
                VoiceGenerator.getInbuiltVoices().forEach((n) -> inbuiltVoiceNames.add(String.format("%s, INBUILT", n)));
                ValNarratorController.getLatestInstance().voices.getItems().addAll(inbuiltVoiceNames);
            });
            try {
                if (VoiceGenerator.isSyncValorantSettingsEnabled())
                    VoiceGenerator.getInstance().syncValorantPlayerSettings();
            } catch (IOException | DataFormatException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        rateSlider.valueProperty().addListener((observable, oldValue, newValue) -> VoiceGenerator.setCurrentRate(newValue.shortValue()));
    }

    public void disableRateSlider() {
        rateSlider.setDisable(true);
    }

    public void enableRateSlider() {
        rateSlider.setDisable(false);
    }

    public void openDiscordInvite() {
        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create("https://valnarrator.vercel.app/discord"));
            logger.info("Opened discord invite link successfully!");
        } catch (IOException e) {
            logger.error("Could not open discord invite link!");
            showAlert("Error!", "Failed to open discord invite link, please try again later.");
        }
    }

    public void ignorePlayer() {
        final String player = addIgnoredPlayer.getValue();
        if (player != null && player.equals("Add RiotId#RiotTag")) return;
        logger.info(String.format("Ignoring player %s", player));
        ChatDataHandler.getInstance().getProperties().addIgnoredPlayer(player);
        addIgnoredPlayer.getItems().remove(player);
        removeIgnoredPlayer.getItems().add(player);
        addIgnoredPlayer.getSelectionModel().select("Add RiotId#RiotTag");
    }

    public void unignorePlayer() {
        final String player = removeIgnoredPlayer.getValue();
        if (player != null && player.equals("View/Remove RiotID#RiotTag")) return;
        logger.info(String.format("Unignoring player %s", player));
        ChatDataHandler.getInstance().getProperties().removeIgnoredPlayer(player);
        removeIgnoredPlayer.getItems().remove(player);
        addIgnoredPlayer.getItems().add(player);
        removeIgnoredPlayer.getSelectionModel().select("View/Remove RiotID#RiotTag");
    }

    public void syncValorantSettingsToggle() throws IOException {
        if (VoiceGenerator.getInstance().syncValorantSettingsToggle()) {
            Platform.runLater(() -> showInformation("Voice Settings SYNC", "Enabled, Valorant settings will be synced at next start-up!"));
        } else {
            Platform.runLater(() -> showInformation("Voice Settings SYNC", "Disabled, Valorant settings will not be synced at next start-up!"));
        }
    }

    public void syncValorantSettings() {
        try {
            VoiceGenerator.getInstance().syncValorantPlayerSettings();
            showInformation("Voice Settings SYNC", "Valorant settings synced successfully!");
        } catch (IOException | DataFormatException | InterruptedException e) {
            logger.error("Failed to sync valorant settings!");
            showAlert("Error!", "Failed to sync valorant settings, please try again later.");
        }
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

    public void markQuotaExhausted() {
        CompletableFuture.runAsync(() -> {
            while (ChatDataHandler.getInstance() == null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            ChatDataHandler.getInstance().getProperties().markQuotaExhausted();
        });
        Platform.runLater(() -> {
            ValNarratorController.getLatestInstance().quotaLabel.setText("Quota Exhausted!");
            ValNarratorController.getLatestInstance().quotaBar.setProgress(0.0);
            ValNarratorController.getLatestInstance().setPremiumDateLabel("No");
            try {
                voices.getSelectionModel().select(String.format("%s, INBUILT", VoiceGenerator.getInbuiltVoices().get(0)));
                ValNarratorApplication.showAlert("Quota Exhausted!", "An inbuilt voice has been automatically selected.");
            } catch (IndexOutOfBoundsException e) {
                ValNarratorApplication.showAlert("Quota Exhausted!", "Please try again later at 00:00 UTC.");
            }
        });
    }

    public void updateRequestQuota(MessageQuota mq) {
        if (mq.remainingQuota() <= 0) {
            markQuotaExhausted();
        } else {
            if (mq.isPremium()) {
                Platform.runLater(() -> {
                    ValNarratorController.getLatestInstance().windowTitle.setVisible(false);
                    ValNarratorController.getLatestInstance().premiumWindowTitle.setVisible(true);
                    ValNarratorController.getLatestInstance().subscribeButton.setVisible(false);
                    ValNarratorController.getLatestInstance().setPremiumDateLabel(String.format("Valid till %s", new SimpleDateFormat("dd/MM/yyyy").format(new Date(Long.parseLong(mq.premiumTill()) * 1000))));
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

    public void browseSubscription() throws IOException {
        String subscriptionURL = ChatDataHandler.getInstance().getAPIHandler().getSubscriptionURL();
        if (subscriptionURL != null) {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(subscriptionURL));
            logger.info("Opened subscription page successfully!");
        } else {
            logger.error("Could not open subscription page!");
            showAlert("Error!", "Failed to open subscription page, please try again later.");
        }
    }

    public void setMessagesSent(long number) {
        messagesSentLabel.setText(Long.toString(number));
    }

    public void setCharactersNarrated(long number) {
        charactersNarratedLabel.setText(Long.toString(number));
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
                if (!isValorantRunning()) {
                    if (showConfirmationAlertAndWait("Confirmation", "Valorant isn't running, do you want to close this app?")) {
                        logger.info("Exiting app due to popup confirmation.");
                        System.exit(0);
                        return;
                    }
                }
                if (panelInfo.isVisible()) lastAnchorPane = panelInfo;
                else if (panelUser.isVisible()) lastAnchorPane = panelUser;
                else if (panelSettings.isVisible()) lastAnchorPane = panelSettings;
                panelInfo.setVisible(false);
                panelSettings.setVisible(false);
                panelUser.setVisible(false);
                btnInfo.setOpacity(0.4);
                btnUser.setOpacity(0.4);
                btnSettings.setOpacity(0.4);
            } else {
                lastAnchorPane.setVisible(true);
                btnInfo.setOpacity(1);
                btnUser.setOpacity(1);
                btnSettings.setOpacity(1);
            }
        }

        if (event.getTarget() == btnInfo && !ChatDataHandler.getInstance().getProperties().isDisabled()) {
            panelInfo.setVisible(true);
            panelSettings.setVisible(false);
            panelUser.setVisible(false);
        } else if (event.getTarget() == btnUser && !ChatDataHandler.getInstance().getProperties().isDisabled()) {
            panelInfo.setVisible(false);
            panelSettings.setVisible(false);
            panelUser.setVisible(true);
        } else if (event.getTarget() == btnSettings && !ChatDataHandler.getInstance().getProperties().isDisabled()) {
            panelInfo.setVisible(false);
            panelSettings.setVisible(true);
            panelUser.setVisible(false);
        }
    }


    public void selectSource() throws IOException {
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
        VoiceGenerator.getInstance().loadCurrentSource(rawSource);
        VoiceGenerator.getInstance().saveConfig();
    }

    public void selectVoice() {
        final String rawVoiceId = voices.getValue();
        if (VoiceGenerator.setCurrentVoice(rawVoiceId)) previousSelection = rawVoiceId;
    }

    public void toggleTeamChat() throws IOException {
        if (VoiceGenerator.getInstance().toggleTeamKey()) {
            logger.info("Toggled Team Chat PTT ON");
        } else {
            logger.info("Toggled Team Chat PTT OFF");
        }
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
            String fileLocation = String.format("%s/ValorantNarrator/SoundVolumeView.exe", System.getenv("ProgramFiles").replace("\\", "/"));
            try {
                String command = fileLocation + " /SetPlaybackThroughDevice \"DefaultCaptureDevice\" \"CABLE Input\"";
                Runtime.getRuntime().exec(command);
                command = fileLocation + " /SetListenToThisDevice \"DefaultCaptureDevice\" 1";
                Runtime.getRuntime().exec(command);
            } catch (IOException ignored) {
            }
            logger.info("Toggled Mic ON");

        } else {
            try {
                String fileLocation = String.format("%s/ValorantNarrator/SoundVolumeView.exe", System.getenv("ProgramFiles").replace("\\", "/"));
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
        if (error.code() == 500) {
            logger.error("Exiting due to an unknown error!");
            Platform.runLater(() -> {
                showAlertAndWait("", "An internal server error occurred, please try again later.");
                System.exit(-1);
            });
        } else {
            logger.error(String.format("Exiting due to %s", error.reason()));
            Platform.runLater(() -> {
                showAlertAndWait("", error.reason());
                System.exit(-1);
            });
        }
    }

    @Override
    public void dispatchEvent(XMPPEvent event) {
        if (event.data().contains("<?xml version='1.0'?>")) {
            Platform.runLater(() -> ValNarratorController.getLatestInstance().progressLoginLabel.setText("Riot client opened."));
        } else if (event.data().contains("_xmpp_session1")) {
            Platform.runLater(() -> ValNarratorController.getLatestInstance().progressLoginLabel.setText("Valorant opened."));
            isLoading = false;
            btnPower.setOpacity(1);
            btnInfo.setOpacity(1);
            btnUser.setOpacity(1);
            btnSettings.setOpacity(1);
        }
    }

}