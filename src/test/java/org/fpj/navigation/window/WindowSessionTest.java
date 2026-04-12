package org.fpj.navigation.window;

import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.fpj.navigation.model.ViewTarget;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WindowSessionTest {

    @Test
    void contentHistoryBehavesAsLifoAndSecondaryWindowClosesOnlyAtRoot() {
        WindowSession session = new WindowSession("transaction-window", "main", false);
        WindowHost host = createHost();
        StackPane rootNode = new StackPane();
        session.attachHost(host, rootNode);

        PresentedView firstContent = PresentedView.content(ViewTarget.TRANSACTION_VIEW, new StackPane(), "first");
        PresentedView secondContent = PresentedView.content(ViewTarget.WALL_COMMENT_VIEW, new StackPane(), "second");

        session.pushContent(firstContent);
        session.pushContent(secondContent);

        assertFalse(session.back());
        assertSame(firstContent, session.currentContent());

        assertFalse(session.back());
        assertSame(rootNode, session.currentContent().rootNode());

        assertTrue(session.back());
    }

    @Test
    void overlayStackBehavesAsLifoAndShowRootClearsTransientState() {
        WindowSession session = new WindowSession("main", null, true);
        WindowHost host = createHost();
        StackPane rootNode = new StackPane();
        session.attachHost(host, rootNode);

        PresentedView content = PresentedView.content(ViewTarget.TRANSACTION_VIEW, new StackPane(), "content");
        PresentedView overlayOne = PresentedView.overlay(ViewTarget.TRANSACTION_DETAIL, new StackPane(), new StackPane(), "overlay-1");
        PresentedView overlayTwo = PresentedView.overlay(ViewTarget.CSV_IMPORT, new StackPane(), new StackPane(), "overlay-2");

        session.pushContent(content);
        session.pushOverlay(overlayOne);
        session.pushOverlay(overlayTwo);

        assertEquals(2, host.overlayLayer().getChildren().size());
        assertSame(overlayTwo, session.topOverlay());

        assertSame(overlayTwo, session.closeTopOverlay());
        assertEquals(1, host.overlayLayer().getChildren().size());
        assertSame(overlayOne, session.topOverlay());

        session.showRoot();

        assertSame(rootNode, session.currentContent().rootNode());
        assertFalse(session.hasOverlays());
        assertFalse(session.hasContentHistory());
        assertTrue(host.overlayLayer().getChildren().isEmpty());
    }

    @Test
    void promoteOverlaysToContentHistoryReusesControllersAndCreatesBackPath() {
        WindowSession session = new WindowSession("main", null, true);
        WindowHost host = createHost();
        StackPane rootNode = new StackPane();
        session.attachHost(host, rootNode);

        Object rootController = "content-controller";
        Object detailController = "detail-controller";
        Object csvController = "csv-controller";

        StackPane contentNode = new StackPane();
        StackPane detailShell = new StackPane();
        StackPane detailContent = new StackPane();
        StackPane csvShell = new StackPane();
        StackPane csvContent = new StackPane();

        PresentedView content = PresentedView.content(ViewTarget.TRANSACTION_VIEW, contentNode, rootController);
        PresentedView detailOverlay = PresentedView.overlay(ViewTarget.TRANSACTION_DETAIL, detailShell, detailContent, detailController);
        PresentedView csvOverlay = PresentedView.overlay(ViewTarget.CSV_IMPORT, csvShell, csvContent, csvController);

        session.pushContent(content);
        session.pushOverlay(detailOverlay);
        session.pushOverlay(csvOverlay);

        assertTrue(session.promoteOverlaysToContentHistory(PresentedView::asContentProjection));
        assertFalse(session.hasOverlays());
        assertTrue(host.overlayLayer().getChildren().isEmpty());
        assertSame(csvContent, session.currentContent().rootNode());
        assertSame(csvController, session.currentContent().controller());

        assertFalse(session.back());
        assertSame(detailContent, session.currentContent().rootNode());
        assertSame(detailController, session.currentContent().controller());

        assertFalse(session.back());
        assertSame(contentNode, session.currentContent().rootNode());
        assertSame(rootController, session.currentContent().controller());

        assertFalse(session.back());
        assertSame(rootNode, session.currentContent().rootNode());
    }

    @Test
    void visibleViewChangesResizeStageToThatViewsDefaults() {
        WindowSession session = new WindowSession("main", null, true);
        Stage stage = mock(Stage.class);
        WindowHost host = createHost();
        StackPane rootNode = new StackPane();

        session.attachStage(stage);
        session.attachHost(host, rootNode);
        verify(stage).setWidth(ViewTarget.MAIN_VIEW.defaultWidth());
        verify(stage).setHeight(ViewTarget.MAIN_VIEW.defaultHeight());
        clearInvocations(stage);

        PresentedView content = PresentedView.content(ViewTarget.TRANSACTION_VIEW, new StackPane(), "content");
        session.pushContent(content);
        verify(stage).setWidth(ViewTarget.TRANSACTION_VIEW.defaultWidth());
        verify(stage).setHeight(ViewTarget.TRANSACTION_VIEW.defaultHeight());
        clearInvocations(stage);

        PresentedView overlay = PresentedView.overlay(ViewTarget.TRANSACTION_DETAIL, new StackPane(), new StackPane(), "overlay");
        session.pushOverlay(overlay);
        verify(stage).setWidth(ViewTarget.TRANSACTION_DETAIL.defaultWidth());
        verify(stage).setHeight(ViewTarget.TRANSACTION_DETAIL.defaultHeight());
        clearInvocations(stage);

        session.closeTopOverlay();
        verify(stage).setWidth(ViewTarget.TRANSACTION_VIEW.defaultWidth());
        verify(stage).setHeight(ViewTarget.TRANSACTION_VIEW.defaultHeight());
        clearInvocations(stage);

        session.showRoot();
        verify(stage).setWidth(ViewTarget.MAIN_VIEW.defaultWidth());
        verify(stage).setHeight(ViewTarget.MAIN_VIEW.defaultHeight());
    }

    @Test
    void promoteAndBackResizeStageAlongVisibleViewChain() {
        WindowSession session = new WindowSession("main", null, true);
        Stage stage = mock(Stage.class);
        WindowHost host = createHost();
        StackPane rootNode = new StackPane();

        session.attachStage(stage);
        session.attachHost(host, rootNode);

        PresentedView content = PresentedView.content(ViewTarget.TRANSACTION_VIEW, new StackPane(), "content");
        PresentedView detailOverlay = PresentedView.overlay(ViewTarget.TRANSACTION_DETAIL, new StackPane(), new StackPane(), "detail");
        PresentedView csvOverlay = PresentedView.overlay(ViewTarget.CSV_IMPORT, new StackPane(), new StackPane(), "csv");

        session.pushContent(content);
        session.pushOverlay(detailOverlay);
        session.pushOverlay(csvOverlay);
        clearInvocations(stage);

        assertTrue(session.promoteOverlaysToContentHistory(PresentedView::asContentProjection));
        verify(stage).setWidth(ViewTarget.CSV_IMPORT.defaultWidth());
        verify(stage).setHeight(ViewTarget.CSV_IMPORT.defaultHeight());
        clearInvocations(stage);

        assertFalse(session.back());
        verify(stage).setWidth(ViewTarget.TRANSACTION_DETAIL.defaultWidth());
        verify(stage).setHeight(ViewTarget.TRANSACTION_DETAIL.defaultHeight());
        clearInvocations(stage);

        assertFalse(session.back());
        verify(stage).setWidth(ViewTarget.TRANSACTION_VIEW.defaultWidth());
        verify(stage).setHeight(ViewTarget.TRANSACTION_VIEW.defaultHeight());
    }

    private WindowHost createHost() {
        return new WindowHost(new StackPane(), new StackPane(), new StackPane(), new StackPane());
    }
}
