package com.jprcoder.valnarratorgui;

import com.jprcoder.valnarratorbackend.ChatUtilityHandler;
import com.jprcoder.valnarratorbackend.VoiceGenerator;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Standalone editor window for the configurable short-form -> full-form table used
 * by {@link ChatUtilityHandler}. Built programmatically so it can layer over the
 * fixed main window without touching its FXML. Edits are applied to the live table
 * and persisted to {@code config.json} on save.
 */
public final class FullFormsManager {
    private static final Logger logger = LoggerFactory.getLogger(FullFormsManager.class);
    private static Stage stage;

    private FullFormsManager() {
    }

    /**
     * Opens the editor (or brings it to the front if already open). FX-thread safe.
     */
    public static void show() {
        if (Platform.isFxApplicationThread()) {
            open();
        } else {
            Platform.runLater(FullFormsManager::open);
        }
    }

    private static void open() {
        if (stage != null && stage.isShowing()) {
            stage.toFront();
            return;
        }

        ObservableList<Row> rows = FXCollections.observableArrayList();
        ChatUtilityHandler.getFullForms().forEach((k, v) -> rows.add(new Row(k, v)));

        TableView<Row> table = buildTable(rows);
        table.getStyleClass().add("app-table");
        VBox.setVgrow(table, Priority.ALWAYS);

        // Hero band (consistent with the rest of the app).
        Label title = new Label("Full-forms");
        title.getStyleClass().add("sub-modal-title");
        Label subtitle = new Label("Expand abbreviations into spoken phrases before narration. Matched whole-word and case-insensitive.");
        subtitle.getStyleClass().add("sub-subtitle");
        subtitle.setWrapText(true);
        VBox hero = new VBox(4, title, subtitle);
        hero.getStyleClass().add("sub-hero");
        hero.setPadding(new Insets(20, 24, 16, 24));

        // Add-entry row.
        TextField shortInput = new TextField();
        shortInput.setPromptText("Short-form (e.g. GLHF)");
        shortInput.setPrefWidth(150);
        TextField fullInput = new TextField();
        fullInput.setPromptText("Full-form (e.g. Good luck, have fun)");
        HBox.setHgrow(fullInput, Priority.ALWAYS);
        Button addButton = new Button("Add");
        addButton.getStyleClass().add("primary-button");
        Runnable addAction = () -> addRow(rows, table, shortInput, fullInput);
        addButton.setOnAction(e -> addAction.run());
        fullInput.setOnAction(e -> addAction.run());
        HBox inputRow = new HBox(8, shortInput, fullInput, addButton);
        inputRow.setAlignment(Pos.CENTER_LEFT);

        // Action buttons.
        Button removeButton = new Button("Remove selected");
        removeButton.getStyleClass().add("ghost-button");
        removeButton.setOnAction(e -> {
            Row selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) rows.remove(selected);
        });
        Button resetButton = new Button("Reset to defaults");
        resetButton.getStyleClass().add("ghost-button");
        resetButton.setOnAction(e -> {
            rows.clear();
            ChatUtilityHandler.getDefaultFullForms().forEach((k, v) -> rows.add(new Row(k, v)));
        });
        Button saveButton = new Button("Save");
        saveButton.getStyleClass().add("primary-button");
        saveButton.setDefaultButton(true);
        saveButton.setOnAction(e -> {
            save(rows);
            stage.close();
        });
        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().add("ghost-button");
        cancelButton.setCancelButton(true);
        cancelButton.setOnAction(e -> stage.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox buttonRow = new HBox(8, removeButton, resetButton, spacer, cancelButton, saveButton);
        buttonRow.setAlignment(Pos.CENTER_LEFT);

        VBox body = new VBox(12, table, inputRow, buttonRow);
        body.setPadding(new Insets(16, 24, 20, 24));
        VBox.setVgrow(body, Priority.ALWAYS);

        VBox root = new VBox(hero, body);
        root.getStyleClass().add("sub-root");

        Scene scene = new Scene(root, 580, 540);
        var css = FullFormsManager.class.getResource("app.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        stage = new Stage();
        stage.setTitle("Valorant Narrator - Full-forms");
        stage.setScene(scene);
        stage.setMinWidth(480);
        stage.setMinHeight(420);
        var icon = FullFormsManager.class.getResourceAsStream("appIcon.png");
        if (icon != null) stage.getIcons().add(new javafx.scene.image.Image(icon));
        stage.show();
        stage.toFront();
    }

    private static TableView<Row> buildTable(ObservableList<Row> rows) {
        TableView<Row> table = new TableView<>(rows);
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No full-forms configured."));

        TableColumn<Row, String> shortCol = new TableColumn<>("Short-form");
        shortCol.setCellValueFactory(c -> c.getValue().shortFormProperty());
        shortCol.setCellFactory(TextFieldTableCell.forTableColumn());
        shortCol.setOnEditCommit(ev -> {
            String value = ev.getNewValue() == null ? "" : ev.getNewValue().trim();
            ev.getRowValue().setShortForm(value);
        });
        shortCol.setMaxWidth(160);
        shortCol.setMinWidth(110);

        TableColumn<Row, String> fullCol = new TableColumn<>("Full-form (spoken)");
        fullCol.setCellValueFactory(c -> c.getValue().fullFormProperty());
        fullCol.setCellFactory(TextFieldTableCell.forTableColumn());
        fullCol.setOnEditCommit(ev -> ev.getRowValue().setFullForm(ev.getNewValue() == null ? "" : ev.getNewValue()));

        table.getColumns().add(shortCol);
        table.getColumns().add(fullCol);
        return table;
    }

    private static void addRow(ObservableList<Row> rows, TableView<Row> table, TextField shortInput, TextField fullInput) {
        String shortForm = shortInput.getText() == null ? "" : shortInput.getText().trim();
        String fullForm = fullInput.getText() == null ? "" : fullInput.getText().trim();
        if (shortForm.isEmpty() || fullForm.isEmpty()) return;

        // Update an existing key in place (case-insensitive) instead of duplicating.
        for (Row row : rows) {
            if (row.getShortForm().equalsIgnoreCase(shortForm)) {
                row.setFullForm(fullForm);
                table.getSelectionModel().select(row);
                shortInput.clear();
                fullInput.clear();
                return;
            }
        }
        Row added = new Row(shortForm, fullForm);
        rows.add(added);
        table.getSelectionModel().select(added);
        table.scrollTo(added);
        shortInput.clear();
        fullInput.clear();
    }

    private static void save(ObservableList<Row> rows) {
        Map<String, String> map = new LinkedHashMap<>();
        for (Row row : rows) {
            String key = row.getShortForm().trim();
            if (!key.isEmpty() && !map.containsKey(key)) {
                map.put(key, row.getFullForm());
            }
        }
        ChatUtilityHandler.setFullForms(map);
        try {
            VoiceGenerator.getInstance().saveConfig();
            logger.info("Saved {} full-form entries.", map.size());
        } catch (IOException e) {
            logger.error("Failed to persist full-forms: {}", e.getMessage());
            ValNarratorApplication.showAlert("Save Failed", "Could not save full-forms: " + e.getMessage());
        }
    }

    /**
     * Editable table row backing a single short-form/full-form pair.
     */
    public static final class Row {
        private final StringProperty shortForm;
        private final StringProperty fullForm;

        Row(String shortForm, String fullForm) {
            this.shortForm = new SimpleStringProperty(Objects.requireNonNullElse(shortForm, ""));
            this.fullForm = new SimpleStringProperty(Objects.requireNonNullElse(fullForm, ""));
        }

        public StringProperty shortFormProperty() {
            return shortForm;
        }

        public StringProperty fullFormProperty() {
            return fullForm;
        }

        public String getShortForm() {
            return shortForm.get();
        }

        public void setShortForm(String value) {
            shortForm.set(value);
        }

        public String getFullForm() {
            return fullForm.get();
        }

        public void setFullForm(String value) {
            fullForm.set(value);
        }
    }
}
