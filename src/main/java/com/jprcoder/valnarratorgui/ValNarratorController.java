package com.jprcoder.valnarratorgui;

import com.jfoenix.controls.JFXToggleButton;
import com.jprcoder.valnarratorbackend.*;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;
import java.util.zip.DataFormatException;

import static com.jprcoder.valnarratorbackend.RiotUtilityHandler.isValorantRunning;
import static com.jprcoder.valnarratorgui.ValNarratorApplication.*;

public class ValNarratorController {
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
    public ComboBox<String> voices, sources, playerDropdown;
    @FXML
    public Label progressLoginLabel;
    @FXML
    public Label premiumDateLabel;
    @FXML
    public TextField userIDLabel;
    @FXML
    public Label quotaLabel;
    @FXML
    public Label messagesSentLabel;
    @FXML
    public Label charactersNarratedLabel;
    @FXML
    public Label wordsSentLabel;
    @FXML
    public Label avgLengthLabel;
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
    public Node panelLogin, panelUser, panelSettings, panelInfo, topBar, disabledOverlay;
    @FXML
    public Region contentArea;
    @FXML
    public Label premiumBadge;
    @FXML
    public Label rateValueLabel;
    private String previousSelection = "Matthew, male";
    volatile boolean isLoading = true;

    private boolean isVoicesVisible = false;

    private boolean selectingKeybind = false;

    public ValNarratorController() {
        ValNarratorController.latestInstance = this;
    }

    public static ValNarratorController getLatestInstance() {
        return latestInstance;
    }

    private static void fadeIn(Node node) {
        FadeTransition fade = new FadeTransition(Duration.millis(160), node);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();
    }

    private static void clipToBounds(Region region) {
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(region.widthProperty());
        clip.heightProperty().bind(region.heightProperty());
        region.setClip(clip);
    }

    public void initialize() {
        logger.info("Initializing app.");
        rateValueLabel.textProperty().bind(rateSlider.valueProperty().asString("%.0f%%"));
        clipToBounds(contentArea); // keep panel content (e.g. the rate slider) inside the card

        // Preview/design mode: render the UI without launching Valorant or touching
        // the Riot client / audio devices. Enabled with -Dvalnarrator.preview=true.
        if (Boolean.getBoolean("valnarrator.preview")) {
            logger.info("Preview mode - skipping Riot client, audio and process startup.");
            return;
        }

        topBar.setDisable(true); // nav unusable until startup completes

        long appStart = System.currentTimeMillis();

        // Kill stale agent voice process (if any)
        try {
            String command = "taskkill /F /IM valorantNarrator-agentVoices.exe";
            ProcessBuilder processBuilder = new ProcessBuilder(command.split("\\s+"));
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            int code = process.waitFor();
            if (code == 0) {
                logger.debug("Closed stale agent-voice process.");
            }
        } catch (Exception e) {
            logger.debug("Could not kill stale agent-voice process: {}", e.getMessage());
        }

        // Set up audio routing (VB-Audio CABLE)
        String soundVolumeView = String.format("%s/ValorantNarrator/SoundVolumeView.exe",
                System.getenv("ProgramFiles").replace("\\", "/"));
        long pid = ProcessHandle.current().pid();
        ProcessUtil.runDetached(soundVolumeView, "/SetAppDefault", "CABLE Input", "all", String.valueOf(pid));
        ProcessUtil.runDetached(soundVolumeView, "/SetPlaybackThroughDevice", "CABLE Output",
                "Default Playback Device");
        ProcessUtil.runDetached(soundVolumeView, "/SetListenToThisDevice", "CABLE Output", "1");
        logger.info("Initialized app's sound output.");

        // -- Riot Client local API connection (whisper chat) ----------------
        logger.info("Connecting via Riot Client local API.");

        // Builds its own trust-all client with TLS hostname verification disabled
        // (required for the Riot local API's self-signed 127.0.0.1 certificate).
        final RiotLocalApiClient localApi = new RiotLocalApiClient();

        // Readiness stage updates -> progress label.
        localApi.setReadinessListener((stage, detail) -> Platform.runLater(() -> progressLoginLabel.setText(detail)));

        // Messages are now processed exclusively via OcrChatClient
        AppInitializer.start(this, localApi);

        logger.info("Initialization dispatched in {} ms.", System.currentTimeMillis() - appStart);
        rateSlider.valueProperty()
                .addListener((observable, oldValue, newValue) -> VoiceGenerator.setCurrentRate(newValue.shortValue()));
    }

