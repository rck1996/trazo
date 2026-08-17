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
}
