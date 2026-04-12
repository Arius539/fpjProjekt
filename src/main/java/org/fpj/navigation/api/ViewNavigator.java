package org.fpj.navigation.api;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.fpj.javafxcontroller.ChatWindowController;
import org.fpj.javafxcontroller.CsvImportDialogController;
import org.fpj.javafxcontroller.TransactionDetailController;
import org.fpj.javafxcontroller.TransactionViewController;
import org.fpj.javafxcontroller.WallCommentViewController;
import org.fpj.navigation.fx.FxViewLoader;
import org.fpj.navigation.fx.OverlayShellFactory;
import org.fpj.navigation.fx.WindowShellFactory;
import org.fpj.navigation.model.OpenRequest;
import org.fpj.navigation.model.ViewTarget;
import org.fpj.navigation.window.PresentedView;
import org.fpj.navigation.window.WindowHost;
import org.fpj.navigation.window.WindowSession;
import org.fpj.ui.theme.ThemeStyleManager;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

@Component
public class ViewNavigator {

    private static final String MAIN_WINDOW_KEY = "main";

    private final FxViewLoader fxViewLoader;
    private final WindowShellFactory windowShellFactory;
    private final OverlayShellFactory overlayShellFactory;
    private final Map<String, WindowSession> sessions = new HashMap<>();
    private final ArrayList<Image> appIcons = new ArrayList<>();

    private boolean whiteMode;

    public ViewNavigator(FxViewLoader fxViewLoader,
                         WindowShellFactory windowShellFactory,
                         OverlayShellFactory overlayShellFactory) {
        this.fxViewLoader = fxViewLoader;
        this.windowShellFactory = windowShellFactory;
        this.overlayShellFactory = overlayShellFactory;
        addIconsToList();
    }

    public boolean isWhiteMode() {
        return whiteMode;
    }

    public void setWhiteMode(boolean whiteMode) {
        this.whiteMode = whiteMode;
        sessions.values().forEach(session -> session.applyTheme(this::applyThemeToNode));
    }

    public void attachMainWindowHost(String windowKey,
                                     StackPane mainContentLayer,
                                     StackPane overlayLayer,
                                     StackPane messageLayer,
                                     Node defaultMainContent) {
        if (windowKey == null || windowKey.isBlank()
                || mainContentLayer == null
                || overlayLayer == null
                || messageLayer == null
                || defaultMainContent == null) {
            return;
        }

        WindowSession session = sessions.computeIfAbsent(
                windowKey,
                key -> new WindowSession(key, null, MAIN_WINDOW_KEY.equals(key))
        );

        if (mainContentLayer.getScene() != null && mainContentLayer.getScene().getWindow() instanceof Stage stage) {
            session.attachStage(stage);
        }

        StackPane rootNode = resolveHostRoot(mainContentLayer);
        session.attachHost(new WindowHost(rootNode, mainContentLayer, overlayLayer, messageLayer), defaultMainContent);
        session.applyTheme(this::applyThemeToNode);

        if (mainContentLayer.getScene() != null) {
            installEscapeHandler(windowKey, mainContentLayer.getScene());
        }
    }

    public void showMainView() {
        showMainView(MAIN_WINDOW_KEY);
    }

    public ViewOpenMode defaultModeForTransactionView() {
        return defaultModeFor(ViewTarget.TRANSACTION_VIEW);
    }

    public ViewOpenMode defaultModeForWallCommentView() {
        return defaultModeFor(ViewTarget.WALL_COMMENT_VIEW);
    }

    public ViewOpenMode defaultModeForChatWindow() {
        return defaultModeFor(ViewTarget.CHAT_WINDOW);
    }

    public ViewOpenMode defaultModeForCsvImport() {
        return defaultModeFor(ViewTarget.CSV_IMPORT);
    }

    public ViewOpenMode defaultModeForTransactionDetail() {
        return defaultModeFor(ViewTarget.TRANSACTION_DETAIL);
    }

    public ViewOpenMode resolveChildDefaultOpenMode(ViewOpenMode targetDefaultMode, NavigationHandle sourceHandle) {
        if (targetDefaultMode == null) {
            return null;
        }
        if (targetDefaultMode != ViewOpenMode.REPLACE_MAIN_CONTENT) {
            return targetDefaultMode;
        }
        if (sourceHandle != null && sourceHandle.presentationKind() == NavigationPresentationKind.OVERLAY) {
            return ViewOpenMode.OVERLAY_IN_WINDOW;
        }
        return targetDefaultMode;
    }

