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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

/**
 * Autocomplete text field for account names.
 * Shows suggestions as the user types, with dropdown for selection.
 *
 * This is a simplified implementation that:
 * - Uses the parent's value directly (no local TextFieldValue state)
 * - Lets TextField manage cursor position internally
 * - Relies on ViewModel debouncing for suggestions (50ms + Job cancellation)
 * - Controls dropdown state independently based on focus and suggestions
 */
@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountAutocomplete(
    value: String,
    suggestions: List<String>,
    suggestionsVersion: Int,
    onValueChange: (String) -> Unit,
    onSuggestionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    onFocusChanged: ((Boolean) -> Unit)? = null
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val focusManager = LocalFocusManager.current

    var expanded by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    // Dropdown expansion: focus + has suggestions + not an exact leaf match
    // Use 'suggestionsVersion' (counter from ViewModel) as key to ensure LaunchedEffect
    // triggers every time suggestions are updated, regardless of list content/size.
    // IMPORTANT: Do NOT add 'value' to dependencies - it causes recomposition on every keystroke,
    // which blocks long-press delete.
    LaunchedEffect(suggestionsVersion, isFocused) {
        // Don't open dropdown if the value is an exact match with only one suggestion (leaf account)
        val isLeafExactMatch = suggestions.size == 1 &&
            suggestions.first().equals(value, ignoreCase = true)
        expanded = isFocused && suggestions.isNotEmpty() && !isLeafExactMatch
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                expanded = false // Close dropdown during typing to avoid keyboard interception
                onValueChange(newValue)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                    // Close dropdown when focus is lost
                    if (!focusState.isFocused) {
                        expanded = false
                    }
                    onFocusChanged?.invoke(focusState.isFocused)
                },
            label = label?.let { { Text(it) } },
            placeholder = placeholder?.let { { Text(it) } },
            enabled = enabled,
            isError = isError,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Next) }
            ),
            colors = OutlinedTextFieldDefaults.colors()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .exposedDropdownSize(matchTextFieldWidth = false)
                .width(screenWidth - 32.dp)
        ) {
            suggestions.take(MAX_SUGGESTIONS).forEach { suggestion ->
                DropdownMenuItem(
                    text = { Text(suggestion) },
                    onClick = {
                        onSuggestionSelected(suggestion)
                        expanded = false
                        // Move to next field if leaf account selected
                        if (isExactMatchWithNoChildren(suggestion, suggestions)) {
                            focusManager.moveFocus(FocusDirection.Next)
                        }
                    }
                )
            }
        }
    }
}

/**
 * Simplified autocomplete for description field.
 * Similar to AccountAutocomplete but without cursor position tracking.
 */
@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DescriptionAutocomplete(
    value: String,
    suggestions: List<String>,
    onValueChange: (String) -> Unit,
    onSuggestionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    onFocusChanged: ((Boolean) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(suggestions, isFocused) {
        expanded = isFocused && suggestions.isNotEmpty()
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                    onFocusChanged?.invoke(focusState.isFocused)
                },
            label = label?.let { { Text(it) } },
            placeholder = placeholder?.let { { Text(it) } },
            enabled = enabled,
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            suggestions.take(MAX_SUGGESTIONS).forEach { suggestion ->
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

/**
 * Check if the input value exactly matches an account name and has no child accounts.
 * Used to auto-close dropdown when user has typed a complete leaf account name.
 */
private fun isExactMatchWithNoChildren(input: String, suggestions: List<String>): Boolean {
    if (input.isBlank()) return false

    val hasExactMatch = suggestions.any { it.equals(input, ignoreCase = true) }
    if (!hasExactMatch) return false

    val childPrefix = "${input.lowercase()}:"
    val hasChildren = suggestions.any { it.lowercase().startsWith(childPrefix) }

    return !hasChildren
}

private const val MAX_SUGGESTIONS = 10
