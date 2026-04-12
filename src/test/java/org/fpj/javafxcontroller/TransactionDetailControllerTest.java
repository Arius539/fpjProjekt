package org.fpj.javafxcontroller;

import org.fpj.navigation.api.NavigationHandle;
import org.fpj.navigation.api.ViewOpenMode;
import org.fpj.payments.domain.TransactionLite;
import org.fpj.payments.domain.TransactionType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.function.BiConsumer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class TransactionDetailControllerTest {

    @Test
    void handleAndCloseUsesNavigationHandleInsteadOfTryingToCloseTheStage() {
        TransactionDetailController controller = new TransactionDetailController();
        TransactionLite transaction = new TransactionLite(
                BigDecimal.TEN,
                TransactionType.UEBERWEISUNG,
                "sender@example.com",
                "recipient@example.com",
                "Test"
        );

        @SuppressWarnings("unchecked")
        BiConsumer<TransactionLite, ViewOpenMode> handler = mock(BiConsumer.class);
        NavigationHandle navigationHandle = mock(NavigationHandle.class);

        ReflectionTestUtils.setField(controller, "transaction", transaction);
        ReflectionTestUtils.setField(controller, "navigationHandle", navigationHandle);

        ReflectionTestUtils.invokeMethod(controller, "handleAndClose", handler, ViewOpenMode.NEW_STAGE);

        verify(handler).accept(transaction, ViewOpenMode.NEW_STAGE);
        verify(navigationHandle).close();
    }

    @Test
    void handleAndCloseKeepsUsingOriginalOverlayHandleWhenHandlerRebindsControllerHandle() {
        TransactionDetailController controller = new TransactionDetailController();
        TransactionLite transaction = new TransactionLite(
                BigDecimal.TEN,
                TransactionType.UEBERWEISUNG,
                "sender@example.com",
                "recipient@example.com",
                "Test"
        );

        NavigationHandle originalHandle = mock(NavigationHandle.class);
        NavigationHandle reboundHandle = mock(NavigationHandle.class);

        BiConsumer<TransactionLite, ViewOpenMode> handler = (clickedTransaction, openMode) ->
                ReflectionTestUtils.setField(controller, "navigationHandle", reboundHandle);

        ReflectionTestUtils.setField(controller, "transaction", transaction);
        ReflectionTestUtils.setField(controller, "navigationHandle", originalHandle);

        ReflectionTestUtils.invokeMethod(controller, "handleAndClose", handler, ViewOpenMode.REPLACE_MAIN_CONTENT);

        verify(originalHandle).close();
        verify(reboundHandle, never()).close();
    }
}
