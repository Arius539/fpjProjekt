package org.fpj.javafxcontroller.mainView;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.controlsfx.control.textfield.TextFields;
import org.fpj.util.AlertService;
import org.fpj.paging.InfinitePager;
import org.fpj.util.UiHelpers;
import org.fpj.navigation.NavigationResponse;
import org.fpj.exceptions.DataNotPresentException;
import org.fpj.exceptions.TransactionException;
import org.fpj.navigation.ViewNavigator;
import org.fpj.javafxcontroller.TransactionDetailController;
import org.fpj.javafxcontroller.TransactionViewController;
import org.fpj.payments.application.TransactionService;
import org.fpj.payments.domain.TransactionLite;
import org.fpj.payments.domain.TransactionResult;
import org.fpj.payments.domain.TransactionRow;
import org.fpj.payments.domain.TransactionType;
import org.fpj.payments.domain.TransactionViewSearchParameter;
import org.fpj.users.application.UserService;
import org.fpj.users.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

@Component
public class TransactionsLiteViewController {

    private static final int PAGE_SIZE_LITE_LIST = 100;
    private static final int PAGE_PRE_FETCH_THRESHOLD = 50;

    private final UserService userService;
    private final TransactionService transactionService;
    private final AlertService alertService;
    private final ViewNavigator viewNavigator;

    @FXML
    private RadioButton rbDeposit;

    @FXML
    private RadioButton rbTransfer;

    @FXML
    private RadioButton rbWithdraw;

    @FXML
    private TextField tfEmpfaenger;

    @FXML
    private TextField tfBetrag;

    @FXML
    private TextField tfBetreff;

    @FXML
    private ListView<TransactionRow> lvTransactions;

    private final ObservableList<TransactionRow> liteTransactionList = FXCollections.observableArrayList();

    private InfinitePager<TransactionRow> transactionPager;
    private User currentUser;
    private Consumer<String> balanceRefreshCallback;

    @Autowired
    public TransactionsLiteViewController(UserService userService, TransactionService transactionService, AlertService alertService, ViewNavigator navigator) {
        this.viewNavigator = navigator;
        this.userService = userService;
        this.transactionService = transactionService;
        this.alertService = alertService;
    }

    // <editor-fold defaultstate="collapsed" desc="initialize">
    public void initialize(User currentUser, Consumer<String> balanceRefreshCallback) {
        this.currentUser = currentUser;
        this.balanceRefreshCallback = balanceRefreshCallback;
        updateBalance();
        initTransactionList();
        initPager();
        setUpAutoCompletion();
    }

    private void initPager() {
        long userId = this.currentUser.getId();

        this.transactionPager = new InfinitePager<>(
                PAGE_SIZE_LITE_LIST,
                (pageIndex, pageSize) -> transactionService.findLiteItemsForUser(userId, pageIndex, pageSize),
                page -> liteTransactionList.addAll(page.getContent()),
                ex -> alertService.error("Fehler", null, "Transaktionen konnten nicht geladen werden: " + (ex != null ? ex.getMessage() : "Unbekannter Fehler")),
                "trx-page-loader-"
        );

        liteTransactionList.clear();
        transactionPager.resetAndLoadFirstPage();
    }

