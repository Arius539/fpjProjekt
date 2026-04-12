package org.fpj.navigation.fx;

import javafx.scene.layout.StackPane;
import org.fpj.navigation.window.WindowHost;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class WindowShellFactory {

    public WindowHost create() {
        StackPane rootNode = new StackPane();
        rootNode.getStyleClass().add("app-root-surface");
        rootNode.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/common.css")).toExternalForm());

        StackPane mainContentLayer = new StackPane();
        mainContentLayer.setPickOnBounds(true);

        StackPane overlayLayer = new StackPane();
        overlayLayer.setPickOnBounds(false);
        overlayLayer.setMouseTransparent(false);

        StackPane messageLayer = new StackPane();
        messageLayer.setPickOnBounds(false);
        messageLayer.setMouseTransparent(true);

        rootNode.getChildren().addAll(mainContentLayer, overlayLayer, messageLayer);
        return new WindowHost(rootNode, mainContentLayer, overlayLayer, messageLayer);
    }
}
