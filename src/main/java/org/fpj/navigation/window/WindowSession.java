package org.fpj.navigation.window;

import javafx.scene.Node;
import javafx.stage.Stage;
import org.fpj.navigation.model.ViewTarget;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.function.Consumer;
import java.util.function.Function;

public final class WindowSession {

    private final String windowKey;
    private final String openerWindowKey;
    private final boolean mainWindow;
    private final Deque<PresentedView> contentHistory = new ArrayDeque<>();
    private final Deque<PresentedView> overlayStack = new ArrayDeque<>();
    private Stage stage;
    private WindowHost host;
    private PresentedView rootContent;
    private PresentedView currentContent;

    public WindowSession(String windowKey, String openerWindowKey, boolean mainWindow) {
        this.windowKey = windowKey;
        this.openerWindowKey = openerWindowKey;
        this.mainWindow = mainWindow;
    }

    public void attachStage(Stage stage) {
        this.stage = stage;
    }

    public void attachHost(WindowHost host, Node defaultMainContent) {
        attachHost(host, PresentedView.content(ViewTarget.MAIN_VIEW, defaultMainContent, null));
    }

    public void attachHost(WindowHost host, PresentedView rootContent) {
        this.host = host;
        if (this.rootContent == null) {
            this.rootContent = rootContent;
            this.currentContent = rootContent;
        }
        if (this.currentContent != null && this.host != null) {
            this.host.mainContentLayer().getChildren().setAll(this.currentContent.rootNode());
        }
        applyCurrentViewDefaultSize();
    }

    public void pushContent(PresentedView nextContent) {
        if (currentContent != null) {
            contentHistory.push(currentContent);
        }
        showContent(nextContent);
    }

    public boolean closeContent(PresentedView view) {
        if (view == null || currentContent != view) {
            return false;
        }

        if (!contentHistory.isEmpty()) {
            showContent(contentHistory.pop());
            return false;
        }

        if (isRootContent(view)) {
            if (mainWindow) {
                showRoot();
                return false;
            }
            return true;
        }

        showContent(rootContent);
        return false;
    }

    public boolean back() {
        if (!overlayStack.isEmpty()) {
            closeTopOverlay();
            return false;
        }

        if (!contentHistory.isEmpty()) {
            showContent(contentHistory.pop());
            return false;
        }

        if (mainWindow) {
            showRoot();
            return false;
        }

        return true;
    }

    public void showRoot() {
        clearOverlays();
        contentHistory.clear();
        showContent(rootContent);
    }

    public void pushOverlay(PresentedView overlayView) {
        if (overlayView == null) {
            return;
        }
        overlayStack.push(overlayView);
        if (host != null) {
            host.overlayLayer().getChildren().add(overlayView.rootNode());
        }
        applyCurrentViewDefaultSize();
    }

    public boolean promoteOverlaysToContentHistory(Function<PresentedView, PresentedView> overlayProjector) {
        if (overlayStack.isEmpty()) {
            return false;
        }

        ArrayList<PresentedView> overlaysBottomToTop = new ArrayList<>(overlayStack);
        Collections.reverse(overlaysBottomToTop);

        clearOverlays();

        if (currentContent != null) {
            contentHistory.push(currentContent);
        }

        for (int index = 0; index < overlaysBottomToTop.size(); index++) {
            PresentedView overlayView = overlaysBottomToTop.get(index);
            PresentedView promotedView = overlayProjector != null
                    ? overlayProjector.apply(overlayView)
                    : overlayView.asContentProjection();

            if (promotedView == null) {
                continue;
            }

            if (index == overlaysBottomToTop.size() - 1) {
                showContent(promotedView);
            } else {
                contentHistory.push(promotedView);
            }
        }

        return true;
    }

    public PresentedView closeTopOverlay() {
        PresentedView topOverlay = overlayStack.poll();
        removeOverlayNode(topOverlay);
        applyCurrentViewDefaultSize();
        return topOverlay;
    }

    public void closeOverlay(PresentedView overlayView) {
        if (overlayView == null || !overlayStack.remove(overlayView)) {
            return;
        }
        removeOverlayNode(overlayView);
        applyCurrentViewDefaultSize();
    }

    public void clearOverlays() {
        overlayStack.clear();
        if (host != null) {
            host.overlayLayer().getChildren().clear();
        }
    }

    public void applyTheme(Consumer<Node> themeConsumer) {
        if (themeConsumer == null) {
            return;
        }

        if (stage != null && stage.getScene() != null && stage.getScene().getRoot() != null) {
            themeConsumer.accept(stage.getScene().getRoot());
        }

        if (host != null) {
            themeConsumer.accept(host.rootNode());
            themeConsumer.accept(host.mainContentLayer());
            themeConsumer.accept(host.overlayLayer());
            themeConsumer.accept(host.messageLayer());
        }

        if (rootContent != null) {
            rootContent.applyTheme(themeConsumer);
        }
        if (currentContent != null && currentContent != rootContent) {
            currentContent.applyTheme(themeConsumer);
        }
        contentHistory.forEach(entry -> entry.applyTheme(themeConsumer));
        overlayStack.forEach(entry -> entry.applyTheme(themeConsumer));
    }

    public boolean hasHost() {
        return host != null;
    }

    public boolean hasRootContent() {
        return rootContent != null;
    }

    public boolean hasOverlays() {
        return !overlayStack.isEmpty();
    }

    public boolean hasContentHistory() {
        return !contentHistory.isEmpty();
    }

    public boolean isRootContent(PresentedView view) {
        return rootContent == view;
    }

    public String windowKey() {
        return windowKey;
    }

    public String openerWindowKey() {
        return openerWindowKey;
    }

    public Stage stage() {
        return stage;
    }

    public PresentedView currentContent() {
        return currentContent;
    }

    public Object currentController() {
        return currentContent != null ? currentContent.controller() : null;
    }

    public PresentedView topOverlay() {
        return overlayStack.peek();
    }

    private PresentedView currentVisibleView() {
        PresentedView overlayView = overlayStack.peek();
        return overlayView != null ? overlayView : currentContent;
    }

    private void applyCurrentViewDefaultSize() {
        PresentedView visibleView = currentVisibleView();
        if (stage == null || visibleView == null || visibleView.target() == null) {
            return;
        }
        stage.setWidth(visibleView.target().defaultWidth());
        stage.setHeight(visibleView.target().defaultHeight());
    }

    private void showContent(PresentedView contentView) {
        if (contentView == null) {
            return;
        }
        this.currentContent = contentView;
        if (host != null) {
            host.mainContentLayer().getChildren().setAll(contentView.rootNode());
        }
        applyCurrentViewDefaultSize();
    }

    private void removeOverlayNode(PresentedView overlayView) {
        if (overlayView == null || host == null) {
            return;
        }
        host.overlayLayer().getChildren().remove(overlayView.rootNode());
    }
}
