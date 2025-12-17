package org.fpj.javafxcontroller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.fpj.navigation.ViewNavigator;
import org.fpj.util.AlertService;
import org.fpj.exportimport.domain.CsvError;
import org.fpj.exportimport.domain.CsvImportResult;
import org.fpj.exportimport.domain.CsvReader;
import org.fpj.exportimport.application.FileHandling;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;

@Component
public class CsvImportDialogController<E> {

    private final ObservableList<CsvError> errorList = FXCollections.observableArrayList();
    private final AlertService alertService;

    @FXML
    private Button chooseFileButton;

    @FXML
    private Label selectedFileLabel;

    @FXML
    private ListView<CsvError> errorListView;

    private String selectedFilePath;
    private CsvReader csvReader;
    private Consumer<List<E>> csvImportConsumer;

    private final ViewNavigator  viewNavigator;

    @Autowired
    public CsvImportDialogController(AlertService alertService, ViewNavigator viewNavigator) {
        this.alertService = alertService;
        this.viewNavigator = viewNavigator;

    }

    // <editor-fold defaultstate="collapsed" desc="initialize">
    @FXML
    private void initialize() {
    }

    public void initialize(CsvReader csvReader, Consumer<List<E>> csvImportConsumer) {
        if (csvReader == null) {
            throw new IllegalStateException("csvReader is null");
        }
        this.csvReader = csvReader;
        this.csvImportConsumer = csvImportConsumer;
        this.selectedFilePath = null;

        selectedFileLabel.setText("Keine Datei ausgewählt");
        errorList.clear();
        errorListView.setItems(errorList);

        initErrorList();
    }
    // </editor-fold>

    @FXML
    private void onChooseFile() {
        Window window = chooseFileButton.getScene().getWindow();
        String path = FileHandling.openFileChooserAndGetPath(window);
        if (path != null) {
            this.selectedFilePath = path;
            selectedFileLabel.setText(path);
        }
    }

    @FXML
    private void onStartImport() {
        errorList.clear();

        if (selectedFilePath == null) {
            alertService.error("Keine Datei ausgewählt", "Bitte zuerst eine CSV-Datei auswählen.");
            return;
        }

        if (csvReader == null || csvImportConsumer == null) {
            alertService.error("Import nicht möglich", "Importer wurde nicht korrekt initialisiert.");
            return;
        }

        if (csvReader.isRunning()) {
            alertService.error("Import nicht möglich", "Importer läuft bereits, bitte warte auf die Verarbeitung");
            return;
        }


        Task<CsvImportResult<E>> importTask = new Task<>() {
            @Override
            protected CsvImportResult<E> call() throws Exception {
                InputStream inputStream = FileHandling.openFileAsStream(selectedFilePath);
                return csvReader.parse(inputStream);
            }
        };

        importTask.setOnSucceeded(event1 -> {
            CsvImportResult<E> result = importTask.getValue();
            if (result.getErrors().isEmpty()) {
                alertService.info("Import erfolgreich", "Der CSV import war erfolgreich.");
                csvImportConsumer.accept(result.getRecords());
                this.viewNavigator.closeCsvDialog();
            } else {
                errorList.addAll(result.getErrors());
            }
        });

        importTask.setOnFailed(event1 -> {
            Throwable ex = importTask.getException();
            alertService.error("Unerwarteter Fehler", "Unerwarteter Fehler: " + ex.getMessage());
        });

        new Thread(importTask).start();
    }

    private void initErrorList() {
        errorListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(CsvError item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    setTooltip(null);
                    setStyle("");
                    return;
                }

                String severityText = severityText(item.getSeverity());
                String columnPart = item.getColumnName() != null ? ", Spalte \"" + item.getColumnName() + "\"" : "";
                String titleText = severityText + " in Zeile " + item.getLine() + columnPart;
                String subtitleText = item.getMessage();
                String raw = item.getRawValue();
                String rawDisplay = raw != null && !raw.isBlank() ? raw : "";

                HBox root = createErrorRowBox(titleText, subtitleText, rawDisplay, item.getSeverity());
                setGraphic(root);
                setTooltip(new Tooltip(buildTooltipText(item, severityText, raw)));
                setStyle(backgroundStyle(item.getSeverity()));
            }
        });
    }

    private HBox createErrorRowBox(String titleText, String subtitleText, String rawDisplay, CsvError.Severity severity) {
        Label title = new Label(titleText);
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;" + severityColor(severity));

        Label subtitle = new Label(subtitleText);
        subtitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");

        Label value = new Label(rawDisplay);

        VBox left = new VBox(2, title, subtitle);
        Region spacer = new Region();
        HBox root = new HBox(12, left, spacer, value);

        HBox.setHgrow(spacer, Priority.ALWAYS);

        return root;
    }

    private String severityText(CsvError.Severity severity) {
        return switch (severity) {
            case FATAL -> "FATAL";
            case ERROR -> "ERROR";
            case WARNING -> "WARNUNG";
        };
    }

    private String severityColor(CsvError.Severity severity) {
        return switch (severity) {
            case FATAL -> "-fx-text-fill: #c0392b;";
            case ERROR -> "-fx-text-fill: #b8860b;";
            case WARNING -> "-fx-text-fill: #2980b9;";
        };
    }

    private String backgroundStyle(CsvError.Severity severity) {
        return switch (severity) {
            case FATAL -> "-fx-background-color: #ffeaea;";
            case ERROR -> "-fx-background-color: #fff3cd;";
            case WARNING -> "-fx-background-color: #e7f7fd;";
        };
    }

    private String buildTooltipText(CsvError item, String severityText, String raw) {
        StringBuilder sb = new StringBuilder();
        sb.append("Zeile: ").append(item.getLine());
        if (item.getColumnName() != null) {
            sb.append("\nSpalte: ").append(item.getColumnName());
        }
        if (item.getColumnIndex() != null) {
            sb.append("\nSpaltenindex: ").append(item.getColumnIndex());
        }
        sb.append("\nSchweregrad: ").append(severityText);
        sb.append("\nNachricht: ").append(item.getMessage());
        if (raw != null && !raw.isBlank()) {
            sb.append("\nWert: ").append(raw);
        }
        return sb.toString();
    }
}
