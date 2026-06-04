package com.jprcoder.valnarratorgui;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/**
 * Dev-only: renders the main FXML in preview mode (no Riot/process startup) and
 * snapshots each panel to target/preview/*.png so the UI can be reviewed without
 * launching the real app. Not part of the shipped product.
 */
public class PreviewLauncher extends Application {
    private static final List<String> PANELS = List.of("panelLogin", "panelUser", "panelSettings", "panelInfo");

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        System.setProperty("valnarrator.preview", "true");

        FXMLLoader loader = new FXMLLoader(getClass().getResource("mainApplication.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 400, 680, Color.web("#0A0F16"));
        var appCss = getClass().getResource("app.css");
        if (appCss != null) scene.getStylesheets().add(appCss.toExternalForm());
        stage.setScene(scene);
        stage.show();

        new File("target/preview").mkdirs();
        captureNext(scene, root, 0);
    }

    private void captureNext(Scene scene, Parent root, int index) {
        if (index >= PANELS.size()) {
            capturePaused(scene, root);
            return;
        }
        String visible = PANELS.get(index);
        for (String id : PANELS) {
            Node n = root.lookup("#" + id);
            if (n != null) n.setVisible(id.equals(visible));
        }
        root.applyCss();
        root.layout();

        // Let a render pulse run so CSS + layout settle before snapshotting.
        PauseTransition pause = new PauseTransition(Duration.millis(220));
        pause.setOnFinished(e -> {
            writePng(scene.snapshot(null), "target/preview/" + visible + ".png");
            captureNext(scene, root, index + 1);
        });
        pause.play();
    }

    private void capturePaused(Scene scene, Parent root) {
        for (String id : PANELS) {
            Node n = root.lookup("#" + id);
            if (n != null) n.setVisible("panelUser".equals(id));
        }
        Node overlay = root.lookup("#disabledOverlay");
        if (overlay != null) overlay.setVisible(true);
        root.applyCss();
        root.layout();
        PauseTransition pause = new PauseTransition(Duration.millis(220));
        pause.setOnFinished(e -> {
            writePng(scene.snapshot(null), "target/preview/paused.png");
            if (overlay != null) overlay.setVisible(false);
            captureFullFormsManager();
        });
        pause.play();
    }

    private void captureFullFormsManager() {
        FullFormsManager.show();
        PauseTransition pause = new PauseTransition(Duration.millis(320));
        pause.setOnFinished(e -> {
            for (Window w : Window.getWindows()) {
                if (w instanceof Stage s && s.getTitle() != null && s.getTitle().contains("Full-forms")) {
                    writePng(s.getScene().snapshot(null), "target/preview/fullFormsManager.png");
                }
            }
            captureSubscription();
        });
        pause.play();
    }

    private void captureSubscription() {
        SubscriptionView.show("https://valnarrator.vercel.app/?user-id=-1833269070", "-1833269070", false);
        PauseTransition pause = new PauseTransition(Duration.millis(400));
        pause.setOnFinished(e -> {
            for (Window w : Window.getWindows()) {
                if (w instanceof Stage s && s.getTitle() != null && s.getTitle().contains("Premium")) {
                    writePng(s.getScene().snapshot(null), "target/preview/subscriptionView.png");
                }
            }
            Platform.exit();
        });
        pause.play();
    }

    private void writePng(WritableImage fx, String path) {
        int w = (int) fx.getWidth();
        int h = (int) fx.getHeight();
        BufferedImage bimg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        PixelReader pr = fx.getPixelReader();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                bimg.setRGB(x, y, pr.getArgb(x, y));
            }
        }
        try {
            ImageIO.write(bimg, "png", new File(path));
            System.out.println("Wrote " + path);
        } catch (Exception e) {
            System.out.println("Failed to write " + path + ": " + e.getMessage());
        }
    }
}
