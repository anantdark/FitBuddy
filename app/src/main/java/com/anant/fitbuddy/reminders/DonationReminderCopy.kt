package com.anant.fitbuddy.reminders

import androidx.annotation.StringRes
import com.anant.fitbuddy.R
import java.util.concurrent.ThreadLocalRandom

/** Rotating cheeky labels for the donate reminder dialog. */
object DonationReminderCopy {

    val softDismiss: List<Int> = listOf(
        R.string.donate_reminder_will_pay_next_week,
        R.string.donate_reminder_not_today,
        R.string.donate_reminder_i_m_broke,
        R.string.donate_reminder_no_thanks,
        R.string.donate_reminder_i_don_t_have_money,
        R.string.donate_reminder_ask_me_after_payday,
        R.string.donate_reminder_my_wallet_is_on_a_diet,
        R.string.donate_reminder_maybe_after_i_hit_my_protein,
        R.string.donate_reminder_rain_check,
        R.string.donate_reminder_guilt_trip_declined,
        R.string.donate_reminder_later_macros_first,
        R.string.donate_reminder_skipping_this_week,
    )

    val alreadyPaid: List<Int> = listOf(
        R.string.donate_reminder_already_paid,
        R.string.donate_reminder_you_want_more,
        R.string.donate_reminder_i_m_on_the_list_promise,
        R.string.donate_reminder_paid_add_me,
        R.string.donate_reminder_check_your_inbox,
        R.string.donate_reminder_done_and_dusted,
        R.string.donate_reminder_already_tipped,
        R.string.donate_reminder_receipt_s_in_the_mail,
    )

    val payNow: List<Int> = listOf(
        R.string.donate_reminder_i_ll_pay,
        R.string.donate_reminder_let_s_pay,
        R.string.donate_reminder_open_the_tip_jar,
        R.string.donate_reminder_buy_the_dev_a_chai,
        R.string.donate_support_fitbuddy,
        R.string.donate_reminder_tip_time,
        R.string.donate_reminder_fuel_the_free_app,
        R.string.donate_reminder_chip_in,
    )

    data class Labels(
        @StringRes val soft: Int,
        @StringRes val paid: Int,
        @StringRes val pay: Int,
    )

    fun randomLabels(): Labels = Labels(
        soft = softDismiss.random(),
        paid = alreadyPaid.random(),
        pay = payNow.random(),
    )

    private fun List<Int>.random(): Int =
        this[ThreadLocalRandom.current().nextInt(size)]
}

/** Title + body pairs for the donate notification. */
object DonationNotificationCopy {

    private val MESSAGES = listOf(
        ReminderMessage(
            R.string.donate_reminder_keep_fitbuddy_free,
            R.string.donate_reminder_a_small_tip_goes_a_long,
        ),
        ReminderMessage(
            R.string.donate_reminder_dev_fuel_check,
            R.string.donate_reminder_fitbuddy_stays_free_and_open_source,
        ),
        ReminderMessage(
            R.string.donate_reminder_tip_jar_whisper,
            R.string.donate_reminder_if_fitbuddy_helped_this_week_consider,
        ),
        ReminderMessage(
            R.string.donate_reminder_chai_money_tuesday,
            R.string.donate_reminder_optional_support_keeps_features_coming_open,
        ),
        ReminderMessage(
            R.string.donate_reminder_still_free_still_yours,
            R.string.donate_reminder_donations_are_optional_if_you_re,
        ),
    )

    fun random(): ReminderMessage =
        MESSAGES[ThreadLocalRandom.current().nextInt(MESSAGES.size)]
}
