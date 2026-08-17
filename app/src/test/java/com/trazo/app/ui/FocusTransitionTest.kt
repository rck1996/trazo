package com.trazo.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FocusTransitionTest {
    @Test fun focusAlwaysMovesToBreak() {
        assertEquals(FocusPhase.BREAK, nextFocusPhase(FocusPhase.FOCUS))
    }

    @Test fun breakMovesBackToFocus() {
        assertEquals(FocusPhase.FOCUS, nextFocusPhase(FocusPhase.BREAK))
    }

    @Test fun timerProgressStartsEmptyAndFillsAsTimePasses() {
        assertEquals(0f, elapsedTimerProgress(1_500, 1_500), 0.0001f)
        assertEquals(.5f, elapsedTimerProgress(750, 1_500), 0.0001f)
        assertEquals(1f, elapsedTimerProgress(0, 1_500), 0.0001f)
    }

    @Test fun timerProgressIsSafeForRestoredOrInvalidValues() {
        assertEquals(0f, elapsedTimerProgress(1_600, 1_500), 0.0001f)
        assertEquals(1f, elapsedTimerProgress(-1, 1_500), 0.0001f)
        assertEquals(0f, elapsedTimerProgress(10, 0), 0.0001f)
    }
}
