package org.fpj.navigation.support;

import javafx.stage.Window;
import org.fpj.javafxcontroller.TransactionDetailController;
import org.fpj.navigation.api.NavigationHandle;
import org.fpj.navigation.api.ViewNavigator;
import org.fpj.navigation.api.ViewOpenMode;
import org.fpj.payments.domain.TransactionLite;
import org.fpj.payments.domain.TransactionViewSearchParameter;
import org.fpj.users.domain.User;
import org.fpj.util.AlertService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Consumer;

@Component
public class TransactionNavigationSupport {

    private final ViewNavigator viewNavigator;
    private final AlertService alertService;

    public TransactionNavigationSupport(ViewNavigator viewNavigator, AlertService alertService) {
        this.viewNavigator = viewNavigator;
        this.alertService = alertService;
    }

    public void openTransactionDetails(Window ownerWindow,
                                       User currentUser,
                                       TransactionLite row,
                                       Consumer<TransactionLite> onReuseClicked) {
        openTransactionDetails(ownerWindow, currentUser, row, onReuseClicked, viewNavigator.defaultModeForTransactionDetail(), null);
    }

    public void openTransactionDetails(Window ownerWindow,
                                       User currentUser,
                                       TransactionLite row,
                                       Consumer<TransactionLite> onReuseClicked,
                                       ViewOpenMode openMode,
                                       NavigationHandle sourceHandle) {
        if (ownerWindow == null || currentUser == null || row == null) {
            return;
        }

        try {
            viewNavigator.loadTransactionDetailView(
                    openMode,
                    ownerWindow,
                    sourceHandle,
                    controller -> initializeDetailController(controller, ownerWindow, currentUser, row, onReuseClicked)
            );
        } catch (Exception e) {
            alertService.error(
                    "Fenster konnte nicht geöffnet werden",
                    "Fehler beim Laden der Transaktionsdetails. Versuche es erneut oder starte die Anwendung neu: " + e.getMessage()
            );
        }
    }

    public void openFilteredTransactionView(Window ownerWindow,
                                            User currentUser,
                                            TransactionViewSearchParameter searchParameter,
                                            ViewOpenMode openMode,
                                            NavigationHandle sourceHandle) {
        if (ownerWindow == null || currentUser == null) {
            return;
        }

        try {
            viewNavigator.loadTransactionView(
                    openMode,
                    ownerWindow,
                    sourceHandle,
                    controller -> controller.initialize(currentUser, searchParameter)
            );
        } catch (Exception e) {
            alertService.error(
                    "Fenster konnte nicht geöffnet werden",
                    "Fehler beim Laden des Transaktionsfensters. Versuche es erneut oder starte die Anwendung neu: " + e.getMessage()
            );
        }
    }

    private TransactionViewSearchParameter descriptionSearch(TransactionLite row) {
        return new TransactionViewSearchParameter(null, row.description(), null, null, null, null, null);
    }

    private TransactionViewSearchParameter senderSearch(TransactionLite row) {
        return new TransactionViewSearchParameter(null, null, null, null, row.senderUsername(), null, null);
    }

    private TransactionViewSearchParameter recipientSearch(TransactionLite row) {
        return new TransactionViewSearchParameter(null, null, null, null, row.recipientUsername(), null, null);
    }

    private TransactionViewSearchParameter amountSearch(TransactionLite row) {
        BigDecimal amount = row.amount();
        BigDecimal min = amount.setScale(0, RoundingMode.FLOOR);
        BigDecimal max = amount.setScale(0, RoundingMode.CEILING);
        return new TransactionViewSearchParameter(null, null, null, null, null, min, max);
    }

    private void initializeDetailController(TransactionDetailController controller,
                                            Window ownerWindow,
                                            User currentUser,
                                            TransactionLite row,
                                            Consumer<TransactionLite> onReuseClicked) {
        controller.initialize(
                row,
                currentUser,
                (transaction, openMode) -> openFilteredTransactionView(resolveActiveOwnerWindow(controller, ownerWindow), currentUser, senderSearch(transaction), openMode, controller.navigationHandle()),
                (transaction, openMode) -> openFilteredTransactionView(resolveActiveOwnerWindow(controller, ownerWindow), currentUser, recipientSearch(transaction), openMode, controller.navigationHandle()),
                onReuseClicked,
                (transaction, openMode) -> openFilteredTransactionView(resolveActiveOwnerWindow(controller, ownerWindow), currentUser, descriptionSearch(transaction), openMode, controller.navigationHandle()),
                (transaction, openMode) -> openFilteredTransactionView(resolveActiveOwnerWindow(controller, ownerWindow), currentUser, amountSearch(transaction), openMode, controller.navigationHandle()),
                viewNavigator.defaultModeForTransactionView()
        );
    }

    private Window resolveActiveOwnerWindow(TransactionDetailController controller, Window fallbackOwnerWindow) {
        if (controller == null) {
            return fallbackOwnerWindow;
        }

        NavigationHandle navigationHandle = controller.navigationHandle();
        if (navigationHandle == null || navigationHandle.window() == null) {
            return fallbackOwnerWindow;
        }
        return navigationHandle.window();
    }
}
