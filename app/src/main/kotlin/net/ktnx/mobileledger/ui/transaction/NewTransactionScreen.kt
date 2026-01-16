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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.GregorianCalendar
import java.util.Locale
import net.ktnx.mobileledger.ui.components.CurrencyPickerDialog
import net.ktnx.mobileledger.ui.components.MoleDatePickerDialog

/**
 * Main composable for the New Transaction screen.
 * Displays a form for creating new transactions with dynamic account rows.
 *
 * Uses three specialized ViewModels:
 * - TransactionFormViewModel: Form fields (date, description, comment) and submission
 * - AccountRowsViewModel: Account row CRUD, amount calculation, currency selection
 * - TemplateApplicatorViewModel: Template search and application
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTransactionScreen(
    formViewModel: TransactionFormViewModel = hiltViewModel(),
    accountRowsViewModel: AccountRowsViewModel = hiltViewModel(),
    templateApplicatorViewModel: TemplateApplicatorViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val formUiState by formViewModel.uiState.collectAsState()
    val accountRowsUiState by accountRowsViewModel.uiState.collectAsState()
    val templateUiState by templateApplicatorViewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val descriptionFocusRequester = remember { FocusRequester() }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showMenuExpanded by remember { mutableStateOf(false) }

    // Compute combined state for submittability
    val isSubmittable = formUiState.isFormValid && accountRowsUiState.isBalanced
    val hasUnsavedChanges = formUiState.hasUnsavedChanges || accountRowsUiState.hasAccountChanges

    // Handle back navigation with unsaved changes check
    BackHandler {
        if (hasUnsavedChanges) {
            showDiscardDialog = true
        } else {
            onNavigateBack()
        }
    }

    // Handle form effects
    LaunchedEffect(Unit) {
        formViewModel.effects.collect { effect ->
            when (effect) {
                is TransactionFormEffect.NavigateBack -> onNavigateBack()

                is TransactionFormEffect.TransactionSaved -> {
                    snackbarHostState.showSnackbar(
                        message = "Transaction saved",
                        duration = SnackbarDuration.Short
                    )
                    // Reset account rows when transaction is saved
                    accountRowsViewModel.onEvent(AccountRowsEvent.Reset)
                }

                is TransactionFormEffect.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = effect.message,
                        duration = SnackbarDuration.Long
                    )
                }

                is TransactionFormEffect.HideKeyboard -> {
                    keyboardController?.hide()
                }

                is TransactionFormEffect.RequestFocus -> {
                    if (effect.element == FocusedElement.Description) {
                        descriptionFocusRequester.requestFocus()
                    }
                }

                is TransactionFormEffect.ShowDiscardChangesDialog -> {
                    showDiscardDialog = true
                }
            }
        }
    }

    // Handle account rows effects
    LaunchedEffect(Unit) {
        accountRowsViewModel.effects.collect { effect ->
            when (effect) {
                is AccountRowsEffect.RequestFocus -> {
                    // Focus handling for account rows is handled by TransactionRowItem
                }

                is AccountRowsEffect.HideKeyboard -> {
                    keyboardController?.hide()
                }
            }
        }
    }

    // Request initial focus on Description field
    LaunchedEffect(Unit) {
        descriptionFocusRequester.requestFocus()
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
    if (formUiState.showDatePicker) {
        MoleDatePickerDialog(
            initialDate = formUiState.date,
            futureDates = formUiState.futureDates,
            onDateSelected = { date ->
                formViewModel.onEvent(TransactionFormEvent.UpdateDate(date))
            },
            onDismiss = {
                formViewModel.onEvent(TransactionFormEvent.DismissDatePicker)
            }
        )
    }

    // Currency picker dialog
    if (accountRowsUiState.showCurrencySelector) {
        CurrencyPickerDialog(
            currencies = accountRowsUiState.availableCurrencies,
            showPositionSettings = true,
            onCurrencySelected = { currency ->
                accountRowsUiState.currencySelectorRowId?.let { rowId ->
                    accountRowsViewModel.onEvent(AccountRowsEvent.UpdateCurrency(rowId, currency))
                }
                accountRowsViewModel.onEvent(AccountRowsEvent.DismissCurrencySelector)
            },
            onCurrencyAdded = { name, position, gap ->
                accountRowsViewModel.onEvent(AccountRowsEvent.AddCurrency(name, position, gap))
            },
            onCurrencyDeleted = { name ->
                accountRowsViewModel.onEvent(AccountRowsEvent.DeleteCurrency(name))
            },
            onNoCurrencySelected = {
                accountRowsUiState.currencySelectorRowId?.let { rowId ->
                    accountRowsViewModel.onEvent(AccountRowsEvent.UpdateCurrency(rowId, ""))
                }
                accountRowsViewModel.onEvent(AccountRowsEvent.DismissCurrencySelector)
            },
            onPositionChanged = { },
            onGapChanged = { },
            onDismiss = {
                accountRowsViewModel.onEvent(AccountRowsEvent.DismissCurrencySelector)
            }
        )
    }

    // Template selector dialog
    if (templateUiState.showTemplateSelector) {
        TemplateSelectorDialog(
            templates = templateUiState.availableTemplates,
            onTemplateSelected = { templateId ->
                templateApplicatorViewModel.onEvent(TemplateApplicatorEvent.ApplyTemplate(templateId))
            },
            onDismiss = {
                templateApplicatorViewModel.onEvent(TemplateApplicatorEvent.DismissTemplateSelector)
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
                            if (hasUnsavedChanges) {
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
                                text = {
                                    Text(
                                        if (accountRowsUiState.showCurrency) "Hide currency" else "Show currency"
                                    )
                                },
                                onClick = {
                                    accountRowsViewModel.onEvent(AccountRowsEvent.ToggleCurrency)
                                    showMenuExpanded = false
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Use template") },
                                onClick = {
                                    templateApplicatorViewModel.onEvent(
                                        TemplateApplicatorEvent.ShowTemplateSelector
                                    )
                                    showMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Reset") },
                                onClick = {
                                    formViewModel.onEvent(TransactionFormEvent.Reset)
                                    accountRowsViewModel.onEvent(AccountRowsEvent.Reset)
                                    showMenuExpanded = false
                                }
                            )
                            if (formUiState.isSimulateSave) {
                                DropdownMenuItem(
                                    text = { Text("Disable simulate save") },
                                    onClick = {
                                        formViewModel.onEvent(TransactionFormEvent.ToggleSimulateSave)
                                        showMenuExpanded = false
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Enable simulate save") },
                                    onClick = {
                                        formViewModel.onEvent(TransactionFormEvent.ToggleSimulateSave)
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
                targetValue = if (isSubmittable && !formUiState.isSubmitting) 1f else 0.5f,
                label = "fabAlpha"
            )

            FloatingActionButton(
                onClick = {
                    if (isSubmittable && !formUiState.isSubmitting) {
                        formViewModel.onEvent(
                            TransactionFormEvent.Submit(accountRowsUiState.accounts)
                        )
                    }
                },
                modifier = Modifier.alpha(fabAlpha)
            ) {
                if (formUiState.isSubmitting) {
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
            if (formUiState.isBusy && !formUiState.isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                NewTransactionContent(
                    formUiState = formUiState,
                    accountRowsUiState = accountRowsUiState,
                    onFormEvent = formViewModel::onEvent,
                    onAccountRowsEvent = accountRowsViewModel::onEvent,
                    descriptionFocusRequester = descriptionFocusRequester
                )
            }
        }
    }
}

@Composable
private fun NewTransactionContent(
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
