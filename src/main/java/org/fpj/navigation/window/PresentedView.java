package org.fpj.navigation.window;

import javafx.scene.Node;
import org.fpj.navigation.api.NavigationHandle;
import org.fpj.navigation.model.ViewTarget;

import java.util.function.Consumer;

public final class PresentedView {

    public enum PresentationKind {
        CONTENT,
        OVERLAY
    }

    private final ViewTarget target;
    private final PresentationKind kind;
    private final Node rootNode;
    private final Node contentNode;
    private final Object controller;
    private NavigationHandle navigationHandle;

    private PresentedView(ViewTarget target,
                          PresentationKind kind,
                          Node rootNode,
                          Node contentNode,
                          Object controller) {
        this.target = target;
        this.kind = kind;
        this.rootNode = rootNode;
        this.contentNode = contentNode;
        this.controller = controller;
    }

    public static PresentedView content(ViewTarget target, Node rootNode, Object controller) {
        return new PresentedView(target, PresentationKind.CONTENT, rootNode, rootNode, controller);
    }

    public static PresentedView overlay(ViewTarget target, Node shellNode, Node contentNode, Object controller) {
        return new PresentedView(target, PresentationKind.OVERLAY, shellNode, contentNode, controller);
    }

    public PresentedView asContentProjection() {
        Node projectedRoot = contentNode != null ? contentNode : rootNode;
        return PresentedView.content(target, projectedRoot, controller);
    }

    public ViewTarget target() {
        return target;
    }

    public Node rootNode() {
        return rootNode;
    }

    public Object controller() {
        return controller;
    }

    public NavigationHandle navigationHandle() {
        return navigationHandle;
    }

    public void setNavigationHandle(NavigationHandle navigationHandle) {
        this.navigationHandle = navigationHandle;
    }

    public void applyTheme(Consumer<Node> themeConsumer) {
        if (themeConsumer == null) {
            return;
        }
        if (rootNode != null) {
            themeConsumer.accept(rootNode);
        }
        if (contentNode != null && contentNode != rootNode) {
            themeConsumer.accept(contentNode);
        }
    }
}
