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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale
import net.ktnx.mobileledger.ui.components.WeakOverscrollContainer
import net.ktnx.mobileledger.utils.SimpleDate

/**
 * Transaction List Tab displaying the list of transactions grouped by date.
 */
@Composable
fun TransactionListTab(
    uiState: TransactionListUiState,
    onAccountFilterChanged: (String?) -> Unit,
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
                onAccountFilterChanged = onAccountFilterChanged,
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
                                is TransactionListDisplayItem.Header -> "header"
                                is TransactionListDisplayItem.DateDelimiter -> "date-${item.date}"
                                is TransactionListDisplayItem.Transaction -> "tx-${item.id}"
                            }
                        }
                    ) { item ->
                        when (item) {
                            is TransactionListDisplayItem.Header -> {
                                TransactionListHeader(text = uiState.headerText)
                            }

                            is TransactionListDisplayItem.DateDelimiter -> {
                                DateDelimiterRow(date = item.date, isMonthShown = item.isMonthShown)
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
private fun AccountFilterBar(
    accountFilter: String,
    onAccountFilterChanged: (String?) -> Unit,
    onClearFilter: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = accountFilter,
            onValueChange = { onAccountFilterChanged(it.ifEmpty { null }) },
            modifier = Modifier.weight(1f),
            placeholder = { Text("Filter by account") },
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = onClearFilter) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear filter"
                    )
                }
            }
        )
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

@Composable
private fun DateDelimiterRow(date: SimpleDate, isMonthShown: Boolean) {
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
        modifier = Modifier
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

@Composable
private fun TransactionCard(transaction: TransactionListDisplayItem.Transaction) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Description
            Text(
                text = transaction.description,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Comment (if any)
            transaction.comment?.let { comment ->
                if (comment.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = comment,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Account rows
            transaction.accounts.forEach { account ->
                TransactionAccountRow(
                    account = account,
                    isBold = account.accountName == transaction.boldAccountName
                )
            }

            // Running total (if filtering by account)
            transaction.runningTotal?.let { total ->
                if (total.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = total,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionAccountRow(account: TransactionAccountDisplayItem, isBold: Boolean) {
    val amountColor = when {
        account.amount > 0 -> Color(0xFF4CAF50)

        // Green for positive
        account.amount < 0 -> Color(0xFFF44336)

        // Red for negative
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Account name
        Text(
            text = account.accountName,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Amount with currency (pre-formatted)
        Text(
            text = account.formattedAmount,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = amountColor,
            textAlign = TextAlign.End
        )
    }
}
