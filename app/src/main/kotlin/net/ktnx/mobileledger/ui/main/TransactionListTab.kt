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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.ktnx.mobileledger.ui.components.WeakOverscrollContainer

/**
 * Transaction List Tab displaying the list of transactions grouped by date.
 */
@Composable
fun TransactionListTab(
    uiState: TransactionListUiState,
    onAccountFilterChanged: (String?) -> Unit,
    onSuggestionSelected: (String) -> Unit,
    onClearFilter: () -> Unit,
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier
) {
    // Scroll to found transaction index when it changes
    LaunchedEffect(uiState.foundTransactionIndex) {
        uiState.foundTransactionIndex?.let { index ->
            listState.animateScrollToItem(index)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Account filter input
        if (uiState.showAccountFilterInput) {
            AccountFilterBar(
                accountFilter = uiState.accountFilter ?: "",
                suggestions = uiState.accountSuggestions,
                onAccountFilterChanged = onAccountFilterChanged,
                onSuggestionSelected = onSuggestionSelected,
                onClearFilter = onClearFilter
            )
        }

        if (uiState.isLoading && uiState.transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            WeakOverscrollContainer(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = uiState.transactions,
                        key = { item ->
                            when (item) {
                                is TransactionListDisplayItem.Header -> Long.MIN_VALUE
                                is TransactionListDisplayItem.DateDelimiter -> -item.date.toDate().time
                                is TransactionListDisplayItem.Transaction -> item.id
                            }
                        },
                        contentType = { item ->
                            when (item) {
                                is TransactionListDisplayItem.Header -> 0
                                is TransactionListDisplayItem.DateDelimiter -> 1
                                is TransactionListDisplayItem.Transaction -> 2
                            }
                        }
                    ) { item ->
                        when (item) {
                            is TransactionListDisplayItem.Header -> {
                                TransactionListHeader(text = uiState.headerText)
                            }

                            is TransactionListDisplayItem.DateDelimiter -> {
                                TransactionDateDelimiter(date = item.date, isMonthShown = item.isMonthShown)
                            }

                            is TransactionListDisplayItem.Transaction -> {
                                TransactionCard(transaction = item)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionListHeader(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
