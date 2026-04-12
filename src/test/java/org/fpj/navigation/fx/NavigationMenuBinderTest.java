package org.fpj.navigation.fx;

import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.fpj.navigation.api.ViewOpenMode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NavigationMenuBinderTest {

    @Test
    void primaryButtonHandlerOpensDefaultMode() {
        AtomicReference<ViewOpenMode> selectedMode = new AtomicReference<>();

        NavigationMenuBinder.createPrimaryButtonHandler(ViewOpenMode.REPLACE_MAIN_CONTENT, selectedMode::set)
                .handle(null);

        assertEquals(ViewOpenMode.REPLACE_MAIN_CONTENT, selectedMode.get());
    }

    @Test
    void primaryButtonHandlerResolvesLazyDefaultAtClickTime() {
        AtomicInteger invocations = new AtomicInteger();
        AtomicReference<ViewOpenMode> selectedMode = new AtomicReference<>();

        NavigationMenuBinder.createPrimaryButtonHandler(
                        () -> invocations.incrementAndGet() == 1
                                ? ViewOpenMode.REPLACE_MAIN_CONTENT
                                : ViewOpenMode.OVERLAY_IN_WINDOW,
                        selectedMode::set
                )
                .handle(null);

        assertEquals(1, invocations.get());
        assertEquals(ViewOpenMode.REPLACE_MAIN_CONTENT, selectedMode.get());
    }

    @Test
    void primaryClickHandlerRequiresConfiguredClickCount() {
        AtomicReference<ViewOpenMode> selectedMode = new AtomicReference<>();

        var handler = NavigationMenuBinder.createPrimaryClickHandler(2, ViewOpenMode.NEW_STAGE, selectedMode::set);

        MouseEvent singlePrimaryClick = createMouseClick(MouseButton.PRIMARY, 1);
        handler.handle(singlePrimaryClick);
        assertNull(selectedMode.get());
        assertFalse(singlePrimaryClick.isConsumed());

        MouseEvent doublePrimaryClick = createMouseClick(MouseButton.PRIMARY, 2);
        handler.handle(doublePrimaryClick);
        assertEquals(ViewOpenMode.NEW_STAGE, selectedMode.get());
        assertTrue(doublePrimaryClick.isConsumed());
    }

    @Test
    void primaryClickHandlerResolvesLazyDefaultAtClickTime() {
        AtomicReference<ViewOpenMode> selectedMode = new AtomicReference<>();

        var handler = NavigationMenuBinder.createPrimaryClickHandler(
                1,
                () -> ViewOpenMode.OVERLAY_IN_WINDOW,
                selectedMode::set
        );

        MouseEvent primaryClick = createMouseClick(MouseButton.PRIMARY, 1);
        handler.handle(primaryClick);

        assertEquals(ViewOpenMode.OVERLAY_IN_WINDOW, selectedMode.get());
        assertTrue(primaryClick.isConsumed());
    }

    @Test
    void menuItemsExposeAllThreeModesAndSecondaryClickDoesNotTriggerDefaultOpen() {
        AtomicReference<ViewOpenMode> selectedMode = new AtomicReference<>();
        var handler = NavigationMenuBinder.createPrimaryClickHandler(1, ViewOpenMode.REPLACE_MAIN_CONTENT, selectedMode::set);

        handler.handle(createMouseClick(MouseButton.SECONDARY, 1));
        assertNull(selectedMode.get());

        List<MenuItem> items = NavigationMenuBinder.createMenuItems(selectedMode::set);
        assertEquals(3, items.size());

        items.get(1).getOnAction().handle(null);
        assertEquals(ViewOpenMode.OVERLAY_IN_WINDOW, selectedMode.get());

        items.get(2).getOnAction().handle(null);
        assertEquals(ViewOpenMode.NEW_STAGE, selectedMode.get());
    }

    private MouseEvent createMouseClick(MouseButton mouseButton, int clickCount) {
        return new MouseEvent(
                MouseEvent.MOUSE_CLICKED,
                0,
                0,
                0,
                0,
                mouseButton,
                clickCount,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                null
        );
    }
}
