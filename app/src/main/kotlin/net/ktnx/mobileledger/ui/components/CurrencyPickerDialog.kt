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

package net.ktnx.mobileledger.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.ktnx.mobileledger.R
import net.ktnx.mobileledger.model.Currency

/**
 * Material3 Currency picker dialog.
 *
 * Replaces CurrencySelectorFragment with a pure Compose implementation.
 *
 * @param currencies List of available currency names
 * @param initialPosition Initial currency position setting
 * @param initialGap Initial gap setting
 * @param showPositionSettings Whether to show position and gap settings
 * @param onCurrencySelected Callback when a currency is selected
 * @param onCurrencyAdded Callback when a new currency is added
 * @param onCurrencyDeleted Callback when a currency is deleted (long press)
 * @param onNoCurrencySelected Callback when "No currency" is selected
 * @param onPositionChanged Callback when position setting changes
 * @param onGapChanged Callback when gap setting changes
 * @param onDismiss Callback when dialog is dismissed
 */
@Composable
fun CurrencyPickerDialog(
    currencies: List<String>,
    initialPosition: Currency.Position = Currency.Position.before,
    initialGap: Boolean = true,
    showPositionSettings: Boolean = true,
    onCurrencySelected: (String) -> Unit,
    onCurrencyAdded: (String, Currency.Position, Boolean) -> Unit,
    onCurrencyDeleted: (String) -> Unit,
    onNoCurrencySelected: () -> Unit,
    onPositionChanged: (Currency.Position) -> Unit,
    onGapChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var isAddingNew by remember { mutableStateOf(false) }
    var newCurrencyName by remember { mutableStateOf("") }
    var position by remember { mutableStateOf(initialPosition) }
    var gap by remember { mutableStateOf(initialGap) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.choose_currency_label))
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Currency grid
                CurrencyGrid(
                    currencies = currencies,
                    onCurrencySelected = { currency ->
                        onCurrencySelected(currency)
                    },
                    onCurrencyLongClick = { currency ->
                        onCurrencyDeleted(currency)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Add new currency section
                if (isAddingNew) {
                    AddCurrencySection(
                        newCurrencyName = newCurrencyName,
                        onNameChange = { newCurrencyName = it },
                        onConfirm = {
                            if (newCurrencyName.isNotBlank()) {
                                onCurrencyAdded(newCurrencyName.trim(), position, gap)
                                newCurrencyName = ""
                                isAddingNew = false
                            }
                        },
                        onCancel = {
                            newCurrencyName = ""
                            isAddingNew = false
                        }
                    )
                }

                // Position and gap settings
                if (showPositionSettings) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    PositionSettings(
                        position = position,
                        gap = gap,
                        onPositionChange = { newPosition ->
                            position = newPosition
                            onPositionChanged(newPosition)
                        },
                        onGapChange = { newGap ->
                            gap = newGap
                            onGapChanged(newGap)
                        }
                    )
                }
            }
        },
        confirmButton = {
            if (!isAddingNew) {
                TextButton(onClick = { isAddingNew = true }) {
                    Text(stringResource(R.string.add_button))
                }
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onNoCurrencySelected) {
                    Text(stringResource(R.string.btn_no_currency))
                }
                if (isAddingNew) {
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = {
                        newCurrencyName = ""
                        isAddingNew = false
                    }) {
                        Text("Cancel")
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CurrencyGrid(
    currencies: List<String>,
    onCurrencySelected: (String) -> Unit,
    onCurrencyLongClick: (String) -> Unit
) {
    if (currencies.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.no_currencies),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .height((((currencies.size + 1) / 2).coerceAtMost(4) * 56).dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(currencies, key = { it }) { currency ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onCurrencySelected(currency) },
                            onLongClick = { onCurrencyLongClick(currency) }
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = currency,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun AddCurrencySection(
    newCurrencyName: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = newCurrencyName,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.new_currency_name_hint)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(
                onClick = onConfirm,
                enabled = newCurrencyName.isNotBlank()
            ) {
                Text("OK")
            }
        }
    }
}

@Composable
private fun PositionSettings(
    position: Currency.Position,
    gap: Boolean,
    onPositionChange: (Currency.Position) -> Unit,
    onGapChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.show_currency_input),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = position == Currency.Position.before,
                onClick = { onPositionChange(Currency.Position.before) }
            )
            Text(
                text = stringResource(R.string.currency_position_left),
                modifier = Modifier.weight(1f)
            )

            RadioButton(
                selected = position == Currency.Position.after,
                onClick = { onPositionChange(Currency.Position.after) }
            )
            Text(
                text = stringResource(R.string.currency_position_right),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.currency_has_gap),
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = gap,
                onCheckedChange = onGapChange
            )
        }
    }
}
