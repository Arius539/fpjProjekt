package org.fpj.navigation.model;

import org.fpj.navigation.api.ViewOpenMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViewTargetTest {

    @Test
    void defaultOpenModesMatchCurrentNavigationPolicy() {
        assertEquals(ViewOpenMode.REPLACE_MAIN_CONTENT, ViewTarget.TRANSACTION_VIEW.defaultOpenMode());
        assertEquals(ViewOpenMode.REPLACE_MAIN_CONTENT, ViewTarget.WALL_COMMENT_VIEW.defaultOpenMode());
        assertEquals(ViewOpenMode.REPLACE_MAIN_CONTENT, ViewTarget.CSV_IMPORT.defaultOpenMode());
        assertEquals(ViewOpenMode.NEW_STAGE, ViewTarget.CHAT_WINDOW.defaultOpenMode());
        assertEquals(ViewOpenMode.OVERLAY_IN_WINDOW, ViewTarget.TRANSACTION_DETAIL.defaultOpenMode());
    }

    @Test
    void stageMetadataIsAvailableOnTargets() {
        assertEquals("PayTalk: Transaktionen", ViewTarget.TRANSACTION_VIEW.defaultStageTitle());
        assertTrue(ViewTarget.TRANSACTION_VIEW.stageUsesWindowShell());
        assertFalse(ViewTarget.MAIN_VIEW.stageUsesWindowShell());
    }

    @Test
    void stageInstancePoliciesMatchCurrentWindowReuseRules() {
        assertEquals(StageInstancePolicy.SINGLETON_TARGET, ViewTarget.LOGIN.stageInstancePolicy());
        assertEquals(StageInstancePolicy.SINGLETON_TARGET, ViewTarget.MAIN_VIEW.stageInstancePolicy());
        assertEquals(StageInstancePolicy.SINGLETON_CUSTOM_KEY, ViewTarget.CHAT_WINDOW.stageInstancePolicy());
        assertEquals(StageInstancePolicy.UNIQUE_PER_OPEN, ViewTarget.TRANSACTION_VIEW.stageInstancePolicy());
        assertEquals(StageInstancePolicy.UNIQUE_PER_OPEN, ViewTarget.WALL_COMMENT_VIEW.stageInstancePolicy());
        assertEquals(StageInstancePolicy.UNIQUE_PER_OPEN, ViewTarget.CSV_IMPORT.stageInstancePolicy());
        assertEquals(StageInstancePolicy.UNIQUE_PER_OPEN, ViewTarget.TRANSACTION_DETAIL.stageInstancePolicy());
    }
}
