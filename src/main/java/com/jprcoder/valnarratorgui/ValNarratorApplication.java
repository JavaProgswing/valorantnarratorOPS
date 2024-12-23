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
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ValNarratorApplication extends Application {
    private static final Logger logger = LoggerFactory.getLogger(ValNarratorApplication.class);
    private boolean firstTime;
    private TrayIcon trayIcon;

    public static void showPreStartupDialog(String headerText, String contentText, MessageType messageType) {
        JOptionPane.showMessageDialog(null, contentText, headerText, messageType.getValue());
    }

    public static void showInformation(String headerText, String contentText) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.show();
    }

    public static void showAlert(String headerText, String contentText) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        Toolkit.getDefaultToolkit().beep();
        alert.show();
    }

    public static void showAlertAndWait(String headerText, String contentText) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        Toolkit.getDefaultToolkit().beep();
        alert.showAndWait();
    }

    public static boolean showConfirmationAlertAndWait(String headerText, String contentText) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Valorant Narrator");
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);

        ButtonType yesButton = new ButtonType("Yes");
        ButtonType noButton = new ButtonType("No");
        alert.getButtonTypes().setAll(yesButton, noButton);

        Optional<ButtonType> result = alert.showAndWait();

        return (result.isPresent() && result.get() == yesButton);
    }

    @Override
    public void start(Stage stage) throws IOException, AWTException {
        CompletableFuture.runAsync(() -> {
            logger.debug("Trying to play start-up tune.");
            long startTime = System.currentTimeMillis();
            try (InputStream is = Objects.requireNonNull(ValNarratorApplication.class.getResourceAsStream("startupTune.mp3"))) {
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
            trayIcon.displayMessage("Minimized to tray.", "Valorant Narrator is still running.", TrayIcon.MessageType.INFO);
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
