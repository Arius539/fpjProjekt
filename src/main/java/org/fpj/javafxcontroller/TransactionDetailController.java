package org.fpj.javafxcontroller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.fpj.navigation.api.NavigationAware;
import org.fpj.navigation.api.NavigationHandle;
import org.fpj.navigation.api.ViewNavigator;
import org.fpj.navigation.api.ViewOpenMode;
import org.fpj.navigation.fx.NavigationMenuBinder;
import org.fpj.payments.domain.TransactionLite;
import org.fpj.users.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Component
@Scope(SCOPE_PROTOTYPE)
public class TransactionDetailController implements NavigationAware {


    public StackPane typeBox;
    @FXML
    private StackPane senderBox;

    @FXML
    private StackPane empfaengerBox;

    @FXML
    private StackPane betragBox;

    @FXML
    private StackPane verwendungszweckBox;

    @FXML
    private Label senderLabel;

    @FXML
    private Label empfaengerLabel;

    @FXML
    private Label betragLabel;

    @FXML
    private Label verwendungszweckLabel;

    @FXML
    private Label transaktionsTyp;

    private TransactionLite transaction;
    private BiConsumer<TransactionLite, ViewOpenMode> onSenderClicked;
    private BiConsumer<TransactionLite, ViewOpenMode> onRecipientClicked;
    private Consumer<TransactionLite> onReuseClicked;
    private BiConsumer<TransactionLite, ViewOpenMode> onDescriptionClicked;
    private BiConsumer<TransactionLite, ViewOpenMode> onValueClicked;
    private Consumer<TransactionLite> onTransactionTypeClicked;
    private User currentUser;
    private NavigationHandle navigationHandle;
    private ViewOpenMode transactionViewDefaultOpenMode = ViewOpenMode.REPLACE_MAIN_CONTENT;
    @Autowired
    private ViewNavigator viewNavigator;

    // <editor-fold defaultstate="collapsed" desc="initialize">
    @FXML
    private void initialize() {
    }

    public void initialize(TransactionLite transaction,
                           User currentUser,
                           BiConsumer<TransactionLite, ViewOpenMode> onSenderClicked,
                           BiConsumer<TransactionLite, ViewOpenMode> onRecipientClicked,
                           Consumer<TransactionLite> onReuseClicked,
                           BiConsumer<TransactionLite, ViewOpenMode> onDescriptionClicked,
                           BiConsumer<TransactionLite, ViewOpenMode> onValueClicked,
                           ViewOpenMode transactionViewDefaultOpenMode) {
        this.currentUser = currentUser;
        this.transaction = transaction;
        this.onSenderClicked = onSenderClicked;
        this.onRecipientClicked = onRecipientClicked;
        this.onReuseClicked = onReuseClicked;
        this.onDescriptionClicked = onDescriptionClicked;
        this.onValueClicked = onValueClicked;
        this.onTransactionTypeClicked= null;
        this.transactionViewDefaultOpenMode = transactionViewDefaultOpenMode != null
                ? transactionViewDefaultOpenMode
                : ViewOpenMode.REPLACE_MAIN_CONTENT;
        updateView();
        updateClickability();
        bindNavigationTargets();
    }
    // </editor-fold>

    @FXML
    private void onReuseClicked() {
        handle(onReuseClicked);
    }

    private void updateView() {
        if (transaction == null || currentUser == null) {
            senderLabel.setText("Sender");
            empfaengerLabel.setText("Empfänger");
            betragLabel.setText("Betrag");
            verwendungszweckLabel.setText("Verwendungszweck");
            transaktionsTyp.setText("Transaktionstyp");
            return;
        }

        String sender = buildPartyLabel(transaction.senderUsername(), "Sender unbekannt");
        String recipient = buildPartyLabel(transaction.recipientUsername(), "Empfänger unbekannt");
        String description = (transaction.description() == null || transaction.description().isBlank()) ? "Kein Verwendungszweck" : transaction.description();
        String transactionType = sender.equals("Sender unbekannt")? "Einzahlung": recipient.equals("Empfänger unbekannt")? "Einzahlung": "Überweisung";

        transaktionsTyp.setText(transactionType);
        senderLabel.setText(sender);
        empfaengerLabel.setText(recipient);
        betragLabel.setText(transaction.amountStringUnsigned());
        verwendungszweckLabel.setText(description);
    }

    private String buildPartyLabel(String username, String unknownLabel) {
        if (username == null || username.isBlank()) {
            return unknownLabel;
        }
        if (currentUser != null && username.equals(currentUser.getUsername())) {
            return "Du";
        }
        return username;
    }

    private void updateClickability() {
        setClickableStyle(typeBox, onTransactionTypeClicked!= null);
        setClickableStyle(senderBox, onSenderClicked != null);
        setClickableStyle(empfaengerBox, onRecipientClicked != null);
        setClickableStyle(betragBox, onValueClicked != null);
        setClickableStyle(verwendungszweckBox, onDescriptionClicked != null);
    }

    private void bindNavigationTargets() {
        NavigationMenuBinder.attach(senderBox, 1, this::resolveTransactionViewDefaultMode, openMode -> handle(onSenderClicked, openMode));
        NavigationMenuBinder.attach(empfaengerBox, 1, this::resolveTransactionViewDefaultMode, openMode -> handle(onRecipientClicked, openMode));
        NavigationMenuBinder.attach(betragBox, 1, this::resolveTransactionViewDefaultMode, openMode -> handle(onValueClicked, openMode));
        NavigationMenuBinder.attach(verwendungszweckBox, 1, this::resolveTransactionViewDefaultMode, openMode -> handle(onDescriptionClicked, openMode));
    }

    private ViewOpenMode resolveTransactionViewDefaultMode() {
        if (viewNavigator == null) {
            return transactionViewDefaultOpenMode;
        }
        return viewNavigator.resolveChildDefaultOpenMode(transactionViewDefaultOpenMode, navigationHandle);
    }

    private void setClickableStyle(StackPane pane, boolean clickable) {
        if (pane == null) {
            return;
        }
        pane.getStyleClass().removeAll("clickable-disabled", "clickable-enabled");
        pane.getStyleClass().add(clickable ? "clickable-enabled" : "clickable-disabled");
        pane.setMouseTransparent(!clickable);
    }

    private void handle(BiConsumer<TransactionLite, ViewOpenMode> handler, ViewOpenMode openMode) {
        if (handler == null || transaction == null) {
            return;
        }
        handler.accept(transaction, openMode);
    }

    private void handle(Consumer<TransactionLite> handler) {
        if (handler == null || transaction == null) {
            return;
        }
        handler.accept(transaction);
    }

    private void closeCurrentStage() {
        if (senderBox == null || senderBox.getScene() == null) {
            return;
        }
        Stage stage = (Stage) senderBox.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }

    @Override
    public void setNavigationHandle(NavigationHandle navigationHandle) {
        this.navigationHandle = navigationHandle;
    }

    public NavigationHandle navigationHandle() {
        return navigationHandle;
    }
}