    public TransactionViewController loadTransactionView(ViewOpenMode openMode,
                                                         Window ownerWindow,
                                                         Consumer<TransactionViewController> initializer) throws IOException {
        return loadTransactionView(openMode, ownerWindow, null, initializer);
    }

    public TransactionViewController loadTransactionView(ViewOpenMode openMode,
                                                         Window ownerWindow,
                                                         NavigationHandle sourceHandle,
                                                         Consumer<TransactionViewController> initializer) throws IOException {
        InternalOpenResult<TransactionViewController> result =
                open(ViewTarget.TRANSACTION_VIEW.<TransactionViewController>request(openMode, ownerWindow), sourceHandle);
        runInitializerIfNew(result.reusedExistingInstance(), result.controller(), initializer);
        return result.controller();
    }

    public WallCommentViewController loadWallCommentView(ViewOpenMode openMode, Window ownerWindow) throws IOException {
        return loadWallCommentView(openMode, ownerWindow, null, null);
    }

    public WallCommentViewController loadWallCommentView(ViewOpenMode openMode,
                                                         Window ownerWindow,
                                                         Consumer<WallCommentViewController> initializer) throws IOException {
        return loadWallCommentView(openMode, ownerWindow, null, initializer);
    }

    public WallCommentViewController loadWallCommentView(ViewOpenMode openMode,
                                                         Window ownerWindow,
                                                         NavigationHandle sourceHandle,
                                                         Consumer<WallCommentViewController> initializer) throws IOException {
        InternalOpenResult<WallCommentViewController> result =
                open(ViewTarget.WALL_COMMENT_VIEW.<WallCommentViewController>request(openMode, ownerWindow), sourceHandle);
        runInitializerIfNew(result.reusedExistingInstance(), result.controller(), initializer);
        return result.controller();
    }

    public ChatWindowController loadChatView(String chatPartner, Window ownerWindow) throws IOException {
        return loadChatView(chatPartner, defaultModeForChatWindow(), ownerWindow, null, null);
    }

    public ChatWindowController loadChatView(String chatPartner,
                                             ViewOpenMode openMode,
                                             Window ownerWindow) throws IOException {
        return loadChatView(chatPartner, openMode, ownerWindow, null, null);
    }

    public ChatWindowController loadChatView(String chatPartner,
                                             ViewOpenMode openMode,
                                             Window ownerWindow,
                                             Consumer<ChatWindowController> initializer) throws IOException {
        return loadChatView(chatPartner, openMode, ownerWindow, null, initializer);
    }

    public ChatWindowController loadChatView(String chatPartner,
                                             ViewOpenMode openMode,
                                             Window ownerWindow,
                                             NavigationHandle sourceHandle,
                                             Consumer<ChatWindowController> initializer) throws IOException {
        String windowTitle = "PayTalk: Chat mit: " + chatPartner;
        String stageKey = openMode == ViewOpenMode.NEW_STAGE ? "chat:" + chatPartner : null;

        OpenRequest<ChatWindowController> request = ViewTarget.CHAT_WINDOW
                .<ChatWindowController>request(openMode, ownerWindow)
                .withStageInstance(stageKey, openMode == ViewOpenMode.NEW_STAGE ? windowTitle : null);

        InternalOpenResult<ChatWindowController> result = open(request, sourceHandle);
        runInitializerIfNew(result.reusedExistingInstance(), result.controller(), initializer);
        return result.controller();
    }

    public TransactionDetailController loadTransactionDetailView(Window ownerWindow) throws IOException {
        return loadTransactionDetailView(ownerWindow, null);
    }

    public TransactionDetailController loadTransactionDetailView(Window ownerWindow,
                                                                 Consumer<TransactionDetailController> initializer) throws IOException {
        return loadTransactionDetailView(defaultModeForTransactionDetail(), ownerWindow, null, initializer);
    }

    public TransactionDetailController loadTransactionDetailView(ViewOpenMode openMode,
                                                                 Window ownerWindow,
                                                                 NavigationHandle sourceHandle,
                                                                 Consumer<TransactionDetailController> initializer) throws IOException {
        InternalOpenResult<TransactionDetailController> result =
                open(ViewTarget.TRANSACTION_DETAIL.<TransactionDetailController>request(openMode, ownerWindow), sourceHandle);
        runInitializerIfNew(result.reusedExistingInstance(), result.controller(), initializer);
        return result.controller();
    }

