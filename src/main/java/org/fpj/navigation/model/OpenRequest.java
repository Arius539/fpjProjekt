package org.fpj.navigation.model;

import javafx.stage.Window;
import org.fpj.navigation.api.ViewOpenMode;

public record OpenRequest<T>(
        ViewTarget target,
        ViewOpenMode openMode,
        Window ownerWindow,
        String stageInstanceKey,
        String stageTitleOverride
) {

    public OpenRequest {
        if (target == null) {
            throw new IllegalArgumentException("target must not be null");
        }
        if (openMode == null) {
            throw new IllegalArgumentException("openMode must not be null");
        }
    }

    public OpenRequest<T> withStageInstance(String key, String titleOverride) {
        return new OpenRequest<>(target, openMode, ownerWindow, key, titleOverride);
    }

    @SuppressWarnings("unchecked")
    public Class<T> controllerType() {
        return (Class<T>) target.controllerType();
    }
}
