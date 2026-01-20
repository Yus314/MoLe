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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import net.ktnx.mobileledger.R

@Composable
internal fun TemplateAccountRowCard(
    index: Int,
    account: TemplateAccountRow,
    patternGroupCount: Int,
    canRemove: Boolean,
    onAccountNameChanged: (MatchableValue) -> Unit,
    onAccountCommentChanged: (MatchableValue) -> Unit,
    onAccountAmountChanged: (MatchableValue) -> Unit,
    onAccountCurrencyChanged: (MatchableValue) -> Unit,
    onAccountNegateChanged: (Boolean) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "行 ${index + 1}",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.weight(1f))
                if (canRemove) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "削除",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            TemplateMatchableValueField(
                label = stringResource(R.string.account_name_label),
                value = account.accountName,
                patternGroupCount = patternGroupCount,
                onValueChanged = onAccountNameChanged
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TemplateMatchableValueField(
                    label = stringResource(R.string.amount_label),
                    value = account.amount,
                    patternGroupCount = patternGroupCount,
                    onValueChanged = onAccountAmountChanged,
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Decimal
                )

                TemplateMatchableValueField(
                    label = stringResource(R.string.currency_label),
                    value = account.currency,
                    patternGroupCount = patternGroupCount,
                    onValueChanged = onAccountCurrencyChanged,
                    modifier = Modifier.weight(1f)
                )
            }

            AnimatedVisibility(
                visible = account.amount.isMatchGroup(),
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.negate_amount_label),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Checkbox(
                        checked = account.negateAmount,
                        onCheckedChange = onAccountNegateChanged
                    )
                }
            }

            TemplateMatchableValueField(
                label = stringResource(R.string.comment_label),
                value = account.accountComment,
                patternGroupCount = patternGroupCount,
                onValueChanged = onAccountCommentChanged
            )
        }
    }
}
