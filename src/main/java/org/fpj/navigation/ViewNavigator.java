package org.fpj.navigation;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.Getter;
import org.fpj.javafxcontroller.TransactionViewController;
import org.fpj.javafxcontroller.WallCommentViewController;
import org.fpj.javafxcontroller.ChatWindowController;
import org.fpj.javafxcontroller.TransactionDetailController;
import org.fpj.javafxcontroller.CsvImportDialogController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class ViewNavigator {

    private final ApplicationContext context;
    private final Map<String, NavigationContext> openWindows = new HashMap<>();
    private final Map<String, StackPane> overlayHosts = new HashMap<>();
    private final Map<String, EmbeddedNavigationContext> openEmbeddedViews = new HashMap<>();
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

    public void registerOverlayHost(String windowKey, StackPane overlayHost) {
        if (windowKey == null || windowKey.isBlank() || overlayHost == null) {
            return;
        }
        overlayHosts.put(windowKey, overlayHost);
    }

    public void unregisterOverlayHost(String windowKey, StackPane overlayHost) {
        if (windowKey == null || windowKey.isBlank() || overlayHost == null) {
            return;
        }
        StackPane registered = overlayHosts.get(windowKey);
        if (registered == overlayHost) {
            overlayHosts.remove(windowKey);
        }
    }

    private Image loadIcon(String resourcePath) {
        InputStream is = Objects.requireNonNull(
                getClass().getResourceAsStream(resourcePath),
                "Icon-Ressource nicht gefunden: " + resourcePath
        );
        return new Image(is);
    }

    private <T> NavigationResponse<T> loadStageView(String key, String fxml, String title, double width, double height, boolean alwaysOnTop, Class<T> controllerType)throws IOException {
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
        stage.setOnHidden(e -> {
            openWindows.remove(key);
            overlayHosts.remove(key);
            openEmbeddedViews.entrySet().removeIf(entry -> entry.getKey().startsWith(key + "|overlay|"));
        });

        if (controllerType == null) {
            return new NavigationResponse<>(null, false);
        }
        if (!controllerType.isInstance(controller)) {
            throw new IllegalStateException("Controller für " + fxml + " hat nicht den erwarteten Typ "
                    + controllerType.getName() + ", sondern " + controller.getClass().getName());
        }
        return new NavigationResponse<>(controllerType.cast(controller), false);
    }

    private <T> NavigationResponse<T> loadViewInWindowOverlay(Window ownerWindow, String viewKey, String fxml, Class<T> controllerType) throws IOException {
        String ownerKey = resolveWindowKey(ownerWindow)
                .orElseThrow(() -> new IllegalStateException("Das aufrufende Fenster ist im Navigator nicht registriert."));
        StackPane overlayHost = overlayHosts.get(ownerKey);
        if (overlayHost == null) {
            throw new IllegalStateException("Für das Fenster \"" + ownerKey + "\" ist kein Overlay-Host registriert.");
        }

        String contextKey = ownerKey + "|overlay|" + viewKey;
        EmbeddedNavigationContext<T> existing = openEmbeddedViews.get(contextKey);
        if (existing != null) {
            Node node = existing.rootNode();
            if (!overlayHost.getChildren().contains(node)) {
                overlayHost.getChildren().add(node);
            }
            node.toFront();
            return new NavigationResponse<>(existing.controller(), true);
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxml));
        loader.setControllerFactory(context::getBean);
        Parent root = loader.load();

        applyThemeIfAttached(ownerWindow, root);

        overlayHost.getChildren().add(root);
        root.toFront();

        if (controllerType == null) {
            openEmbeddedViews.put(contextKey, new EmbeddedNavigationContext<>(root, null));
            return new NavigationResponse<>(null, false);
        }

        Object controller = loader.getController();
        if (!controllerType.isInstance(controller)) {
            throw new IllegalStateException("Controller für " + fxml + " hat nicht den erwarteten Typ "
                    + controllerType.getName() + ", sondern " + controller.getClass().getName());
        }

        T typedController = controllerType.cast(controller);
        openEmbeddedViews.put(contextKey, new EmbeddedNavigationContext<>(root, typedController));
        return new NavigationResponse<>(typedController, false);
    }

    private Optional<String> resolveWindowKey(Window window) {
        if (!(window instanceof Stage stage)) {
            return Optional.empty();
        }

        for (Map.Entry<String, NavigationContext> entry : openWindows.entrySet()) {
            if (entry.getValue().windowStage() == stage) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    private void applyThemeIfAttached(Window ownerWindow, Parent root) {
        if (root == null) {
            return;
        }
        Scene ownerScene = ownerWindow != null ? ownerWindow.getScene() : null;
        if (ownerScene == null) {
            return;
        }

        root.getStyleClass().remove("rootDark");
        if (!isWhiteMode) {
            root.getStyleClass().add("rootDark");
        }
    }

    public void loadMain() throws IOException {
        loadStageView("main", "mainview/main_view.fxml", "PayTalk", 1280, 860, false, null);
    }

    public void loadLogin() throws IOException {
        loadStageView("login", "login.fxml", "PayTalk: Login", 400, 400, false, null);
    }

    public void closeLogin() {
        NavigationContext info = openWindows.get("login");
        if (info == null) { return; }
        Stage stage = info.windowStage();
        if (stage != null && stage.isShowing()) { stage.close(); } else { openWindows.remove("login"); }
    }

    public NavigationResponse<TransactionViewController> loadTransactionView() throws IOException {
        return loadStageView("transactionView", "transactionView.fxml", "PayTalk: Transaktionsübersicht", 1280, 860, false, TransactionViewController.class);
    }

    public NavigationResponse<WallCommentViewController> loadWallCommentView() throws IOException {
        return loadStageView("wallCommentView", "wallCommentView.fxml", "PayTalk: Pinnwand", 1280, 860, false, WallCommentViewController.class);
    }

    public NavigationResponse<ChatWindowController> loadChatView(String chatPartner) throws IOException {
        return loadStageView("chat:" + chatPartner, "chat_window.fxml", "PayTalk: Chat mit: " + chatPartner, 800, 600, false, ChatWindowController.class);
    }

    public NavigationResponse<TransactionDetailController> loadTransactionDetailView() throws IOException {
        return loadStageView("transactionDetail", "transaction_detail.fxml", "PayTalk: Transaktionsinfos", 600, 300, false, TransactionDetailController.class);
    }

    public NavigationResponse<CsvImportDialogController> loadCsvDialogView(Window ownerWindow) throws IOException {
        return loadView("csvImport", "csvImportDialog.fxml", CsvImportDialogController.class, ViewOpenMode.OVERLAY_IN_WINDOW, ownerWindow);
    }

    public void closeCsvDialog(Window ownerWindow) {
        resolveWindowKey(ownerWindow).ifPresent(ownerKey -> closeEmbeddedView(ownerKey, "csvImport"));
    }

    private <T> NavigationResponse<T> loadView(String key, String fxml, Class<T> controllerType, ViewOpenMode openMode, Window ownerWindow) throws IOException {
        if (openMode == ViewOpenMode.NEW_STAGE) {
            throw new IllegalStateException("Für NEW_STAGE muss loadStageView verwendet werden.");
        }
        if (openMode == ViewOpenMode.OVERLAY_IN_WINDOW) {
            return loadViewInWindowOverlay(ownerWindow, key, fxml, controllerType);
        }
        throw new UnsupportedOperationException("Open mode " + openMode + " ist aktuell nicht implementiert.");
    }

    private void closeEmbeddedView(String ownerKey, String viewKey) {
        String contextKey = ownerKey + "|overlay|" + viewKey;
        EmbeddedNavigationContext context = openEmbeddedViews.remove(contextKey);
        if (context != null && context.rootNode() != null) {
            context.rootNode().setVisible(false);
            context.rootNode().setManaged(false);
            if (context.rootNode().getParent() instanceof StackPane parentPane) {
                parentPane.getChildren().remove(context.rootNode());
            }
        }
    }

    private record EmbeddedNavigationContext<T>(Node rootNode, T controller) {}
}
