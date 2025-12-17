package org.fpj.javafxcontroller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.fpj.util.AlertService;
import org.fpj.paging.InfinitePager;
import org.fpj.util.UiHelpers;
import org.fpj.exportimport.application.DirectMessageCsvExporter;
import org.fpj.exportimport.application.FileHandling;
import org.fpj.messaging.application.DirectMessageService;
import org.fpj.messaging.domain.DirectMessage;
import org.fpj.messaging.domain.DirectMessageRow;
import org.fpj.users.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Component
@Scope(SCOPE_PROTOTYPE)
public class ChatWindowController {

    private static final int PAGE_SIZE_CHAT_MESSAGES = 50;
    private static final double PAGE_PRE_FETCH_THRESHOLD = 0.1;

    private final DirectMessageService directMessageService;
    private final DirectMessageCsvExporter directMessageCsvExporter = new DirectMessageCsvExporter();
    private final AlertService alertService;

    private InfinitePager<DirectMessage> messagesPager;
    private User currentUser;
    private User currentChatPartner;

    @FXML
    private Label lblContact;

    @FXML
    private Button btnExport;

    @FXML
    private VBox vbMessages;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private TextArea taInput;

    @Autowired
    public ChatWindowController(DirectMessageService directMessageService,  AlertService alertService) {
        this.directMessageService = directMessageService;
        this.alertService = alertService;
    }

    // <editor-fold defaultstate="collapsed" desc="initialize">
    @FXML
    public void initialize() {
    }

    public void openChat(User currentUser, User chatPartner) {
        if (currentUser == null || chatPartner == null) {
            alertService.error("Chat kann nicht geöffnet werden", "Benutzer oder Chatpartner ist nicht gesetzt.");
            return;
        }

        this.currentUser = currentUser;
        this.currentChatPartner = chatPartner;

        setUpContactLabel();
        setupScrollPanel();
        initChatMessages();
        scrollToBottom();
    }

    private void initChatMessages() {
        vbMessages.getChildren().clear();

        messagesPager = new InfinitePager<>(
                PAGE_SIZE_CHAT_MESSAGES,
                (pageIndex, pageSize) -> {
                    PageRequest pageRequest = PageRequest.of(pageIndex, pageSize);
                    return directMessageService.getConversation(currentUser, this.currentChatPartner, pageRequest);
                },
                page -> {
                    List<DirectMessage> desc = page.getContent();

                    int beforeCount = this.vbMessages.getChildren().size();
                    double oldV = this.scrollPane.getVvalue();

                    for (DirectMessage msg : desc) {
                        addMessageNode(msg, true);
                    }

                    int afterCount = this.vbMessages.getChildren().size();
                    if (afterCount > 0 && beforeCount > 0) {
                        double ratio = (double) beforeCount / (double) afterCount;
                        double newV = ratio * oldV + (1.0 - ratio);
                        this.scrollPane.setVvalue(newV);
                    }

                    if (beforeCount == 0 && afterCount > 0) {
                        scrollToBottom();
                    }
                },
                ex -> alertService.error("Chat-Nachrichten konnten nicht geladen werden: " + (ex != null ? ex.getMessage() : "Unbekannter Fehler")),
                "chat-messages-loader-"
        );

        messagesPager.resetAndLoadFirstPage();
    }

