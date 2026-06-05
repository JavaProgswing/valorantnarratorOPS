package com.jprcoder.valnarratorgui;

import com.jprcoder.valnarratorbackend.*;
import javafx.application.Platform;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.DataFormatException;

/**
 * Drives the asynchronous start-up sequence: launch Valorant, connect to the
 * Riot local API, then build the backend singletons and reveal the main UI.
 * Kept out of {@link ValNarratorController} so the controller stays focused on
 * view wiring.
 */
class AppInitializer {
    private static final Logger logger = LoggerFactory.getLogger(AppInitializer.class);
    // Valorant-exit watch: poll cadence + consecutive misses required before acting, so a
    // single transient tasklist hiccup is not mistaken for Valorant closing.
    private static final long VALORANT_POLL_MS = 3_000;
    private static final int VALORANT_MISS_LIMIT = 2;
    // Exclusive-fullscreen -> Borderless handling: prompt once, apply on the next Valorant close.
    private static final AtomicBoolean fullscreenPrompted = new AtomicBoolean(false);
    private static volatile boolean pendingBorderlessFix = false;

    private AppInitializer() {
    }

    static void start(ValNarratorController controller, RiotLocalApiClient localApi) {
        // Indeterminate bar while we connect; flipped to "done" on success.
        Platform.runLater(() -> controller.progressLogin.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS));

