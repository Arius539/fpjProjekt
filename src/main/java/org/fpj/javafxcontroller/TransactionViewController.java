package org.fpj.javafxcontroller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;
import org.fpj.exceptions.UserInputNormalizationException;
import org.fpj.util.AlertService;
import org.fpj.paging.InfinitePager;
import org.fpj.util.UiHelpers;
import org.fpj.navigation.NavigationResponse;
import org.fpj.exceptions.DataNotPresentException;
import org.fpj.exceptions.TransactionException;
import org.fpj.navigation.ViewNavigator;
import org.fpj.exportimport.application.MassTransferCsvReader;
import org.fpj.exportimport.application.TransactionCsvExporter;
import org.fpj.exportimport.application.FileHandling;
import org.fpj.payments.application.TransactionService;
import org.fpj.payments.domain.MassTransfer;
import org.fpj.payments.domain.TransactionLite;
import org.fpj.payments.domain.TransactionResult;
import org.fpj.payments.domain.TransactionRow;
import org.fpj.payments.domain.TransactionType;
import org.fpj.payments.domain.TransactionViewSearchParameter;
import org.fpj.users.application.UserService;
import org.fpj.users.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.fpj.util.UiHelpers.parseAmountTolerant;

@Component
public class TransactionViewController {

    private static final int PAGE_SIZE_LIST = 40;
    private static final int PAGE_PRE_FETCH_THRESHOLD = 20;

    private final UserService userService;
    private final TransactionService transactionService;
    private final TransactionCsvExporter transactionCsvExporter = new TransactionCsvExporter();
    private final AlertService alertService;
    private final ViewNavigator viewNavigator;

    @FXML
    public Label balanceLabelBatch;

    @FXML
    private Label currentBalanceLabel;

    @FXML
    private TextField filterTextField;

    @FXML
    private Label selectedTransactionBalanceLabel;

    @FXML
    private ComboBox<String> filterFieldComboBox;

    @FXML
    private ListView<TransactionRow> transactionTable;

    @FXML
    private TextField receiverUsernameField;

    @FXML
    private TextField amountField;

    @FXML
    private TextField purposeField;

    @FXML
    private ToggleGroup transactionTypeToggleGroup;

    @FXML
    private RadioButton depositRadio;

    @FXML
    private RadioButton withdrawRadio;

    @FXML
    private RadioButton transferRadio;

    @FXML
    private Button importCsvButton;

    @FXML
    private ListView<TransactionLite> batchTransactionTable;

    private User currentUser;
    private TransactionViewSearchParameter searchParameter;
    private TransactionViewSearchParameter searchParameterPerformedSearch;

    private final ObservableList<TransactionRow> transactionList = FXCollections.observableArrayList();
    private final ObservableList<TransactionLite> batchTransactionList = FXCollections.observableArrayList();

    private InfinitePager<TransactionRow> transactionPager;

    private AutoCompletionBinding<String> autoCompletionBinding;
    private String beforeActionComboBoxValue;

    @Autowired
    public TransactionViewController(UserService userService, TransactionService transactionService, AlertService alertService, ViewNavigator  viewNavigator) {
        this.viewNavigator = viewNavigator;
        this.userService = userService;
        this.transactionService = transactionService;
        this.alertService = alertService;
    }

    // <editor-fold defaultstate="collapsed" desc="initialize">
    public void initialize(User currentUser, TransactionViewSearchParameter searchParameter) {
        this.currentUser = currentUser;
        this.searchParameter = searchParameter;

        if (this.currentUser == null) {
            alertService.error("Benutzer nicht gesetzt", "Fehler beim Laden der nötigen Daten, bitte starte die Anwendung neu.");
            return;
        }

        processSearchParameter();
        initUiElements();
        initTransactionList();
        initBatchTransactionList();
        initPager();
        updateBalances();
        setUpAutoCompletion();
    }

    private void processSearchParameter() {
        if (this.searchParameter == null) {
            this.searchParameter = new TransactionViewSearchParameter(null, null, null, null, null, null, null);
        }
        this.searchParameter.setCurrentUserID(this.currentUser.getId());
    }

    private void initUiElements() {
        if (batchTransactionTable != null) {
            batchTransactionTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        }
        if (transactionTypeToggleGroup != null && transferRadio != null) {
            transactionTypeToggleGroup.selectToggle(transferRadio);
        }
    }

