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

package net.ktnx.mobileledger.ui.templates

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import net.ktnx.mobileledger.R
import net.ktnx.mobileledger.ui.components.ConfirmDialog
import net.ktnx.mobileledger.ui.components.LoadingIndicator
import net.ktnx.mobileledger.ui.components.UnsavedChangesDialog
import net.ktnx.mobileledger.ui.theme.MoLeTheme

/**
 * Template detail screen for editing templates with regex pattern matching.
 */
@Composable
fun TemplateDetailScreen(
    viewModel: TemplateDetailViewModelCompose = hiltViewModel(),
    templateId: Long? = null,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(templateId) {
        viewModel.initialize(templateId)
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                TemplateDetailEffect.NavigateBack -> onNavigateBack()

                TemplateDetailEffect.TemplateSaved -> {
                    snackbarHostState.showSnackbar(
                        message = "テンプレートを保存しました",
                        duration = SnackbarDuration.Short
                    )
                }

                TemplateDetailEffect.TemplateDeleted -> {
                    snackbarHostState.showSnackbar(
                        message = "テンプレートを削除しました",
                        duration = SnackbarDuration.Short
                    )
                }

                is TemplateDetailEffect.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = effect.message,
                        duration = SnackbarDuration.Long
                    )
                }
            }
        }
    }

    BackHandler(enabled = uiState.hasUnsavedChanges) {
        viewModel.onEvent(TemplateDetailEvent.NavigateBack)
    }

    TemplateDetailContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateDetailContent(
    uiState: TemplateDetailUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (TemplateDetailEvent) -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets.ime,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isNewTemplate) {
                            stringResource(R.string.title_new_template)
                        } else {
                            uiState.name.ifEmpty { stringResource(R.string.title_new_template) }
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onEvent(TemplateDetailEvent.NavigateBack) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る"
                        )
                    }
                },
                actions = {
                    if (uiState.canDelete) {
                        IconButton(onClick = { onEvent(TemplateDetailEvent.ShowDeleteConfirmDialog) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "削除"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onEvent(TemplateDetailEvent.Save) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "保存",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                LoadingIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Template name
                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = { onEvent(TemplateDetailEvent.UpdateName(it)) },
                        label = { Text(stringResource(R.string.template_name_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    // Pattern section
                    PatternSection(
                        pattern = uiState.pattern,
                        testText = uiState.testText,
                        patternError = uiState.patternError,
                        testMatchResult = uiState.testMatchResult,
                        patternGroupCount = uiState.patternGroupCount,
                        onPatternChanged = { onEvent(TemplateDetailEvent.UpdatePattern(it)) },
                        onTestTextChanged = { onEvent(TemplateDetailEvent.UpdateTestText(it)) }
                    )

                    HorizontalDivider()

                    // Transaction extraction section
                    TransactionExtractionSection(
                        transactionDescription = uiState.transactionDescription,
                        transactionComment = uiState.transactionComment,
                        dateYear = uiState.dateYear,
                        dateMonth = uiState.dateMonth,
                        dateDay = uiState.dateDay,
                        patternGroupCount = uiState.patternGroupCount,
                        onDescriptionChanged = { onEvent(TemplateDetailEvent.UpdateTransactionDescription(it)) },
                        onCommentChanged = { onEvent(TemplateDetailEvent.UpdateTransactionComment(it)) },
                        onYearChanged = { onEvent(TemplateDetailEvent.UpdateDateYear(it)) },
                        onMonthChanged = { onEvent(TemplateDetailEvent.UpdateDateMonth(it)) },
                        onDayChanged = { onEvent(TemplateDetailEvent.UpdateDateDay(it)) }
                    )

                    HorizontalDivider()

                    // Account rows section
                    AccountRowsSection(
                        accounts = uiState.accounts,
                        patternGroupCount = uiState.patternGroupCount,
                        onAccountNameChanged = { index, value ->
                            onEvent(TemplateDetailEvent.UpdateAccountName(index, value))
                        },
                        onAccountCommentChanged = { index, value ->
                            onEvent(TemplateDetailEvent.UpdateAccountComment(index, value))
                        },
                        onAccountAmountChanged = { index, value ->
                            onEvent(TemplateDetailEvent.UpdateAccountAmount(index, value))
                        },
                        onAccountCurrencyChanged = { index, value ->
                            onEvent(TemplateDetailEvent.UpdateAccountCurrency(index, value))
                        },
                        onAccountNegateChanged = { index, negate ->
                            onEvent(TemplateDetailEvent.UpdateAccountNegateAmount(index, negate))
                        },
                        onRemoveRow = { index ->
                            onEvent(TemplateDetailEvent.RemoveAccountRow(index))
                        },
                        onAddRow = { onEvent(TemplateDetailEvent.AddAccountRow) }
                    )

                    HorizontalDivider()

                    // Fallback option
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.is_fallback_label),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = uiState.isFallback,
                            onCheckedChange = { onEvent(TemplateDetailEvent.UpdateIsFallback(it)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(72.dp)) // Space for FAB
                }
            }
        }
    }

    // Dialogs
    if (uiState.showUnsavedChangesDialog) {
        UnsavedChangesDialog(
            onSave = { onEvent(TemplateDetailEvent.Save) },
            onDiscard = { onEvent(TemplateDetailEvent.ConfirmDiscardChanges) },
            onDismiss = { onEvent(TemplateDetailEvent.DismissUnsavedChangesDialog) }
        )
    }

    if (uiState.showDeleteConfirmDialog) {
        ConfirmDialog(
            title = uiState.name.ifEmpty { "テンプレート" },
            message = stringResource(R.string.remove_template_dialog_message),
            confirmText = stringResource(R.string.Remove),
            dismissText = stringResource(android.R.string.cancel),
            onConfirm = { onEvent(TemplateDetailEvent.ConfirmDelete) },
            onDismiss = { onEvent(TemplateDetailEvent.DismissDeleteConfirmDialog) },
            isDestructive = true
        )
    }
}

@Composable
private fun PatternSection(
    pattern: String,
    testText: String,
    patternError: String?,
    testMatchResult: androidx.compose.ui.text.AnnotatedString?,
    patternGroupCount: Int,
    onPatternChanged: (String) -> Unit,
    onTestTextChanged: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.pattern_label),
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = pattern,
            onValueChange = onPatternChanged,
            label = { Text(stringResource(R.string.pattern_hint)) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            isError = patternError != null,
            supportingText = patternError?.let {
                { Text(it, color = MaterialTheme.colorScheme.error) }
            },
            minLines = 2,
            maxLines = 4
        )

        OutlinedTextField(
            value = testText,
            onValueChange = onTestTextChanged,
            label = { Text(stringResource(R.string.test_text_label)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )

        if (testMatchResult != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = testMatchResult,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (patternGroupCount > 0) {
            Text(
                text = "グループ数: $patternGroupCount",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TransactionExtractionSection(
    transactionDescription: MatchableValue,
    transactionComment: MatchableValue,
    dateYear: MatchableValue,
    dateMonth: MatchableValue,
    dateDay: MatchableValue,
    patternGroupCount: Int,
    onDescriptionChanged: (MatchableValue) -> Unit,
    onCommentChanged: (MatchableValue) -> Unit,
    onYearChanged: (MatchableValue) -> Unit,
    onMonthChanged: (MatchableValue) -> Unit,
    onDayChanged: (MatchableValue) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.transaction_extraction_label),
            style = MaterialTheme.typography.titleMedium
        )

        MatchableValueField(
            label = stringResource(R.string.transaction_description_label),
            value = transactionDescription,
            patternGroupCount = patternGroupCount,
            onValueChanged = onDescriptionChanged
        )

        MatchableValueField(
            label = stringResource(R.string.transaction_comment_label),
            value = transactionComment,
            patternGroupCount = patternGroupCount,
            onValueChanged = onCommentChanged
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MatchableValueField(
                label = stringResource(R.string.date_year_label),
                value = dateYear,
                patternGroupCount = patternGroupCount,
                onValueChanged = onYearChanged,
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Number
            )

            MatchableValueField(
                label = stringResource(R.string.date_month_label),
                value = dateMonth,
                patternGroupCount = patternGroupCount,
                onValueChanged = onMonthChanged,
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Number
            )

            MatchableValueField(
                label = stringResource(R.string.date_day_label),
                value = dateDay,
                patternGroupCount = patternGroupCount,
                onValueChanged = onDayChanged,
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Number
            )
        }
    }
}

@Composable
private fun MatchableValueField(
    label: String,
    value: MatchableValue,
    patternGroupCount: Int,
    onValueChanged: (MatchableValue) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))

            // Toggle between literal and match group
            TextButton(
                onClick = {
                    if (value.isLiteral()) {
                        onValueChanged(MatchableValue.MatchGroup(1))
                    } else {
                        onValueChanged(MatchableValue.Literal(""))
                    }
                }
            ) {
                Text(
                    text = if (value.isLiteral()) "グループ" else "リテラル",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        when (value) {
            is MatchableValue.Literal -> {
                OutlinedTextField(
                    value = value.value,
                    onValueChange = { onValueChanged(MatchableValue.Literal(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = keyboardType,
                        imeAction = ImeAction.Next
                    )
                )
            }

            is MatchableValue.MatchGroup -> {
                OutlinedTextField(
                    value = value.group.toString(),
                    onValueChange = { text ->
                        val group = text.toIntOrNull() ?: 1
                        onValueChanged(MatchableValue.MatchGroup(group.coerceIn(1, patternGroupCount.coerceAtLeast(1))))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    label = { Text("グループ番号") },
                    isError = value.group > patternGroupCount && patternGroupCount > 0,
                    supportingText = if (value.group > patternGroupCount && patternGroupCount > 0) {
                        { Text("グループ番号が範囲外です (1-$patternGroupCount)") }
                    } else {
                        null
                    }
                )
            }
        }
    }
}

@Composable
private fun AccountRowsSection(
    accounts: List<TemplateAccountRow>,
    patternGroupCount: Int,
    onAccountNameChanged: (Int, MatchableValue) -> Unit,
    onAccountCommentChanged: (Int, MatchableValue) -> Unit,
    onAccountAmountChanged: (Int, MatchableValue) -> Unit,
    onAccountCurrencyChanged: (Int, MatchableValue) -> Unit,
    onAccountNegateChanged: (Int, Boolean) -> Unit,
    onRemoveRow: (Int) -> Unit,
    onAddRow: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.accounts_label),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onAddRow) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "行を追加"
                )
            }
        }

        accounts.forEachIndexed { index, account ->
            AccountRowCard(
                index = index,
                account = account,
                patternGroupCount = patternGroupCount,
                canRemove = accounts.size > 2,
                onAccountNameChanged = { onAccountNameChanged(index, it) },
                onAccountCommentChanged = { onAccountCommentChanged(index, it) },
                onAccountAmountChanged = { onAccountAmountChanged(index, it) },
                onAccountCurrencyChanged = { onAccountCurrencyChanged(index, it) },
                onAccountNegateChanged = { onAccountNegateChanged(index, it) },
                onRemove = { onRemoveRow(index) }
            )
        }
    }
}

