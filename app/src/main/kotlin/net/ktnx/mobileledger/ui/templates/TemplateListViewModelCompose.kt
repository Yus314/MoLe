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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ktnx.mobileledger.dao.TemplateHeaderDAO
import net.ktnx.mobileledger.db.TemplateHeader
import net.ktnx.mobileledger.utils.Logger

/**
 * ViewModel for the template list screen using Compose.
 * Uses StateFlow for reactive UI updates.
 */
@HiltViewModel
class TemplateListViewModelCompose @Inject constructor(private val templateHeaderDAO: TemplateHeaderDAO) : ViewModel() {

    private val _uiState = MutableStateFlow(TemplateListUiState(isLoading = true))
    val uiState: StateFlow<TemplateListUiState> = _uiState.asStateFlow()

    private val _effects = Channel<TemplateListEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    // For undo functionality
    private var deletedTemplate: TemplateHeader? = null

    init {
        loadTemplates()
    }

    private fun loadTemplates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Observe LiveData from DAO and convert to StateFlow
                templateHeaderDAO.getTemplates().observeForever { templates ->
                    _uiState.update { state ->
                        state.copy(
                            templates = templates.map { header ->
                                TemplateListItem(
                                    id = header.id,
                                    name = header.name,
                                    pattern = header.regularExpression,
                                    isFallback = header.isFallback
                                )
                            },
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                Logger.debug("template-list", "Error loading templates: ${e.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "テンプレートの読み込みに失敗しました"
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
            try {
                val templateName = withContext(Dispatchers.IO) {
                    val template = templateHeaderDAO.getTemplateSync(templateId)
                    if (template != null) {
                        deletedTemplate = TemplateHeader(template)
                        templateHeaderDAO.deleteSync(template)
                        template.name
                    } else {
                        null
                    }
                }

                if (templateName != null) {
                    _effects.send(TemplateListEffect.ShowUndoSnackbar(templateName, templateId))
                }
            } catch (e: Exception) {
                Logger.debug("template-list", "Error deleting template: ${e.message}")
                _effects.send(TemplateListEffect.ShowError("テンプレートの削除に失敗しました"))
            }
        }
    }

    private fun undoDelete() {
        val template = deletedTemplate ?: return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    templateHeaderDAO.insertSync(template)
                }
                deletedTemplate = null
            } catch (e: Exception) {
                Logger.debug("template-list", "Error restoring template: ${e.message}")
                _effects.send(TemplateListEffect.ShowError("テンプレートの復元に失敗しました"))
            }
        }
    }

    private fun duplicateTemplate(templateId: Long) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    templateHeaderDAO.duplicateTemplateWithAccounts(templateId, null)
                }
            } catch (e: Exception) {
                Logger.debug("template-list", "Error duplicating template: ${e.message}")
                _effects.send(TemplateListEffect.ShowError("テンプレートの複製に失敗しました"))
            }
        }
    }

    companion object {
        private const val TAG = "template-list-vm"
    }
}