    private void initPager() {
        this.transactionPager = new InfinitePager<>(
                PAGE_SIZE_LIST,
                (pageIndex, pageSize) ->{
                    searchParameterPerformedSearch= this.searchParameter.copy();
                    return  transactionService.searchTransactions(this.searchParameter, pageIndex, pageSize);
                },
                page -> transactionList.addAll(page.getContent()),
                ex -> alertService.error("Transaktionen konnten nicht geladen werden: " + (ex != null ? ex.getMessage() : "Unbekannter Fehler")),
                "trx-page-loader-"
        );

        transactionList.clear();
        transactionPager.resetAndLoadFirstPage();
    }

    private void initTransactionList() {
        transactionTable.setItems(transactionList);

        transactionTable.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(TransactionRow item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                boolean outgoing = item.isOutgoing(currentUser.getId());
                String name = outgoing
                        ? (item.recipientUsername() != null ? item.recipientUsername() : "Empfänger unbekannt")
                        : (item.senderUsername() != null ? item.senderUsername() : "Sender unbekannt");

                String counterparty = switch (item.type()) {
                    case EINZAHLUNG -> "Einzahlung";
                    case AUSZAHLUNG -> "Auszahlung";
                    case UEBERWEISUNG -> (outgoing ? "Überweisung an " : "Überweisung von ") + name;
                };

                String subtitle = UiHelpers.formatInstant(item.createdAt()) + "  •  " + UiHelpers.truncateFull(item.description(), 20);
                String amountText = item.amountString(currentUser.getId());

                HBox root = createTransactionRowBox(counterparty, subtitle, amountText);
                setGraphic(root);

                int index = getIndex();
                if (transactionPager != null) {
                    transactionPager.ensureLoadedForIndex(index, transactionList.size(), PAGE_PRE_FETCH_THRESHOLD);
                }

                setOnMouseClicked(ev -> {
                    if (ev.getClickCount() == 1) {
                        BigDecimal after = transactionService.findUserBalanceAfterTransaction(currentUser.getId(), item.id());
                        updateSelectedBalanceLabel(after);
                    }
                    if (ev.getClickCount() == 2) {
                        openTransactionDetails(TransactionLite.fromTransactionRow(item));
                    }
                });
            }
        });
    }

    private void initBatchTransactionList() {
        batchTransactionTable.setItems(batchTransactionList);
        batchTransactionTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        batchTransactionTable.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(TransactionLite item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                boolean outgoing = item.isOutgoing(currentUser.getUsername());
                String name = outgoing
                        ? (item.recipientUsername() != null ? item.recipientUsername() : "Empfänger unbekannt")
                        : (item.senderUsername() != null ? item.senderUsername() : "Sender unbekannt");

                String counterparty = switch (item.type()) {
                    case EINZAHLUNG -> "Einzahlung";
                    case AUSZAHLUNG -> "Auszahlung";
                    case UEBERWEISUNG -> (outgoing ? "Überweisung an " : "Überweisung von ") + name;
                };

                String subtitle = UiHelpers.truncateFull(item.description(), 30);
                String amountText = item.amountStringSigned(currentUser.getUsername());

                HBox root = createTransactionRowBox(counterparty, subtitle, amountText);
                setGraphic(root);

                setOnMouseClicked(ev -> {
                    if (ev.getClickCount() == 2) {
                        openTransactionDetails(item);
                    }
                });
            }
        });
    }

    private void setUpAutoCompletion() {
        TextFields.bindAutoCompletion(receiverUsernameField, request -> {
            String term = request.getUserText();
            if (term == null || term.isBlank()) {
                return List.of();
            }
            return userService.usernameContaining(term);
        });
    }
    // </editor-fold>

    private HBox createTransactionRowBox(String titleText, String subtitleText, String amountText) {
        Label title = new Label(titleText);
        Label subtitle = new Label(subtitleText);
        VBox left = new VBox(2, title, subtitle);

        Label amount = new Label(amountText);
        Region spacer = new Region();

        HBox root = new HBox(8, left, spacer, amount);
        HBox.setHgrow(spacer, Priority.ALWAYS);

        return root;
    }

    private void updateBalances() {
        updateCurrentBalanceLabel();
        updateBatchTransactionBalanceLabel();
    }

    private void updateCurrentBalanceLabel() {
        BigDecimal balance = transactionService.computeBalance(currentUser.getId());
        currentBalanceLabel.setText(UiHelpers.formatAmount(balance, false,false, true, ',', true, '\0', false));
    }

    private void updateSelectedBalanceLabel(BigDecimal amount) {
        selectedTransactionBalanceLabel.setText(UiHelpers.formatAmount(amount, true,true, true, ',', true, '\0', false));
    }

    private BigDecimal getBalanceAfterListOfItems(List<TransactionLite> transactions) {
        BigDecimal currentBalance = transactionService.computeBalance(currentUser.getId());
        for (TransactionLite transactionLite : transactions) {
            if (transactionLite.isOutgoing(currentUser.getUsername())) {
                currentBalance = currentBalance.subtract(transactionLite.amount());
            } else {
                currentBalance = currentBalance.add(transactionLite.amount());
            }
        }
        return currentBalance;
    }

    private void updateBatchTransactionBalanceLabel() {
        BigDecimal newBalance = getBalanceAfterListOfItems(batchTransactionList);
        balanceLabelBatch.setText(UiHelpers.formatAmount(newBalance, true,true, true, ',', true, '\0', false));
    }

    private ArrayList<TransactionResult> executeTransactionByList(List<TransactionLite> transactions) {
        try {
            return transactionService.sendBulkTransfers(transactions, currentUser);
        } catch (TransactionException | IllegalArgumentException e) {
            alertService.error( "Transaktion fehlgeschlagen", "Transaktionsfehler: " + e.getMessage());
        } catch (Exception e) {
            alertService.error("Unerwarteter Fehler", "Unerwarteter Fehler: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    private TransactionLite transactionInfosToTransactionLite() {
        try {
            String amount = amountField.getText();
            String subjectRaw = purposeField.getText();
            String subject = subjectRaw == null ? "" : UiHelpers.truncate(subjectRaw, subjectRaw.length());
            String recipient =UiHelpers.safe(receiverUsernameField.getText());
            BigDecimal amountNum = parseAmountTolerant(amount);

            String sender = null;
            TransactionType type;
            if (depositRadio.isSelected()) {
                recipient = currentUser.getUsername();
                type = TransactionType.EINZAHLUNG;
            } else if (withdrawRadio.isSelected()) {
                sender = currentUser.getUsername();
                type = TransactionType.AUSZAHLUNG;
            } else if (transferRadio.isSelected()) {
                sender = currentUser.getUsername();
                UiHelpers.isValidEmail(recipient);
                type = TransactionType.UEBERWEISUNG;
            } else {
                throw new IllegalStateException("Kein Transaktionstyp ausgewählt.");
            }
            amountField.setText(UiHelpers.formatAmount(amountNum, false,false, true, ',', true, '\0', false));
            if(!UiHelpers.amountCheck(amount,amountNum ) )throw new UserInputNormalizationException("Wir konnten den Betrag nicht eindeutig lesen bestätige deine Eingabe");
            return transactionService.transactionInfosToTransactionLite(amountNum, sender, recipient, subject, type);
        } catch (UserInputNormalizationException ex) {
            alertService.error("Überprüfe deine Eingabe", ex.getMessage());
        }  catch (TransactionException ex) {
            alertService.error("Transaktion fehlgeschlagen", "Transaktion fehlgeschlagen: " + ex.getMessage());
        } catch (IllegalArgumentException | DataNotPresentException ex) {
            alertService.error("Eingabe ungültig", "Eingabe ungültig: " + ex.getMessage());
        } catch (Exception ex) {
            alertService.error("Unerwarteter Fehler", "Unerwarteter Fehler: " + ex.getMessage());
        }
        return null;
    }

    private void useTransactionAsTemplate(TransactionLite row) {
        amountField.setText(row.amountStringUnsigned());
        purposeField.setText(row.description());

        switch (row.type()) {
            case UEBERWEISUNG:
                transferRadio.setSelected(true);
                receiverUsernameField.setText(row.recipientUsername());
                break;
            case AUSZAHLUNG:
                withdrawRadio.setSelected(true);
                break;
            case EINZAHLUNG:
                depositRadio.setSelected(true);
                break;
        }

        applyTypeVisibility();
    }

    private void parseValuesFromSearchField(String selected) {
        String text = filterTextField.getText();

        try {
            switch (selected) {
                case "Verwendungszweck" -> {
                    String value = (text == null || text.isBlank()) ? null : text;
                    searchParameter.setDescription(value);
                }
                case "Empfänger, Sender" -> {
                    String value = (text == null || text.isBlank()) ? null : text;
                    searchParameter.setSenderRecipientUsername(value);
                }
                case "Created at von" -> {
                    searchParameter.setCreatedFrom(UiHelpers.parseDateTolerant(text));
                }
                case "Created at bis" -> {
                    searchParameter.setCreatedTo(UiHelpers.parseDateTolerant(text));
                }
                case "Betrag ab" -> {
                    var value = (text == null || text.isBlank())
                            ? null
                            : UiHelpers.parseAmountTolerant(text).abs();
                    searchParameter.setAmountFrom(value);
                }
                case "Betrag bis" -> {
                    var value = (text == null || text.isBlank())
                            ? null
                            : UiHelpers.parseAmountTolerant(text).abs();
                    searchParameter.setAmountTo(value);
                }
                default -> {
                }
            }
        } catch (Exception e) {
            if (text != null && !text.isBlank()) {
                alertService.error(
                        "Filter ungültig",
                        "Es ist ein Fehler beim Lesen des Filterwertes aufgetreten: " + e.getMessage()
                );
            }
        }
    }


    private String getTextValueSelectedFilter(String selected) {
        if (this.searchParameter == null) {
            return "";
        }
        try {
            return switch (selected) {
                case "Verwendungszweck" -> {
                    String description = this.searchParameter.getDescription();
                    yield description != null ? description : "";
                }
                case "Empfänger, Sender" -> {
                    String senderRecipient = this.searchParameter.getSenderRecipientUsername();
                    yield senderRecipient != null ? senderRecipient : "";
                }
                case "Created at von" -> {
                    Instant createdFrom = this.searchParameter.getCreatedFrom();
                    yield createdFrom != null ? UiHelpers.formatInstantToDate(createdFrom) : "";
                }
                case "Created at bis" -> {
                    Instant createdTo = this.searchParameter.getCreatedTo();
                    yield createdTo != null ? UiHelpers.formatInstantToDate(createdTo) : "";
                }
                case "Betrag ab" -> {
                    BigDecimal amountFrom = this.searchParameter.getAmountFrom();
                    yield amountFrom != null ? UiHelpers.formatAmount(amountFrom, false,false, false,',', false, '\0', false) : "";
                }
                case "Betrag bis" -> {
                    BigDecimal amountTo = this.searchParameter.getAmountTo();
                    yield amountTo != null ? UiHelpers.formatAmount(amountTo, false,false, false,',', false, '\0', false) : "";
                }
                default -> "";
            };
        } catch (Exception e) {
            return "";
        }
    }

    private void applyTypeVisibility() {
        boolean isTransfer = transferRadio.isSelected();
        receiverUsernameField.setVisible(isTransfer);
        if (!isTransfer) {
            receiverUsernameField.clear();
        }
    }

    private void reloadTransactionList() {
        transactionList.clear();
        if (transactionPager != null) {
            transactionPager.resetAndLoadFirstPage();
        }
    }

    private void updateAutoCompletion(boolean enable) {
        if (autoCompletionBinding != null) {
            autoCompletionBinding.dispose();
            autoCompletionBinding = null;
        }

        if (enable) {
            autoCompletionBinding = TextFields.bindAutoCompletion(filterTextField, request -> {
                String term = request.getUserText();
                if (term == null || term.isBlank()) {
                    return List.of();
                }
                return userService.usernameContaining(term);
            });
        }
    }

    private void openTransactionDetails(TransactionLite row) {
        if (row == null) {
            return;
        }
        try {
            NavigationResponse<TransactionDetailController> response = viewNavigator.loadTransactionDetailView();
            response.controller().initialize(row, currentUser, this::onTransactionDetailSenderClicked, this::onTransactionDetailRecipientClicked, this::useTransactionAsTemplate, this::onTransactionDetailDescriptionClicked, this::onTransactionDetailAmountClicked);
        } catch (Exception e) {
            alertService.error("Fenster konnte nicht geöffnet werden", "Fehler beim Laden der Transaktionsdetails. Versuche es erneut oder starte die Anwendung neu: " + e.getMessage());
        }
    }

    private void openCsvImportDialog() {
        try {
            Window ownerWindow = importCsvButton.getScene().getWindow();
            NavigationResponse<CsvImportDialogController> response = viewNavigator.loadCsvDialogView(ownerWindow);
            if(response.isLoaded()) {
                alertService.info("Info", "Info", "Es ist bereits ein Importer Fenster offen, schließe dieses bitte zuerst");
                return;
            }
            CsvImportDialogController<MassTransfer> dialogController = response.controller();
            MassTransferCsvReader reader = new MassTransferCsvReader();
            reader.setCurrentUser(this.currentUser);
            reader.setUserService(this.userService);
            dialogController.initialize(reader, this::addTransactionToBatch);
        } catch (Exception e) {
            alertService.error("Fenster konnte nicht geöffnet werden", "Fehler beim Laden des CSV-Import-Dialogs. Versuche es erneut oder starte die Anwendung neu: " + e.getMessage());
        }
    }

    private void addTransactionToBatch(List<MassTransfer> massTransfers) {
        BigDecimal amount = BigDecimal.ZERO;
        for (MassTransfer massTransfer : massTransfers) {
            amount= amount.add(massTransfer.betrag());
        }
        BigDecimal balance = transactionService.computeBalance(currentUser.getId());
        BigDecimal after = balance.subtract(amount);

        if(after.compareTo(BigDecimal.ZERO) < 0) {
            boolean response = alertService.confirmYesNo(
                    "CSV Import fehlgeschlagen",
                    "Guthaben reicht nicht",
                    "CSV wurde erfolgreich eingelesen.\n"
                            + "Endsaldo wäre: " + UiHelpers.formatAmount(after, true, true, true, ',', true, '.', false) + "\n\n"
                            + "Trotzdem in die Liste für gesammelte Transaktionen übernehmen?"
            );
            if(!response)return;
        }

        for (MassTransfer massTransfer : massTransfers) {
            batchTransactionList.add(new TransactionLite(massTransfer.betrag(), TransactionType.UEBERWEISUNG, currentUser.getUsername(), massTransfer.empfaenger(), massTransfer.beschreibung()));
        }
        updateBalances();
    }

    private void addTransactionsToList(List<TransactionResult> transactions) {
        for (TransactionResult transactionResult : transactions) {
            transactionList.add(0, TransactionRow.fromTransaction(transactionResult.transaction()));
        }
    }

    private void onTransactionDetailDescriptionClicked(TransactionLite row) {
        this.searchParameter = new TransactionViewSearchParameter(null, row.description(), null, null, null, null, null);
        initialize(currentUser, searchParameter);
    }

    private void onTransactionDetailSenderClicked(TransactionLite row) {
        this.searchParameter = new TransactionViewSearchParameter(null, null, null, null, row.senderUsername(), null, null);
        initialize(currentUser, searchParameter);
    }

    private void onTransactionDetailRecipientClicked(TransactionLite row) {
        this.searchParameter = new TransactionViewSearchParameter(null, null, null, null, row.recipientUsername(), null, null);
        initialize(currentUser, searchParameter);
    }

    private void onTransactionDetailAmountClicked(TransactionLite row) {
        BigDecimal amount = row.amount();
        BigDecimal min = amount.setScale(0, RoundingMode.FLOOR);
        BigDecimal max = amount.setScale(0, RoundingMode.CEILING);

        this.searchParameter = new TransactionViewSearchParameter(null, null, null, null, null, min, max);
        initialize(currentUser, searchParameter);
    }

    @FXML
    private void onReloadTransactions() {
        reloadTransactionList();
    }

    @FXML
    private void onReloadBatches() {
        updateBalances();
    }

    @FXML
    private void onFilterChanged() {
        String selected = filterFieldComboBox.getValue();
        if (beforeActionComboBoxValue != null) {
            parseValuesFromSearchField(beforeActionComboBoxValue);
        }
        String filterText = getTextValueSelectedFilter(selected);
        filterTextField.setText(filterText);
        beforeActionComboBoxValue = selected;
        updateAutoCompletion("Empfänger, Sender".equals(selected));
    }

    @FXML
    private void onClearFilter() {
        filterTextField.setText("");
        filterFieldComboBox.getSelectionModel().clearSelection();
        TransactionViewSearchParameter searchParameter = this.searchParameter.copy();
        this.searchParameter = null;
        processSearchParameter();
        if(!searchParameter.equals(this.searchParameter))  reloadTransactionList();
    }

    @FXML
    private void onSearch() {
        String selected = filterFieldComboBox.getValue();
        if (selected == null) {
            return;
        }
        parseValuesFromSearchField(selected);
        if(!searchParameter.equals(this.searchParameterPerformedSearch)) reloadTransactionList();
    }

    @FXML
    private void onDeleteTransaction() {
        List<TransactionLite> selectedTransactions = new ArrayList<>(batchTransactionTable.getSelectionModel().getSelectedItems());
        if (selectedTransactions.isEmpty()) {
            return;
        }
        for (TransactionLite transactionLite : selectedTransactions) {
            batchTransactionList.remove(transactionLite);
        }
        batchTransactionTable.getSelectionModel().clearSelection();
        updateBalances();
    }

    @FXML
    private void onExecuteSingleFromContext() {
        List<TransactionLite> selectedTransactions = new ArrayList<>(batchTransactionTable.getSelectionModel().getSelectedItems());
        batchTransactionTable.getSelectionModel().clearSelection();
        ArrayList<TransactionResult> result = executeTransactionByList(selectedTransactions);
        if (!result.isEmpty()) {
            for (TransactionLite transactionLite : selectedTransactions) {
                batchTransactionList.remove(transactionLite);
            }
        }
        updateBalances();
        addTransactionsToList(result);
    }

    @FXML
    private void onTransactionTypeChanged() {
        applyTypeVisibility();
    }

    @FXML
    private void exportTransactions() {
        if (currentUser == null) {
            alertService.error("Export nicht möglich", "Benutzer ist nicht gesetzt.");
            return;
        }

        try {
            if (transactionCsvExporter.isRunning()) {
                alertService.error("Eine andere Export-Instanz läuft noch. Warte bitte, bis diese abgeschlossen ist.");
                return;
            }

            Window window = importCsvButton.getScene().getWindow();

            String fileName ="TransaktionenExport_" +this.currentUser.getUsername()+ ".csv";

            String path = FileHandling.openDirectoryChooserAndGetPath(window);
            if (path == null) {
                throw new IllegalStateException("Das Auswählen des Dateipfades ist fehlgeschlagen.");
            }
            File file = FileHandling.createFileVersioned(path, fileName);

            List<TransactionRow> messages = transactionService.transactionsForUserAsList(currentUser.getId());
            transactionCsvExporter.export(messages.iterator(), FileHandling.openFileAsOutStream(file.getAbsolutePath()));
            alertService.info("Export erfolgreich", "Der Export der Transaktionen war erfolgreich. Du findest die Einträge in: " + file.getAbsolutePath());
        } catch (IllegalArgumentException e) {
            alertService.error("Export fehlgeschlagen", "Fehler beim Exportieren der Transaktionen: " + e.getMessage());
        } catch (Exception e) {
            alertService.error("Unerwarteter Fehler", "Ein unbekannter Fehler ist aufgetreten: " + e.getMessage());
        }
    }

    @FXML
    private void onImportCsv() {
        openCsvImportDialog();
        updateBalances();
    }

    @FXML
    private void onExecuteSingle() {
        TransactionLite transactionLite = transactionInfosToTransactionLite();
        if (transactionLite == null) {
            return;
        }
        List<TransactionLite> list = new ArrayList<>();
        list.add(transactionLite);
        ArrayList<TransactionResult> result = executeTransactionByList(list);
        addTransactionsToList(result);
        updateBalances();
    }

    @FXML
    private void onAddToBatch() {
        TransactionLite transactionLite = transactionInfosToTransactionLite();
        if (transactionLite == null) {
            return;
        }
        batchTransactionList.add(transactionLite);
        updateBalances();
    }

    @FXML
    private void onExecuteAll() {
        ArrayList<TransactionResult> result = executeTransactionByList(batchTransactionList);
        if (!result.isEmpty()) {
            batchTransactionList.clear();
        }
        updateBalances();
        addTransactionsToList(result);
    }
}
