package org.fpj.javafxcontroller.mainView;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;
import org.fpj.util.AlertService;
import org.fpj.paging.InfinitePager;
import org.fpj.util.UiHelpers;
import org.fpj.navigation.NavigationResponse;
import org.fpj.exceptions.DataNotPresentException;
import org.fpj.navigation.ViewNavigator;
import org.fpj.javafxcontroller.ChatWindowController;
import org.fpj.messaging.application.DirectMessageService;
import org.fpj.messaging.domain.ChatPreview;
import org.fpj.users.application.UserService;
import org.fpj.users.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@Component
public class ChatPreviewController {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final int PAGE_SIZE_CHAT_PREVIEWS = 20;
    private static final int PAGE_PRE_FETCH_THRESHOLD = 5;

    private final ApplicationContext applicationContext;
    private final UserService userService;
    private final DirectMessageService directMessageService;
    private final AlertService alertService;
    private final ViewNavigator viewNavigator;

    private final ObservableList<ChatPreview> chatPreviews = FXCollections.observableArrayList();
    private InfinitePager<ChatPreview> chatPreviewPager;

    @FXML
    private TextField chatsUsernameSearch;

    @FXML
    private ListView<ChatPreview> lvChats;

    private User currentUser;

    @Autowired
    public ChatPreviewController(ApplicationContext applicationContext, UserService userService, DirectMessageService directMessageService, AlertService alertService, ViewNavigator viewNavigator) {
        this.viewNavigator = viewNavigator;
        this.applicationContext = applicationContext;
        this.userService = userService;
        this.directMessageService = directMessageService;
        this.alertService = alertService;
    }

    // <editor-fold defaultstate="collapsed" desc="initialize">
    public void initialize(User currentUser) {
        this.currentUser = Objects.requireNonNull(currentUser, "currentUser must not be null");
        initChatList();
        initPager();
        setUpAutoCompletion();
    }

    private void initPager() {
        chatPreviews.clear();
        lvChats.getItems().clear();

        chatPreviewPager = new InfinitePager<>(PAGE_SIZE_CHAT_PREVIEWS, (pageIndex, pageSize) -> {
            PageRequest pageRequest = PageRequest.of(pageIndex, pageSize);
            return directMessageService.getChatPreviews(currentUser, pageRequest);
        }, page -> chatPreviews.addAll(page.getContent()), ex -> alertService.error("Chat-Übersicht konnte nicht geladen werden: " + (ex != null ? ex.getMessage() : "Unbekannter Fehler")), "chat-preview-loader-");

        chatPreviewPager.resetAndLoadFirstPage();
    }

    private void initChatList() {
        lvChats.setItems(chatPreviews);

        lvChats.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(ChatPreview item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                String titleText = buildTitleText(item);
                String subtitleText = buildSubtitleText(item);
                String timestampText = buildTimestampText(item);

                HBox root = createChatPreviewBox(titleText, subtitleText, timestampText, () -> openChatForPreview(item));
                setGraphic(root);

                int index = getIndex();
                chatPreviewPager.ensureLoadedForIndex(index, chatPreviews.size(), PAGE_PRE_FETCH_THRESHOLD);
            }

            private String buildTitleText(ChatPreview item) {
                String previewName = item.name();
                if (previewName == null) {
                    previewName = "";
                }
                String currentUsername = currentUser.getUsername();
                return previewName.equals(currentUsername) ? "Du" : previewName;
            }

            private String buildSubtitleText(ChatPreview item) {
                String lastMessage = item.lastMessage();
                if (lastMessage == null || lastMessage.isBlank()) {
                    return "Noch keine Nachrichten";
                }

                String lastMsgUsername = item.lastMessageUsername();
                String currentUsername = currentUser.getUsername();

                String senderPrefix = currentUsername.equals(lastMsgUsername) ? "Du: " : lastMsgUsername + ": ";
                String truncatedPrefix = UiHelpers.truncateFull(senderPrefix, 20);
                String truncatedMessage = UiHelpers.truncateFull(lastMessage, 20);

                return truncatedPrefix + truncatedMessage;
            }

            private String buildTimestampText(ChatPreview item) {
                if (item.timestamp() == null) {
                    return "";
                }
                return TIMESTAMP_FORMATTER.format(item.timestamp());
            }
        });
    }

    private void setUpAutoCompletion() {
        AutoCompletionBinding<String> binding = TextFields.bindAutoCompletion(chatsUsernameSearch, request -> {
            String term = request.getUserText();
            if (term == null || term.isBlank()) {
                return List.of();
            }
            return userService.usernameContaining(term);
        });

        binding.setOnAutoCompleted(event -> {
            String selected = event.getCompletion();
            chatsUsernameSearch.setText(selected);
            openChatForUsername(selected);
        });
    }
    // </editor-fold>

    private HBox createChatPreviewBox(String titleText, String subtitleText, String timestampText, Runnable onDoubleClick) {
        Label title = new Label(titleText);
        Label subtitle = new Label(subtitleText);
        VBox left = new VBox(2, title, subtitle);

        Label timestampLabel = new Label(timestampText);
        Region spacer = new Region();

        HBox root = new HBox(8, left, spacer, timestampLabel);
        HBox.setHgrow(spacer, Priority.ALWAYS);

        root.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && onDoubleClick != null) {
                onDoubleClick.run();
            }
        });

        return root;
    }

    private void openChatForPreview(ChatPreview preview) {
        if (preview == null) {
            return;
        }
        openChatForUsername(preview.name());
    }

    private void openChatForUsername(String username) {
        try {
            validateUsername(username);
            User chatPartner = userService.findByUsername(username);
            NavigationResponse<ChatWindowController> chatView = viewNavigator.loadChatView(username);
            if(!chatView.isLoaded()) chatView.controller().openChat(currentUser, chatPartner);
        } catch (IllegalArgumentException | DataNotPresentException ex) {
            alertService.error("Fehler beim Laden des Chatfensters", ex.getMessage());
        } catch (Exception ex) {
            alertService.error( "Unerwarteter Fehler", "Es ist ein unerwarteter Fehler beim Laden des Chatfensters aufgetreten. Bitte versuche es später erneut.");
            System.out.println("Fehler beim Laden des Chatfensters"+ ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Kein Benutzername für den Chat ausgewählt.");
        }
        if (!UiHelpers.isValidEmailBool(username)) {
            throw new IllegalArgumentException("Der eingegebene Benutzername war im falschen Format.");
        }
    }

    @FXML
    private void onReloadChats() {
        chatPreviews.clear();
        if (chatPreviewPager != null) {
            chatPreviewPager.resetAndLoadFirstPage();
        }
    }
}
