package org.fpj.navigation.model;

import javafx.stage.Window;
import org.fpj.javafxcontroller.ChatWindowController;
import org.fpj.javafxcontroller.CsvImportDialogController;
import org.fpj.javafxcontroller.LoginController;
import org.fpj.javafxcontroller.TransactionDetailController;
import org.fpj.javafxcontroller.TransactionViewController;
import org.fpj.javafxcontroller.WallCommentViewController;
import org.fpj.javafxcontroller.mainView.MainViewController;
import org.fpj.navigation.api.ViewOpenMode;

public enum ViewTarget {
    LOGIN(
            "login.fxml",
            LoginController.class,
            ViewOpenMode.NEW_STAGE,
            "PayTalk: Login",
            400,
            400,
            false,
            false,
            StageInstancePolicy.SINGLETON_TARGET
    ),
    MAIN_VIEW(
            "mainview/main_view.fxml",
            MainViewController.class,
            ViewOpenMode.NEW_STAGE,
            "PayTalk",
            1280,
            860,
            false,
            false,
            StageInstancePolicy.SINGLETON_TARGET
    ),
    TRANSACTION_VIEW(
            "transactionView.fxml",
            TransactionViewController.class,
            ViewOpenMode.REPLACE_MAIN_CONTENT,
            "PayTalk: Transaktionen",
            1280,
            860,
            false,
            true,
            StageInstancePolicy.UNIQUE_PER_OPEN
    ),
    WALL_COMMENT_VIEW(
            "wallCommentView.fxml",
            WallCommentViewController.class,
            ViewOpenMode.REPLACE_MAIN_CONTENT,
            "PayTalk: Pinnwand",
            1280,
            860,
            false,
            true,
            StageInstancePolicy.UNIQUE_PER_OPEN
    ),
    CHAT_WINDOW(
            "chat_window.fxml",
            ChatWindowController.class,
            ViewOpenMode.NEW_STAGE,
            "PayTalk: Chat",
            800,
            600,
            false,
            true,
            StageInstancePolicy.SINGLETON_CUSTOM_KEY
    ),
    TRANSACTION_DETAIL(
            "transaction_detail.fxml",
            TransactionDetailController.class,
            ViewOpenMode.OVERLAY_IN_WINDOW,
            "PayTalk: Transaktionsdetails",
            980,
            720,
            false,
            true,
            StageInstancePolicy.UNIQUE_PER_OPEN
    ),
    CSV_IMPORT(
            "csvImportDialog.fxml",
            CsvImportDialogController.class,
            ViewOpenMode.REPLACE_MAIN_CONTENT,
            "PayTalk: CSV-Import",
            800,
            520,
            false,
            true,
            StageInstancePolicy.UNIQUE_PER_OPEN
    );

    private final String fxmlPath;
    private final Class<?> controllerType;
    private final ViewOpenMode defaultOpenMode;
    private final String defaultStageTitle;
    private final double defaultWidth;
    private final double defaultHeight;
    private final boolean alwaysOnTop;
    private final boolean stageUsesWindowShell;
    private final StageInstancePolicy stageInstancePolicy;

    ViewTarget(String fxmlPath,
               Class<?> controllerType,
               ViewOpenMode defaultOpenMode,
               String defaultStageTitle,
               double defaultWidth,
               double defaultHeight,
               boolean alwaysOnTop,
               boolean stageUsesWindowShell,
               StageInstancePolicy stageInstancePolicy) {
        this.fxmlPath = fxmlPath;
        this.controllerType = controllerType;
        this.defaultOpenMode = defaultOpenMode;
        this.defaultStageTitle = defaultStageTitle;
        this.defaultWidth = defaultWidth;
        this.defaultHeight = defaultHeight;
        this.alwaysOnTop = alwaysOnTop;
        this.stageUsesWindowShell = stageUsesWindowShell;
        this.stageInstancePolicy = stageInstancePolicy;
    }

    public String fxmlPath() {
        return fxmlPath;
    }

    public Class<?> controllerType() {
        return controllerType;
    }

    public ViewOpenMode defaultOpenMode() {
        return defaultOpenMode;
    }

    public String defaultStageTitle() {
        return defaultStageTitle;
    }

    public double defaultWidth() {
        return defaultWidth;
    }

    public double defaultHeight() {
        return defaultHeight;
    }

    public boolean alwaysOnTop() {
        return alwaysOnTop;
    }

    public boolean stageUsesWindowShell() {
        return stageUsesWindowShell;
    }

    public StageInstancePolicy stageInstancePolicy() {
        return stageInstancePolicy;
    }

    public <T> OpenRequest<T> request(ViewOpenMode openMode, Window ownerWindow) {
        return new OpenRequest<>(this, openMode != null ? openMode : defaultOpenMode, ownerWindow, null, null);
    }

}
