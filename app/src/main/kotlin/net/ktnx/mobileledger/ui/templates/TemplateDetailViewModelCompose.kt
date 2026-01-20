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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.logcat
import net.ktnx.mobileledger.data.repository.TemplateRepository
import net.ktnx.mobileledger.domain.usecase.TemplateAccountRowManager
import net.ktnx.mobileledger.domain.usecase.TemplateDataMapper
import net.ktnx.mobileledger.domain.usecase.TemplatePatternValidator

/**
 * ViewModel for the template detail screen using Compose.
 * Manages template editing with regex pattern matching.
 */
@HiltViewModel
class TemplateDetailViewModelCompose @Inject constructor(
    private val templateRepository: TemplateRepository,
    private val patternValidator: TemplatePatternValidator,
    private val rowManager: TemplateAccountRowManager,
    private val dataMapper: TemplateDataMapper
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
                val template = templateRepository.getTemplateAsDomain(templateId)

                if (template != null) {
                    val accountRows = dataMapper.toAccountRows(template) {
                        syntheticId.getAndDecrement()
                    }

                    _uiState.update {
                        TemplateDetailUiState(
                            templateId = template.id,
                            name = template.name,
                            pattern = template.pattern,
                            testText = template.testText ?: "",
                            transactionDescription = dataMapper.extractMatchableValue(
                                template.transactionDescription,
                                template.transactionDescriptionMatchGroup
                            ),
                            transactionComment = dataMapper.extractMatchableValue(
                                template.transactionComment,
                                template.transactionCommentMatchGroup
                            ),
                            dateYear = dataMapper.extractMatchableValueInt(
                                template.dateYear,
                                template.dateYearMatchGroup
                            ),
                            dateMonth = dataMapper.extractMatchableValueInt(
                                template.dateMonth,
                                template.dateMonthMatchGroup
                            ),
                            dateDay = dataMapper.extractMatchableValueInt(
                                template.dateDay,
                                template.dateDayMatchGroup
                            ),
                            accounts = accountRows,
                            isFallback = template.isFallback,
                            isLoading = false
                        )
                    }

                    // Validate the pattern
                    validatePattern(template.pattern, template.testText ?: "")
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                logcat { "Error loading template: ${e.message}" }
                _uiState.update { it.copy(isLoading = false) }
                _effects.send(TemplateDetailEffect.ShowError("テンプレートの読み込みに失敗しました"))
            }
        }
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
            val updated = rowManager.updateRow(state.accounts, index) { it.copy(accountName = value) }
            state.copy(accounts = updated, hasUnsavedChanges = true)
        }
        ensureEmptyRow()
    }

    private fun updateAccountComment(index: Int, value: MatchableValue) {
        _uiState.update { state ->
            val updated = rowManager.updateRow(state.accounts, index) { it.copy(accountComment = value) }
            state.copy(accounts = updated, hasUnsavedChanges = true)
        }
    }

    private fun updateAccountAmount(index: Int, value: MatchableValue) {
        _uiState.update { state ->
            val updated = rowManager.updateRow(state.accounts, index) { it.copy(amount = value) }
            state.copy(accounts = updated, hasUnsavedChanges = true)
        }
        ensureEmptyRow()
    }

    private fun updateAccountCurrency(index: Int, value: MatchableValue) {
        _uiState.update { state ->
            val updated = rowManager.updateRow(state.accounts, index) { it.copy(currency = value) }
            state.copy(accounts = updated, hasUnsavedChanges = true)
        }
    }

    private fun updateAccountNegateAmount(index: Int, negate: Boolean) {
        _uiState.update { state ->
            val updated = rowManager.updateRow(state.accounts, index) { it.copy(negateAmount = negate) }
            state.copy(accounts = updated, hasUnsavedChanges = true)
        }
    }

    private fun removeAccountRow(index: Int) {
        _uiState.update { state ->
            val updated = rowManager.removeRow(state.accounts, index)
            state.copy(accounts = updated, hasUnsavedChanges = true)
        }
        ensureEmptyRow()
    }

    private fun moveAccountRow(fromIndex: Int, toIndex: Int) {
        _uiState.update { state ->
            val updated = rowManager.moveRow(state.accounts, fromIndex, toIndex)
            state.copy(accounts = updated, hasUnsavedChanges = true)
        }
    }

    private fun addAccountRow() {
        _uiState.update { state ->
            val updated = rowManager.addRow(state.accounts, syntheticId.getAndDecrement())
            state.copy(accounts = updated, hasUnsavedChanges = true)
        }
    }

    private fun ensureEmptyRow() {
        _uiState.update { state ->
            val updated = rowManager.ensureValidRowState(state.accounts) { syntheticId.getAndDecrement() }
            state.copy(accounts = updated)
        }
    }

    private fun validatePattern(pattern: String, testText: String) {
        val result = patternValidator.validate(pattern, testText)
        _uiState.update {
            it.copy(
                patternError = result.error,
                testMatchResult = result.matchResult,
                patternGroupCount = result.groupCount
            )
        }
    }

    private fun saveTemplate() {
        val state = _uiState.value
        if (!state.isFormValid) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            try {
                val template = dataMapper.toTemplate(state)
                templateRepository.saveTemplate(template)

                _uiState.update { it.copy(isSaving = false, hasUnsavedChanges = false) }
                _effects.send(TemplateDetailEffect.TemplateSaved)
                _effects.send(TemplateDetailEffect.NavigateBack)
            } catch (e: Exception) {
                logcat { "Error saving template: ${e.message}" }
                _uiState.update { it.copy(isSaving = false) }
                _effects.send(TemplateDetailEffect.ShowError("テンプレートの保存に失敗しました"))
            }
        }
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
                    templateRepository.deleteTemplateById(templateId)
                }

                _uiState.update { it.copy(isLoading = false) }
                _effects.send(TemplateDetailEffect.TemplateDeleted)
                _effects.send(TemplateDetailEffect.NavigateBack)
            } catch (e: Exception) {
                logcat { "Error deleting template: ${e.message}" }
                _uiState.update { it.copy(isLoading = false) }
                _effects.send(TemplateDetailEffect.ShowError("テンプレートの削除に失敗しました"))
            }
        }
    }
}
