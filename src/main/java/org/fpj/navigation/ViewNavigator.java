package org.fpj.navigation;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.Getter;
import org.fpj.javafxcontroller.ChatWindowController;
import org.fpj.javafxcontroller.CsvImportDialogController;
import org.fpj.javafxcontroller.TransactionDetailController;
import org.fpj.javafxcontroller.TransactionViewController;
import org.fpj.javafxcontroller.WallCommentViewController;
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

    private static final String MAIN_WINDOW_KEY = "main";

    private final ApplicationContext context;
    private final Map<String, NavigationContext<?>> openWindows = new HashMap<>();
    private final Map<String, WindowLayers> registeredWindowLayers = new HashMap<>();
    private final Map<String, EmbeddedNavigationContext<?>> contentViews = new HashMap<>();
    private final Map<String, EmbeddedNavigationContext<?>> overlayViews = new HashMap<>();
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

        for (NavigationContext<?> navigationContext : openWindows.values()) {
            applyTheme(navigationContext.windowStage().getScene());
        }

        for (WindowLayers layers : registeredWindowLayers.values()) {
            applyThemeToNode(layers.mainContentLayer());
            applyThemeToNode(layers.overlayLayer());
            applyThemeToNode(layers.messageLayer());
        }
    }

    public void registerMainWindowLayers(String windowKey,
                                         StackPane mainContentLayer,
                                         StackPane overlayLayer,
                                         StackPane messageLayer,
                                         Node defaultMainContent) {
        if (windowKey == null || windowKey.isBlank() || mainContentLayer == null || overlayLayer == null || messageLayer == null) {
            return;
        }

        WindowLayers layers = new WindowLayers(mainContentLayer, overlayLayer, messageLayer, defaultMainContent);
        registeredWindowLayers.put(windowKey, layers);

        applyThemeToNode(mainContentLayer);
        applyThemeToNode(overlayLayer);
        applyThemeToNode(messageLayer);

        Scene scene = mainContentLayer.getScene();
        if (scene != null) {
            scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                    if (closeTopMostOverlay(windowKey)) {
                        event.consume();
                    }
                }
            });
        }
    }

    public void showMainView() {
        WindowLayers layers = requireWindowLayers(MAIN_WINDOW_KEY);
        Node defaultContent = layers.defaultMainContent();
        if (defaultContent == null) {
            return;
        }
        layers.mainContentLayer().getChildren().setAll(defaultContent);
    }

    public void loadMain() throws IOException {
        loadStageView(MAIN_WINDOW_KEY, "mainview/main_view.fxml", "PayTalk", 1280, 860, false, null);
    }

    public void loadLogin() throws IOException {
        loadStageView("login", "login.fxml", "PayTalk: Login", 400, 400, false, null);
    }

    public void closeLogin() {
        NavigationContext<?> info = openWindows.get("login");
        if (info == null) {
            return;
        }
        Stage stage = info.windowStage();
        if (stage != null && stage.isShowing()) {
            stage.close();
        } else {
            openWindows.remove("login");
        }
    }

    public NavigationResponse<TransactionViewController> loadTransactionView() throws IOException {
        return loadInMainContentLayer("transactionView", "transactionView.fxml", TransactionViewController.class);
    }

    public NavigationResponse<WallCommentViewController> loadWallCommentView() throws IOException {
        return loadInMainContentLayer("wallCommentView", "wallCommentView.fxml", WallCommentViewController.class);
    }

    public NavigationResponse<ChatWindowController> loadChatView(String chatPartner) throws IOException {
        return loadStageView("chat:" + chatPartner, "chat_window.fxml", "PayTalk: Chat mit: " + chatPartner, 800, 600, false, ChatWindowController.class);
    }

    public NavigationResponse<TransactionDetailController> loadTransactionDetailView() throws IOException {
        Window mainWindow = requireMainWindow();
        return loadInOverlayLayer(mainWindow, "transactionDetail", "transaction_detail.fxml", TransactionDetailController.class);
    }

    public NavigationResponse<CsvImportDialogController> loadCsvDialogView(Window ownerWindow) throws IOException {
        return loadInOverlayLayer(ownerWindow, "csvImport", "csvImportDialog.fxml", CsvImportDialogController.class);
    }

    public void closeCsvDialog(Window ownerWindow) {
        resolveWindowKey(ownerWindow).ifPresent(ownerKey -> closeOverlay(ownerKey, "csvImport"));
    }

    private <T> NavigationResponse<T> loadStageView(String key,
                                                    String fxml,
                                                    String title,
                                                    double width,
                                                    double height,
                                                    boolean alwaysOnTop,
                                                    Class<T> controllerType) throws IOException {
        NavigationContext<?> existing = openWindows.get(key);
        if (existing != null) {
            Stage stage = existing.windowStage();
            if (stage.isShowing()) {
                stage.toFront();
                stage.requestFocus();
                if (controllerType == null) {
                    return new NavigationResponse<>(null, true);
                }
                return new NavigationResponse<>(controllerType.cast(existing.controller()), true);
            }
            openWindows.remove(key);
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
        openWindows.put(key, new NavigationContext<>(stage, controller));
        stage.setOnHidden(e -> cleanupWindow(key));

        if (controllerType == null) {
            return new NavigationResponse<>(null, false);
        }
        if (!controllerType.isInstance(controller)) {
            throw new IllegalStateException("Controller für " + fxml + " hat nicht den erwarteten Typ "
                    + controllerType.getName() + ", sondern " + controller.getClass().getName());
        }
        return new NavigationResponse<>(controllerType.cast(controller), false);
    }

    private <T> NavigationResponse<T> loadInMainContentLayer(String viewKey, String fxml, Class<T> controllerType) throws IOException {
        WindowLayers layers = requireWindowLayers(MAIN_WINDOW_KEY);
        EmbeddedNavigationContext<?> existing = contentViews.get(viewKey);

        if (existing != null) {
            layers.mainContentLayer().getChildren().setAll(existing.rootNode());
            @SuppressWarnings("unchecked") T controller = (T) existing.controller();
            return new NavigationResponse<>(controller, true);
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxml));
        loader.setControllerFactory(context::getBean);
        Parent root = loader.load();
        applyThemeToNode(root);

        Object controller = loader.getController();
        if (!controllerType.isInstance(controller)) {
            throw new IllegalStateException("Controller für " + fxml + " hat nicht den erwarteten Typ "
                    + controllerType.getName() + ", sondern " + controller.getClass().getName());
        }

        layers.mainContentLayer().getChildren().setAll(root);
        T typedController = controllerType.cast(controller);
        contentViews.put(viewKey, new EmbeddedNavigationContext<>(root, typedController));
        return new NavigationResponse<>(typedController, false);
    }

    private <T> NavigationResponse<T> loadInOverlayLayer(Window ownerWindow,
                                                         String viewKey,
                                                         String fxml,
                                                         Class<T> controllerType) throws IOException {
        String windowKey = resolveWindowKey(ownerWindow)
                .orElseThrow(() -> new IllegalStateException("Das aufrufende Fenster ist im Navigator nicht registriert."));

        WindowLayers layers = requireWindowLayers(windowKey);
        String contextKey = windowKey + "|overlay|" + viewKey;
        EmbeddedNavigationContext<?> existing = overlayViews.get(contextKey);

        if (existing != null) {
            layers.overlayLayer().getChildren().setAll(existing.rootNode());
            existing.rootNode().requestFocus();
            @SuppressWarnings("unchecked") T controller = (T) existing.controller();
            return new NavigationResponse<>(controller, true);
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxml));
        loader.setControllerFactory(context::getBean);
        Parent root = loader.load();
        applyThemeToNode(root);

        Node overlayShell = buildOverlayShell(windowKey, viewKey, root);
        layers.overlayLayer().getChildren().setAll(overlayShell);
        overlayShell.requestFocus();

        Object controller = loader.getController();
        if (!controllerType.isInstance(controller)) {
            throw new IllegalStateException("Controller für " + fxml + " hat nicht den erwarteten Typ "
                    + controllerType.getName() + ", sondern " + controller.getClass().getName());
        }

        T typedController = controllerType.cast(controller);
        overlayViews.put(contextKey, new EmbeddedNavigationContext<>(overlayShell, typedController));
        return new NavigationResponse<>(typedController, false);
    }

    private Node buildOverlayShell(String ownerKey, String viewKey, Parent content) {
        StackPane shell = new StackPane();
        shell.getStyleClass().add("app-overlay-shell");
        shell.setFocusTraversable(true);

        StackPane card = new StackPane(content);
        card.getStyleClass().add("app-overlay-card");

        Button closeButton = new Button("✕");
        closeButton.getStyleClass().add("app-overlay-close-button");
        closeButton.setOnAction(event -> closeOverlay(ownerKey, viewKey));
        closeButton.setFocusTraversable(false);

        StackPane.setAlignment(card, Pos.CENTER);
        StackPane.setMargin(card, new Insets(24));
        StackPane.setAlignment(closeButton, Pos.TOP_RIGHT);
        StackPane.setMargin(closeButton, new Insets(24));

        shell.getChildren().addAll(card, closeButton);
        shell.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ESCAPE -> {
                    closeOverlay(ownerKey, viewKey);
                    event.consume();
                }
                default -> {
                }
            }
        });

        return shell;
    }

    private boolean closeTopMostOverlay(String windowKey) {
        WindowLayers layers = registeredWindowLayers.get(windowKey);
        if (layers == null || layers.overlayLayer().getChildren().isEmpty()) {
            return false;
        }

        Optional<String> anyOverlayKey = overlayViews.keySet().stream()
                .filter(key -> key.startsWith(windowKey + "|overlay|"))
                .findFirst();

        if (anyOverlayKey.isEmpty()) {
            return false;
        }

        String contextKey = anyOverlayKey.get();
        String viewKey = contextKey.substring((windowKey + "|overlay|").length());
        closeOverlay(windowKey, viewKey);
        return true;
    }

    private void closeOverlay(String ownerKey, String viewKey) {
        String contextKey = ownerKey + "|overlay|" + viewKey;
        EmbeddedNavigationContext<?> context = overlayViews.remove(contextKey);
        if (context == null) {
            return;
        }

        WindowLayers layers = registeredWindowLayers.get(ownerKey);
        if (layers != null) {
            layers.overlayLayer().getChildren().remove(context.rootNode());
        }
    }

    private WindowLayers requireWindowLayers(String windowKey) {
        WindowLayers layers = registeredWindowLayers.get(windowKey);
        if (layers == null) {
            throw new IllegalStateException("Für das Fenster \"" + windowKey + "\" sind keine Layer registriert.");
        }
        return layers;
    }

    private Window requireMainWindow() {
        NavigationContext<?> mainContext = openWindows.get(MAIN_WINDOW_KEY);
        if (mainContext == null) {
            throw new IllegalStateException("Das Hauptfenster ist aktuell nicht geöffnet.");
        }
        return mainContext.windowStage();
    }

    private Optional<String> resolveWindowKey(Window window) {
        if (!(window instanceof Stage stage)) {
            return Optional.empty();
        }

        for (Map.Entry<String, NavigationContext<?>> entry : openWindows.entrySet()) {
            if (entry.getValue().windowStage() == stage) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    private void cleanupWindow(String windowKey) {
        openWindows.remove(windowKey);
        registeredWindowLayers.remove(windowKey);
        overlayViews.entrySet().removeIf(entry -> entry.getKey().startsWith(windowKey + "|overlay|"));
    }

    private void applyTheme(Scene scene) {
        if (scene == null || scene.getRoot() == null) {
            return;
        }
        applyThemeToNode(scene.getRoot());
    }

    private void applyThemeToNode(Node node) {
        ThemeStyleManager.applyTo(node, !isWhiteMode);
    }

    private void addIconsToList() {
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

    private record EmbeddedNavigationContext<T>(Node rootNode, T controller) {
    }

    private record WindowLayers(StackPane mainContentLayer, StackPane overlayLayer, StackPane messageLayer, Node defaultMainContent) {
    }
}
