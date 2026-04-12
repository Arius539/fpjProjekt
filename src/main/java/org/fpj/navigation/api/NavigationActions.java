package org.fpj.navigation.api;

public final class NavigationActions {

    private NavigationActions() {
    }

    public static void backOrElse(NavigationHandle navigationHandle, Runnable fallback) {
        if (navigationHandle != null) {
            navigationHandle.back();
            return;
        }
        if (fallback != null) {
            fallback.run();
        }
    }

    public static void closeOrElse(NavigationHandle navigationHandle, Runnable fallback) {
        if (navigationHandle != null) {
            navigationHandle.close();
            return;
        }
        if (fallback != null) {
            fallback.run();
        }
    }
}
