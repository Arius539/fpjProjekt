package org.fpj.navigation.api;

import org.fpj.navigation.model.OpenRequest;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ViewNavigatorTest {

    @Test
    void runInitializerIfNewRunsInitializerForFreshViewInstances() {
        AtomicInteger invocationCount = new AtomicInteger();

        ViewNavigator.runInitializerIfNew(false, "controller", ignored -> invocationCount.incrementAndGet());

        assertEquals(1, invocationCount.get());
    }

    @Test
    void runInitializerIfNewSkipsInitializerForReusedViewInstances() {
        AtomicInteger invocationCount = new AtomicInteger();

        ViewNavigator.runInitializerIfNew(true, "controller", ignored -> invocationCount.incrementAndGet());

        assertEquals(0, invocationCount.get());
    }

    @Test
    void resolveChildDefaultOpenModePromotesReplaceContentToOverlayForOverlaySources() {
        ViewNavigator navigator = new ViewNavigator(null, null, null);

        ViewOpenMode resolvedMode = navigator.resolveChildDefaultOpenMode(
                ViewOpenMode.REPLACE_MAIN_CONTENT,
                NavigationHandle.of(null, null, null, NavigationPresentationKind.OVERLAY)
        );

        assertEquals(ViewOpenMode.OVERLAY_IN_WINDOW, resolvedMode);
    }

    @Test
    void resolveChildDefaultOpenModeKeepsSpecialDefaultsUntouched() {
        ViewNavigator navigator = new ViewNavigator(null, null, null);

        assertEquals(
                ViewOpenMode.NEW_STAGE,
                navigator.resolveChildDefaultOpenMode(
                        ViewOpenMode.NEW_STAGE,
                        NavigationHandle.of(null, null, null, NavigationPresentationKind.OVERLAY)
                )
        );
        assertEquals(
                ViewOpenMode.OVERLAY_IN_WINDOW,
                navigator.resolveChildDefaultOpenMode(
                        ViewOpenMode.OVERLAY_IN_WINDOW,
                        NavigationHandle.of(null, null, null, NavigationPresentationKind.OVERLAY)
                )
        );
    }

    @Test
    void resolveChildDefaultOpenModeKeepsReplaceContentForNonOverlaySources() {
        ViewNavigator navigator = new ViewNavigator(null, null, null);

        assertEquals(
                ViewOpenMode.REPLACE_MAIN_CONTENT,
                navigator.resolveChildDefaultOpenMode(
                        ViewOpenMode.REPLACE_MAIN_CONTENT,
                        NavigationHandle.of(null, null, null, NavigationPresentationKind.CONTENT)
                )
        );
        assertEquals(
                ViewOpenMode.REPLACE_MAIN_CONTENT,
                navigator.resolveChildDefaultOpenMode(ViewOpenMode.REPLACE_MAIN_CONTENT, null)
        );
        assertNull(navigator.resolveChildDefaultOpenMode(null, null));
    }

    @Test
    void resolveReusableStageKeyReturnsStableKeysOnlyForSingletonPolicies() {
        assertEquals(
                "login",
                ViewNavigator.resolveReusableStageKey(
                        new OpenRequest<>(org.fpj.navigation.model.ViewTarget.LOGIN, ViewOpenMode.NEW_STAGE, null, "login", null)
                )
        );
        assertEquals(
                "chat:alice@example.com",
                ViewNavigator.resolveReusableStageKey(
                        new OpenRequest<>(org.fpj.navigation.model.ViewTarget.CHAT_WINDOW, ViewOpenMode.NEW_STAGE, null, "chat:alice@example.com", null)
                )
        );
        assertNull(
                ViewNavigator.resolveReusableStageKey(
                        new OpenRequest<>(org.fpj.navigation.model.ViewTarget.TRANSACTION_VIEW, ViewOpenMode.NEW_STAGE, null, "transaction", null)
                )
        );
        assertNull(
                ViewNavigator.resolveReusableStageKey(
                        new OpenRequest<>(org.fpj.navigation.model.ViewTarget.CHAT_WINDOW, ViewOpenMode.REPLACE_MAIN_CONTENT, null, "chat:alice@example.com", null)
                )
        );
    }
}
