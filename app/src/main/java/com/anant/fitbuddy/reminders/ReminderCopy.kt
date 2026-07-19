package com.anant.fitbuddy.reminders

import java.util.concurrent.ThreadLocalRandom

/** Motivational title + body pairs for daily log reminders. */
data class ReminderMessage(
    val title: String,
    val body: String
)

object ReminderCopy {

    private val MESSAGES = listOf(
        ReminderMessage(
            "Small log, big clarity",
            "Got a minute? Log today’s food or a workout so FitBuddy can keep your day honest."
        ),
        ReminderMessage(
            "Your future self says thanks",
            "Quick check-in: add what you ate or how you moved — consistency beats perfection."
        ),
        ReminderMessage(
            "Thali tracked is progress",
            "Roti, dal, or a walk — log a meal or activity now and stay on top of your goals."
        ),
        ReminderMessage(
            "Don’t leave the day blank",
            "Even one entry helps. Log food or exercise before the evening slips away."
        ),
        ReminderMessage(
            "Fuel & movement matter",
            "Snap a plate or note a workout — FitBuddy is ready when you are."
        ),
        ReminderMessage(
            "Steady beats perfect",
            "Missed a meal earlier? Log what you can now — food or activity, both count."
        ),
        ReminderMessage(
            "Evening check-in",
            "How did today go? Log a meal or some movement and keep your streak of awareness going."
        ),
        ReminderMessage(
            "One tap toward your goal",
            "Open FitBuddy and log food or activity — tiny habits compound."
        ),
        ReminderMessage(
            "Kitchen & gym, same story",
            "Whether it’s dinner or a session, capture it so your macros and burn stay accurate."
        ),
        ReminderMessage(
            "You’re building the habit",
            "Log today’s meals or workouts. Future-you will love the clearer dashboard."
        ),
        ReminderMessage(
            "Progress loves receipts",
            "Got chai and a snack? Or a walk? Log food or activity — FitBuddy’s listening."
        ),
        ReminderMessage(
            "Keep the chain going",
            "A quick food or exercise log keeps your Progress chart meaningful. Tap to add one."
        ),
        ReminderMessage(
            "Honesty over guessing",
            "Don’t estimate tomorrow — log what you ate or did today while it’s fresh."
        ),
        ReminderMessage(
            "Your body keeps score",
            "Help FitBuddy keep up: log a meal or an activity in under a minute."
        ),
        ReminderMessage(
            "Tonight’s nudge",
            "Before you wind down, jot a meal or workout. Small logs → smarter insights."
        ),
        ReminderMessage(
            "Still time to show up",
            "Log food or movement now — every entry makes your targets more useful."
        )
    )

    /** Picks a random motivational reminder (unique each call). */
    fun random(): ReminderMessage {
        val index = ThreadLocalRandom.current().nextInt(MESSAGES.size)
        return MESSAGES[index]
    }
}