    public <E> CsvImportDialogController<E> loadCsvDialogView(ViewOpenMode openMode, Window ownerWindow) throws IOException {
        return loadCsvDialogView(openMode, ownerWindow, null, null);
    }

    public <E> CsvImportDialogController<E> loadCsvDialogView(ViewOpenMode openMode,
                                                              Window ownerWindow,
                                                              Consumer<CsvImportDialogController<E>> initializer) throws IOException {
        return loadCsvDialogView(openMode, ownerWindow, null, initializer);
    }

    public <E> CsvImportDialogController<E> loadCsvDialogView(ViewOpenMode openMode,
                                                              Window ownerWindow,
                                                              NavigationHandle sourceHandle,
                                                              Consumer<CsvImportDialogController<E>> initializer) throws IOException {
        InternalOpenResult<CsvImportDialogController<E>> result =
                open(ViewTarget.CSV_IMPORT.<CsvImportDialogController<E>>request(openMode, ownerWindow), sourceHandle);
        runInitializerIfNew(result.reusedExistingInstance(), result.controller(), initializer);
        return result.controller();
    }

    public void loadMain() throws IOException {
        open(rootWindowRequest(ViewTarget.MAIN_VIEW, MAIN_WINDOW_KEY), null);
    }

    public void loadLogin() throws IOException {
        open(rootWindowRequest(ViewTarget.LOGIN, "login"), null);
    }

    public void closeLogin() {
        closeWindow("login");
    }

    public void closeCurrentPresentation(Window ownerWindow) {
        resolveWindowKey(ownerWindow).ifPresent(windowKey -> {
            WindowSession session = sessions.get(windowKey);
            if (session == null) {
                return;
            }

            if (session.hasOverlays()) {
                session.closeTopOverlay();
                return;
            }

            if (MAIN_WINDOW_KEY.equals(windowKey)) {
                showMainView(windowKey);
            } else {
                closeWindow(windowKey);
            }
        });
    }

    private OpenRequest<Object> rootWindowRequest(ViewTarget target, String windowKey) {
        return target
                .<Object>request(ViewOpenMode.NEW_STAGE, null)
                .withStageInstance(windowKey, target.defaultStageTitle());
    }

    private void showMainView(String windowKey) {
        WindowSession session = sessions.get(windowKey);
        if (session == null || !session.hasRootContent()) {
            focusWindow(windowKey);
            return;
        }

        session.showRoot();
        focusWindow(windowKey);
    }

    static <T> void runInitializerIfNew(boolean reusedExistingInstance, T controller, Consumer<T> initializer) {
        if (!reusedExistingInstance && controller != null && initializer != null) {
            initializer.accept(controller);
        }
    }

    private ViewOpenMode defaultModeFor(ViewTarget target) {
        return target.defaultOpenMode();
    }

    private <T> InternalOpenResult<T> open(OpenRequest<T> request, NavigationHandle sourceHandle) throws IOException {
        return switch (request.openMode()) {
            case REPLACE_MAIN_CONTENT -> openInMainContent(request, sourceHandle);
            case OVERLAY_IN_WINDOW -> openInOverlay(request);
            case NEW_STAGE -> openInNewStage(request);
        };
    }

    private <T> InternalOpenResult<T> openInMainContent(OpenRequest<T> request, NavigationHandle sourceHandle) throws IOException {
        String windowKey = resolveWindowKey(request.ownerWindow())
                .orElseThrow(() -> new IllegalStateException("Das Ziel-Fenster ist im Navigator nicht registriert."));
        WindowSession session = requireSession(windowKey);
        requireHost(session);

        FxViewLoader.LoadedFxView<T> loadedView = fxViewLoader.load(request.target());
        PresentedView contentView = PresentedView.content(request.target(), loadedView.rootNode(), loadedView.controller());

        applyThemeToNode(loadedView.rootNode());
        NavigationHandle navigationHandle = createContentHandle(session, contentView);
        contentView.setNavigationHandle(navigationHandle);
        applyNavigationHandle(loadedView.controller(), navigationHandle);

        if (shouldPromoteOverlayContent(sourceHandle)) {
            session.promoteOverlaysToContentHistory(overlayView -> promoteOverlayToContent(session, overlayView));
        }
        session.pushContent(contentView);
        return new InternalOpenResult<>(loadedView.controller(), false);
    }