    public void openDiscordInvite() {
        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create("https://valnarrator.vercel.app/discord"));
            logger.debug("Opened discord invite link successfully!");
        } catch (IOException e) {
            logger.error("Could not open discord invite link!");
            showAlert("Error!", "Failed to open discord invite link, please try again later.");
        }
    }

    public void handlePlayerAction() {
        String player = playerDropdown.getValue();
        if (player == null || player.trim().isEmpty())
            return;

        // Check if the player is already in the list (ignoring the suffix)
        String cleanPlayerName = player.replace(" (IGNORED)", "");
        Chat props = ChatDataHandler.getInstance().getProperties();
        if (!props.getPlayerNameTable().containsKey(cleanPlayerName)) {
            // Unknown name typed in the editable box - nothing to toggle.
            Platform.runLater(() -> playerDropdown.getSelectionModel().clearSelection());
            return;
        }
        boolean isIgnored = props.isIgnoredPlayerID(props.getPlayerNameTable().get(cleanPlayerName));

        if (isIgnored) {
            // Unignore
            logger.debug("Unignoring player {}", cleanPlayerName);
            ChatDataHandler.getInstance().getProperties().removeIgnoredPlayer(cleanPlayerName);

            // Update dropdown item
            Platform.runLater(() -> {
                playerDropdown.getItems().remove(player);
                playerDropdown.getItems().add(cleanPlayerName);
                playerDropdown.getSelectionModel().clearSelection();
                playerDropdown.setPromptText("Type to Ignore / Select to Toggle");
            });
        } else {
            // Ignore
            logger.debug("Ignoring player {}", cleanPlayerName);
            ChatDataHandler.getInstance().getProperties().addIgnoredPlayer(cleanPlayerName);

            // Update dropdown item
            Platform.runLater(() -> {
                playerDropdown.getItems().remove(player);
                playerDropdown.getItems().add(cleanPlayerName + " (IGNORED)");
                playerDropdown.getSelectionModel().clearSelection();
                playerDropdown.setPromptText("Type to Ignore / Select to Toggle");
            });
        }
    }

    public void syncValorantSettingsToggle() throws IOException {
        if (VoiceGenerator.getInstance().syncValorantSettingsToggle()) {
            Platform.runLater(() -> showInformation("Voice Settings SYNC",
                    "Enabled, Valorant settings will be synced at next start-up!"));
        } else {
            Platform.runLater(() -> showInformation("Voice Settings SYNC",
                    "Disabled, Valorant settings will not be synced at next start-up!"));
        }
    }

    public void syncValorantSettings() {
        // Heavy (file IO + network + process wait) - never run on the FX thread or the
        // UI freezes.
        CompletableFuture.runAsync(() -> {
            try {
                VoiceGenerator.getInstance().syncValorantPlayerSettings();
                showInformation("Voice Settings SYNC", "Valorant settings synced successfully!");
            } catch (IOException | DataFormatException | InterruptedException | RuntimeException e) {
                if (e instanceof InterruptedException)
                    Thread.currentThread().interrupt();
                logger.error("Failed to sync valorant settings: {}", e.getMessage());
                showAlert("Error!", "Failed to sync valorant settings, please try again later.");
            }
        });
    }

    public void revertVoiceSelection() {
        voices.getSelectionModel().select(previousSelection);
    }

    public void openFullFormsManager() {
        FullFormsManager.show();
    }

    public void keybindChange(javafx.scene.input.KeyEvent event) throws IOException {
        if (!selectingKeybind)
            return;

        KeyCode keyCode = event.getCode();
        String keyText = keyCode.getName();

        if (keyText != null && !keyText.isEmpty()) {
            Platform.runLater(() -> keybindTextField.setText(keyText));
        }

        VoiceGenerator.getInstance().setKeyEvent(keyCode.getCode());
        Platform.runLater(() -> keybindText.setText("Team voice key: " + keyText));
    }

    public void markQuotaExhausted() {
        CompletableFuture.runAsync(() -> {
            while (ChatDataHandler.getInstance() == null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            ChatDataHandler.getInstance().getProperties().markQuotaExhausted();
        });
        Platform.runLater(() -> {
            ValNarratorController.getLatestInstance().quotaLabel.setText("Quota Exhausted!");
            ValNarratorController.getLatestInstance().quotaBar.setProgress(0.0);
            ValNarratorController.getLatestInstance().setPremiumDateLabel("N/A");
            try {
                voices.getSelectionModel()
                        .select(String.format("%s, INBUILT", VoiceGenerator.getInbuiltVoices().get(0)));
                ValNarratorApplication.showAlert("Quota Exhausted!",
                        "An inbuilt voice has been automatically selected.");
            } catch (IndexOutOfBoundsException e) {
                ValNarratorApplication.showAlert("Quota Exhausted!", "Please try again later at 00:00 UTC.");
            }
        });
    }

    public void updateRequestQuota(MessageQuota mq) {
        if (mq.remainingQuota() <= 0) {
            markQuotaExhausted();
        } else if (mq.isPremium()) {
            Platform.runLater(() -> {
                windowTitle.setVisible(false);
                premiumWindowTitle.setVisible(true);
                quotaLabel.setText("Unlimited, Enjoy!");
                setPremiumDateLabel(String.format("Valid till %s", new SimpleDateFormat("dd/MM/yyyy HH:mm")
                        .format(new Date(Long.parseLong(mq.premiumTill()) * 1000))));
                applyPlanStyle(true);
            });
        } else {
            Platform.runLater(() -> {
                quotaLabel.setText(
                        mq.remainingQuota() + "/" + ChatDataHandler.getInstance().getProperties().getQuotaLimit());
                quotaBar.setProgress(
                        (double) mq.remainingQuota() / ChatDataHandler.getInstance().getProperties().getQuotaLimit());
                setPremiumDateLabel("N/A");
                applyPlanStyle(false);
            });
        }
    }

    public void setMessagesSent(long number) {
        messagesSentLabel.setText(Long.toString(number));
    }

    public void setCharactersNarrated(long number) {
        charactersNarratedLabel.setText(Long.toString(number));
    }

    /**
     * Reflect premium vs free in the account badge + subscribe button.
     */
    private void applyPlanStyle(boolean premium) {
        if (premiumBadge != null) {
            premiumBadge.setText(premium ? "PREMIUM" : "FREE");
            premiumBadge.getStyleClass().removeAll("premium-badge", "free-badge");
            premiumBadge.getStyleClass().add(premium ? "premium-badge" : "free-badge");
        }
        if (subscribeButton != null) {
            subscribeButton.setText(premium ? "Manage subscription" : "Upgrade to Premium");
        }
    }

    public void browseSubscription() {
        String subscriptionURL = ChatDataHandler.getInstance().getAPIHandler().getSubscriptionURL();
        if (subscriptionURL == null) {
            logger.error("Could not resolve the subscription URL.");
            showAlert("Error!", "Failed to open the subscription page, please try again later.");
            return;
        }
        String url = subscriptionURL + (subscriptionURL.contains("?") ? "&" : "?") + "user-id=" + Main.serialNumber;
        SubscriptionView.show(url, Main.serialNumber, ChatDataHandler.getInstance().isPremium());
        logger.debug("Opening subscription view for user {}.", Main.serialNumber);
    }

    /**
     * The free agent-voice trial is used up. Open the premium upgrade modal and drop the user onto a
     * free inbuilt voice so chat keeps being narrated. Must be called on the FX thread.
     */
    public void promptAgentPremiumUpgrade(String message) {
        selectFirstInbuiltVoice();
        ValNarratorApplication.showInformation("Premium Agent Voices", message
                + "\n\nSwitched you to a free voice for now.");
        openSubscriptionModal();
    }

    /**
     * The selected agent voice has been disabled (quality/other). Tell the user and fall back to a
     * free inbuilt voice. Must be called on the FX thread.
     */
    public void notifyAgentVoiceDisabled(String message) {
        selectFirstInbuiltVoice();
        ValNarratorApplication.showAlert("Agent Voice Unavailable", message
                + "\n\nSwitched you to a free voice for now.");
    }

    /** Selects the first inbuilt voice in the dropdown; the selection listener applies it. */
    private void selectFirstInbuiltVoice() {
        try {
            voices.getSelectionModel().select(String.format("%s, INBUILT", VoiceGenerator.getInbuiltVoices().get(0)));
        } catch (IndexOutOfBoundsException e) {
            logger.warn("No inbuilt voice available to fall back to.");
        }
    }

    private void openSubscriptionModal() {
        String subscriptionURL = ChatDataHandler.getInstance().getAPIHandler().getSubscriptionURL();
        if (subscriptionURL == null) {
            logger.error("Could not resolve the subscription URL.");
            return;
        }
        String url = subscriptionURL + (subscriptionURL.contains("?") ? "&" : "?") + "user-id=" + Main.serialNumber;
        SubscriptionView.show(url, Main.serialNumber, ChatDataHandler.getInstance().isPremium());
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

    public void enterKeybind() {
        selectingKeybind = true;
    }

    public void exitKeybind() {
        selectingKeybind = false;
    }

    public void setWordsNarrated(long number) {
        wordsSentLabel.setText(Long.toString(number));
    }

    public void setAverageLength(long number) {
        avgLengthLabel.setText(Long.toString(number));
    }

    public void handleButtonAction(MouseEvent event) {
        if (isLoading) {
            return;
        }
        Chat props = ChatDataHandler.getInstance().getProperties();

        if (event.getTarget() == btnPower) {
            boolean nowDisabled = props.toggleState();
            if (nowDisabled && !isValorantRunning()
                    && showConfirmationAlertAndWait("Close Valorant Narrator?",
                    "Valorant isn't running. Do you want to close the app?")) {
                logger.debug("Exiting app due to popup confirmation.");
                System.exit(0);
                return;
            }
            setPaused(nowDisabled);
            return;
        }

        if (props.isDisabled()) {
            return; // ignore tab switches while narration is paused
        }
        if (event.getTarget() == btnInfo) {
            showOnly(panelInfo);
            setActiveTab(btnInfo);
        } else if (event.getTarget() == btnUser) {
            showOnly(panelUser);
            setActiveTab(btnUser);
        } else if (event.getTarget() == btnSettings) {
            showOnly(panelSettings);
            setActiveTab(btnSettings);
        }
    }

    /**
     * Show exactly one content panel (with a quick fade-in) and remember it as
     * active.
     */
    private void showOnly(Node panel) {
        panelInfo.setVisible(panel == panelInfo);
        panelUser.setVisible(panel == panelUser);
        panelSettings.setVisible(panel == panelSettings);
        fadeIn(panel);
    }

    /**
     * Highlight the active tab icon; dim the others.
     */
    public void setActiveTab(ImageView active) {
        for (ImageView icon : new ImageView[]{btnUser, btnInfo, btnSettings}) {
            icon.setOpacity(icon == active ? 1.0 : 0.45);
        }
    }

    /**
     * Toggle the "narration paused" UI state (power button).
     */
    private void setPaused(boolean paused) {
        if (disabledOverlay != null)
            disabledOverlay.setVisible(paused);
        btnInfo.setDisable(paused);
        btnUser.setDisable(paused);
        btnSettings.setDisable(paused);
        if (paused) {
            if (!btnPower.getStyleClass().contains("power-active"))
                btnPower.getStyleClass().add("power-active");
        } else {
            btnPower.getStyleClass().remove("power-active");
        }
        logger.debug(paused ? "Narration paused." : "Narration resumed.");
    }

    public void selectSource() throws IOException {
        final String rawSource = sources.getValue();
        if (rawSource == null || rawSource.isBlank())
            return;
        EnumSet<Source> selectedSources = Source.fromString(rawSource);

        Chat props = ChatDataHandler.getInstance().getProperties();
        final boolean self = selectedSources.contains(Source.SELF);
        if (self)
            logger.debug("Self narration enabled.");
        else
            logger.debug("Self narration disabled.");
        props.setSelf(self);

        final boolean party = selectedSources.contains(Source.PARTY);
        if (party)
            logger.debug("Party narration enabled.");
        else
            logger.debug("Party narration disabled.");
        props.setParty(party);

        final boolean team = selectedSources.contains(Source.TEAM);
        if (team)
            logger.debug("Team narration enabled.");
        else
            logger.debug("Team narration disabled.");
        props.setTeam(team);

        final boolean all = selectedSources.contains(Source.ALL);
        if (all)
            logger.debug("All narration enabled.");
        else
            logger.debug("All narration disabled.");
        props.setAll(all);

        logger.debug("Selected sources: " + Source.toString(selectedSources));

        VoiceGenerator.getInstance().loadCurrentSource(Source.toString(selectedSources));
        VoiceGenerator.getInstance().saveConfig();
    }

    public void selectVoice() {
        final String rawVoiceId = voices.getValue();
        if (VoiceGenerator.setCurrentVoice(rawVoiceId))
            previousSelection = rawVoiceId;
    }

    public void toggleTeamChat() throws IOException {
        if (VoiceGenerator.getInstance().toggleTeamKey()) {
            logger.debug("Toggled Team Chat PTT ON");
        } else {
            logger.debug("Toggled Team Chat PTT OFF");
        }
    }

    public void togglePrivateMessages() {
        if (privateChatButton.isSelected()) {
            ChatDataHandler.getInstance().getProperties().setPrivateEnabled();
            logger.debug("Toggled Private Messages ON");
        } else {
            ChatDataHandler.getInstance().getProperties().setPrivateDisabled();
            logger.debug("Toggled Private Messages OFF");
        }
    }

    public void toggleMic() {
        String soundVolumeView = String.format("%s/ValorantNarrator/SoundVolumeView.exe",
                System.getenv("ProgramFiles").replace("\\", "/"));
        if (micButton.isSelected()) {
            ProcessUtil.runDetached(soundVolumeView, "/SetPlaybackThroughDevice", "DefaultCaptureDevice",
                    "CABLE Input");
            ProcessUtil.runDetached(soundVolumeView, "/SetListenToThisDevice", "DefaultCaptureDevice", "1");
            logger.debug("Toggled Mic ON");
        } else {
            ProcessUtil.runDetached(soundVolumeView, "/SetPlaybackThroughDevice", "DefaultCaptureDevice",
                    "CABLE Input");
            ProcessUtil.runDetached(soundVolumeView, "/SetListenToThisDevice", "DefaultCaptureDevice", "0");
            logger.debug("Toggled Mic OFF");
        }
    }

    public boolean isVoicesVisible() {
        return isVoicesVisible;
    }

    public void setVoicesVisible(boolean voicesVisible) {
        isVoicesVisible = voicesVisible;
    }
}
