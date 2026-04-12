package org.fpj.navigation.api;

import javafx.stage.Window;

public interface NavigationHandle {

    void close();

    void back();

    Window window();

    NavigationPresentationKind presentationKind();

    static NavigationHandle of(Runnable closeAction,
                               Runnable backAction,
                               Window window,
                               NavigationPresentationKind presentationKind) {
        return new NavigationHandle() {
            @Override
            public void close() {
                if (closeAction != null) {
                    closeAction.run();
                }
            }

            @Override
            public void back() {
                if (backAction != null) {
                    backAction.run();
                } else {
                    close();
                }
            }

            @Override
            public Window window() {
                return window;
            }

            @Override
            public NavigationPresentationKind presentationKind() {
                return presentationKind != null ? presentationKind : NavigationPresentationKind.CONTENT;
            }
        };
    }

    static NavigationHandle of(Runnable closeAction, Runnable backAction, Window window) {
        return of(closeAction, backAction, window, NavigationPresentationKind.CONTENT);
    }
}
