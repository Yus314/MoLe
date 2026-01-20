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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
                    // Reset form and account rows immediately
                    formViewModel.onEvent(TransactionFormEvent.Reset)
                    accountRowsViewModel.onEvent(AccountRowsEvent.Reset)
                    // Show snackbar (suspends until dismissed, but reset already done)
                    snackbarHostState.showSnackbar(
                        message = "Transaction saved",
                        duration = SnackbarDuration.Short
                    )
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

    // Dialogs
    NewTransactionDialogs(
        showDiscardDialog = showDiscardDialog,
        showDatePicker = formUiState.showDatePicker,
        showCurrencySelector = accountRowsUiState.showCurrencySelector,
        showTemplateSelector = templateUiState.showTemplateSelector,
        formUiState = formUiState,
        accountRowsUiState = accountRowsUiState,
        templateUiState = templateUiState,
        onDiscardDialogDismiss = { showDiscardDialog = false },
        onDiscardConfirm = {
            showDiscardDialog = false
            onNavigateBack()
        },
        onFormEvent = formViewModel::onEvent,
        onAccountRowsEvent = accountRowsViewModel::onEvent,
        onTemplateEvent = templateApplicatorViewModel::onEvent
    )

    Scaffold(
        topBar = {
            NewTransactionTopBar(
                hasUnsavedChanges = hasUnsavedChanges,
                showCurrency = accountRowsUiState.showCurrency,
                isSimulateSave = formUiState.isSimulateSave,
                showMenuExpanded = showMenuExpanded,
                onBackClick = {
                    if (hasUnsavedChanges) {
                        showDiscardDialog = true
                    } else {
                        onNavigateBack()
                    }
                },
                onMenuExpandedChange = { showMenuExpanded = it },
                onToggleCurrency = { accountRowsViewModel.onEvent(AccountRowsEvent.ToggleCurrency) },
                onShowTemplateSelector = {
                    templateApplicatorViewModel.onEvent(TemplateApplicatorEvent.ShowTemplateSelector)
                },
                onReset = {
                    formViewModel.onEvent(TransactionFormEvent.Reset)
                    accountRowsViewModel.onEvent(AccountRowsEvent.Reset)
                },
                onToggleSimulateSave = {
                    formViewModel.onEvent(TransactionFormEvent.ToggleSimulateSave)
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewTransactionTopBar(
    hasUnsavedChanges: Boolean,
    showCurrency: Boolean,
    isSimulateSave: Boolean,
    showMenuExpanded: Boolean,
    onBackClick: () -> Unit,
    onMenuExpandedChange: (Boolean) -> Unit,
    onToggleCurrency: () -> Unit,
    onShowTemplateSelector: () -> Unit,
    onReset: () -> Unit,
    onToggleSimulateSave: () -> Unit
) {
    TopAppBar(
        title = { Text("New Transaction") },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            // Overflow menu
            Box {
                IconButton(onClick = { onMenuExpandedChange(true) }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options"
                    )
                }
                DropdownMenu(
                    expanded = showMenuExpanded,
                    onDismissRequest = { onMenuExpandedChange(false) }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(if (showCurrency) "Hide currency" else "Show currency")
                        },
                        onClick = {
                            onToggleCurrency()
                            onMenuExpandedChange(false)
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Use template") },
                        onClick = {
                            onShowTemplateSelector()
                            onMenuExpandedChange(false)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Reset") },
                        onClick = {
                            onReset()
                            onMenuExpandedChange(false)
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(if (isSimulateSave) "Disable simulate save" else "Enable simulate save")
                        },
                        onClick = {
                            onToggleSimulateSave()
                            onMenuExpandedChange(false)
                        }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors()
    )
}

@Composable
private fun NewTransactionDialogs(
    showDiscardDialog: Boolean,
    showDatePicker: Boolean,
    showCurrencySelector: Boolean,
    showTemplateSelector: Boolean,
    formUiState: TransactionFormUiState,
    accountRowsUiState: AccountRowsUiState,
    templateUiState: TemplateApplicatorUiState,
    onDiscardDialogDismiss: () -> Unit,
    onDiscardConfirm: () -> Unit,
    onFormEvent: (TransactionFormEvent) -> Unit,
    onAccountRowsEvent: (AccountRowsEvent) -> Unit,
    onTemplateEvent: (TemplateApplicatorEvent) -> Unit
) {
    // Discard changes confirmation dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = onDiscardDialogDismiss,
            title = { Text("Discard changes?") },
            text = { Text("You have unsaved changes. Are you sure you want to discard them?") },
            confirmButton = {
                TextButton(onClick = onDiscardConfirm) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = onDiscardDialogDismiss) {
                    Text("Keep editing")
                }
            }
        )
    }

    // Date picker dialog
    if (showDatePicker) {
        MoleDatePickerDialog(
            initialDate = formUiState.date,
            futureDates = formUiState.futureDates,
            onDateSelected = { date ->
                onFormEvent(TransactionFormEvent.UpdateDate(date))
            },
            onDismiss = {
                onFormEvent(TransactionFormEvent.DismissDatePicker)
            }
        )
    }

    // Currency picker dialog
    if (showCurrencySelector) {
        CurrencyPickerDialog(
            currencies = accountRowsUiState.availableCurrencies,
            showPositionSettings = true,
            onCurrencySelected = { currency ->
                accountRowsUiState.currencySelectorRowId?.let { rowId ->
                    onAccountRowsEvent(AccountRowsEvent.UpdateCurrency(rowId, currency))
                }
                onAccountRowsEvent(AccountRowsEvent.DismissCurrencySelector)
            },
            onCurrencyAdded = { name, position, gap ->
                onAccountRowsEvent(AccountRowsEvent.AddCurrency(name, position, gap))
            },
            onCurrencyDeleted = { name ->
                onAccountRowsEvent(AccountRowsEvent.DeleteCurrency(name))
            },
            onNoCurrencySelected = {
                accountRowsUiState.currencySelectorRowId?.let { rowId ->
                    onAccountRowsEvent(AccountRowsEvent.UpdateCurrency(rowId, ""))
                }
                onAccountRowsEvent(AccountRowsEvent.DismissCurrencySelector)
            },
            onPositionChanged = { },
            onGapChanged = { },
            onDismiss = {
                onAccountRowsEvent(AccountRowsEvent.DismissCurrencySelector)
            }
        )
    }

    // Template selector dialog
    if (showTemplateSelector) {
        TemplateSelectorDialog(
            templates = templateUiState.availableTemplates,
            onTemplateSelected = { templateId ->
                onTemplateEvent(TemplateApplicatorEvent.ApplyTemplate(templateId))
            },
            onDismiss = {
                onTemplateEvent(TemplateApplicatorEvent.DismissTemplateSelector)
            }
        )
    }
}
