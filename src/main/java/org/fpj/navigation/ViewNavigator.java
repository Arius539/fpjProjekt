package org.fpj.navigation;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import lombok.Getter;
import org.fpj.javafxcontroller.TransactionViewController;
import org.fpj.javafxcontroller.WallCommentViewController;
import org.fpj.javafxcontroller.ChatWindowController;
import org.fpj.javafxcontroller.TransactionDetailController;
import org.fpj.javafxcontroller.CsvImportDialogController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class ViewNavigator {

    private final ApplicationContext context;
    private final Map<String, NavigationContext> openWindows = new HashMap<>();
    private final ArrayList<Image> appIcons;
    @Getter
    private boolean isWhiteMode = false;

    @Autowired
    public ViewNavigator(ApplicationContext context) {
        this.context = context;
        this.appIcons = new ArrayList<>();
        addIconsToList();
    }

    public void setWhiteMode(boolean whiteMode) {
        this.isWhiteMode = whiteMode;
        for (NavigationContext ctx : openWindows.values()) {
            Scene scene = ctx.windowStage().getScene();
            if (scene != null) {
                applyTheme(scene);
            }
        }
    }

    private void applyTheme(Scene scene) {
        if (scene == null) {
            return;
        }

        Parent root = scene.getRoot();
        if (root == null) {
            return;
        }

        var styleClasses = root.getStyleClass();

        styleClasses.remove("rootDark");

        if (!isWhiteMode) {
            if (!styleClasses.contains("rootDark")) {
                styleClasses.add("rootDark");
            }
        }
    }

    private void addIconsToList(){
        appIcons.add(loadIcon("/icons/app-icon-16.png"));
        appIcons.add(loadIcon("/icons/app-icon-32.png"));
        appIcons.add(loadIcon("/icons/app-icon-48.png"));
        appIcons.add(loadIcon("/icons/app-icon-64.png"));
        appIcons.add(loadIcon("/icons/app-icon-128.png"));
        appIcons.add(loadIcon("/icons/app-icon-256.png"));
        appIcons.add(loadIcon("/icons/app-icon-512.png"));
        appIcons.add(loadIcon("/icons/app-icon-1024.png"));
    }

    private Image loadIcon(String resourcePath) {
        InputStream is = Objects.requireNonNull(
                getClass().getResourceAsStream(resourcePath),
                "Icon-Ressource nicht gefunden: " + resourcePath
        );
        return new Image(is);
    }

    private <T> NavigationResponse<T> loadView(String key, String fxml, String title, double width, double height, boolean alwaysOnTop, Class<T> controllerType)throws IOException {
        NavigationContext<T> existing = openWindows.get(key);
        if (existing != null) {
            Stage stage = existing.windowStage();
            if (stage.isShowing()) {
                stage.toFront();
                stage.requestFocus();
                if (controllerType == null) {
                    return new NavigationResponse<>(null, true);
                }
                return new NavigationResponse<>(controllerType.cast(existing.controller()), true);
            } else {
                openWindows.remove(key);
            }
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxml));
        loader.setControllerFactory(context::getBean);
        Parent root = loader.load();

        Stage stage = new Stage();
        stage.setTitle(title);

        Scene scene = new Scene(root, width, height);
        applyTheme(scene);

        stage.setScene(scene);
        stage.setAlwaysOnTop(alwaysOnTop);
        stage.setIconified(false);
        stage.show();
        stage.toFront();
        stage.requestFocus();

        stage.getIcons().setAll(appIcons);

        Object controller = loader.getController();
        NavigationContext<T> info = new NavigationContext(stage, controller);
        openWindows.put(key, info);
        stage.setOnHidden(e -> openWindows.remove(key));

        if (controllerType == null) {
            return new NavigationResponse<>(null, false);
        }
        if (!controllerType.isInstance(controller)) {
            throw new IllegalStateException("Controller für " + fxml + " hat nicht den erwarteten Typ "
                    + controllerType.getName() + ", sondern " + controller.getClass().getName());
        }
        return new NavigationResponse<>(controllerType.cast(controller), false);
    }

    public void loadMain() throws IOException {
        loadView("main", "mainview/main_view.fxml", "PayTalk", 1280, 860, false, null);
    }

    public void loadLogin() throws IOException {
        loadView("login", "login.fxml", "PayTalk: Login", 400, 400, false, null);
    }

    public void closeLogin() {
        NavigationContext info = openWindows.get("login");
        if (info == null) { return; }
        Stage stage = info.windowStage();
        if (stage != null && stage.isShowing()) { stage.close(); } else { openWindows.remove("login"); }
    }

    public NavigationResponse<TransactionViewController> loadTransactionView() throws IOException {
        return loadView("transactionView", "transactionView.fxml", "PayTalk: Transaktionsübersicht", 1280, 860, false, TransactionViewController.class);
    }

    public NavigationResponse<WallCommentViewController> loadWallCommentView() throws IOException {
        return loadView("wallCommentView", "wallCommentView.fxml", "PayTalk: Pinnwand", 1280, 860, false, WallCommentViewController.class);
    }

    public NavigationResponse<ChatWindowController> loadChatView(String chatPartner) throws IOException {
        return loadView("chat:" + chatPartner, "chat_window.fxml", "PayTalk: Chat mit: " + chatPartner, 800, 600, false, ChatWindowController.class);
    }

    public NavigationResponse<TransactionDetailController> loadTransactionDetailView() throws IOException {
        return loadView("transactionDetail", "transaction_detail.fxml", "PayTalk: Transaktionsinfos", 600, 300, false, TransactionDetailController.class);
    }

    public NavigationResponse<CsvImportDialogController> loadCsvDialogView() throws IOException {
        return loadView("csvImport", "csvImportDialog.fxml", "PayTalk: Csv Importer", 800, 400, false, CsvImportDialogController.class);
    }

    public void closeCsvDialog() {
        NavigationContext info = openWindows.get("csvImport");
        if (info == null) { return; }
        Stage stage = info.windowStage();
        if (stage != null && stage.isShowing()) { stage.close(); } else { openWindows.remove("login"); }
    }
}
