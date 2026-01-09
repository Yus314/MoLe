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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import net.ktnx.mobileledger.utils.Logger

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
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val focusManager = LocalFocusManager.current

    var expanded by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    // Track pending leaf account selection for deferred focus move
    var pendingLeafAccount by remember { mutableStateOf<String?>(null) }

    // Track TextFieldValue for cursor position management
    // Note: Don't use value/cursorPosition as remember keys to preserve IME composition state
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = value,
                selection = TextRange(cursorPosition)
            )
        )
    }

    // Update when external value changes (only when not in composition/IME conversion)
    // Also handle deferred focus move when value is confirmed
    LaunchedEffect(value) {
        Logger.debug(
            "autocomplete-ui",
            "LaunchedEffect(value): value='${value.take(20)}', " +
                "text='${textFieldValue.text.take(20)}', composition=${textFieldValue.composition}, " +
                "pendingLeafAccount=${pendingLeafAccount?.take(20)}"
        )

        // Check if pending leaf account value is confirmed
        if (pendingLeafAccount != null && value.equals(pendingLeafAccount, ignoreCase = true)) {
            Logger.debug(
                "autocomplete-ui",
                "Value confirmed as '$value', moving focus to next field"
            )
            focusManager.moveFocus(FocusDirection.Next)
            pendingLeafAccount = null
        }

        // Only sync if not in IME composition and text differs
        if (textFieldValue.composition == null && textFieldValue.text != value) {
            Logger.debug("autocomplete-ui", "LaunchedEffect: Updating textFieldValue to '$value'")
            textFieldValue = TextFieldValue(
                text = value,
                selection = TextRange(value.length)
            )
        }
    }

    // Show dropdown when focused and suggestions available
    // Close dropdown when input exactly matches a leaf account (no children)
    LaunchedEffect(value, suggestions, isFocused) {
        val isLeafMatch = isExactMatchWithNoChildren(value, suggestions)
        val shouldExpand = isFocused && suggestions.isNotEmpty() && !isLeafMatch
        Logger.debug(
            "autocomplete-ui",
            "LaunchedEffect: isFocused=$isFocused, suggestions=${suggestions.size}, " +
                "isLeafMatch=$isLeafMatch, expanded=$shouldExpand"
        )
        expanded = shouldExpand
    }

    // Log current state for debugging
    Logger.debug(
        "autocomplete-ui",
        "Render: value='${value.take(10)}', expanded=$expanded, " +
            "isFocused=$isFocused, suggestions=${suggestions.size}"
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                Logger.debug(
                    "autocomplete-ui",
                    "onValueChange: text='${newValue.text.take(20)}', " +
                        "cursor=${newValue.selection.start}, composition=${newValue.composition}"
                )
                textFieldValue = newValue
                onValueChange(newValue.text, newValue.selection.start)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    Logger.debug(
                        "autocomplete-ui",
                        "onFocusChanged: ${focusState.isFocused} for value='${value.take(10)}'"
                    )
                    isFocused = focusState.isFocused
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
            onDismissRequest = {
                Logger.debug("autocomplete-ui", "onDismissRequest called")
                expanded = false
            },
            modifier = Modifier
                .exposedDropdownSize(matchTextFieldWidth = false)
                .width(screenWidth - 32.dp)
        ) {
            Logger.debug(
                "autocomplete-ui",
                "DropdownMenu content: ${suggestions.take(MAX_SUGGESTIONS).size} items"
            )
            suggestions.take(MAX_SUGGESTIONS).forEach { suggestion ->
                DropdownMenuItem(
                    text = { Text(suggestion) },
                    onClick = {
                        Logger.debug(
                            "autocomplete-ui",
                            "DropdownMenuItem clicked: '$suggestion' (length=${suggestion.length})"
                        )
                        // 1. まず textFieldValue を直接更新（composition をクリア）
                        textFieldValue = TextFieldValue(
                            text = suggestion,
                            selection = TextRange(suggestion.length)
                        )
                        // 2. ViewModel に通知
                        onSuggestionSelected(suggestion)
                        // 3. ドロップダウンを閉じる
                        expanded = false
                        // 4. リーフアカウントならフォーカス移動を予約
                        //    (実際の移動は LaunchedEffect(value) で値が確定した後に行う)
                        val isLeaf = isExactMatchWithNoChildren(suggestion, suggestions)
                        if (isLeaf) {
                            Logger.debug(
                                "autocomplete-ui",
                                "Leaf account selected, scheduling focus move for '$suggestion'"
                            )
                            pendingLeafAccount = suggestion
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
