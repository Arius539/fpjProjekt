package org.fpj.ui.theme;

import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThemeStyleManagerTest {

    @Test
    void applyToThemesTransparentLayersWithoutInjectingABackgroundClass() {
        StackPane transparentLayer = new StackPane();

        ThemeStyleManager.applyTo(transparentLayer, true);

        assertTrue(transparentLayer.getStyleClass().contains(ThemeStyleManager.THEME_ROOT_CLASS));
        assertTrue(transparentLayer.getStyleClass().contains(ThemeStyleManager.THEME_DARK_CLASS));
        assertFalse(transparentLayer.getStyleClass().contains(ThemeStyleManager.ROOT_SURFACE_CLASS));

        StackPane rootSurface = new StackPane();
        rootSurface.getStyleClass().add(ThemeStyleManager.ROOT_SURFACE_CLASS);
        rootSurface.getStyleClass().add(ThemeStyleManager.LEGACY_DARK_ROOT_CLASS);

        ThemeStyleManager.applyTo(rootSurface, false);

        assertTrue(rootSurface.getStyleClass().contains(ThemeStyleManager.THEME_ROOT_CLASS));
        assertTrue(rootSurface.getStyleClass().contains(ThemeStyleManager.ROOT_SURFACE_CLASS));
        assertFalse(rootSurface.getStyleClass().contains(ThemeStyleManager.THEME_DARK_CLASS));
        assertFalse(rootSurface.getStyleClass().contains(ThemeStyleManager.LEGACY_DARK_ROOT_CLASS));
    }
}