        CompletableFuture.runAsync(() -> {
            try {
                // Only auto-launch Valorant if it isn't already running; otherwise just
                // connect.
                if (RiotUtilityHandler.isValorantRunning()) {
                    Platform.runLater(() -> controller.progressLoginLabel
                            .setText("Valorant already running - connecting to chat..."));
                } else {
                    Platform.runLater(() -> controller.progressLoginLabel.setText("Launching Valorant..."));
                    RiotUtilityHandler.launchValorant();
                }
                localApi.connectBlocking();
                // connectBlocking() starts chat polling once all readiness stages pass.
            } catch (IOException | InterruptedException e) {
                if (e instanceof InterruptedException)
                    Thread.currentThread().interrupt();
                logger.error("Local API connection failed: {}", e.getMessage());
                Platform.runLater(() -> {
                    controller.progressLogin.setProgress(0);
                    ValNarratorApplication.showAlertAndWait("Connection Error",
                            "Could not connect to Riot Client: " + e.getMessage());
                    controller.progressLoginLabel.setText("Connection failed. Restart the app to try again.");
                });
                return;
            }

            String selfPuuid = localApi.getSelfPuuid();
            logger.info("Self PUUID: {}", selfPuuid);

            // Mark loading complete, switch to the user panel.
            controller.isLoading = false;
            Platform.runLater(() -> {
                controller.progressLogin.setProgress(1);
                controller.panelInfo.setVisible(false);
                controller.panelSettings.setVisible(false);
                controller.panelLogin.setVisible(false);
                controller.panelUser.setVisible(true);
                controller.topBar.setDisable(false); // nav usable now
                controller.setActiveTab(controller.btnUser);
            });

            // Initialize backend singletons now that we have a session.
            ChatDataHandler.generateSingleton();
            ChatDataHandler.getInstance().getProperties().setSelfID(selfPuuid);
            new VoiceTokenHandler(ChatDataHandler.getInstance().getAPIHandler()).startRefreshToken();
            VoiceGenerator.generateSingleton();
            VoiceGenerator.initializeAgentSynthesizer();

            // Start the OCR chat sidecar LAST: it feeds ChatDataHandler.message, which
            // narrates through VoiceGenerator, so every backend singleton must be live
            // first - otherwise an early in-game line could hit an uninitialised singleton.
            try {
                OcrChatClient ocrChat = new OcrChatClient(
                        localApi::getSelfName, // own-message detection by display name
                        message -> {
                            if (ChatDataHandler.getInstance() != null) {
                                ChatDataHandler.getInstance().message(message);
                            }
                        });
                // Sidecar reports if Valorant is in exclusive fullscreen (capture blank); prompt
                // the user to switch to Borderless - needed to read chat and to stop the stutter.
                ocrChat.setDisplayModeListener(mode -> Platform.runLater(() -> {
                    if (OcrChatClient.DISPLAY_FULLSCREEN.equals(mode)) onValorantFullscreen();
                    else if (OcrChatClient.DISPLAY_OK.equals(mode)) fullscreenPrompted.set(false);
                }));
                ocrChat.start();
            } catch (IOException e) {
                logger.warn("Could not start OCR chat sidecar: {}", e.getMessage());
            }

            Platform.runLater(() -> {
                ArrayList<String> inbuiltVoiceNames = new ArrayList<>();
                VoiceGenerator.getInbuiltVoices().forEach(n -> inbuiltVoiceNames.add(String.format("%s, INBUILT", n)));
                controller.voices.getItems().addAll(inbuiltVoiceNames);

                // Populate the player dropdown with anyone we already know about.
                controller.playerDropdown.getItems().clear();
                var properties = ChatDataHandler.getInstance().getProperties();
                if (properties != null && properties.getPlayerNameTable() != null) {
                    for (String playerName : properties.getPlayerNameTable().keySet()) {
                        String playerID = properties.getPlayerNameTable().get(playerName);
                        controller.playerDropdown.getItems().add(
                                properties.isIgnoredPlayerID(playerID) ? playerName + " (IGNORED)" : playerName);
                    }
                }
            });

            try {
                if (VoiceGenerator.isSyncValorantSettingsEnabled()) {
                    VoiceGenerator.getInstance().syncValorantPlayerSettings();
                }
            } catch (IOException | DataFormatException | InterruptedException e) {
                if (e instanceof InterruptedException)
                    Thread.currentThread().interrupt();
                logger.warn("Failed to sync Valorant settings on startup: {}", e.getMessage());
            }

            // Start-up succeeded (Valorant is running) - now watch for it closing.
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            watchForValorantExit(controller);
        });
    }

    /**
     * After a successful start-up, polls for Valorant exiting. Valorant is the app's only
     * reason to run (it is the chat source), so when it closes the user is told and can
     * either quit or keep ValorantNarrator waiting in the tray for Valorant to reopen.
     */
    private static void watchForValorantExit(ValNarratorController controller) {
        Thread watcher = new Thread(() -> {
            int misses = 0;
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(VALORANT_POLL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                if (RiotUtilityHandler.isValorantRunning()) {
                    misses = 0;
                    continue;
                }
                if (++misses < VALORANT_MISS_LIMIT) continue; // tolerate a transient miss

                logger.info("Valorant has closed.");
                Platform.runLater(() -> onValorantClosed(controller));
                return; // handled once
            }
        }, "valorant-exit-watcher");
        watcher.setDaemon(true);
        watcher.start();
    }

    /** On the FX thread: offer to switch Valorant to Borderless; the change is applied on close. */
    private static void onValorantFullscreen() {
        if (!fullscreenPrompted.compareAndSet(false, true)) return; // ask once per fullscreen spell
        boolean fix = ValNarratorApplication.showConfirmationAlertAndWait("Switch Valorant to Borderless?",
                "Valorant is running in exclusive Fullscreen. With any overlay (Discord, this app) that "
                        + "causes periodic stutter, and chat narration cannot read the screen in this mode.\n\n"
                        + "Automatically switch Valorant to Windowed Fullscreen (Borderless)? It applies the "
                        + "next time Valorant restarts.");
        if (fix) {
            pendingBorderlessFix = true;
            ValNarratorApplication.showInformation("Borderless Queued",
                    "Valorant will open in Borderless the next time you restart it.");
        }
    }

    /**
     * On the FX thread: tell the user Valorant closed, then quit or hide to the tray.
     */
    private static void onValorantClosed(ValNarratorController controller) {
        if (pendingBorderlessFix) {
            pendingBorderlessFix = false;
            // Valorant is closed now so the write sticks. Run off the FX thread - it walks the
            // Valorant config tree and writes files, which must not block/freeze the UI.
            CompletableFuture.runAsync(() -> {
                int n = RiotUtilityHandler.setBorderlessMode();
                logger.info("Applied borderless to {} Valorant config(s) on close.", n);
            });
        }
        boolean exit = ValNarratorApplication.showConfirmationAlertAndWait("Valorant Closed",
                "Valorant has closed. ValorantNarrator only narrates chat while Valorant is "
                        + "running.\n\nExit now? Choose No to keep it running in the tray - it "
                        + "resumes automatically when Valorant reopens.");
        if (exit) {
            logger.info("Exiting after Valorant closed.");
            Platform.exit();
            System.exit(0);
        } else {
            logger.info("Keeping ValorantNarrator in the tray after Valorant closed.");
            try {
                Stage stage = (Stage) controller.topBar.getScene().getWindow();
                if (stage != null) stage.hide();
            } catch (Exception e) {
                logger.debug("Could not minimise to tray: {}", e.getMessage());
            }
        }
    }
}
