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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.atomic.AtomicLong
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.ktnx.mobileledger.data.repository.TemplateRepository
import net.ktnx.mobileledger.db.TemplateAccount
import net.ktnx.mobileledger.db.TemplateHeader
import net.ktnx.mobileledger.utils.Logger
import net.ktnx.mobileledger.utils.Misc

/**
 * ViewModel for the template detail screen using Compose.
 * Manages template editing with regex pattern matching.
 */
@HiltViewModel
class TemplateDetailViewModelCompose @Inject constructor(
    private val templateRepository: TemplateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TemplateDetailUiState())
    val uiState: StateFlow<TemplateDetailUiState> = _uiState.asStateFlow()

    private val _effects = Channel<TemplateDetailEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var isInitialized = false
    private val syntheticId = AtomicLong(-1)

    fun initialize(templateId: Long?) {
        if (isInitialized) return
        isInitialized = true

        if (templateId == null || templateId <= 0) {
            // New template - already has default state
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val templateWithAccounts = templateRepository.getTemplateWithAccountsSync(templateId)

                if (templateWithAccounts != null) {
                    val header = templateWithAccounts.header
                    val accounts = templateWithAccounts.accounts.sortedBy { it.position }

                    val accountRows = accounts.map { acc ->
                        TemplateAccountRow(
                            id = acc.id,
                            position = acc.position.toInt(),
                            accountName = extractMatchableValue(
                                acc.accountName,
                                acc.accountNameMatchGroup
                            ),
                            accountComment = extractMatchableValue(
                                acc.accountComment,
                                acc.accountCommentMatchGroup
                            ),
                            amount = extractMatchableValueFloat(
                                acc.amount,
                                acc.amountMatchGroup
                            ),
                            currency = extractMatchableValueCurrency(
                                acc.currency,
                                acc.currencyMatchGroup
                            ),
                            negateAmount = acc.negateAmount == true
                        )
                    }.ifEmpty {
                        listOf(
                            TemplateAccountRow(id = syntheticId.getAndDecrement()),
                            TemplateAccountRow(id = syntheticId.getAndDecrement())
                        )
                    }

                    _uiState.update {
                        TemplateDetailUiState(
                            templateId = header.id,
                            name = header.name,
                            pattern = header.regularExpression,
                            testText = header.testText ?: "",
                            transactionDescription = extractMatchableValue(
                                header.transactionDescription,
                                header.transactionDescriptionMatchGroup
                            ),
                            transactionComment = extractMatchableValue(
                                header.transactionComment,
                                header.transactionCommentMatchGroup
                            ),
                            dateYear = extractMatchableValueInt(
                                header.dateYear,
                                header.dateYearMatchGroup
                            ),
                            dateMonth = extractMatchableValueInt(
                                header.dateMonth,
                                header.dateMonthMatchGroup
                            ),
                            dateDay = extractMatchableValueInt(
                                header.dateDay,
                                header.dateDayMatchGroup
                            ),
                            accounts = accountRows,
                            isFallback = header.isFallback,
                            isLoading = false
                        )
                    }

                    // Validate the pattern
                    validatePattern(header.regularExpression, header.testText ?: "")
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                Logger.debug(TAG, "Error loading template: ${e.message}")
                _uiState.update { it.copy(isLoading = false) }
                _effects.send(TemplateDetailEffect.ShowError("テンプレートの読み込みに失敗しました"))
            }
        }
    }

    private fun extractMatchableValue(literal: String?, matchGroup: Int?): MatchableValue =
        if (matchGroup != null && matchGroup > 0) {
            MatchableValue.MatchGroup(matchGroup)
        } else {
            MatchableValue.Literal(literal ?: "")
        }

    private fun extractMatchableValueInt(literal: Int?, matchGroup: Int?): MatchableValue =
        if (matchGroup != null && matchGroup > 0) {
            MatchableValue.MatchGroup(matchGroup)
        } else {
            MatchableValue.Literal(literal?.toString() ?: "")
        }

    private fun extractMatchableValueFloat(literal: Float?, matchGroup: Int?): MatchableValue =
        if (matchGroup != null && matchGroup > 0) {
            MatchableValue.MatchGroup(matchGroup)
        } else {
            MatchableValue.Literal(literal?.toString() ?: "")
        }

    private fun extractMatchableValueCurrency(literal: Long?, matchGroup: Int?): MatchableValue =
        if (matchGroup != null && matchGroup > 0) {
            MatchableValue.MatchGroup(matchGroup)
        } else {
            MatchableValue.Literal(literal?.toString() ?: "")
        }

    fun onEvent(event: TemplateDetailEvent) {
        when (event) {
            is TemplateDetailEvent.UpdateName -> updateName(event.name)

            is TemplateDetailEvent.UpdatePattern -> updatePattern(event.pattern)

            is TemplateDetailEvent.UpdateTestText -> updateTestText(event.text)

            is TemplateDetailEvent.UpdateIsFallback -> updateIsFallback(event.isFallback)

            is TemplateDetailEvent.UpdateTransactionDescription ->
                updateTransactionDescription(event.value)

            is TemplateDetailEvent.UpdateTransactionComment ->
                updateTransactionComment(event.value)

            is TemplateDetailEvent.UpdateDateYear -> updateDateYear(event.value)

            is TemplateDetailEvent.UpdateDateMonth -> updateDateMonth(event.value)

            is TemplateDetailEvent.UpdateDateDay -> updateDateDay(event.value)

            is TemplateDetailEvent.UpdateAccountName ->
                updateAccountName(event.index, event.value)

            is TemplateDetailEvent.UpdateAccountComment ->
                updateAccountComment(event.index, event.value)

            is TemplateDetailEvent.UpdateAccountAmount ->
                updateAccountAmount(event.index, event.value)

            is TemplateDetailEvent.UpdateAccountCurrency ->
                updateAccountCurrency(event.index, event.value)

            is TemplateDetailEvent.UpdateAccountNegateAmount ->
                updateAccountNegateAmount(event.index, event.negate)

            is TemplateDetailEvent.RemoveAccountRow -> removeAccountRow(event.index)

            is TemplateDetailEvent.MoveAccountRow -> moveAccountRow(event.fromIndex, event.toIndex)

            TemplateDetailEvent.AddAccountRow -> addAccountRow()

            TemplateDetailEvent.Save -> saveTemplate()

            TemplateDetailEvent.Delete -> showDeleteConfirmDialog()

            TemplateDetailEvent.NavigateBack -> handleNavigateBack()

            TemplateDetailEvent.ConfirmDiscardChanges -> confirmDiscardChanges()

            TemplateDetailEvent.DismissUnsavedChangesDialog -> dismissUnsavedChangesDialog()

            TemplateDetailEvent.ShowDeleteConfirmDialog -> showDeleteConfirmDialog()

            TemplateDetailEvent.DismissDeleteConfirmDialog -> dismissDeleteConfirmDialog()

            TemplateDetailEvent.ConfirmDelete -> confirmDelete()
        }
    }

    private fun updateName(name: String) {
        _uiState.update { it.copy(name = name, hasUnsavedChanges = true) }
    }

    private fun updatePattern(pattern: String) {
        _uiState.update { it.copy(pattern = pattern, hasUnsavedChanges = true) }
        validatePattern(pattern, _uiState.value.testText)
    }

    private fun updateTestText(text: String) {
        _uiState.update { it.copy(testText = text, hasUnsavedChanges = true) }
        validatePattern(_uiState.value.pattern, text)
    }

    private fun updateIsFallback(isFallback: Boolean) {
        _uiState.update { it.copy(isFallback = isFallback, hasUnsavedChanges = true) }
    }

    private fun updateTransactionDescription(value: MatchableValue) {
        _uiState.update { it.copy(transactionDescription = value, hasUnsavedChanges = true) }
    }

    private fun updateTransactionComment(value: MatchableValue) {
        _uiState.update { it.copy(transactionComment = value, hasUnsavedChanges = true) }
    }

    private fun updateDateYear(value: MatchableValue) {
        _uiState.update { it.copy(dateYear = value, hasUnsavedChanges = true) }
    }

    private fun updateDateMonth(value: MatchableValue) {
        _uiState.update { it.copy(dateMonth = value, hasUnsavedChanges = true) }
    }

    private fun updateDateDay(value: MatchableValue) {
        _uiState.update { it.copy(dateDay = value, hasUnsavedChanges = true) }
    }

    private fun updateAccountName(index: Int, value: MatchableValue) {
        _uiState.update { state ->
            val accounts = state.accounts.toMutableList()
            if (index in accounts.indices) {
                accounts[index] = accounts[index].copy(accountName = value)
            }
            state.copy(accounts = accounts, hasUnsavedChanges = true)
        }
        ensureEmptyRow()
    }

    private fun updateAccountComment(index: Int, value: MatchableValue) {
        _uiState.update { state ->
            val accounts = state.accounts.toMutableList()
            if (index in accounts.indices) {
                accounts[index] = accounts[index].copy(accountComment = value)
            }
            state.copy(accounts = accounts, hasUnsavedChanges = true)
        }
    }

    private fun updateAccountAmount(index: Int, value: MatchableValue) {
        _uiState.update { state ->
            val accounts = state.accounts.toMutableList()
            if (index in accounts.indices) {
                accounts[index] = accounts[index].copy(amount = value)
            }
            state.copy(accounts = accounts, hasUnsavedChanges = true)
        }
        ensureEmptyRow()
    }

    private fun updateAccountCurrency(index: Int, value: MatchableValue) {
        _uiState.update { state ->
            val accounts = state.accounts.toMutableList()
            if (index in accounts.indices) {
                accounts[index] = accounts[index].copy(currency = value)
            }
            state.copy(accounts = accounts, hasUnsavedChanges = true)
        }
    }

    private fun updateAccountNegateAmount(index: Int, negate: Boolean) {
        _uiState.update { state ->
            val accounts = state.accounts.toMutableList()
            if (index in accounts.indices) {
                accounts[index] = accounts[index].copy(negateAmount = negate)
            }
            state.copy(accounts = accounts, hasUnsavedChanges = true)
        }
    }

    private fun removeAccountRow(index: Int) {
        _uiState.update { state ->
            val accounts = state.accounts.toMutableList()
            if (index in accounts.indices && accounts.size > 2) {
                accounts.removeAt(index)
                // Update positions
                accounts.forEachIndexed { i, row ->
                    accounts[i] = row.copy(position = i)
                }
            }
            state.copy(accounts = accounts, hasUnsavedChanges = true)
        }
        ensureEmptyRow()
    }

    private fun moveAccountRow(fromIndex: Int, toIndex: Int) {
        _uiState.update { state ->
            val accounts = state.accounts.toMutableList()
            if (fromIndex in accounts.indices && toIndex in accounts.indices) {
                val item = accounts.removeAt(fromIndex)
                accounts.add(toIndex, item)
                // Update positions
                accounts.forEachIndexed { i, row ->
                    accounts[i] = row.copy(position = i)
                }
            }
            state.copy(accounts = accounts, hasUnsavedChanges = true)
        }
    }

    private fun addAccountRow() {
        _uiState.update { state ->
            val accounts = state.accounts.toMutableList()
            val newRow = TemplateAccountRow(
                id = syntheticId.getAndDecrement(),
                position = accounts.size
            )
            accounts.add(newRow)
            state.copy(accounts = accounts, hasUnsavedChanges = true)
        }
    }

    private fun ensureEmptyRow() {
        _uiState.update { state ->
            val accounts = state.accounts.toMutableList()

            // Ensure we have at least 2 rows
            while (accounts.size < 2) {
                accounts.add(
                    TemplateAccountRow(
                        id = syntheticId.getAndDecrement(),
                        position = accounts.size
                    )
                )
            }

            // Ensure there's at least one empty row at the end
            val hasEmptyRow = accounts.any { it.isEmpty() }
            if (!hasEmptyRow) {
                accounts.add(
                    TemplateAccountRow(
                        id = syntheticId.getAndDecrement(),
                        position = accounts.size
                    )
                )
            }

            // Remove extra empty rows (keep max 1 empty row, unless at positions 0-1)
            val emptyIndices = accounts.mapIndexedNotNull { index, row ->
                if (row.isEmpty() && index >= 2) index else null
            }
            if (emptyIndices.size > 1) {
                // Keep only the last empty row
                emptyIndices.dropLast(1).reversed().forEach { index ->
                    accounts.removeAt(index)
                }
            }

            // Update positions
            accounts.forEachIndexed { i, row ->
                if (row.position != i) {
                    accounts[i] = row.copy(position = i)
                }
            }

            state.copy(accounts = accounts)
        }
    }

    private fun validatePattern(pattern: String, testText: String) {
        if (pattern.isEmpty()) {
            _uiState.update {
                it.copy(
                    patternError = "パターンが空です",
                    testMatchResult = null,
                    patternGroupCount = 0
                )
            }
            return
        }

        try {
            val compiledPattern = Pattern.compile(pattern)
            val groupCount = compiledPattern.matcher("").groupCount()

            val matchResult = if (testText.isNotEmpty()) {
                val matcher = compiledPattern.matcher(testText)
                if (matcher.find()) {
                    buildAnnotatedString {
                        // Before match
                        if (matcher.start() > 0) {
                            pushStyle(SpanStyle(color = Color.Gray))
                            append(testText.substring(0, matcher.start()))
                            pop()
                        }

                        // Matched portion
                        pushStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                        append(testText.substring(matcher.start(), matcher.end()))
                        pop()

                        // Highlight captured groups
                        for (g in 1..matcher.groupCount()) {
                            val start = matcher.start(g)
                            val end = matcher.end(g)
                            if (start >= 0 && end > start) {
                                // Note: In Compose AnnotatedString, we can't easily overlay styles
                                // This is a simplified version
                            }
                        }

                        // After match
                        if (matcher.end() < testText.length) {
                            pushStyle(SpanStyle(color = Color.Gray))
                            append(testText.substring(matcher.end()))
                            pop()
                        }
                    }
                } else {
                    buildAnnotatedString {
                        pushStyle(SpanStyle(color = Color.Gray))
                        append(testText)
                        pop()
                    }
                }
            } else {
                null
            }

            _uiState.update {
                it.copy(
                    patternError = null,
                    testMatchResult = matchResult,
                    patternGroupCount = groupCount
                )
            }
        } catch (e: PatternSyntaxException) {
            _uiState.update {
                it.copy(
                    patternError = e.description,
                    testMatchResult = null,
                    patternGroupCount = 0
                )
            }
        }
    }

    private fun saveTemplate() {
        val state = _uiState.value
        if (!state.isFormValid) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            try {
                val header = buildTemplateHeader(state)

                // Build list of accounts to save
                val accounts = mutableListOf<TemplateAccount>()
                state.accounts.forEachIndexed { index, row ->
                    if (!row.isEmpty() || index < 2) {
                        val account = buildTemplateAccount(row, header.id, index.toLong())
                        accounts.add(account)
                    }
                }

                templateRepository.saveTemplateWithAccounts(header, accounts)

                _uiState.update { it.copy(isSaving = false, hasUnsavedChanges = false) }
                _effects.send(TemplateDetailEffect.TemplateSaved)
                _effects.send(TemplateDetailEffect.NavigateBack)
            } catch (e: Exception) {
                Logger.debug(TAG, "Error saving template: ${e.message}")
                _uiState.update { it.copy(isSaving = false) }
                _effects.send(TemplateDetailEffect.ShowError("テンプレートの保存に失敗しました"))
            }
        }
    }

    private fun buildTemplateHeader(state: TemplateDetailUiState): TemplateHeader {
        val header = TemplateHeader(
            state.templateId ?: 0,
            Misc.trim(state.name) ?: "",
            state.pattern
        )

        header.testText = state.testText.ifEmpty { null }

        when (val desc = state.transactionDescription) {
            is MatchableValue.Literal -> header.transactionDescription = desc.value
            is MatchableValue.MatchGroup -> header.transactionDescriptionMatchGroup = desc.group
        }

        when (val comment = state.transactionComment) {
            is MatchableValue.Literal -> header.transactionComment = comment.value
            is MatchableValue.MatchGroup -> header.transactionCommentMatchGroup = comment.group
        }

        when (val year = state.dateYear) {
            is MatchableValue.Literal -> header.dateYear = year.value.toIntOrNull()
            is MatchableValue.MatchGroup -> header.dateYearMatchGroup = year.group
        }

        when (val month = state.dateMonth) {
            is MatchableValue.Literal -> header.dateMonth = month.value.toIntOrNull()
            is MatchableValue.MatchGroup -> header.dateMonthMatchGroup = month.group
        }

        when (val day = state.dateDay) {
            is MatchableValue.Literal -> header.dateDay = day.value.toIntOrNull()
            is MatchableValue.MatchGroup -> header.dateDayMatchGroup = day.group
        }

        header.isFallback = state.isFallback

        return header
    }

    private fun buildTemplateAccount(row: TemplateAccountRow, templateId: Long, position: Long): TemplateAccount {
        val account = TemplateAccount(
            if (row.id > 0) row.id else 0,
            templateId,
            position
        )

        when (val name = row.accountName) {
            is MatchableValue.Literal -> account.accountName = name.value
            is MatchableValue.MatchGroup -> account.accountNameMatchGroup = name.group
        }

        when (val comment = row.accountComment) {
            is MatchableValue.Literal -> account.accountComment = comment.value
            is MatchableValue.MatchGroup -> account.accountCommentMatchGroup = comment.group
        }

        when (val amount = row.amount) {
            is MatchableValue.Literal -> account.amount = amount.value.toFloatOrNull()

            is MatchableValue.MatchGroup -> {
                account.amountMatchGroup = amount.group
                account.negateAmount = if (row.negateAmount) true else null
            }
        }

        when (val currency = row.currency) {
            is MatchableValue.Literal -> account.currency = currency.value.toLongOrNull()
            is MatchableValue.MatchGroup -> account.currencyMatchGroup = currency.group
        }

        return account
    }

    private fun handleNavigateBack() {
        if (_uiState.value.hasUnsavedChanges) {
            _uiState.update { it.copy(showUnsavedChangesDialog = true) }
        } else {
            viewModelScope.launch {
                _effects.send(TemplateDetailEffect.NavigateBack)
            }
        }
    }

    private fun confirmDiscardChanges() {
        _uiState.update { it.copy(showUnsavedChangesDialog = false) }
        viewModelScope.launch {
            _effects.send(TemplateDetailEffect.NavigateBack)
        }
    }

    private fun dismissUnsavedChangesDialog() {
        _uiState.update { it.copy(showUnsavedChangesDialog = false) }
    }

    private fun showDeleteConfirmDialog() {
        _uiState.update { it.copy(showDeleteConfirmDialog = true) }
    }

    private fun dismissDeleteConfirmDialog() {
        _uiState.update { it.copy(showDeleteConfirmDialog = false) }
    }

    private fun confirmDelete() {
        viewModelScope.launch {
            _uiState.update { it.copy(showDeleteConfirmDialog = false, isLoading = true) }

            try {
                val templateId = _uiState.value.templateId
                if (templateId != null && templateId > 0) {
                    val template = templateRepository.getTemplateByIdSync(templateId)
                    if (template != null) {
                        templateRepository.deleteTemplate(template)
                    }
                }

                _uiState.update { it.copy(isLoading = false) }
                _effects.send(TemplateDetailEffect.TemplateDeleted)
                _effects.send(TemplateDetailEffect.NavigateBack)
            } catch (e: Exception) {
                Logger.debug(TAG, "Error deleting template: ${e.message}")
                _uiState.update { it.copy(isLoading = false) }
                _effects.send(TemplateDetailEffect.ShowError("テンプレートの削除に失敗しました"))
            }
        }
    }

    companion object {
        private const val TAG = "template-detail-vm"
    }
}
