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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.GregorianCalendar
import java.util.Locale

@Composable
internal fun NewTransactionContent(
    formUiState: TransactionFormUiState,
    accountRowsUiState: AccountRowsUiState,
    onFormEvent: (TransactionFormEvent) -> Unit,
    onAccountRowsEvent: (AccountRowsEvent) -> Unit,
    descriptionFocusRequester: FocusRequester
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.US) }
    val formattedDate = remember(formUiState.date) {
        val calendar = GregorianCalendar(
            formUiState.date.year,
            formUiState.date.month - 1,
            formUiState.date.day
        )
        dateFormat.format(calendar.time)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header row (date + description)
        item(key = "header") {
            TransactionHeaderRow(
                date = formattedDate,
                description = formUiState.description,
                descriptionSuggestions = formUiState.descriptionSuggestions,
                transactionComment = formUiState.transactionComment,
                isCommentExpanded = formUiState.isTransactionCommentExpanded,
                onDateClick = { onFormEvent(TransactionFormEvent.ShowDatePicker) },
                onDescriptionChange = { onFormEvent(TransactionFormEvent.UpdateDescription(it)) },
                onDescriptionSuggestionSelected = { description ->
                    onFormEvent(TransactionFormEvent.UpdateDescription(description))
                    onFormEvent(TransactionFormEvent.LoadFromDescription(description))
                },
                onTransactionCommentChange = {
                    onFormEvent(TransactionFormEvent.UpdateTransactionComment(it))
                },
                onToggleComment = { onFormEvent(TransactionFormEvent.ToggleTransactionComment) },
                onFocusChanged = { element ->
                    onAccountRowsEvent(AccountRowsEvent.NoteFocus(null, element))
                },
                descriptionFocusRequester = descriptionFocusRequester
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }

        // Account rows
        itemsIndexed(
            items = accountRowsUiState.accounts,
            key = { _, row -> row.id }
        ) { index, row ->
            AnimatedVisibility(
                visible = true,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                TransactionRowItem(
                    row = row,
                    accountSuggestions = if (accountRowsUiState.accountSuggestionsForRowId == row.id) {
                        accountRowsUiState.accountSuggestions
                    } else {
                        emptyList()
                    },
                    accountSuggestionsVersion = accountRowsUiState.accountSuggestionsVersion,
                    showCurrency = accountRowsUiState.showCurrency,
                    canDelete = accountRowsUiState.accounts.size > 2,
                    onAccountNameChange = { name ->
                        onAccountRowsEvent(AccountRowsEvent.UpdateAccountName(row.id, name))
                    },
                    onAccountSuggestionSelected = { name ->
                        onAccountRowsEvent(AccountRowsEvent.UpdateAccountName(row.id, name))
                    },
                    onAmountChange = { amount ->
                        onAccountRowsEvent(AccountRowsEvent.UpdateAmount(row.id, amount))
                    },
                    onCurrencyClick = {
                        onAccountRowsEvent(AccountRowsEvent.ShowCurrencySelector(row.id))
                    },
                    onCommentChange = { comment ->
                        onAccountRowsEvent(AccountRowsEvent.UpdateAccountComment(row.id, comment))
                    },
                    onToggleComment = {
                        onAccountRowsEvent(AccountRowsEvent.ToggleAccountComment(row.id))
                    },
                    onDelete = {
                        onAccountRowsEvent(AccountRowsEvent.RemoveAccountRow(row.id))
                    },
                    onFocusChanged = { element ->
                        onAccountRowsEvent(AccountRowsEvent.NoteFocus(row.id, element))
                    }
                )
            }
        }

        // Add row button
        item(key = "add_row") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(
                    onClick = { onAccountRowsEvent(AccountRowsEvent.AddAccountRow(null)) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Add account row")
                }
            }

            // Bottom padding for FAB
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
