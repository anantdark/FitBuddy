package com.anant.fitbuddy.ui.components

import com.anant.fitbuddy.R

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.anant.fitbuddy.util.DateUtils
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

/**
 * Read-only field that opens a Material date picker. Defaults to the draft's current day;
 * changing the date relocates [timestamp]'s wall-clock time onto the chosen calendar day.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogDateField(
    timestamp: Long,
    onTimestampChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null,
    realToday: String = DateUtils.today()
) {
    var showPicker by remember { mutableStateOf(false) }
    val dateString = DateUtils.format(timestamp)
    val title = DateUtils.dayTitle(dateString, realToday = realToday)
    val subtitle = DateUtils.displayDateSubtitle(dateString)
    val resolvedLabel = label ?: stringResource(R.string.log_date_log_date)

    OutlinedTextField(
        value = title,
        onValueChange = {},
        readOnly = true,
        enabled = enabled,
        label = { Text(resolvedLabel) },
        supportingText = { Text(subtitle) },
        trailingIcon = {
            IconButton(
                onClick = { if (enabled) showPicker = true },
                enabled = enabled
            ) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = stringResource(R.string.common_pick_log_date))
            }
        },
        modifier = modifier.fillMaxWidth()
    )

    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.dateStringToUtcEpochDayMillis(dateString)
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = pickerState.selectedDateMillis
                        if (millis != null) {
                            val picked = DateUtils.utcEpochDayMillisToDateString(millis)
                            onTimestampChange(DateUtils.timestampOnDate(picked, timestamp))
                        }
                        showPicker = false
                    }
                ) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.common_cancel)) }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