    private void setupScrollPanel() {
        if (scrollPane == null) {
            return;
        }

        scrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() <= PAGE_PRE_FETCH_THRESHOLD && messagesPager != null) {
                messagesPager.ensureLoadedForScroll();
            }
        });
    }
    // </editor-fold>

    @FXML
    private void send() {
        if (currentUser == null || currentChatPartner == null) {
            alertService.error("Nachricht kann nicht gesendet werden", "Benutzer oder Chatpartner ist nicht gesetzt.");
            return;
        }

        String input = taInput.getText();
        if (input == null || input.isBlank()) {
            return;
        }

        input = UiHelpers.truncate(input, input.length());
        DirectMessageRow row = new DirectMessageRow(this.currentUser, this.currentChatPartner, input);
        DirectMessage directMessage = directMessageService.addDirectMessage(row);

        addMessageNode(directMessage, false);
        taInput.clear();
        Platform.runLater(() -> vbMessages.layout());
        scrollToBottom();
    }

    @FXML
    private void exportChat() {
        if (currentUser == null || currentChatPartner == null) {
            alertService.error("Export nicht möglich", "Benutzer oder Chatpartner ist nicht gesetzt.");
            return;
        }

        try {
            if (directMessageCsvExporter.isRunning()) {
                alertService.error("Eine andere Export-Instanz läuft noch. Warte bitte, bis diese abgeschlossen ist.");
                return;
            }

            Window window = btnExport.getScene().getWindow();
            String path = FileHandling.openFileChooserAndGetPath(window);

            if (path == null) {
                throw new IllegalStateException("Das Auswählen des Dateipfades ist fehlgeschlagen.");
            }

            List<DirectMessage> messages = directMessageService.getConversationMessageList(this.currentUser.getId(), this.currentChatPartner.getId());
            directMessageCsvExporter.export(messages.iterator(), FileHandling.openFileAsOutStream(path));

            alertService.info("Export erfolgreich","Der Export der Nachrichten war erfolgreich. Du findest die Einträge in: " + path);
        } catch (IllegalArgumentException e) {
            alertService.error("Export fehlgeschlagen", "Fehler beim Exportieren der Nachrichten: " + e.getMessage());
        } catch (Exception e) {
            alertService.error("Unerwarteter Fehler", "Ein unbekannter Fehler ist aufgetreten: " + e.getMessage());
        }
    }

    @FXML
    private void onReloadChat() {
        vbMessages.getChildren().clear();
        if (messagesPager != null) {
            messagesPager.resetAndLoadFirstPage();
        }
        scrollToBottom();
    }

    private void setUpContactLabel() {
        String username = this.currentChatPartner.getUsername();
        if (username == null) {
            lblContact.setText("Unbekannt");
            return;
        }

        if (username.equals(currentUser.getUsername())) {
            lblContact.setText("Du");
        } else {
            lblContact.setText(UiHelpers.usernameFromEmail(username));
        }
    }

    private void addMessageNode(DirectMessage msg, boolean prepend) {
        if (msg == null) {
            return;
        }

        boolean outgoing = isOutgoing(msg);
        String text = msg.getContent() != null ? msg.getContent() : "";
        String timestamp = UiHelpers.formatInstant(msg.getCreatedAt());

        HBox row = createMessageNode(text, timestamp, outgoing);

        if (prepend) {
            vbMessages.getChildren().add(0, row);
        } else {
            vbMessages.getChildren().add(row);
        }
    }

    private HBox createMessageNode(String text, String timestamp, boolean outgoing) {
        HBox row = new HBox(8);
        row.getStyleClass().add("message-row");
        row.getStyleClass().add(outgoing ? "outgoing" : "incoming");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox bubble = new VBox(2);
        bubble.getStyleClass().add("message-bubble");
        bubble.getStyleClass().add(outgoing ? "outgoing" : "incoming");

        Label lblText = new Label(text);
        lblText.setWrapText(true);
        lblText.setMaxWidth(440);
        lblText.getStyleClass().add("message-text");

        Label lblTimestamp = new Label(timestamp);
        lblTimestamp.getStyleClass().add("message-meta");

        bubble.getChildren().addAll(lblText, lblTimestamp);

        if (outgoing) {
            row.getChildren().addAll(spacer, bubble);
        } else {
            row.getChildren().addAll(bubble, spacer);
        }

        return row;
    }

    private boolean isOutgoing(DirectMessage msg) {
        if (currentUser == null || msg == null || msg.getSender() == null) {
            return false;
        }
        return currentUser.getId().equals(msg.getSender().getId());
    }

    private void scrollToBottom() {
        if (scrollPane == null) {
            return;
        }
        scrollPane.layout();
        scrollPane.setVvalue(1.0);
    }
}
