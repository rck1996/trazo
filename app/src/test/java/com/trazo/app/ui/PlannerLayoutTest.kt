package com.trazo.app.ui

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlannerLayoutTest {
    private val date = LocalDate.of(2026, 8, 28)

    @Test fun `vertical drag snaps to fifteen minutes`() {
        val result = plannerDrop(date, 9, 7, 0f, 23f, 100f, 2f)
        assertEquals(9, result.hour)
        assertEquals(15, result.minute)
        assertEquals(8, result.minuteDelta)
    }

    @Test fun `horizontal drag changes day`() {
        val result = plannerDrop(date, 9, 0, 210f, 0f, 100f, 2f)
        assertEquals(date.plusDays(2), result.date)
        assertEquals(2, result.dayDelta)
    }

    @Test fun `drop stays inside a valid day`() {
        val before = plannerDrop(date, 0, 0, 0f, -10000f, 100f, 1f)
        val after = plannerDrop(date, 23, 50, 0f, 10000f, 100f, 1f)
        assertEquals(0, before.hour)
        assertEquals(0, before.minute)
        assertEquals(23, after.hour)
        assertEquals(45, after.minute)
    }

    @Test fun `block height grows with duration and remains bounded`() {
        assertTrue(plannerBlockHeightDp(90) > plannerBlockHeightDp(30))
        assertEquals(92, plannerBlockHeightDp(5))
        assertEquals(288, plannerBlockHeightDp(480))
    }
}
