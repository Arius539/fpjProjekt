package org.fpj.javafxcontroller.mainView;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.application.Platform;
import org.fpj.util.AlertService;
import org.fpj.navigation.NavigationResponse;
import org.fpj.navigation.ViewNavigator;
import org.fpj.javafxcontroller.TransactionViewController;
import org.fpj.javafxcontroller.WallCommentViewController;
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
    private HBox dashboardContent;

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
        Platform.runLater(() -> viewNavigator.registerMainWindowLayers("main", mainContentLayer, overlayLayer, messageLayer, dashboardContent));

        boolean isDark = !viewNavigator.isWhiteMode();
        themeButton.setSelected(isDark);
        updateThemeIcon(isDark);
    }
    // </editor-fold>

    @FXML
    public void actionTransactions() {
        try{
            NavigationResponse<TransactionViewController> response= viewNavigator.loadTransactionView();
            if(!response.isLoaded()) response.controller().initialize(currentUser, null);
        }catch (Exception e){
            this.alertService.error("Fehler", "Es ist eine Fehler beim Laden des Transaktionsfensters aufgetreten");
        }

    }

    @FXML
    public void actionWallComments() {
        try{
           NavigationResponse<WallCommentViewController> response= viewNavigator.loadWallCommentView();
           if(!response.isLoaded()) response.controller().load(currentUser, currentUser);
        }catch (Exception e){
            this.alertService.error( "Fehler", "Es ist eine Fehler beim Laden des Transaktionsfensters aufgetreten");
        }
    }

    @FXML
    public void actionDashboard() {
        viewNavigator.showMainDashboard();
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
