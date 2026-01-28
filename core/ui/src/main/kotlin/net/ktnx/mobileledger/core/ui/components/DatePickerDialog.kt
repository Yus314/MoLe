/*
 * Copyright © 2024 Damyan Ivanov.
 * This file is part of MoLe.
 * MoLe is free software: you can distribute it and/or modify it
 * under the term of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your opinion), any later version.
 *
 * MoLe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License terms for details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MoLe. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ktnx.mobileledger.core.ui.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.util.Calendar
import java.util.GregorianCalendar
import net.ktnx.mobileledger.core.common.utils.SimpleDate
import net.ktnx.mobileledger.core.domain.model.FutureDates

/**
 * Material3 DatePicker dialog for selecting dates.
 *
 * Replaces DatePickerFragment with a pure Compose implementation.
 *
 * @param initialDate Initial date to show in the picker
 * @param minDate Optional minimum selectable date
 * @param maxDate Optional maximum selectable date
 * @param futureDates Future dates restriction setting
 * @param onDateSelected Callback when a date is selected
 * @param onDismiss Callback when the dialog is dismissed
 * @param confirmText Text for confirm button
 * @param dismissText Text for dismiss button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoleDatePickerDialog(
    initialDate: SimpleDate,
    minDate: SimpleDate? = null,
    maxDate: SimpleDate? = null,
    futureDates: FutureDates = FutureDates.All,
    onDateSelected: (SimpleDate) -> Unit,
    onDismiss: () -> Unit,
    confirmText: String = "OK",
    dismissText: String = "Cancel"
) {
    val initialCalendar = remember(initialDate) {
        Calendar.getInstance().apply {
            set(initialDate.year, initialDate.month - 1, initialDate.day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    val minDateMillis = remember(minDate) {
        minDate?.let {
            Calendar.getInstance().apply {
                set(it.year, it.month - 1, it.day, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
    }

    val maxDateMillis = remember(maxDate, futureDates) {
        calculateMaxDateMillis(maxDate, futureDates)
    }

    val selectableDates = remember(minDateMillis, maxDateMillis) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val afterMin = minDateMillis == null || utcTimeMillis >= minDateMillis
                val beforeMax = maxDateMillis == null || utcTimeMillis <= maxDateMillis
                return afterMin && beforeMax
            }

            override fun isSelectableYear(year: Int): Boolean {
                val minYear = minDateMillis?.let {
                    Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.YEAR)
                }
                val maxYear = maxDateMillis?.let {
                    Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.YEAR)
                }
                val afterMinYear = minYear == null || year >= minYear
                val beforeMaxYear = maxYear == null || year <= maxYear
                return afterMinYear && beforeMaxYear
            }
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialCalendar.timeInMillis,
        selectableDates = selectableDates
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selectedCalendar = Calendar.getInstance().apply {
                            timeInMillis = millis
                        }
                        val date = SimpleDate(
                            selectedCalendar.get(Calendar.YEAR),
                            selectedCalendar.get(Calendar.MONTH) + 1,
                            selectedCalendar.get(Calendar.DAY_OF_MONTH)
                        )
                        onDateSelected(date)
                    }
                }
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

/**
 * Calculate the maximum date in milliseconds based on maxDate and futureDates settings.
 */
private fun calculateMaxDateMillis(maxDate: SimpleDate?, futureDates: FutureDates): Long? {
    val fromMaxDate = maxDate?.let {
        Calendar.getInstance().apply {
            set(it.year, it.month - 1, it.day, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    val fromFutureDates = when (futureDates) {
        FutureDates.All -> null

        FutureDates.None -> {
            GregorianCalendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
        }

        FutureDates.OneWeek -> {
            GregorianCalendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, 7)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
        }

        FutureDates.TwoWeeks -> {
            GregorianCalendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, 14)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
        }

        FutureDates.OneMonth -> {
            GregorianCalendar.getInstance().apply {
                add(Calendar.MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
        }

        FutureDates.TwoMonths -> {
            GregorianCalendar.getInstance().apply {
                add(Calendar.MONTH, 2)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
        }

        FutureDates.ThreeMonths -> {
            GregorianCalendar.getInstance().apply {
                add(Calendar.MONTH, 3)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
        }

        FutureDates.SixMonths -> {
            GregorianCalendar.getInstance().apply {
                add(Calendar.MONTH, 6)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
        }

        FutureDates.OneYear -> {
            GregorianCalendar.getInstance().apply {
                add(Calendar.YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
        }
    }

    return when {
        fromMaxDate != null && fromFutureDates != null -> minOf(fromMaxDate, fromFutureDates)
        fromMaxDate != null -> fromMaxDate
        else -> fromFutureDates
    }
}
