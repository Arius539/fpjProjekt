package org.fpj.navigation.fx;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.fpj.navigation.model.ViewTarget;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class FxViewLoader {

    private final ApplicationContext context;

    public FxViewLoader(ApplicationContext context) {
        this.context = context;
    }

    public <T> LoadedFxView<T> load(ViewTarget target) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + target.fxmlPath()));
        loader.setControllerFactory(context::getBean);
        Parent root = loader.load();
        Object controller = loader.getController();

        if (!target.controllerType().isInstance(controller)) {
            throw new IllegalStateException(
                    "Controller für " + target.fxmlPath() + " hat nicht den erwarteten Typ "
                            + target.controllerType().getName() + ", sondern " + controller.getClass().getName()
            );
        }

        @SuppressWarnings("unchecked")
        T typedController = (T) controller;
        return new LoadedFxView<>(root, typedController);
    }

    public record LoadedFxView<T>(Parent rootNode, T controller) {
    }
}
