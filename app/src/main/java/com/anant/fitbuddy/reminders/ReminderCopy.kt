package com.anant.fitbuddy.reminders

import android.content.Context
import androidx.annotation.StringRes
import com.anant.fitbuddy.R
import java.util.concurrent.ThreadLocalRandom

/** Motivational title + body pairs for daily log reminders. */
data class ReminderMessage(
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int,
) {
    fun resolve(context: Context): Resolved =
        Resolved(context.getString(titleRes), context.getString(bodyRes))

    data class Resolved(val title: String, val body: String)
}

object ReminderCopy {

    private val MESSAGES = listOf(
        ReminderMessage(
            R.string.reminder_small_log_big_clarity,
            R.string.reminder_got_a_minute_log_today_s,
        ),
        ReminderMessage(
            R.string.reminder_your_future_self_says_thanks,
            R.string.reminder_quick_check_in_add_what_you,
        ),
        ReminderMessage(
            R.string.reminder_thali_tracked_is_progress,
            R.string.reminder_roti_dal_or_a_walk_log,
        ),
        ReminderMessage(
            R.string.reminder_don_t_leave_the_day_blank,
            R.string.reminder_even_one_entry_helps_log_food,
        ),
        ReminderMessage(
            R.string.reminder_fuel_movement_matter,
            R.string.reminder_snap_a_plate_or_note_a,
        ),
        ReminderMessage(
            R.string.reminder_steady_beats_perfect,
            R.string.reminder_missed_a_meal_earlier_log_what,
        ),
        ReminderMessage(
            R.string.reminder_evening_check_in,
            R.string.reminder_how_did_today_go_log_a,
        ),
        ReminderMessage(
            R.string.reminder_one_tap_toward_your_goal,
            R.string.reminder_open_fitbuddy_and_log_food_or,
        ),
        ReminderMessage(
            R.string.reminder_kitchen_gym_same_story,
            R.string.reminder_whether_it_s_dinner_or_a,
        ),
        ReminderMessage(
            R.string.reminder_you_re_building_the_habit,
            R.string.reminder_log_today_s_meals_or_workouts,
        ),
        ReminderMessage(
            R.string.reminder_progress_loves_receipts,
            R.string.reminder_got_chai_and_a_snack_or,
        ),
        ReminderMessage(
            R.string.reminder_keep_the_chain_going,
            R.string.reminder_a_quick_food_or_exercise_log,
        ),
        ReminderMessage(
            R.string.reminder_honesty_over_guessing,
            R.string.reminder_don_t_estimate_tomorrow_log_what,
        ),
        ReminderMessage(
            R.string.reminder_your_body_keeps_score,
            R.string.reminder_help_fitbuddy_keep_up_log_a,
        ),
        ReminderMessage(
            R.string.reminder_tonight_s_nudge,
            R.string.reminder_before_you_wind_down_jot_a,
        ),
        ReminderMessage(
            R.string.reminder_still_time_to_show_up,
            R.string.reminder_log_food_or_movement_now_every,
        ),
    )

    /** Picks a random motivational reminder (unique each call). */
    fun random(): ReminderMessage {
        val index = ThreadLocalRandom.current().nextInt(MESSAGES.size)
        return MESSAGES[index]
    }
}