    private <T> InternalOpenResult<T> openInOverlay(OpenRequest<T> request) throws IOException {
        String windowKey = resolveWindowKey(request.ownerWindow())
                .orElseThrow(() -> new IllegalStateException("Das aufrufende Fenster ist im Navigator nicht registriert."));
        WindowSession session = requireSession(windowKey);
        requireHost(session);

        FxViewLoader.LoadedFxView<T> loadedView = fxViewLoader.load(request.target());
        applyThemeToNode(loadedView.rootNode());

        final PresentedView[] overlayReference = new PresentedView[1];
        Node shellNode = overlayShellFactory.build(loadedView.rootNode(), () -> session.closeOverlay(overlayReference[0]));
        PresentedView overlayView = PresentedView.overlay(request.target(), shellNode, loadedView.rootNode(), loadedView.controller());
        overlayReference[0] = overlayView;

        NavigationHandle navigationHandle = createOverlayHandle(session, overlayView);
        overlayView.setNavigationHandle(navigationHandle);
        applyNavigationHandle(loadedView.controller(), navigationHandle);

        session.pushOverlay(overlayView);
        overlayView.rootNode().requestFocus();
        return new InternalOpenResult<>(loadedView.controller(), false);
    }

    private <T> InternalOpenResult<T> openInNewStage(OpenRequest<T> request) throws IOException {
        String reusableStageKey = resolveReusableStageKey(request);
        if (reusableStageKey != null) {
            WindowSession existing = sessions.get(reusableStageKey);
            if (existing != null && existing.stage() != null && existing.stage().isShowing()) {
                focusStage(existing.stage());
                T controller = request.controllerType().cast(existing.currentController());
                return new InternalOpenResult<>(controller, true);
            }
        }

        FxViewLoader.LoadedFxView<T> loadedView = fxViewLoader.load(request.target());
        applyThemeToNode(loadedView.rootNode());

        String windowKey = reusableStageKey != null ? reusableStageKey : buildWindowKey(request.target());
        String openerWindowKey = resolveWindowKey(request.ownerWindow()).orElse(null);

        WindowSession session = new WindowSession(windowKey, openerWindowKey, MAIN_WINDOW_KEY.equals(windowKey));
        Stage stage = new Stage();
        session.attachStage(stage);

        Scene scene;

        if (request.target().stageUsesWindowShell()) {
            WindowHost windowHost = windowShellFactory.create();
            PresentedView rootView = PresentedView.content(request.target(), loadedView.rootNode(), loadedView.controller());
            NavigationHandle navigationHandle = createContentHandle(session, rootView);
            rootView.setNavigationHandle(navigationHandle);
            applyNavigationHandle(loadedView.controller(), navigationHandle);

            session.attachHost(windowHost, rootView);
            session.applyTheme(this::applyThemeToNode);
            scene = new Scene(windowHost.rootNode(), request.target().defaultWidth(), request.target().defaultHeight());
        } else {
            scene = new Scene(loadedView.rootNode(), request.target().defaultWidth(), request.target().defaultHeight());
        }

        applyTheme(scene);
        stage.setTitle(request.stageTitleOverride() != null ? request.stageTitleOverride() : request.target().defaultStageTitle());
        stage.setAlwaysOnTop(request.target().alwaysOnTop());
        stage.setIconified(false);
        stage.getIcons().setAll(appIcons);
        stage.setScene(scene);

        sessions.put(windowKey, session);
        installEscapeHandler(windowKey, scene);
        stage.setOnHidden(ignoredEvent -> cleanupWindow(windowKey));
        stage.show();
        focusStage(stage);

        return new InternalOpenResult<>(loadedView.controller(), false);
    }

    private NavigationHandle createContentHandle(WindowSession session, PresentedView contentView) {
        return NavigationHandle.of(
                () -> {
                    if (session.closeContent(contentView)) {
                        closeWindow(session.windowKey());
                    } else {
                        focusWindow(session.windowKey());
                    }
                },
                () -> {
                    if (session.back()) {
                        closeWindow(session.windowKey());
                    } else {
                        focusWindow(session.windowKey());
                    }
                },
                session.stage(),
                NavigationPresentationKind.CONTENT
        );
    }

    private NavigationHandle createOverlayHandle(WindowSession session, PresentedView overlayView) {
        return NavigationHandle.of(
                () -> session.closeOverlay(overlayView),
                () -> session.closeOverlay(overlayView),
                session.stage(),
                NavigationPresentationKind.OVERLAY
        );
    }

