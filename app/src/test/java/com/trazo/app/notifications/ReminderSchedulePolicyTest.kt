package com.trazo.app.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class ReminderSchedulePolicyTest {
    @Test
    fun `a future daily time stays on the same day`() {
        val now = LocalDateTime.of(2026, 8, 23, 8, 15)

        assertEquals(
            LocalDateTime.of(2026, 8, 23, 9, 30),
            ReminderSchedulePolicy.nextDaily(9, 30, now)
        )
    }

    @Test
    fun `a daily time already reached moves to tomorrow`() {
        val now = LocalDateTime.of(2026, 8, 23, 9, 30)

        assertEquals(
            LocalDateTime.of(2026, 8, 24, 9, 30),
            ReminderSchedulePolicy.nextDaily(9, 30, now)
        )
    }

    @Test
    fun `recent missed reminder can be recovered`() {
        val now = LocalDateTime.of(2026, 8, 23, 14, 0)

        assertTrue(ReminderSchedulePolicy.shouldRecover(now.minusHours(5), now))
    }

    @Test
    fun `old reminder is not recovered to avoid noise`() {
        val now = LocalDateTime.of(2026, 8, 23, 14, 0)

        assertFalse(ReminderSchedulePolicy.shouldRecover(now.minusHours(7), now))
    }

    @Test
    fun `future reminder is never considered missed`() {
        val now = LocalDateTime.of(2026, 8, 23, 14, 0)

        assertFalse(ReminderSchedulePolicy.shouldRecover(now.plusMinutes(1), now))
    }
}
