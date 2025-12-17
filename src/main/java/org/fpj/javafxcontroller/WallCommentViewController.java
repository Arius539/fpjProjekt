package org.fpj.javafxcontroller;

import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;
import org.fpj.util.AlertService;
import org.fpj.paging.InfinitePager;
import org.fpj.util.UiHelpers;
import org.fpj.exceptions.DataNotPresentException;
import org.fpj.exportimport.application.WallCommentCsvExporter;
import org.fpj.exportimport.application.FileHandling;
import org.fpj.users.application.UserService;
import org.fpj.users.domain.User;
import org.fpj.wall.application.WallCommentService;
import org.fpj.wall.domain.WallComment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Component
@Scope(SCOPE_PROTOTYPE)
public class WallCommentViewController {

    private static final int PAGE_SIZE_COMMENTS = 30;
    private static final double PAGE_PRE_FETCH_THRESHOLD = 0.1;
    private static final int COLUMNS = 3;

    private final WallCommentService wallCommentService;
    private final UserService userService;
    private final AlertService alertService;
    private final WallCommentCsvExporter wallCommentCsvExporter = new WallCommentCsvExporter();

    @FXML
    private TextField searchField;

    @FXML
    private Label headlineLabel;

    @FXML
    private GridPane commentGrid;

    @FXML
    private TextArea newCommentTextArea;

    @FXML
    private Button sendButton;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private RadioButton rbWrittenBy;

    @FXML
    private RadioButton rbWrittenFor;

    @FXML
    private Button exportButton;

    @FXML
    private Button reloadButton;

    @FXML
    private ToggleGroup commentFilterToggleGroup;

    private InfinitePager<WallComment> commentsPager;
    private User currentUser;
    private User wallOwner;
    private AutoCompletionBinding<String> autoCompletionBinding;

    @Autowired
    public WallCommentViewController(WallCommentService wallCommentService, UserService userService, AlertService alertService) {
        this.wallCommentService = wallCommentService;
        this.userService = userService;
        this.alertService = alertService;

    }

    // <editor-fold defaultstate="collapsed" desc="initialize">
    @FXML
    private void initialize() {
    }

    public void load(User currentUser, User wallCommentOwner) {
        this.currentUser = currentUser;
        this.wallOwner = wallCommentOwner;
        setUpAutoCompletion();
        openWall();
    }

    private void openWall() {
        setHeadlineLabel();
        initComments();
        setupScrollPanel();
    }

    private void reload() {
        reloadComments();
        setHeadlineLabel();
    }

    private void reloadComments() {
        commentGrid.getChildren().clear();
        if (commentsPager != null) {
            commentsPager.resetAndLoadFirstPage();
            setupScrollPanel();
        } else {
            initComments();
            setupScrollPanel();
        }
    }

    private void initComments() {
        commentGrid.getChildren().clear();

        commentsPager = new InfinitePager<>(
                PAGE_SIZE_COMMENTS,
                (pageIndex, pageSize) -> {
                    PageRequest pageRequest = PageRequest.of(pageIndex, pageSize);
                    if (isGetByAuthor()) {
                        return wallCommentService.getWallCommentsByAuthor(wallOwner.getId(), pageRequest);
                    } else {
                        return wallCommentService.getWallCommentsCreatedByWallOwner(wallOwner.getId(), pageRequest);
                    }
                },
                page -> {
                    List<WallComment> content = page.getContent();
                    int beforeCount = commentGrid.getChildren().size();

                    for (int i = 0; i < content.size(); i++) {
                        WallComment c = content.get(i);
                        int logicalIndex = beforeCount + i;
                        int col = logicalIndex % COLUMNS;
                        int row = logicalIndex / COLUMNS;

                        Node card = createCommentCard(c);
                        commentGrid.add(card, col, row);
                        GridPane.setFillWidth(card, false);
                        GridPane.setHgrow(card, Priority.NEVER);
                        GridPane.setVgrow(card, Priority.NEVER);
                        GridPane.setHalignment(card, HPos.LEFT);
                        GridPane.setValignment(card, VPos.TOP);
                    }
                },
                ex -> alertService.error("Pinnwand-Kommentare konnten nicht geladen werden: " + (ex != null ? ex.getMessage() : "Unbekannter Fehler")),
                "wall-comments-loader-"
        );

        commentsPager.resetAndLoadFirstPage();
    }

