// src/main/java/org/fpj/ui/FxApp.java
package org.fpj;

import javafx.application.Application;
import javafx.stage.Stage;
import org.fpj.navigation.api.ViewNavigator;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.builder.SpringApplicationBuilder;

public class JavafxApp extends Application {

    private ConfigurableApplicationContext context;

    @Override
    public void init() {
        context = new SpringApplicationBuilder(App.class).run();
    }

    @Override
    public void start(Stage stage) throws Exception {
        ViewNavigator viewNavigator = context.getBean(ViewNavigator.class);
        viewNavigator.loadLogin();
    }

    @Override
    public void stop() {
        context.close();
    }
}
