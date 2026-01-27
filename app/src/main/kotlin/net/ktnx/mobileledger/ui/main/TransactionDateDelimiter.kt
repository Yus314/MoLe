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

package net.ktnx.mobileledger.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale
import net.ktnx.mobileledger.core.common.utils.SimpleDate

/**
 * Date delimiter row displayed between transaction groups.
 *
 * Shows the date information with optional month/year header when
 * transitioning to a new month.
 */
@Composable
fun TransactionDateDelimiter(date: SimpleDate, isMonthShown: Boolean, modifier: Modifier = Modifier) {
    val monthName = remember(date.month) {
        DateFormatSymbols.getInstance(Locale.getDefault()).months[date.month - 1]
    }

    val dayOfWeek = remember(date) {
        val calendar = Calendar.getInstance().apply {
            set(date.year, date.month - 1, date.day)
        }
        DateFormatSymbols.getInstance(Locale.getDefault())
            .weekdays[calendar.get(Calendar.DAY_OF_WEEK)]
    }

    val dateText = remember(date, isMonthShown, monthName, dayOfWeek) {
        if (isMonthShown) {
            String.format(Locale.US, "%s %d", monthName, date.year)
        } else {
            dayOfWeek
        }
    }

    val fullDateText = remember(date, monthName) {
        String.format(Locale.US, "%d %s", date.day, monthName)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        if (isMonthShown) {
            Text(
                text = dateText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = fullDateText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
