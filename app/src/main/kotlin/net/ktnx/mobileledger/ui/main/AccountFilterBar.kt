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

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList

/**
 * Account filter bar with autocomplete suggestions.
 *
 * Displays an input field for filtering transactions by account name,
 * with dropdown suggestions based on matching account names.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountFilterBar(
    accountFilter: String,
    suggestions: ImmutableList<String>,
    onAccountFilterChanged: (String?) -> Unit,
    onSuggestionSelected: (String) -> Unit,
    onClearFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(suggestions, isFocused) {
        expanded = isFocused && suggestions.isNotEmpty()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        @Suppress("DEPRECATION")
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = accountFilter,
                onValueChange = { onAccountFilterChanged(it.ifEmpty { null }) },
                modifier = Modifier
                    .menuAnchor()
                    .onFocusChanged { focusState -> isFocused = focusState.isFocused }
                    .fillMaxWidth(),
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

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                suggestions.forEach { suggestion ->
                    DropdownMenuItem(
                        text = { Text(suggestion) },
                        onClick = {
                            onSuggestionSelected(suggestion)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
