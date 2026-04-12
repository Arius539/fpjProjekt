package org.fpj.navigation.fx;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.fpj.navigation.api.ViewOpenMode;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class NavigationMenuBinder {

    private NavigationMenuBinder() {
    }

    public static void attach(Button button, ViewOpenMode defaultMode, Consumer<ViewOpenMode> onModeSelected) {
        attach(button, () -> defaultMode, onModeSelected);
    }

    public static void attach(Button button, Supplier<ViewOpenMode> defaultModeSupplier, Consumer<ViewOpenMode> onModeSelected) {
        if (button == null || onModeSelected == null || defaultModeSupplier == null) {
            return;
        }

        ContextMenu contextMenu = createContextMenu(onModeSelected);
        button.setOnAction(createPrimaryButtonHandler(defaultModeSupplier, onModeSelected));
        button.setOnContextMenuRequested(event -> {
            showContextMenu(contextMenu, button, event.getScreenX(), event.getScreenY());
            event.consume();
        });
    }

    public static void attach(Node node, int primaryClickCount, ViewOpenMode defaultMode, Consumer<ViewOpenMode> onModeSelected) {
        attach(node, primaryClickCount, () -> defaultMode, onModeSelected);
    }

    public static void attach(Node node, int primaryClickCount, Supplier<ViewOpenMode> defaultModeSupplier, Consumer<ViewOpenMode> onModeSelected) {
        if (node == null || onModeSelected == null || defaultModeSupplier == null) {
            return;
        }

        ContextMenu contextMenu = createContextMenu(onModeSelected);
        node.setOnMouseClicked(createPrimaryClickHandler(primaryClickCount, defaultModeSupplier, onModeSelected));
        node.setOnContextMenuRequested(event -> {
            showContextMenu(contextMenu, node, event.getScreenX(), event.getScreenY());
            event.consume();
        });
    }

    public static void attachContextMenu(Node node, Consumer<ViewOpenMode> onModeSelected) {
        if (node == null || onModeSelected == null) {
            return;
        }

        ContextMenu contextMenu = createContextMenu(onModeSelected);
        node.setOnContextMenuRequested(event -> {
            showContextMenu(contextMenu, node, event.getScreenX(), event.getScreenY());
            event.consume();
        });
    }

    static EventHandler<ActionEvent> createPrimaryButtonHandler(ViewOpenMode defaultMode, Consumer<ViewOpenMode> onModeSelected) {
        return createPrimaryButtonHandler(() -> defaultMode, onModeSelected);
    }

    static EventHandler<ActionEvent> createPrimaryButtonHandler(Supplier<ViewOpenMode> defaultModeSupplier, Consumer<ViewOpenMode> onModeSelected) {
        return ignoredEvent -> {
            ViewOpenMode defaultMode = defaultModeSupplier != null ? defaultModeSupplier.get() : null;
            if (defaultMode != null && onModeSelected != null) {
                onModeSelected.accept(defaultMode);
            }
        };
    }

    static EventHandler<MouseEvent> createPrimaryClickHandler(int primaryClickCount, ViewOpenMode defaultMode, Consumer<ViewOpenMode> onModeSelected) {
        return createPrimaryClickHandler(primaryClickCount, () -> defaultMode, onModeSelected);
    }

    static EventHandler<MouseEvent> createPrimaryClickHandler(int primaryClickCount, Supplier<ViewOpenMode> defaultModeSupplier, Consumer<ViewOpenMode> onModeSelected) {
        int requiredClickCount = Math.max(1, primaryClickCount);
        return event -> {
            if (event == null || event.getButton() != MouseButton.PRIMARY || event.getClickCount() != requiredClickCount) {
                return;
            }
            ViewOpenMode defaultMode = defaultModeSupplier != null ? defaultModeSupplier.get() : null;
            if (defaultMode != null && onModeSelected != null) {
                onModeSelected.accept(defaultMode);
                event.consume();
            }
        };
    }

    static List<MenuItem> createMenuItems(Consumer<ViewOpenMode> onModeSelected) {
        if (onModeSelected == null) {
            return List.of();
        }

        return List.of(
                createMenuItem("Im gleichen Fenster", ViewOpenMode.REPLACE_MAIN_CONTENT, onModeSelected),
                createMenuItem("In neuem Layer", ViewOpenMode.OVERLAY_IN_WINDOW, onModeSelected),
                createMenuItem("In neuem Fenster", ViewOpenMode.NEW_STAGE, onModeSelected)
        );
    }

    private static ContextMenu createContextMenu(Consumer<ViewOpenMode> onModeSelected) {
        List<MenuItem> items = createMenuItems(onModeSelected);
        return new ContextMenu(items.toArray(MenuItem[]::new));
    }

    private static void showContextMenu(ContextMenu contextMenu, Node anchor, double screenX, double screenY) {
        if (contextMenu == null || anchor == null) {
            return;
        }

        if (contextMenu.isShowing()) {
            contextMenu.hide();
        }
        contextMenu.show(anchor, screenX, screenY);
    }

    private static MenuItem createMenuItem(String title, ViewOpenMode openMode, Consumer<ViewOpenMode> onModeSelected) {
        MenuItem menuItem = new MenuItem(title);
        menuItem.setOnAction(ignoredEvent -> onModeSelected.accept(openMode));
        return menuItem;
    }
}
