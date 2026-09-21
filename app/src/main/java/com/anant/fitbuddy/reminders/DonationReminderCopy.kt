package com.anant.fitbuddy.reminders

import java.util.concurrent.ThreadLocalRandom

/** Rotating cheeky labels for the weekly donate reminder dialog. */
object DonationReminderCopy {

    val softDismiss: List<String> = listOf(
        "Will pay next week",
        "Not today",
        "I'm broke",
        "No thanks",
        "I don't have money",
        "Ask me after payday",
        "My wallet is on a diet",
        "Maybe after I hit my protein goal",
        "Rain check",
        "Guilt trip declined",
        "Later, macros first",
        "Skipping this week",
    )

    val alreadyPaid: List<String> = listOf(
        "Already paid",
        "You want more?",
        "I'm on the list (promise)",
        "Paid, add me",
        "Check your inbox",
        "Done and dusted",
        "Already tipped",
        "Receipt's in the mail",
    )

    val payNow: List<String> = listOf(
        "I'll pay",
        "Let's pay",
        "Open the tip jar",
        "Buy the dev a chai",
        "Support FitBuddy",
        "Tip time",
        "Fuel the free app",
        "Chip in",
    )

    data class Labels(
        val soft: String,
        val paid: String,
        val pay: String,
    )

    fun randomLabels(): Labels = Labels(
        soft = softDismiss.random(),
        paid = alreadyPaid.random(),
        pay = payNow.random(),
    )

    private fun List<String>.random(): String =
        this[ThreadLocalRandom.current().nextInt(size)]
}

/** Title + body pairs for the weekly donate notification. */
object DonationNotificationCopy {

    private val MESSAGES = listOf(
        ReminderMessage(
            "Keep FitBuddy free?",
            "A small tip goes a long way. Open the app this evening if you can chip in.",
        ),
        ReminderMessage(
            "Dev fuel check",
            "FitBuddy stays free and open source with optional support. No pressure, just a nudge.",
        ),
        ReminderMessage(
            "Tip jar whisper",
            "If FitBuddy helped this week, consider a small donation. We'll ask again tonight in-app.",
        ),
        ReminderMessage(
            "Chai money Tuesday",
            "Optional support keeps features coming. Open FitBuddy later to tip or snooze.",
        ),
        ReminderMessage(
            "Still free. Still yours.",
            "Donations are optional. If you're able, tonight's a good time to say thanks.",
        ),
    )

    fun random(): ReminderMessage =
        MESSAGES[ThreadLocalRandom.current().nextInt(MESSAGES.size)]
}
