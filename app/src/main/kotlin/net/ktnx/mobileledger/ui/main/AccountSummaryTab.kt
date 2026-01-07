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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.ktnx.mobileledger.utils.Colors

/**
 * Account Summary Tab displaying the list of accounts with their balances.
 */
@Composable
fun AccountSummaryTab(
    uiState: AccountSummaryUiState,
    onToggleExpanded: (Long) -> Unit,
    onToggleAmountsExpanded: (Long) -> Unit,
    onAccountClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.isLoading && uiState.accounts.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // Build a map of account name to expanded state for visibility calculation
    val expandedStateMap = remember(uiState.accounts) {
        uiState.accounts
            .filterIsInstance<AccountSummaryListItem.Account>()
            .associate { it.name to it.isExpanded }
    }

    // Filter visible accounts based on parent's expanded state
    val visibleItems = remember(uiState.accounts, expandedStateMap) {
        uiState.accounts.filter { item ->
            when (item) {
                is AccountSummaryListItem.Header -> true

                is AccountSummaryListItem.Account -> {
                    isAccountVisible(item.parentName, expandedStateMap)
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        items(
            items = visibleItems,
            key = { item ->
                when (item) {
                    is AccountSummaryListItem.Header -> "header"
                    is AccountSummaryListItem.Account -> item.id
                }
            }
        ) { item ->
            when (item) {
                is AccountSummaryListItem.Header -> {
                    AccountSummaryHeader(
                        text = item.text,
                        modifier = Modifier.animateItem(
                            fadeInSpec = null,
                            fadeOutSpec = null,
                            placementSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    )
                }

                is AccountSummaryListItem.Account -> {
                    Column(
                        modifier = Modifier.animateItem(
                            fadeInSpec = null,
                            fadeOutSpec = null,
                            placementSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    ) {
                        AccountSummaryRow(
                            account = item,
                            onToggleExpanded = { onToggleExpanded(item.id) },
                            onToggleAmountsExpanded = { onToggleAmountsExpanded(item.id) },
                            onClick = { onAccountClick(item.name) }
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Check if an account is visible based on all its ancestors being expanded.
 */
private fun isAccountVisible(parentName: String?, expandedStateMap: Map<String, Boolean>): Boolean {
    if (parentName == null) return true // Top-level accounts are always visible

    // Check if parent is expanded
    val parentExpanded = expandedStateMap[parentName] ?: true
    if (!parentExpanded) return false

    // Recursively check grandparent
    // Find parent's parent by looking for the longest matching prefix
    val grandparentName = parentName.substringBeforeLast(':', "").ifEmpty { null }
    return isAccountVisible(grandparentName, expandedStateMap)
}

@Composable
private fun AccountSummaryHeader(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
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
private fun AccountSummaryRow(
    account: AccountSummaryListItem.Account,
    onToggleExpanded: () -> Unit,
    onToggleAmountsExpanded: () -> Unit,
    onClick: () -> Unit
) {
    // For leaf nodes, align text with parent's text position (no spacer needed)
    // Parent text starts at: 8 + parentLevel*16 + 32 (IconButton)
    // Leaf text should start at same position: 8 + level*16 + 16 (without spacer)
    val indentPadding = remember(account.level, account.hasSubAccounts) {
        if (account.hasSubAccounts) {
            (account.level * 16).dp
        } else {
            (account.level * 16 + 16).dp
        }
    }

    // Arrow rotation animation
    val rotation by animateFloatAsState(
        targetValue = if (account.isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "arrowRotation"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick)
            .padding(start = 8.dp + indentPadding, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Expand/collapse indicator for accounts with sub-accounts
        // Leaf nodes have no spacer - their text aligns with parent's text
        if (account.hasSubAccounts) {
            IconButton(
                onClick = onToggleExpanded,
                modifier = Modifier.width(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (account.isExpanded) "Collapse" else "Expand",
                    modifier = Modifier.rotate(rotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Account name
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = account.shortName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (account.hasSubAccounts) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Amounts
        AccountAmountsColumn(
            amounts = account.amounts,
            isExpanded = account.amountsExpanded,
            onToggleExpanded = onToggleAmountsExpanded
        )
    }
}

@Composable
private fun AccountAmountsColumn(amounts: List<AccountAmount>, isExpanded: Boolean, onToggleExpanded: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier
            .clickable(enabled = amounts.size > 1, onClick = onToggleExpanded)
            .padding(start = 8.dp)
    ) {
        if (amounts.isEmpty()) {
            Text(
                text = "",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.End
            )
        } else {
            // First amount is always visible
            AmountText(amount = amounts.first())

            // Additional amounts with animation
            AnimatedVisibility(
                visible = isExpanded && amounts.size > 1,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    amounts.drop(1).forEach { amount ->
                        AmountText(amount = amount)
                    }
                }
            }

            // "+N more" indicator with animation
            AnimatedVisibility(
                visible = !isExpanded && amounts.size > 1,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Text(
                    text = "+${amounts.size - 1} more",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun AmountText(amount: AccountAmount) {
    val textColor = when {
        amount.amount > 0 -> Color(0xFF4CAF50)

        // Green for positive
        amount.amount < 0 -> Color(0xFFF44336)

        // Red for negative
        else -> MaterialTheme.colorScheme.onSurface
    }

    Text(
        text = amount.formattedAmount,
        style = MaterialTheme.typography.bodyMedium,
        color = textColor,
        textAlign = TextAlign.End
    )
}
