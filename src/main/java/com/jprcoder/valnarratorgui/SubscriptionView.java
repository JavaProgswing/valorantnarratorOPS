package com.jprcoder.valnarratorgui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * Subscription / upgrade modal.
 * <p>
 * Rendered natively (a JavaFX modal, not an embedded browser): the project's site is a
 * modern single-page app the bundled WebKit can't render, and the payment gateways
 * (PayPal / Razorpay) need a real browser anyway. The modal pitches premium, shows the
 * copyable user id the checkout asks for, and opens the real checkout page - with the
 * user id appended - in the system browser.
 */
public final class SubscriptionView {
    private static final Logger logger = LoggerFactory.getLogger(SubscriptionView.class);

    /**
     * Premium benefits, mirrored from the website's feature list.
     */
    private static final String[] BENEFITS = {
            "Unlimited narration quota",
            "Premium AWS neural voices",
            "12+ voices, including all agents",
            "Zero-lag background performance"
    };

    private static Stage stage;

    private SubscriptionView() {
    }

    /**
     * Opens the upgrade modal (or brings it to the front). FX-thread safe.
     *
     * @param url     the checkout page URL, already including the user id
     * @param userId  the user's id, shown so it can be copied into checkout
     * @param premium whether the current account is already premium
     */
    public static void show(String url, String userId, boolean premium) {
        if (Platform.isFxApplicationThread()) {
            open(url, userId, premium);
        } else {
            Platform.runLater(() -> open(url, userId, premium));
        }
    }

    private static void open(String url, String userId, boolean premium) {
        if (stage != null && stage.isShowing()) {
            stage.toFront();
            return;
        }

        VBox root = new VBox(buildHero(premium), buildBody(url, userId, premium));
        root.getStyleClass().add("sub-root");

        Scene scene = new Scene(root, 600, 640);
        var css = SubscriptionView.class.getResource("app.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        stage = new Stage();
        stage.setTitle("Valorant Narrator - Premium");
        stage.setScene(scene);
        stage.setResizable(false);
        var icon = SubscriptionView.class.getResourceAsStream("appIcon.png");
        if (icon != null) stage.getIcons().add(new Image(icon));
        stage.show();
        stage.toFront();
    }

    private static VBox buildHero(boolean premium) {
        Label brand = new Label("VALORANT NARRATOR");
        brand.getStyleClass().add("sub-brand");

        Label title = new Label("Go Premium");
        title.getStyleClass().add("sub-modal-title");

        Label badge = new Label(premium ? "PREMIUM" : "FREE PLAN");
        badge.getStyleClass().add(premium ? "premium-badge" : "free-badge");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox titleRow = new HBox(10, title, spacer, badge);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label subtitle = new Label("Unlimited narration and premium voices for your matches.");
        subtitle.getStyleClass().add("sub-subtitle");

        VBox hero = new VBox(4, brand, titleRow, subtitle);
        hero.getStyleClass().add("sub-hero");
        hero.setPadding(new Insets(22, 26, 18, 26));
        return hero;
    }

    private static VBox buildBody(String url, String userId, boolean premium) {
        // User id row (the checkout page asks for it).
        Label uidLabel = new Label("Your User ID");
        uidLabel.getStyleClass().add("section-header");
        TextField uidField = new TextField(userId);
        uidField.setEditable(false);
        uidField.getStyleClass().add("sub-uid-field");
        HBox.setHgrow(uidField, Priority.ALWAYS);
        Button copy = new Button("Copy");
        copy.getStyleClass().add("ghost-button");
        copy.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(userId);
            Clipboard.getSystemClipboard().setContent(content);
            copy.setText("Copied");
        });
        HBox uidRow = new HBox(8, uidField, copy);
        uidRow.setAlignment(Pos.CENTER_LEFT);
        VBox uidBox = new VBox(4, uidLabel, uidRow);

        // Pricing cards.
        HBox pricing = new HBox(14,
                priceCard("MONTHLY", "$2", "/mo", "Cancel anytime", true),
                priceCard("ONE-TIME", "$4.99", "", "Pay once, yours forever", false));
        HBox.setHgrow(pricing.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(pricing.getChildren().get(1), Priority.ALWAYS);

        // Benefits.
        VBox benefits = new VBox(8);
        for (String benefit : BENEFITS) {
            benefits.getChildren().add(benefitRow(benefit));
        }

        Label note = new Label("Secure checkout with PayPal or Razorpay. Your User ID links the purchase to this app.");
        note.getStyleClass().add("section-desc");
        note.setWrapText(true);

        Button close = new Button("Maybe later");
        close.getStyleClass().add("ghost-button");
        close.setCancelButton(true);
        close.setOnAction(e -> stage.close());
        Button getPremium = new Button(premium ? "Manage subscription" : "Get Premium");
        getPremium.getStyleClass().add("primary-button");
        getPremium.setDefaultButton(true);
        getPremium.setOnAction(e -> openInBrowser(url));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox footer = new HBox(10, spacer, close, getPremium);
        footer.setAlignment(Pos.CENTER_RIGHT);

        Region grow = new Region();
        VBox.setVgrow(grow, Priority.ALWAYS);
        VBox body = new VBox(16, uidBox, pricing, benefits, note, grow, footer);
        body.setPadding(new Insets(20, 26, 22, 26));
        VBox.setVgrow(body, Priority.ALWAYS);
        return body;
    }

    private static VBox priceCard(String label, String price, String period, String sub, boolean recommended) {
        Label cardLabel = new Label(label);
        cardLabel.getStyleClass().add("price-card-label");

        Label priceTag = new Label(price);
        priceTag.getStyleClass().add("price-tag");
        Label periodTag = new Label(period);
        periodTag.getStyleClass().add("price-period");
        HBox priceRow = new HBox(2, priceTag, periodTag);
        priceRow.setAlignment(Pos.BASELINE_LEFT);

        Label subLabel = new Label(sub);
        subLabel.getStyleClass().add("price-sub");
        subLabel.setWrapText(true);

        VBox card = new VBox(4, cardLabel, priceRow, subLabel);
        card.getStyleClass().add(recommended ? "price-card-recommended" : "price-card");
        card.setPadding(new Insets(14, 16, 14, 16));

        if (recommended) {
            Label tag = new Label("BEST VALUE");
            tag.getStyleClass().add("price-tag-best");
            card.getChildren().add(0, tag);
        }
        return card;
    }

    private static HBox benefitRow(String text) {
        Region dot = new Region();
        dot.getStyleClass().add("benefit-dot");
        dot.setMinSize(7, 7);
        dot.setPrefSize(7, 7);
        Label label = new Label(text);
        label.getStyleClass().add("benefit-text");
        HBox row = new HBox(10, dot, label);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static void openInBrowser(String url) {
        // Desktop.browse can block briefly; keep it off the FX thread.
        new Thread(() -> {
            try {
                java.awt.Desktop.getDesktop().browse(URI.create(url));
                logger.info("Opened the checkout page in the system browser.");
            } catch (Exception e) {
                logger.warn("Failed to open the system browser: {}", e.getMessage());
                ValNarratorApplication.showAlert("Couldn't open browser", "Open this link manually:\n" + url);
            }
        }, "open-browser").start();
    }
}
