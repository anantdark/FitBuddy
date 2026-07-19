package com.anant.fitbuddy.reminders

import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ReminderSchedulerTest {

    @Test
    fun `nextTriggerMillis is later today when time is in the future`() {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val trigger = ReminderScheduler.nextTriggerMillis(20, 0, now)
        val cal = Calendar.getInstance().apply { timeInMillis = trigger }
        assertTrue(trigger > now)
        assertTrue(cal.get(Calendar.HOUR_OF_DAY) == 20)
        assertTrue(cal.get(Calendar.MINUTE) == 0)
    }

    @Test
    fun `nextTriggerMillis rolls to next day when time already passed`() {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 21)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val trigger = ReminderScheduler.nextTriggerMillis(20, 0, now)
        assertTrue(trigger > now)
        val dayDiff = (trigger - now) / (24 * 60 * 60 * 1000.0)
        assertTrue(dayDiff in 0.9..1.1)
    }
}