    private void setupScrollPanel() {
        if (scrollPane == null) {
            return;
        }

        scrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() >= 1.0 - PAGE_PRE_FETCH_THRESHOLD && commentsPager != null) {
                commentsPager.ensureLoadedForScroll();
            }
        });
    }

    private void setUpAutoCompletion() {
        if (autoCompletionBinding != null) {
            autoCompletionBinding.dispose();
            autoCompletionBinding = null;
        }

        autoCompletionBinding = TextFields.bindAutoCompletion(searchField, request -> {
            String term = request.getUserText();
            if (term == null || term.isBlank()) {
                return List.of();
            }
            return userService.usernameContaining(term);
        });

        autoCompletionBinding.setOnAutoCompleted(event -> {
            String selected = event.getCompletion();
            searchField.setText(selected);
            openWallForUsername(selected);
        });
    }
    // </editor-fold>

    @FXML
    private void onSendClicked() {
        String input = newCommentTextArea.getText();
        if (input == null || input.isBlank()) {
            return;
        }

        try {
            WallComment comment = new WallComment();
            comment.setContent(UiHelpers.truncate(input, input.length()));
            comment.setAuthor(currentUser);
            comment.setWallOwner(wallOwner);
            WallComment created = wallCommentService.add(comment);
            newCommentTextArea.clear();
           if(!isGetByAuthor()) addComment(created);
        } catch (IllegalArgumentException e) {
            alertService.error("Kommentar konnte nicht gespeichert werden", "Kommentar konnte nicht gespeichert werden: " + e.getMessage());
        } catch (Exception e) {
            alertService.error("Unerwarteter Fehler", "Es ist ein unerwarteter Fehler aufgetreten: Kommentar konnte nicht gespeichert werden: " + e.getMessage());
        }
    }

    @FXML
    private void onReloadWall() {
        reloadComments();
    }

    @FXML
    private void onExport() {
        if (currentUser == null || wallOwner == null) {
            alertService.error("Export nicht möglich", "Benutzer oder Pinnwand-Besitzer ist nicht gesetzt.");
            return;
        }

        try {
            if (!currentUser.getUsername().equals(wallOwner.getUsername())) {
                throw new IllegalArgumentException("Du kannst nur die Pinnwandkommentare an deiner eigenen Pinnwand exportieren.");
            }
            if (wallCommentCsvExporter.isRunning()) {
                throw new IllegalStateException("Eine andere Export-Instanz läuft noch. Warte bitte, bis diese abgeschlossen ist.");
            }

            Window window = exportButton.getScene().getWindow();
            String path = FileHandling.openFileChooserAndGetPath(window);
            if (path == null) {
                throw new IllegalStateException("Das Auswählen des Dateipfades ist fehlgeschlagen.");
            }

            List<WallComment> comments = isGetByAuthor()
                    ? wallCommentService.toListByAuthor(currentUser.getId())
                    : wallCommentService.toListByWallOwner(currentUser.getId());

            wallCommentCsvExporter.export(comments.iterator(), FileHandling.openFileAsOutStream(path));
            alertService.info("Export erfolgreich", "Der Export der Pinnwandkommentare war erfolgreich. Du findest die Einträge in: " + path);
        } catch (IllegalArgumentException | IllegalStateException e) {
            alertService.error("Export fehlgeschlagen", "Fehler beim Exportieren der Pinnwandkommentare: " + e.getMessage());
        } catch (Exception e) {
            alertService.error("Unerwarteter Fehler", "Ein unbekannter Fehler ist aufgetreten: " + e.getMessage());
        }
    }

    @FXML
    private void onToCurrentUserWallClicked() {
        this.wallOwner = this.currentUser;
        this.openWall();
    }

    @FXML
    private void selectionTypeChanged() {
        reloadComments();
    }

    private void openWallForUsername(String username) {
        try {
            this.wallOwner = userService.findByUsername(username);
            reload();
        } catch (DataNotPresentException e) {
            alertService.error("Pinnwand konnte nicht geladen werden", "Es ist ein Fehler beim Laden der Pinnwand aufgetreten, versuche es erneut oder starte die Anwendung neu.");
        } catch (Exception e) {
            alertService.error("Unerwarteter Fehler", "Es ist ein unerwarteter Fehler beim Laden der Pinnwand aufgetreten, versuche es erneut oder starte die Anwendung neu.");
        }
    }

    private boolean isGetByAuthor() {
        return rbWrittenBy != null && rbWrittenBy.isSelected();
    }

    private void setHeadlineLabel() {
        if (headlineLabel == null || wallOwner == null) {
            return;
        }

        String ownerName = wallOwner.getUsername();
        String currentName = currentUser != null ? currentUser.getUsername() : null;

        if (currentName != null && currentName.equals(ownerName)) {
            headlineLabel.setText("Deine Pinnwand".toUpperCase());
        } else {
            headlineLabel.setText(UiHelpers.usernameFromEmail(ownerName).toUpperCase());
        }
    }

    private void addComment(WallComment comment) {
        int startIndex = commentGrid.getChildren().size();
        int col = startIndex % COLUMNS;
        int row = startIndex / COLUMNS;

        Node card = createCommentCard(comment);
        commentGrid.add(card, col, row);
        GridPane.setFillWidth(card, false);
        GridPane.setHgrow(card, Priority.NEVER);
        GridPane.setVgrow(card, Priority.NEVER);
        GridPane.setHalignment(card, HPos.LEFT);
        GridPane.setValignment(card, VPos.TOP);
    }

    private Node createCommentCard(WallComment comment) {
        VBox box = new VBox(8);
        box.getStyleClass().add("comment-card");
        box.setMaxWidth(Region.USE_PREF_SIZE);
        box.setMaxHeight(Region.USE_PREF_SIZE);
        box.setFillWidth(true);

        Label textLabel = new Label(comment.getContent());
        textLabel.setWrapText(true);
        textLabel.setMaxWidth(Region.USE_PREF_SIZE);
        VBox.setVgrow(textLabel, Priority.NEVER);

        String metaText = buildMetaText(comment);
        Label metaLabel = new Label(metaText);
        metaLabel.getStyleClass().add("comment-meta-label");

        metaLabel.setOnMouseClicked(event -> onMetaClicked(comment));
        metaLabel.setOnMouseEntered(e -> metaLabel.getStyleClass().add("hover"));
        metaLabel.setOnMouseExited(e -> metaLabel.getStyleClass().remove("hover"));

        box.getChildren().addAll(textLabel, metaLabel);
        return box;
    }

    private String buildMetaText(WallComment comment) {
        String rawName;
        if (isGetByAuthor()) {
            rawName = comment.getWallOwner() != null ? comment.getWallOwner().getUsername() : "Unbekannt";
        } else {
            rawName = comment.getAuthor() != null ? comment.getAuthor().getUsername() : "Unbekannt";
        }

        String authorName = currentUser != null && rawName.equals(currentUser.getUsername()) ? "Du" : rawName;
        return authorName + " · " + UiHelpers.formatInstantToDate(comment.getCreatedAt());
    }

    private void onMetaClicked(WallComment comment) {
        User toUser = isGetByAuthor() ? comment.getWallOwner() : comment.getAuthor();
        if (toUser == null) {
            return;
        }
        this.wallOwner = toUser;
        reload();
    }
}