    private void initTransactionList() {
        lvTransactions.setItems(this.liteTransactionList);

        lvTransactions.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(TransactionRow item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                String counterparty = getCounterparty(item);

                String subtitle = UiHelpers.formatInstant(item.createdAt()) + "  •  " + UiHelpers.truncateFull(item.description(), 20);
                String amountText = item.amountString(currentUser.getId());

                HBox root = createTransactionRowBox(counterparty, subtitle, amountText, () -> openTransactionDetails(item));
                setGraphic(root);

                int index = getIndex();
                transactionPager.ensureLoadedForIndex(index, liteTransactionList.size(), PAGE_PRE_FETCH_THRESHOLD);
            }
        });
    }

    private String getCounterparty(TransactionRow item) {
        boolean outgoing = item.isOutgoing(currentUser.getId());

        String name = outgoing
                ? (item.recipientUsername() != null ? item.recipientUsername() : "Empfänger unbekannt")
                : (item.senderUsername() != null ? item.senderUsername() : "Sender unbekannt");

        return switch (item.type()) {
            case EINZAHLUNG -> "Einzahlung";
            case AUSZAHLUNG -> "Auszahlung";
            case UEBERWEISUNG -> (outgoing ? "Überweisung an " : "Überweisung von ") + name;
        };
    }

    private void setUpAutoCompletion() {
        TextFields.bindAutoCompletion(tfEmpfaenger, request -> {
            String term = request.getUserText();
            if (term == null || term.isBlank()) {
                return List.of();
            }
            return userService.usernameContaining(term);
        });
    }
    // </editor-fold>

    private HBox createTransactionRowBox(String titleText, String subtitleText, String amountText, Runnable onDoubleClick) {
        Label title = new Label(titleText);
        Label subtitle = new Label(subtitleText);
        VBox left = new VBox(2, title, subtitle);

        Label amount = new Label(amountText);
        Region spacer = new Region();

        HBox root = new HBox(8, left, spacer, amount);
        HBox.setHgrow(spacer, Priority.ALWAYS);

        root.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2 && onDoubleClick != null) {
                onDoubleClick.run();
            }
        });

        return root;
    }

    private void updateBalance() {
        BigDecimal balance = transactionService.computeBalance(this.currentUser.getId());
        this.balanceRefreshCallback.accept(UiHelpers.formatEuro(balance));
    }

    @FXML
    private void onTypeChanged() {
        applyTypeVisibility();
    }

    private void applyTypeVisibility() {
        boolean isTransfer = rbTransfer.isSelected();
        tfEmpfaenger.setVisible(isTransfer);
        if (!isTransfer) {
            tfEmpfaenger.clear();
        }
    }

    @FXML
    private void sendTransfers() {
        try {
            String amount = tfBetrag.getText();
            String subjectRaw = tfBetreff.getText();
            String subject = subjectRaw == null ? "" : subjectRaw.trim();
            String recipient = tfEmpfaenger.getText();

            String sender;
            TransactionType type;

            if (rbDeposit.isSelected()) {
                sender = null;
                recipient = this.currentUser.getUsername();
                type = TransactionType.EINZAHLUNG;
            } else if (rbWithdraw.isSelected()) {
                sender = this.currentUser.getUsername();
                recipient = null;
                type = TransactionType.AUSZAHLUNG;
            } else if (rbTransfer.isSelected()) {
                sender = this.currentUser.getUsername();
                UiHelpers.isValidEmail(recipient);
                type = TransactionType.UEBERWEISUNG;
            } else {
                throw new IllegalStateException("Kein Transaktionstyp ausgewählt.");
            }

            TransactionLite transactionLite = transactionService.transactionInfosToTransactionLite(amount, sender, recipient, subject, type);
            TransactionResult result = transactionService.sendTransfers(transactionLite, this.currentUser);
            this.balanceRefreshCallback.accept(UiHelpers.formatEuro(result.newBalance()));

            TransactionRow row = TransactionRow.fromTransaction(result.transaction());
            addLiteTransaction(row);

            tfBetrag.clear();
            tfBetreff.clear();
            tfEmpfaenger.clear();
            updateBalance();
        } catch (TransactionException | DataNotPresentException | NoSuchElementException ex) {
            alertService.error("Fehler", "Transaktion fehlgeschlagen", "Transaktion fehlgeschlagen: " + ex.getMessage());
        } catch (IllegalArgumentException ex) {
            alertService.error("Fehler", "Eingabe ungültig", "Eingabe ungültig: " + ex.getMessage());
        } catch (Exception ex) {
            alertService.error("Fehler", "Unerwarteter Fehler", "Unerwarteter Fehler: " + ex.getMessage());
        }
    }

    @FXML
    private void onReloadTransaction() {
        liteTransactionList.clear();
        if (transactionPager != null) {
            transactionPager.resetAndLoadFirstPage();
        }
        updateBalance();
    }

    private void openTransactionDetails(TransactionRow row) {
        if (row == null) {
            return;
        }
        try {
            NavigationResponse<TransactionDetailController> response= viewNavigator.loadTransactionDetailView();
            response.controller().initialize(
                    TransactionLite.fromTransactionRow(row),
                    this.currentUser,
                    this::onTransactionDetailSenderClicked,
                    this::onTransactionDetailRecipientClicked,
                    this::useTransactionAsTemplate,
                    this::onTransactionDetailDescriptionClicked,
                    this::onTransactionDetailAmountClicked
            );
        } catch (Exception e) {
            alertService.error("Fehler", "Fenster konnte nicht geöffnet werden", "Fehler beim Laden der Transaktionsdetails. Versuche es erneut oder starte die Anwendung neu.");
        }
    }

    private void useTransactionAsTemplate(TransactionLite row) {
        tfBetrag.setText(row.amountStringUnsigned());
        tfBetreff.setText(row.description());

        switch (row.type()) {
            case UEBERWEISUNG:
                rbTransfer.setSelected(true);
                tfEmpfaenger.setText(row.recipientUsername());
                break;
            case AUSZAHLUNG:
                rbWithdraw.setSelected(true);
                break;
            case EINZAHLUNG:
                rbDeposit.setSelected(true);
                break;
        }

        onTypeChanged();
    }

    private void onTransactionDetailDescriptionClicked(TransactionLite row) {
        TransactionViewSearchParameter sp = new TransactionViewSearchParameter(null, row.description(), null, null, null, null, null);
        this.openTransactionViewWindow(sp);
    }

    private void onTransactionDetailSenderClicked(TransactionLite row) {
        TransactionViewSearchParameter sp = new TransactionViewSearchParameter(null, null, null, null, row.senderUsername(), null, null);
        this.openTransactionViewWindow(sp);
    }

    private void onTransactionDetailRecipientClicked(TransactionLite row) {
        TransactionViewSearchParameter sp = new TransactionViewSearchParameter(null, null, null, null, row.recipientUsername(), null, null);
        this.openTransactionViewWindow(sp);
    }

    private void onTransactionDetailAmountClicked(TransactionLite row) {
        BigDecimal amount = row.amount();
        BigDecimal min = amount.setScale(0, RoundingMode.FLOOR);
        BigDecimal max = amount.setScale(0, RoundingMode.CEILING);

        TransactionViewSearchParameter sp = new TransactionViewSearchParameter(null, null, null, null, null, min, max);
        this.openTransactionViewWindow(sp);
    }

    private void openTransactionViewWindow(TransactionViewSearchParameter transactionViewSearchParameter) {
        try {
            NavigationResponse<TransactionViewController> response= viewNavigator.loadTransactionView();
            response.controller().initialize(currentUser, transactionViewSearchParameter);
        } catch (Exception e) {
            alertService.error("Fehler", "Fenster konnte nicht geöffnet werden", "Fehler beim Laden des Transaktionsfensters. Versuche es erneut oder starte die Anwendung neu.");
        }
    }

    private void addLiteTransaction(TransactionRow transactionItem) {
        this.liteTransactionList.add(0, transactionItem);
    }
}
