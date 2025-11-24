package com.jprcoder.valnarratorgui;

import dev.mccue.jlayer.decoder.JavaLayerException;
import dev.mccue.jlayer.player.advanced.AdvancedPlayer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class ValNarratorApplication extends Application {
    private static final Logger logger = LoggerFactory.getLogger(ValNarratorApplication.class);
    private boolean firstTime;
    private TrayIcon trayIcon;

    public static void showDialog(String headerText, String contentText, MessageType messageType) {
        Alert.AlertType alertType;
        switch (messageType) {
            case INFORMATION_MESSAGE -> alertType = Alert.AlertType.INFORMATION;
            case WARNING_MESSAGE -> alertType = Alert.AlertType.WARNING;
            case ERROR_MESSAGE -> alertType = Alert.AlertType.ERROR;
            default -> alertType = Alert.AlertType.NONE;
        }
        FXInit.init();
        Platform.runLater(() -> {
            Alert alert = new Alert(alertType);
            alert.setHeaderText(headerText);
            alert.setContentText(contentText);
            alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            alert.showAndWait();
        });
    }

    public static CompletableFuture<Alert> showNonBlockingDialog(
            String headerText,
            String contentText,
            MessageType messageType) {
        CompletableFuture<Alert> future = new CompletableFuture<>();

        FXInit.init();
        Platform.runLater(() -> {
            try {

                Alert.AlertType alertType = switch (messageType) {
                    case INFORMATION_MESSAGE -> Alert.AlertType.INFORMATION;
                    case WARNING_MESSAGE -> Alert.AlertType.WARNING;
                    case ERROR_MESSAGE -> Alert.AlertType.ERROR;
                    default -> Alert.AlertType.NONE;
                };

                Alert alert = new Alert(alertType);
                alert.setHeaderText(headerText);
                alert.setContentText(contentText);
                alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

                alert.show(); // runs on FX thread — safe

                future.complete(alert);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    public static void showInformation(String headerText, String contentText) {
        FXInit.init();
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText(headerText);
            alert.setContentText(contentText);
            alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            alert.show();
        });
    }

    public static void showAlert(String headerText, String contentText) {
        FXInit.init();
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(headerText);
            alert.setContentText(contentText);
            alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            Toolkit.getDefaultToolkit().beep();
            alert.show();
        });
    }

    public static void showAlertAndWait(String headerText, String contentText) {
        FXInit.init();
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(headerText);
            alert.setContentText(contentText);
            alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            Toolkit.getDefaultToolkit().beep();
            alert.showAndWait();
        });
    }

    public static boolean showConfirmationAlertAndWait(String headerText, String contentText) {
        FXInit.init();
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean userChoice = new AtomicBoolean(false);

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Valorant Narrator");
            alert.setHeaderText(headerText);
            alert.setContentText(contentText);

            ButtonType yesButton = new ButtonType("Yes");
            ButtonType noButton = new ButtonType("No");
            alert.getButtonTypes().setAll(yesButton, noButton);

            Optional<ButtonType> result = alert.showAndWait();
            userChoice.set(result.isPresent() && result.get() == yesButton);

            latch.countDown();
        });

        try {
            latch.await();
        } catch (InterruptedException ignored) {
        }

        return userChoice.get();
    }

    public static String showInputDialogAndWait(String headerText, String contentText) {
        FXInit.init();
        final CountDownLatch latch = new CountDownLatch(1);
        final StringBuilder userInput = new StringBuilder();

        Platform.runLater(() -> {
            javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
            dialog.setTitle("Valorant Narrator");
            dialog.setHeaderText(headerText);
            dialog.setContentText(contentText);

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(userInput::append);

            latch.countDown();
        });

        try {
            latch.await();
        } catch (InterruptedException ignored) {
            logger.error("Interrupted while waiting for user input dialog.", ignored);
        }

        return !userInput.isEmpty() ? userInput.toString() : null;
    }

    @Override
    public void start(Stage stage) throws IOException, AWTException {
        CompletableFuture.runAsync(() -> {
            logger.debug("Trying to play start-up tune.");
            long startTime = System.currentTimeMillis();
            try (InputStream is = Objects
                    .requireNonNull(ValNarratorApplication.class.getResourceAsStream("startupTune.mp3"))) {
                AdvancedPlayer player = new AdvancedPlayer(is);
                player.play();
                logger.debug(String.format("Finished playing tune in %d ms.", System.currentTimeMillis() - startTime));
            } catch (JavaLayerException | IOException e) {
                logger.error("Error while playing start-up tune.", e);
            }
        });
        FXMLLoader fxmlLoader = new FXMLLoader(ValNarratorApplication.class.getResource("mainApplication.fxml"));
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setTitle("Valorant Narrator");
        createTrayIcon(stage);
        firstTime = true;
        Platform.setImplicitExit(false);
        Scene scene = new Scene(fxmlLoader.load());
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public void createTrayIcon(final Stage stage) throws IOException, AWTException {
        if (SystemTray.isSupported()) {
            SystemTray tray = SystemTray.getSystemTray();
            Image image = ImageIO.read(Objects.requireNonNull(ValNarratorApplication.class.getResource("appIcon.png")));

            stage.setOnCloseRequest(t -> hide(stage));
            final ActionListener closeListener = e -> System.exit(0);

            ActionListener showListener = e -> Platform.runLater(stage::show);
            PopupMenu popup = new PopupMenu();

            MenuItem showItem = new MenuItem("Show");
            showItem.addActionListener(showListener);
            popup.add(showItem);

            MenuItem closeItem = new MenuItem("Close");
            closeItem.addActionListener(closeListener);
            popup.add(closeItem);
            trayIcon = new TrayIcon(Objects.requireNonNull(image), "Valorant Narrator", popup);
            trayIcon.addActionListener(showListener);
            tray.add(trayIcon);
        } else {
            logger.warn("System tray not supported!");
        }
    }

    public void showProgramIsMinimizedMsg() {
        if (firstTime) {
            trayIcon.displayMessage("Minimized to tray.", "Valorant Narrator is still running.",
                    TrayIcon.MessageType.INFO);
            firstTime = false;
        }
    }

    private void hide(final Stage stage) {
        Platform.runLater(() -> {
            if (SystemTray.isSupported()) {
                stage.hide();
                showProgramIsMinimizedMsg();
            }
        });
    }

}
