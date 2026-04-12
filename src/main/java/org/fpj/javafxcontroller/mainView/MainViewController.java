package org.fpj.javafxcontroller.mainView;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Window;
import javafx.application.Platform;
import org.fpj.util.AlertService;
import org.fpj.navigation.api.ViewNavigator;
import org.fpj.navigation.api.ViewOpenMode;
import org.fpj.navigation.fx.NavigationMenuBinder;
import org.fpj.users.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class MainViewController {

    private final ApplicationContext applicationContext;
    private final TransactionsLiteViewController transactionsLiteController;
    private final ChatPreviewController chatPreviewController;
    private final AlertService alertService;
    private final ViewNavigator viewNavigator;

    @FXML
    private ToggleButton themeButton;

    @FXML
    private Button transactionsButton;

    @FXML
    private Button wallCommentsButton;

    @FXML
    private Label lblEmail;

    @FXML
    private Label lblBalance;

    @FXML
    private StackPane mainContentLayer;

    @FXML
    private StackPane overlayLayer;

    @FXML
    private StackPane messageLayer;

    @FXML
    private GridPane mainViewContent;

    private User currentUser;

    @Autowired
    public MainViewController(ViewNavigator viewNavigator,ApplicationContext context, TransactionsLiteViewController transactionsLiteController, ChatPreviewController chatPreviewController, AlertService alertService) {
        this.viewNavigator = viewNavigator;
        this.applicationContext = context;
        this.transactionsLiteController = transactionsLiteController;
        this.chatPreviewController = chatPreviewController;
        this.alertService = alertService;
    }

    // <editor-fold defaultstate="collapsed" desc="initialize">
    @FXML
    public void initialize() {
        try {
            currentUser = applicationContext.getBean("loggedInUser", User.class);
        } catch (Exception e) {
            alertService.error("Benutzer konnte nicht geladen werden", "Der angemeldete Benutzer konnte nicht aus dem Kontext geladen werden.");
            return;
        }

        lblEmail.setText(currentUser.getUsername());
        transactionsLiteController.initialize(currentUser, this::updateBalanceLabel);
        chatPreviewController.initialize(currentUser);
        Platform.runLater(() -> viewNavigator.attachMainWindowHost("main", mainContentLayer, overlayLayer, messageLayer, mainViewContent));
        NavigationMenuBinder.attach(
                transactionsButton,
                viewNavigator.defaultModeForTransactionView(),
                this::openTransactionView
        );
        NavigationMenuBinder.attach(
                wallCommentsButton,
                viewNavigator.defaultModeForWallCommentView(),
                this::openWallCommentView
        );

        boolean isDark = !viewNavigator.isWhiteMode();
        themeButton.setSelected(isDark);
        updateThemeIcon(isDark);
    }
    // </editor-fold>

    private void openTransactionView(ViewOpenMode openMode) {
        try{
            Window ownerWindow = mainContentLayer.getScene().getWindow();
            viewNavigator.loadTransactionView(
                    openMode,
                    ownerWindow,
                    controller -> controller.initialize(currentUser, null)
            );
        }catch (Exception e){
            this.alertService.error("Fehler", "Es ist ein Fehler beim Laden der Transaktionsansicht aufgetreten.");
        }

    }

    private void openWallCommentView(ViewOpenMode openMode) {
        try{
           Window ownerWindow = mainContentLayer.getScene().getWindow();
           viewNavigator.loadWallCommentView(
                   openMode,
                   ownerWindow,
                   controller -> controller.load(currentUser, currentUser)
           );
        }catch (Exception e){
            this.alertService.error("Fehler", "Es ist ein Fehler beim Laden der Pinnwand aufgetreten.");
        }
    }

    @FXML
    private void onToggleTheme() {
        boolean useDark = themeButton.isSelected();
        updateThemeIcon(useDark);
        this.viewNavigator.setWhiteMode(!useDark);
    }

    private void updateBalanceLabel(String balance) {
        lblBalance.setText(balance);
    }

    private void updateThemeIcon(boolean isSelected) {
        if (isSelected) {
            themeButton.setText("🌙");
        } else {
            themeButton.setText("☀");
        }
    }
}
