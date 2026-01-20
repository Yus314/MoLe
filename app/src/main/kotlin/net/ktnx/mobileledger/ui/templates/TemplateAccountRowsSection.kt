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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.ktnx.mobileledger.R

@Composable
internal fun TemplateAccountRowsSection(
    accounts: List<TemplateAccountRow>,
    patternGroupCount: Int,
    onAccountNameChanged: (Int, MatchableValue) -> Unit,
    onAccountCommentChanged: (Int, MatchableValue) -> Unit,
    onAccountAmountChanged: (Int, MatchableValue) -> Unit,
    onAccountCurrencyChanged: (Int, MatchableValue) -> Unit,
    onAccountNegateChanged: (Int, Boolean) -> Unit,
    onRemoveRow: (Int) -> Unit,
    onAddRow: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.accounts_label),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onAddRow) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "行を追加"
                )
            }
        }

        accounts.forEachIndexed { index, account ->
            TemplateAccountRowCard(
                index = index,
                account = account,
                patternGroupCount = patternGroupCount,
                canRemove = accounts.size > 2,
                onAccountNameChanged = { onAccountNameChanged(index, it) },
                onAccountCommentChanged = { onAccountCommentChanged(index, it) },
                onAccountAmountChanged = { onAccountAmountChanged(index, it) },
                onAccountCurrencyChanged = { onAccountCurrencyChanged(index, it) },
                onAccountNegateChanged = { onAccountNegateChanged(index, it) },
                onRemove = { onRemoveRow(index) }
            )
        }
    }
}