    private boolean shouldPromoteOverlayContent(NavigationHandle sourceHandle) {
        return sourceHandle != null && sourceHandle.presentationKind() == NavigationPresentationKind.OVERLAY;
    }

    private PresentedView promoteOverlayToContent(WindowSession session, PresentedView overlayView) {
        PresentedView promotedView = overlayView != null ? overlayView.asContentProjection() : null;
        if (promotedView == null) {
            return null;
        }
        applyThemeToNode(promotedView.rootNode());
        NavigationHandle promotedHandle = createContentHandle(session, promotedView);
        promotedView.setNavigationHandle(promotedHandle);
        applyNavigationHandle(promotedView.controller(), promotedHandle);
        return promotedView;
    }

    private void closeWindow(String windowKey) {
        WindowSession session = sessions.get(windowKey);
        String openerWindowKey = session != null ? session.openerWindowKey() : null;
        Stage stage = session != null ? session.stage() : null;

        if (stage != null && stage.isShowing()) {
            stage.close();
        } else {
            cleanupWindow(windowKey);
        }

        if (openerWindowKey != null) {
            focusWindow(openerWindowKey);
        }
    }

    private void installEscapeHandler(String windowKey, Scene scene) {
        if (scene == null) {
            return;
        }

        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() != javafx.scene.input.KeyCode.ESCAPE) {
                return;
            }
            WindowSession session = sessions.get(windowKey);
            if (session == null || !session.hasOverlays()) {
                return;
            }
            session.closeTopOverlay();
            event.consume();
        });
    }

    private void applyTheme(Scene scene) {
        if (scene == null || scene.getRoot() == null) {
            return;
        }
        applyThemeToNode(scene.getRoot());
    }

    private void applyThemeToNode(Node node) {
        ThemeStyleManager.applyTo(node, !whiteMode);
    }

    private void applyNavigationHandle(Object controller, NavigationHandle navigationHandle) {
        if (controller instanceof NavigationAware navigationAware) {
            navigationAware.setNavigationHandle(navigationHandle);
        }
    }

    private WindowSession requireSession(String windowKey) {
        WindowSession session = sessions.get(windowKey);
        if (session == null) {
            throw new IllegalStateException("Für das Fenster \"" + windowKey + "\" existiert kein Navigationszustand.");
        }
        return session;
    }

    private void requireHost(WindowSession session) {
        if (session == null || !session.hasHost()) {
            throw new IllegalStateException("Für das Fenster ist kein WindowHost registriert.");
        }
    }

    private Optional<String> resolveWindowKey(Window window) {
        if (!(window instanceof Stage stage)) {
            return Optional.empty();
        }

        return sessions.entrySet()
                .stream()
                .filter(entry -> entry.getValue().stage() == stage)
                .map(Map.Entry::getKey)
                .findFirst();
    }

    private void focusWindow(String windowKey) {
        WindowSession session = sessions.get(windowKey);
        if (session == null) {
            return;
        }
        focusStage(session.stage());
    }

    private void focusStage(Stage stage) {
        if (stage == null) {
            return;
        }
        stage.toFront();
        stage.requestFocus();
    }

    private String buildWindowKey(ViewTarget target) {
        return target.name().toLowerCase() + ":" + System.nanoTime();
    }

    static String resolveReusableStageKey(OpenRequest<?> request) {
        if (request == null || request.target() == null || request.openMode() != ViewOpenMode.NEW_STAGE) {
            return null;
        }

        return switch (request.target().stageInstancePolicy()) {
            case SINGLETON_TARGET, SINGLETON_CUSTOM_KEY -> request.stageInstanceKey();
            case UNIQUE_PER_OPEN -> null;
        };
    }

    private void cleanupWindow(String windowKey) {
        sessions.remove(windowKey);
    }

    private StackPane resolveHostRoot(StackPane mainContentLayer) {
        if (mainContentLayer != null
                && mainContentLayer.getScene() != null
                && mainContentLayer.getScene().getRoot() instanceof StackPane rootPane) {
            return rootPane;
        }
        return mainContentLayer;
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
        InputStream inputStream = Objects.requireNonNull(
                getClass().getResourceAsStream(resourcePath),
                "Icon-Ressource nicht gefunden: " + resourcePath
        );
        return new Image(inputStream);
    }

    private record InternalOpenResult<T>(T controller, boolean reusedExistingInstance) {
    }
}
