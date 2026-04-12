package org.fpj.navigation.support;

import javafx.stage.Window;
import org.fpj.javafxcontroller.TransactionDetailController;
import org.fpj.javafxcontroller.TransactionViewController;
import org.fpj.navigation.api.NavigationHandle;
import org.fpj.navigation.api.ViewNavigator;
import org.fpj.navigation.api.ViewOpenMode;
import org.fpj.payments.domain.TransactionLite;
import org.fpj.payments.domain.TransactionType;
import org.fpj.payments.domain.TransactionViewSearchParameter;
import org.fpj.users.domain.User;
import org.fpj.util.AlertService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TransactionNavigationSupportTest {

    @Test
    void openFilteredTransactionViewDelegatesToNavigatorFacade() throws Exception {
        ViewNavigator viewNavigator = mock(ViewNavigator.class);
        AlertService alertService = mock(AlertService.class);
        TransactionNavigationSupport support = new TransactionNavigationSupport(viewNavigator, alertService);
        Window ownerWindow = mock(Window.class);
        User currentUser = mock(User.class);
        TransactionViewSearchParameter searchParameter =
                new TransactionViewSearchParameter(null, "desc", null, null, null, null, null);
        NavigationHandle sourceHandle = mock(NavigationHandle.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<TransactionViewController>> initializerCaptor = ArgumentCaptor.forClass(Consumer.class);

        support.openFilteredTransactionView(ownerWindow, currentUser, searchParameter, ViewOpenMode.NEW_STAGE, sourceHandle);

        verify(viewNavigator).loadTransactionView(eq(ViewOpenMode.NEW_STAGE), same(ownerWindow), same(sourceHandle), initializerCaptor.capture());

        TransactionViewController controller = mock(TransactionViewController.class);
        initializerCaptor.getValue().accept(controller);

        verify(controller).initialize(currentUser, searchParameter);
        verifyNoInteractions(alertService);
    }

    @Test
    void openTransactionDetailsDelegatesToNavigatorFacadeWithNavigatorDefaultMode() throws Exception {
        ViewNavigator viewNavigator = mock(ViewNavigator.class);
        when(viewNavigator.defaultModeForTransactionView()).thenReturn(ViewOpenMode.NEW_STAGE);
        when(viewNavigator.defaultModeForTransactionDetail()).thenReturn(ViewOpenMode.OVERLAY_IN_WINDOW);

        AlertService alertService = mock(AlertService.class);
        TransactionNavigationSupport support = new TransactionNavigationSupport(viewNavigator, alertService);
        Window ownerWindow = mock(Window.class);
        User currentUser = mock(User.class);
        TransactionLite row = new TransactionLite(
                BigDecimal.TEN,
                TransactionType.UEBERWEISUNG,
                "sender@example.com",
                "recipient@example.com",
                "Test"
        );
        @SuppressWarnings("unchecked")
        Consumer<TransactionLite> onReuseClicked = mock(Consumer.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<TransactionDetailController>> initializerCaptor = ArgumentCaptor.forClass(Consumer.class);

        support.openTransactionDetails(ownerWindow, currentUser, row, onReuseClicked);

        verify(viewNavigator).loadTransactionDetailView(
                eq(ViewOpenMode.OVERLAY_IN_WINDOW),
                same(ownerWindow),
                isNull(),
                initializerCaptor.capture()
        );

        TransactionDetailController controller = mock(TransactionDetailController.class);
        NavigationHandle navigationHandle = mock(NavigationHandle.class);
        Window detailWindow = mock(Window.class);
        when(controller.navigationHandle()).thenReturn(navigationHandle);
        when(navigationHandle.window()).thenReturn(detailWindow);
        initializerCaptor.getValue().accept(controller);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<BiConsumer<TransactionLite, ViewOpenMode>> senderClickCaptor = ArgumentCaptor.forClass(BiConsumer.class);
        verify(controller).initialize(
                same(row),
                same(currentUser),
                senderClickCaptor.capture(),
                any(BiConsumer.class),
                same(onReuseClicked),
                any(BiConsumer.class),
                any(BiConsumer.class),
                eq(ViewOpenMode.NEW_STAGE)
        );

        senderClickCaptor.getValue().accept(row, ViewOpenMode.OVERLAY_IN_WINDOW);

        verify(viewNavigator).loadTransactionView(
                eq(ViewOpenMode.OVERLAY_IN_WINDOW),
                same(detailWindow),
                same(navigationHandle),
                any()
        );
        verify(viewNavigator, never()).loadTransactionView(
                eq(ViewOpenMode.OVERLAY_IN_WINDOW),
                same(ownerWindow),
                same(navigationHandle),
                any()
        );
        verifyNoInteractions(alertService);
    }
}
