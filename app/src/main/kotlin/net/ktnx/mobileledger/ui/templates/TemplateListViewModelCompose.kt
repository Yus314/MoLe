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
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.asLog
import logcat.logcat
import net.ktnx.mobileledger.core.domain.model.Template
import net.ktnx.mobileledger.domain.usecase.DeleteTemplateUseCase
import net.ktnx.mobileledger.domain.usecase.DuplicateTemplateUseCase
import net.ktnx.mobileledger.domain.usecase.GetTemplateUseCase
import net.ktnx.mobileledger.domain.usecase.ObserveTemplatesUseCase
import net.ktnx.mobileledger.domain.usecase.SaveTemplateUseCase

/**
 * ViewModel for the template list screen using Compose.
 * Uses StateFlow for reactive UI updates.
 */
@HiltViewModel
class TemplateListViewModelCompose @Inject constructor(
    private val observeTemplatesUseCase: ObserveTemplatesUseCase,
    private val getTemplateUseCase: GetTemplateUseCase,
    private val saveTemplateUseCase: SaveTemplateUseCase,
    private val deleteTemplateUseCase: DeleteTemplateUseCase,
    private val duplicateTemplateUseCase: DuplicateTemplateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TemplateListUiState(isLoading = true))
    val uiState: StateFlow<TemplateListUiState> = _uiState.asStateFlow()

    private val _effects = Channel<TemplateListEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    // For undo functionality
    private var deletedTemplate: Template? = null

    init {
        loadTemplates()
    }

    private fun loadTemplates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            observeTemplatesUseCase()
                .catch { e ->
                    logcat { "Error loading templates: ${e.message}" }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "テンプレートの読み込みに失敗しました"
                        )
                    }
                }
                .collect { templates ->
                    _uiState.update { state ->
                        state.copy(
                            templates = templates.map { template ->
                                TemplateListItem(
                                    id = template.id ?: 0L,
                                    name = template.name,
                                    pattern = template.pattern,
                                    isFallback = template.isFallback
                                )
                            },
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun onEvent(event: TemplateListEvent) {
        when (event) {
            TemplateListEvent.CreateNewTemplate -> navigateToDetail(null)
            is TemplateListEvent.EditTemplate -> navigateToDetail(event.templateId)
            is TemplateListEvent.DeleteTemplate -> deleteTemplate(event.templateId)
            is TemplateListEvent.DuplicateTemplate -> duplicateTemplate(event.templateId)
            is TemplateListEvent.UndoDelete -> undoDelete()
        }
    }

    private fun navigateToDetail(templateId: Long?) {
        viewModelScope.launch {
            _effects.send(TemplateListEffect.NavigateToDetail(templateId))
        }
    }

    private fun deleteTemplate(templateId: Long) {
        viewModelScope.launch {
            getTemplateUseCase(templateId)
                .onSuccess { template ->
                    if (template != null) {
                        deletedTemplate = template
                        deleteTemplateUseCase(templateId)
                        _effects.send(TemplateListEffect.ShowUndoSnackbar(template.name, templateId))
                    }
                }
                .onFailure { e ->
                    logcat { "Error deleting template: ${e.asLog()}" }
                    _effects.send(TemplateListEffect.ShowError("テンプレートの削除に失敗しました"))
                }
        }
    }

    private fun undoDelete() {
        val template = deletedTemplate ?: return
        viewModelScope.launch {
            saveTemplateUseCase(template)
                .onSuccess {
                    deletedTemplate = null
                }
                .onFailure { e ->
                    logcat { "Error restoring template: ${e.asLog()}" }
                    _effects.send(TemplateListEffect.ShowError("テンプレートの復元に失敗しました"))
                }
        }
    }

    private fun duplicateTemplate(templateId: Long) {
        viewModelScope.launch {
            duplicateTemplateUseCase(templateId)
                .onFailure { e ->
                    logcat { "Error duplicating template: ${e.message}" }
                    _effects.send(TemplateListEffect.ShowError("テンプレートの複製に失敗しました"))
                }
        }
    }

    companion object {
    }
}
