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

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import net.ktnx.mobileledger.utils.SimpleDate

/**
 * Main composable for the New Transaction screen.
 * Displays a form for creating new transactions with dynamic account rows.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTransactionScreen(viewModel: NewTransactionViewModel = hiltViewModel(), onNavigateBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showMenuExpanded by remember { mutableStateOf(false) }

    // Handle back navigation with unsaved changes check
    BackHandler {
        if (uiState.hasUnsavedChanges) {
            showDiscardDialog = true
        } else {
            onNavigateBack()
        }
    }

    // Handle effects
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is NewTransactionEffect.NavigateBack -> onNavigateBack()

                is NewTransactionEffect.TransactionSaved -> {
                    snackbarHostState.showSnackbar(
                        message = "Transaction saved",
                        duration = SnackbarDuration.Short
                    )
                }

                is NewTransactionEffect.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = effect.message,
                        duration = SnackbarDuration.Long
                    )
                }

                is NewTransactionEffect.HideKeyboard -> {
                    keyboardController?.hide()
                }

                is NewTransactionEffect.RequestFocus -> {
                    // Focus handling would require FocusRequester setup
                }
            }
        }
    }

    // Discard changes confirmation dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard changes?") },
            text = { Text("You have unsaved changes. Are you sure you want to discard them?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Keep editing")
                }
            }
        )
    }

    // Date picker dialog
    if (uiState.showDatePicker) {
        DatePickerDialogCompose(
            currentDate = uiState.date,
            onDateSelected = { date ->
                viewModel.onEvent(NewTransactionEvent.UpdateDate(date))
            },
            onDismiss = {
                viewModel.onEvent(NewTransactionEvent.DismissDatePicker)
            }
        )
    }

    // Template selector dialog
    if (uiState.showTemplateSelector) {
        TemplateSelectorDialog(
            templates = uiState.availableTemplates,
            onTemplateSelected = { templateId ->
                viewModel.onEvent(NewTransactionEvent.ApplyTemplate(templateId))
            },
            onDismiss = {
                viewModel.onEvent(NewTransactionEvent.DismissTemplateSelector)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Transaction") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (uiState.hasUnsavedChanges) {
                                showDiscardDialog = true
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Overflow menu
                    Box {
                        IconButton(onClick = { showMenuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options"
                            )
                        }
                        DropdownMenu(
                            expanded = showMenuExpanded,
                            onDismissRequest = { showMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (uiState.showCurrency) "Hide currency" else "Show currency") },
                                onClick = {
                                    viewModel.onEvent(NewTransactionEvent.ToggleCurrency)
                                    showMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (uiState.showComments) "Hide comments" else "Show comments") },
                                onClick = {
                                    viewModel.onEvent(NewTransactionEvent.ToggleComments)
                                    showMenuExpanded = false
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Use template") },
                                onClick = {
                                    viewModel.onEvent(NewTransactionEvent.ShowTemplateSelector)
                                    showMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Reset") },
                                onClick = {
                                    viewModel.onEvent(NewTransactionEvent.Reset)
                                    showMenuExpanded = false
                                }
                            )
                            if (uiState.isSimulateSave) {
                                DropdownMenuItem(
                                    text = { Text("Disable simulate save") },
                                    onClick = {
                                        viewModel.onEvent(NewTransactionEvent.ToggleSimulateSave)
                                        showMenuExpanded = false
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Enable simulate save") },
                                    onClick = {
                                        viewModel.onEvent(NewTransactionEvent.ToggleSimulateSave)
                                        showMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        },
        floatingActionButton = {
            val fabAlpha by animateFloatAsState(
                targetValue = if (uiState.isSubmittable && !uiState.isSubmitting) 1f else 0.5f,
                label = "fabAlpha"
            )

            FloatingActionButton(
                onClick = {
                    if (uiState.isSubmittable && !uiState.isSubmitting) {
                        viewModel.onEvent(NewTransactionEvent.Submit)
                    }
                },
                modifier = Modifier.alpha(fabAlpha)
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Save transaction"
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isBusy && !uiState.isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                NewTransactionContent(
                    uiState = uiState,
                    onEvent = viewModel::onEvent
                )
            }
        }
    }
}

@Composable
private fun NewTransactionContent(uiState: NewTransactionUiState, onEvent: (NewTransactionEvent) -> Unit) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.US) }
    val formattedDate = remember(uiState.date) {
        val calendar = GregorianCalendar(uiState.date.year, uiState.date.month - 1, uiState.date.day)
        dateFormat.format(calendar.time)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header row (date + description)
        item(key = "header") {
            TransactionHeaderRow(
                date = formattedDate,
                description = uiState.description,
                descriptionSuggestions = uiState.descriptionSuggestions,
                transactionComment = uiState.transactionComment,
                showComments = uiState.showComments,
                onDateClick = { onEvent(NewTransactionEvent.ShowDatePicker) },
                onDescriptionChange = { onEvent(NewTransactionEvent.UpdateDescription(it)) },
                onDescriptionSuggestionSelected = { description ->
                    onEvent(NewTransactionEvent.UpdateDescription(description))
                    onEvent(NewTransactionEvent.LoadFromDescription(description))
                },
                onTransactionCommentChange = { onEvent(NewTransactionEvent.UpdateTransactionComment(it)) },
                onFocusChanged = { element ->
                    onEvent(NewTransactionEvent.NoteFocus(null, element))
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }

        // Account rows
        itemsIndexed(
            items = uiState.accounts,
            key = { _, row -> row.id }
        ) { index, row ->
            AnimatedVisibility(
                visible = true,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                TransactionRowItem(
                    row = row,
                    accountSuggestions = if (uiState.accountSuggestionsForRowId == row.id) {
                        uiState.accountSuggestions
                    } else {
                        emptyList()
                    },
                    showCurrency = uiState.showCurrency,
                    showComments = uiState.showComments,
                    canDelete = uiState.accounts.size > 2,
                    onAccountNameChange = { name, cursor ->
                        onEvent(NewTransactionEvent.UpdateAccountName(row.id, name, cursor))
                    },
                    onAccountSuggestionSelected = { name ->
                        net.ktnx.mobileledger.utils.Logger.debug(
                            "autocomplete",
                            "onAccountSuggestionSelected: row.id=${row.id}, " +
                                "name='$name', name.length=${name.length}"
                        )
                        onEvent(NewTransactionEvent.UpdateAccountName(row.id, name, name.length))
                    },
                    onAmountChange = { amount ->
                        onEvent(NewTransactionEvent.UpdateAmount(row.id, amount))
                    },
                    onCurrencyClick = {
                        onEvent(NewTransactionEvent.ShowCurrencySelector(row.id))
                    },
                    onCommentChange = { comment ->
                        onEvent(NewTransactionEvent.UpdateAccountComment(row.id, comment))
                    },
                    onDelete = {
                        onEvent(NewTransactionEvent.RemoveAccountRow(row.id))
                    },
                    onFocusChanged = { element ->
                        onEvent(NewTransactionEvent.NoteFocus(row.id, element))
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
                    onClick = { onEvent(NewTransactionEvent.AddAccountRow(null)) }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialogCompose(
    currentDate: SimpleDate,
    onDateSelected: (SimpleDate) -> Unit,
    onDismiss: () -> Unit
) {
    val calendar = remember(currentDate) {
        Calendar.getInstance().apply {
            set(currentDate.year, currentDate.month - 1, currentDate.day)
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = calendar.timeInMillis
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selectedCalendar = Calendar.getInstance().apply {
                            timeInMillis = millis
                        }
                        val date = SimpleDate(
                            selectedCalendar.get(Calendar.YEAR),
                            selectedCalendar.get(Calendar.MONTH) + 1,
                            selectedCalendar.get(Calendar.DAY_OF_MONTH)
                        )
                        onDateSelected(date)
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun TemplateSelectorDialog(
    templates: List<TemplateItem>,
    onTemplateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select template") },
        text = {
            if (templates.isEmpty()) {
                Text("No templates available")
            } else {
                LazyColumn {
                    itemsIndexed(templates) { _, template ->
                        TextButton(
                            onClick = { onTemplateSelected(template.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = template.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                template.description?.let { desc ->
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
