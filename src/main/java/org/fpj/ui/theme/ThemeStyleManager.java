package org.fpj.ui.theme;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;

public final class ThemeStyleManager {

    public static final String THEME_ROOT_CLASS = "theme-root";
    public static final String THEME_DARK_CLASS = "theme-dark";
    public static final String LEGACY_DARK_ROOT_CLASS = "rootDark";
    public static final String ROOT_SURFACE_CLASS = "app-root-surface";

    private ThemeStyleManager() {
    }

    public static void applyTo(Node node, boolean useDarkTheme) {
        if (!(node instanceof Parent parent)) {
            return;
        }

        ObservableList<String> styleClasses = parent.getStyleClass();
        styleClasses.remove(LEGACY_DARK_ROOT_CLASS);
        ensureStyleClass(styleClasses, THEME_ROOT_CLASS);

        if (useDarkTheme) {
            ensureStyleClass(styleClasses, THEME_DARK_CLASS);
        } else {
            styleClasses.remove(THEME_DARK_CLASS);
        }
    }

    private static void ensureStyleClass(ObservableList<String> styleClasses, String styleClass) {
        if (!styleClasses.contains(styleClass)) {
            styleClasses.add(styleClass);
        }
    }
}
