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

    @Test
    fun `alarm duration uses the selected seconds`() {
        assertEquals(30_000L, AlarmNotificationPolicy.timeoutMillis(30))
    }

    @Test
    fun `alarm duration is bounded to avoid endless ringing`() {
        assertEquals(15_000L, AlarmNotificationPolicy.timeoutMillis(1))
        assertEquals(60_000L, AlarmNotificationPolicy.timeoutMillis(600))
    }

    @Test
    fun `notification is delivered once at the scheduled time without alarm sound`() {
        val eventAt = LocalDateTime.of(2026, 8, 24, 9, 0)

        val deliveries = AlarmNotificationPolicy.deliveries(eventAt, ReminderDeliveryMode.NOTIFICATION, 10)

        assertEquals(listOf(AlarmNotificationPolicy.Delivery(eventAt, "on_time", false)), deliveries)
    }

    @Test
    fun `early alarm is delivered only before the scheduled time`() {
        val eventAt = LocalDateTime.of(2026, 8, 24, 9, 0)

        val deliveries = AlarmNotificationPolicy.deliveries(eventAt, ReminderDeliveryMode.EARLY_ALARM, 15)

        assertEquals(listOf(AlarmNotificationPolicy.Delivery(eventAt.minusMinutes(15), "early", true)), deliveries)
    }

    @Test
    fun `on time alarm is delivered at the scheduled time`() {
        val eventAt = LocalDateTime.of(2026, 8, 24, 9, 0)

        val deliveries = AlarmNotificationPolicy.deliveries(eventAt, ReminderDeliveryMode.ON_TIME_ALARM, 10)

        assertEquals(listOf(AlarmNotificationPolicy.Delivery(eventAt, "on_time", true)), deliveries)
    }

    @Test
    fun `both alarms are ordered before and at the scheduled time`() {
        val eventAt = LocalDateTime.of(2026, 8, 24, 9, 0)

        val deliveries = AlarmNotificationPolicy.deliveries(eventAt, ReminderDeliveryMode.BOTH_ALARMS, 30)

        assertEquals(
            listOf(
                AlarmNotificationPolicy.Delivery(eventAt.minusMinutes(30), "early", true),
                AlarmNotificationPolicy.Delivery(eventAt, "on_time", true)
            ),
            deliveries
        )
    }
}
