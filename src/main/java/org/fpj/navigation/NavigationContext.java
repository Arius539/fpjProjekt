package org.fpj.navigation;

import javafx.stage.Stage;

public record NavigationContext<T>(Stage windowStage, T controller) {}
