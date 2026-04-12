package org.fpj.navigation.fx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import org.springframework.stereotype.Component;

@Component
public class OverlayShellFactory {

    public Node build(Parent content, Runnable closeAction) {
        StackPane shell = new StackPane();
        shell.getStyleClass().add("app-overlay-shell");
        shell.setFocusTraversable(true);

        StackPane card = new StackPane(content);
        card.getStyleClass().add("app-overlay-card");

        Button closeButton = new Button("✕");
        closeButton.getStyleClass().add("app-overlay-close-button");
        closeButton.setFocusTraversable(false);
        closeButton.setOnAction(ignoredEvent -> {
            if (closeAction != null) {
                closeAction.run();
            }
        });

        StackPane.setAlignment(card, Pos.CENTER);
        StackPane.setMargin(card, new Insets(24));
        StackPane.setAlignment(closeButton, Pos.TOP_RIGHT);
        StackPane.setMargin(closeButton, new Insets(24));

        shell.getChildren().addAll(card, closeButton);
        return shell;
    }
}
