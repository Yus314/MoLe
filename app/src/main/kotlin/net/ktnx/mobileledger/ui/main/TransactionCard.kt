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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.ktnx.mobileledger.core.domain.model.AmountStyle

/**
 * Card displaying a single transaction with its accounts.
 *
 * Shows the transaction description, optional comment, account rows
 * with amounts, and optional running total when filtered by account.
 */
@Composable
fun TransactionCard(transaction: TransactionListDisplayItem.Transaction, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
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

            // Account rows - memoize bold account comparison
            val boldAccountFlags = remember(transaction.boldAccountName, transaction.accounts) {
                transaction.accounts.map { it.accountName == transaction.boldAccountName }
            }
            transaction.accounts.forEachIndexed { index, account ->
                TransactionAccountRow(
                    account = account,
                    isBold = boldAccountFlags[index]
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

/**
 * Single account row within a transaction card.
 *
 * Displays the account name and formatted amount with color coding
 * (green for positive, red for negative).
 */
@Composable
fun TransactionAccountRow(account: TransactionAccountDisplayItem, isBold: Boolean, modifier: Modifier = Modifier) {
    // Memoize color calculation based on amount (green for positive, red for negative)
    val amountColor = remember(account.amount) {
        when {
            account.amount > 0 -> Color(0xFF4CAF50)
            account.amount < 0 -> Color(0xFFF44336)
            else -> null
        }
    } ?: MaterialTheme.colorScheme.onSurface

    // Memoize amount formatting - only format when visible
    val formattedAmount = remember(account.amount, account.currency, account.amountStyle) {
        AmountStyle.formatAccountAmount(
            account.amount,
            account.currency.ifEmpty { null },
            account.amountStyle
        )
    }

    Row(
        modifier = modifier
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

        // Amount with currency (formatted on demand)
        Text(
            text = formattedAmount,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = amountColor,
            textAlign = TextAlign.End
        )
    }
}
