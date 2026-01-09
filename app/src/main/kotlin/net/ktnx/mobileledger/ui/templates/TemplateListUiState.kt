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

/**
 * UI state for the template list screen.
 */
data class TemplateListUiState(
    val templates: List<TemplateListItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Represents a single template item in the list.
 */
data class TemplateListItem(val id: Long, val name: String, val pattern: String, val isFallback: Boolean)

/**
 * One-shot effects for the template list screen.
 */
sealed class TemplateListEffect {
    data class NavigateToDetail(val templateId: Long?) : TemplateListEffect()
    data class ShowError(val message: String) : TemplateListEffect()
    data class ShowUndoSnackbar(val templateName: String, val templateId: Long) : TemplateListEffect()
}

/**
 * Events from the template list screen.
 */
sealed class TemplateListEvent {
    data object CreateNewTemplate : TemplateListEvent()
    data class EditTemplate(val templateId: Long) : TemplateListEvent()
    data class DeleteTemplate(val templateId: Long) : TemplateListEvent()
    data class DuplicateTemplate(val templateId: Long) : TemplateListEvent()
    data class UndoDelete(val templateId: Long) : TemplateListEvent()
}
