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

package net.ktnx.mobileledger.ui.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import net.ktnx.mobileledger.R

@Composable
internal fun TemplateTransactionExtractionSection(
    transactionDescription: MatchableValue,
    transactionComment: MatchableValue,
    dateYear: MatchableValue,
    dateMonth: MatchableValue,
    dateDay: MatchableValue,
    patternGroupCount: Int,
    onDescriptionChanged: (MatchableValue) -> Unit,
    onCommentChanged: (MatchableValue) -> Unit,
    onYearChanged: (MatchableValue) -> Unit,
    onMonthChanged: (MatchableValue) -> Unit,
    onDayChanged: (MatchableValue) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.transaction_extraction_label),
            style = MaterialTheme.typography.titleMedium
        )

        TemplateMatchableValueField(
            label = stringResource(R.string.transaction_description_label),
            value = transactionDescription,
            patternGroupCount = patternGroupCount,
            onValueChanged = onDescriptionChanged
        )

        TemplateMatchableValueField(
            label = stringResource(R.string.transaction_comment_label),
            value = transactionComment,
            patternGroupCount = patternGroupCount,
            onValueChanged = onCommentChanged
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TemplateMatchableValueField(
                label = stringResource(R.string.date_year_label),
                value = dateYear,
                patternGroupCount = patternGroupCount,
                onValueChanged = onYearChanged,
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Number
            )

            TemplateMatchableValueField(
                label = stringResource(R.string.date_month_label),
                value = dateMonth,
                patternGroupCount = patternGroupCount,
                onValueChanged = onMonthChanged,
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Number
            )

            TemplateMatchableValueField(
                label = stringResource(R.string.date_day_label),
                value = dateDay,
                patternGroupCount = patternGroupCount,
                onValueChanged = onDayChanged,
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Number
            )
        }
    }
}