@Composable
private fun AccountRowCard(
    index: Int,
    account: TemplateAccountRow,
    patternGroupCount: Int,
    canRemove: Boolean,
    onAccountNameChanged: (MatchableValue) -> Unit,
    onAccountCommentChanged: (MatchableValue) -> Unit,
    onAccountAmountChanged: (MatchableValue) -> Unit,
    onAccountCurrencyChanged: (MatchableValue) -> Unit,
    onAccountNegateChanged: (Boolean) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "行 ${index + 1}",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.weight(1f))
                if (canRemove) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "削除",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            MatchableValueField(
                label = stringResource(R.string.account_name_label),
                value = account.accountName,
                patternGroupCount = patternGroupCount,
                onValueChanged = onAccountNameChanged
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MatchableValueField(
                    label = stringResource(R.string.amount_label),
                    value = account.amount,
                    patternGroupCount = patternGroupCount,
                    onValueChanged = onAccountAmountChanged,
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Decimal
                )

                MatchableValueField(
                    label = stringResource(R.string.currency_label),
                    value = account.currency,
                    patternGroupCount = patternGroupCount,
                    onValueChanged = onAccountCurrencyChanged,
                    modifier = Modifier.weight(1f)
                )
            }

            AnimatedVisibility(
                visible = account.amount.isMatchGroup(),
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.negate_amount_label),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Checkbox(
                        checked = account.negateAmount,
                        onCheckedChange = onAccountNegateChanged
                    )
                }
            }

            MatchableValueField(
                label = stringResource(R.string.comment_label),
                value = account.accountComment,
                patternGroupCount = patternGroupCount,
                onValueChanged = onAccountCommentChanged
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TemplateDetailScreenPreview() {
    MoLeTheme {
        TemplateDetailContent(
            uiState = TemplateDetailUiState(
                name = "銀行振込",
                pattern = "(\\d+)円.*振込",
                testText = "1000円が振込されました",
                patternGroupCount = 1
            ),
            snackbarHostState = SnackbarHostState(),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TemplateDetailScreenNewPreview() {
    MoLeTheme {
        TemplateDetailContent(
            uiState = TemplateDetailUiState(),
            snackbarHostState = SnackbarHostState(),
            onEvent = {}
        )
    }
}
