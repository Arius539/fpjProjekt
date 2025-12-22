package org.fpj.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.springframework.stereotype.Component;

import java.util.Optional;

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

    public boolean confirmYesNo(String title, String header, String message) {
        ButtonType yes = new ButtonType("Ja");
        ButtonType no  = new ButtonType("Nein");

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, yes, no);
        alert.setTitle(title);
        alert.setHeaderText(header);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == yes;
    }
}
