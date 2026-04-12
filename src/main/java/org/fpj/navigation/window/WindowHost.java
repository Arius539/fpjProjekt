package org.fpj.navigation.window;

import javafx.scene.layout.StackPane;

public record WindowHost(
        StackPane rootNode,
        StackPane mainContentLayer,
        StackPane overlayLayer,
        StackPane messageLayer
) {
}
