package org.fpj.javafxcontroller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import lombok.Setter;
import org.fpj.util.AlertService;
import org.fpj.exceptions.LoginFailedException;
import org.fpj.navigation.ViewNavigator;
import org.fpj.users.application.LoginService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Setter
public class LoginController {

    private static final String REGISTER_STRING = "registrieren";
    private static final String LOGIN_STRING = "anmelden";
    private static final String NO_ACCOUNT = "noch kein Konto?";
    private static final String ACCOUNT_EXISTENT = "du hast bereits ein Konto?";

    private final ViewNavigator viewNavigator;
    private final LoginService loginService;
    private final AlertService alertService;

    @Autowired
    public LoginController(ViewNavigator viewNavigator, LoginService loginService, AlertService alertService){
        this.viewNavigator = viewNavigator;
        this.loginService = loginService;
        this.alertService = alertService;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginController.class);

    @FXML
    private TextField usernameInput;
    @FXML
    private PasswordField passwordInput;
    @FXML
    private PasswordField passwordCheck;
    @FXML
    private Button loginButton;
    @FXML
    private Button toggleButton;

    @FXML
    public void submit(ActionEvent event){
        Button button = (Button) event.getSource();
        final String username = usernameInput.getText();
        final String password = passwordInput.getText();
        if (button.getText().equals(LOGIN_STRING)){
            doLogin(username, password);
        }
        else {
            doRegister(username, password);
        }
    }

    private void doRegister(String username, String password) {
        final String check = passwordCheck.getText();
        try {
            loginService.register(username, password, check);
            alertService.info("Info", "Registrierung erfolgreich",
                    "Für Benutzer " + username + " wurde erfolgreich ein Account erstellt. Bitte melde dich im nächsten Schritt an.");
            toggleLoginAndRegister();
        }
        catch (LoginFailedException e){
            alertService.warn("Login fehlgeschlagen", e.getMessage());
        }
    }

    private void doLogin(String username, String password) {
        try {
            loginService.login(username, password);
            viewNavigator.loadMain();
            viewNavigator.closeLogin();
        }
        catch (LoginFailedException e){
            alertService.warn("Login fehlgeschlagen", e.getMessage());
        }
        catch (IOException e){
            LOGGER.error("Fenster konnte nicht geladen werden", e);
            System.exit(0);
        }
    }

    @FXML
    public void toggleLoginAndRegister(){
        if (loginButton.getText().equals(LOGIN_STRING)){
            loginButton.setText(REGISTER_STRING);
            toggleButton.setText(ACCOUNT_EXISTENT);
            passwordCheck.setVisible(true);
        } else {
            loginButton.setText(LOGIN_STRING);
            toggleButton.setText(NO_ACCOUNT);
            passwordCheck.setVisible(false);
        }
    }
}
