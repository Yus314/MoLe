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

/**
 * UI State for template applicator.
 * Handles template searching, selection, and application.
 */
data class TemplateApplicatorUiState(
    val showTemplateSelector: Boolean = false,
    val availableTemplates: List<TemplateItem> = emptyList(),
    val selectedTemplateId: Long? = null,
    val isSearching: Boolean = false
)

/**
 * Events from UI to TemplateApplicatorViewModel.
 */
sealed class TemplateApplicatorEvent {
    data object ShowTemplateSelector : TemplateApplicatorEvent()
    data object DismissTemplateSelector : TemplateApplicatorEvent()
    data class ApplyTemplate(val templateId: Long) : TemplateApplicatorEvent()
    data class ApplyTemplateFromQr(val qrText: String) : TemplateApplicatorEvent()
    data class SearchTemplates(val query: String) : TemplateApplicatorEvent()
    data object ClearSelection : TemplateApplicatorEvent()
}

/**
 * Side effects for one-time UI actions from TemplateApplicatorViewModel.
 * The ApplyTemplate effect contains the data to be applied to other ViewModels.
 */
sealed class TemplateApplicatorEffect {
    /**
     * Effect emitted when a template should be applied.
     * The Activity coordinates applying this data to TransactionFormViewModel and AccountRowsViewModel.
     */
    data class ApplyTemplate(
        val description: String,
        val transactionComment: String?,
        val date: net.ktnx.mobileledger.core.common.utils.SimpleDate?,
        val accounts: List<TransactionAccountRow>
    ) : TemplateApplicatorEffect()
}
