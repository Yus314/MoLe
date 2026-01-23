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

package net.ktnx.mobileledger.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.ktnx.mobileledger.R

@Suppress("UnusedParameter")
@Composable
internal fun ProfilePostingOptionsSection(
    permitPosting: Boolean,
    showCommentsByDefault: Boolean,
    showCommodityByDefault: Boolean,
    preferredAccountsFilter: String,
    defaultCommodity: String?,
    onPermitPostingChanged: (Boolean) -> Unit,
    onShowCommentsByDefaultChanged: (Boolean) -> Unit,
    onShowCommodityByDefaultChanged: (Boolean) -> Unit,
    onPreferredAccountsFilterChanged: (String) -> Unit,
    onDefaultCommodityChanged: (String?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.posting_permitted),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = permitPosting,
                onCheckedChange = onPermitPostingChanged
            )
        }

        AnimatedVisibility(
            visible = permitPosting,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.show_comments_by_default),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Checkbox(
                        checked = showCommentsByDefault,
                        onCheckedChange = onShowCommentsByDefaultChanged
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.show_commodity_by_default),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Checkbox(
                        checked = showCommodityByDefault,
                        onCheckedChange = onShowCommodityByDefaultChanged
                    )
                }

                OutlinedTextField(
                    value = preferredAccountsFilter,
                    onValueChange = onPreferredAccountsFilterChanged,
                    label = { Text(stringResource(R.string.preferred_accounts_filter_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // TODO: Integrate CurrencyPickerDialog - requires:
                            // 1. Add showCurrencyPickerDialog state to ProfileDetailUiState
                            // 2. Add ShowCurrencyPickerDialog/DismissCurrencyPickerDialog events
                            // 3. Load currencies from CurrencyRepository via ViewModel
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.default_commodity_label),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = defaultCommodity ?: stringResource(R.string.btn_no_currency),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (defaultCommodity == null) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }
    }
}
