package org.fpj.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.springframework.stereotype.Component;

@Component
public class AlertService {

    public void info(String title, String header, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public void info(String title, String message){
        info(title, null, message);
    }

    public void info(String message){
        info(null, null, message);
    }

    public void warn(String title, String header, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public void warn(String header, String message){
        warn("Warnung", header, message);
    }

    public void warn(String message){
        warn("Warnung", null, message);
    }

    public void error(String title, String header, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public void error(String header, String message){
        error("Fehler", header, message);
    }

    public void error(String message){
        error("Fehler", null, message);
    }
}
