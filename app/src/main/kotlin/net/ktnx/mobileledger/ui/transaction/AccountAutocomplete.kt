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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Autocomplete text field for account names.
 * Shows suggestions as the user types, with dropdown for selection.
 */
@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountAutocomplete(
    value: String,
    cursorPosition: Int,
    suggestions: List<String>,
    onValueChange: (String, Int) -> Unit,
    onSuggestionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    onFocusChanged: ((Boolean) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    // Track TextFieldValue for cursor position management
    var textFieldValue by remember(value, cursorPosition) {
        mutableStateOf(
            TextFieldValue(
                text = value,
                selection = TextRange(cursorPosition)
            )
        )
    }

    // Update when external value changes
    LaunchedEffect(value) {
        if (textFieldValue.text != value) {
            textFieldValue = TextFieldValue(
                text = value,
                selection = TextRange(value.length)
            )
        }
    }

    // Show dropdown when focused and suggestions available
    LaunchedEffect(suggestions, isFocused) {
        expanded = isFocused && suggestions.isNotEmpty()
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                onValueChange(newValue.text, newValue.selection.start)
            },
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
            isError = isError,
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
                        textFieldValue = TextFieldValue(
                            text = suggestion,
                            selection = TextRange(suggestion.length)
                        )
                        expanded = false
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

private const val MAX_SUGGESTIONS = 10
