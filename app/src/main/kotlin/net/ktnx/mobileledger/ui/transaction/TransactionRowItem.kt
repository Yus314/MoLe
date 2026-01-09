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

package net.ktnx.mobileledger.ui.transaction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * A single account row in the new transaction form.
 * Contains account name (with autocomplete), amount, optional currency, and optional comment.
 */
@Composable
fun TransactionRowItem(
    row: TransactionAccountRow,
    accountSuggestions: List<String>,
    accountSuggestionsVersion: Int,
    showCurrency: Boolean,
    showComments: Boolean,
    canDelete: Boolean,
    onAccountNameChange: (String) -> Unit,
    onAccountSuggestionSelected: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onCurrencyClick: () -> Unit,
    onCommentChange: (String) -> Unit,
    onDelete: () -> Unit,
    onFocusChanged: (FocusedElement?) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        // Main row: Account name and Amount
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Account name with autocomplete
            AccountAutocomplete(
                value = row.accountName,
                suggestions = accountSuggestions,
                suggestionsVersion = accountSuggestionsVersion,
                onValueChange = onAccountNameChange,
                onSuggestionSelected = onAccountSuggestionSelected,
                modifier = Modifier.weight(1f),
                placeholder = "Account",
                onFocusChanged = { focused ->
                    if (focused) onFocusChanged(FocusedElement.Account)
                }
            )

            // Amount field
            OutlinedTextField(
                value = row.amountText,
                onValueChange = onAmountChange,
                modifier = Modifier
                    .width(120.dp)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) onFocusChanged(FocusedElement.Amount)
                    },
                placeholder = row.amountHint?.let { { Text(it, textAlign = TextAlign.End) } },
                singleLine = true,
                isError = !row.isAmountValid,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Next) }
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    textAlign = TextAlign.End
                ),
                colors = OutlinedTextFieldDefaults.colors()
            )

            // Currency button (optional)
            AnimatedVisibility(
                visible = showCurrency,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                TextButton(
                    onClick = onCurrencyClick,
                    modifier = Modifier.width(72.dp)
                ) {
                    Text(
                        text = row.currency.ifEmpty { "---" },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Delete button (only if more than 2 rows)
            if (canDelete) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete row",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
        }

        // Comment field (optional, shown below main row)
        AnimatedVisibility(
            visible = showComments,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            OutlinedTextField(
                value = row.comment,
                onValueChange = onCommentChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) onFocusChanged(FocusedElement.AccountComment)
                    },
                placeholder = { Text("Comment") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Next) }
                ),
                colors = OutlinedTextFieldDefaults.colors()
            )
        }
    }
}

/**
 * Transaction header row containing date, description, and optional transaction comment.
 */
@Composable
fun TransactionHeaderRow(
    date: String,
    description: String,
    descriptionSuggestions: List<String>,
    transactionComment: String,
    showComments: Boolean,
    onDateClick: () -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDescriptionSuggestionSelected: (String) -> Unit,
    onTransactionCommentChange: (String) -> Unit,
    onFocusChanged: (FocusedElement?) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Date and Description row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Date button/field
            TextButton(
                onClick = onDateClick,
                modifier = Modifier.width(100.dp)
            ) {
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            // Description with autocomplete
            DescriptionAutocomplete(
                value = description,
                suggestions = descriptionSuggestions,
                onValueChange = onDescriptionChange,
                onSuggestionSelected = onDescriptionSuggestionSelected,
                modifier = Modifier.weight(1f),
                placeholder = "Description",
                onFocusChanged = { focused ->
                    if (focused) onFocusChanged(FocusedElement.Description)
                }
            )
        }

        // Transaction comment (optional)
        AnimatedVisibility(
            visible = showComments,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            OutlinedTextField(
                value = transactionComment,
                onValueChange = onTransactionCommentChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) onFocusChanged(FocusedElement.TransactionComment)
                    },
                label = { Text("Transaction comment") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Next) }
                ),
                colors = OutlinedTextFieldDefaults.colors()
            )
        }
    }
}
