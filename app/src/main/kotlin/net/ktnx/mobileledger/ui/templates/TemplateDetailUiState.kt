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

import androidx.compose.ui.text.AnnotatedString

/**
 * UI state for the template detail screen.
 * Templates use regex patterns to extract transaction data from text (e.g., SMS messages).
 */
data class TemplateDetailUiState(
    val templateId: Long? = null,
    val name: String = "",
    val pattern: String = "",
    val testText: String = "",
    val patternError: String? = null,
    val testMatchResult: AnnotatedString? = null,
    val patternGroupCount: Int = 0,

    // Transaction extraction settings
    val transactionDescription: MatchableValue = MatchableValue.Literal(""),
    val transactionComment: MatchableValue = MatchableValue.Literal(""),
    val dateYear: MatchableValue = MatchableValue.Literal(""),
    val dateMonth: MatchableValue = MatchableValue.Literal(""),
    val dateDay: MatchableValue = MatchableValue.Literal(""),

    // Account rows
    val accounts: List<TemplateAccountRow> = listOf(
        TemplateAccountRow(id = -1),
        TemplateAccountRow(id = -2)
    ),

    // Flags
    val isFallback: Boolean = false,

    // UI state
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val showUnsavedChangesDialog: Boolean = false
) {
    val isNewTemplate: Boolean
        get() = templateId == null || templateId <= 0

    val canDelete: Boolean
        get() = !isNewTemplate

    val isFormValid: Boolean
        get() = name.isNotBlank() && pattern.isNotBlank() && patternError == null
}

/**
 * Represents a value that can either be a literal string or a regex match group reference.
 */
sealed class MatchableValue {
    data class Literal(val value: String) : MatchableValue()
    data class MatchGroup(val group: Int) : MatchableValue()

    fun isLiteral(): Boolean = this is Literal
    fun isMatchGroup(): Boolean = this is MatchGroup

    fun getLiteralValue(): String = when (this) {
        is Literal -> value
        is MatchGroup -> ""
    }

    fun getMatchGroup(): Int = when (this) {
        is Literal -> 0
        is MatchGroup -> group
    }
}

/**
 * Represents an account row in the template.
 */
data class TemplateAccountRow(
    val id: Long = 0,
    val position: Int = 0,
    val accountName: MatchableValue = MatchableValue.Literal(""),
    val accountComment: MatchableValue = MatchableValue.Literal(""),
    val amount: MatchableValue = MatchableValue.Literal(""),
    val currency: MatchableValue = MatchableValue.Literal(""),
    val negateAmount: Boolean = false
) {
    fun isEmpty(): Boolean = accountName.getLiteralValue().isBlank() &&
        accountComment.getLiteralValue().isBlank() &&
        amount.getLiteralValue().isBlank()
}

/**
 * One-shot effects for the template detail screen.
 */
sealed class TemplateDetailEffect {
    data object NavigateBack : TemplateDetailEffect()
    data object TemplateSaved : TemplateDetailEffect()
    data object TemplateDeleted : TemplateDetailEffect()
    data class ShowError(val message: String) : TemplateDetailEffect()
}

/**
 * Events from the template detail screen.
 */
sealed class TemplateDetailEvent {
    // Header fields
    data class UpdateName(val name: String) : TemplateDetailEvent()
    data class UpdatePattern(val pattern: String) : TemplateDetailEvent()
    data class UpdateTestText(val text: String) : TemplateDetailEvent()
    data class UpdateIsFallback(val isFallback: Boolean) : TemplateDetailEvent()

    // Transaction extraction
    data class UpdateTransactionDescription(val value: MatchableValue) : TemplateDetailEvent()
    data class UpdateTransactionComment(val value: MatchableValue) : TemplateDetailEvent()
    data class UpdateDateYear(val value: MatchableValue) : TemplateDetailEvent()
    data class UpdateDateMonth(val value: MatchableValue) : TemplateDetailEvent()
    data class UpdateDateDay(val value: MatchableValue) : TemplateDetailEvent()

    // Account rows
    data class UpdateAccountName(val index: Int, val value: MatchableValue) : TemplateDetailEvent()
    data class UpdateAccountComment(val index: Int, val value: MatchableValue) : TemplateDetailEvent()
    data class UpdateAccountAmount(val index: Int, val value: MatchableValue) : TemplateDetailEvent()
    data class UpdateAccountCurrency(val index: Int, val value: MatchableValue) : TemplateDetailEvent()
    data class UpdateAccountNegateAmount(val index: Int, val negate: Boolean) : TemplateDetailEvent()
    data class RemoveAccountRow(val index: Int) : TemplateDetailEvent()
    data class MoveAccountRow(val fromIndex: Int, val toIndex: Int) : TemplateDetailEvent()
    data object AddAccountRow : TemplateDetailEvent()

    // Actions
    data object Save : TemplateDetailEvent()
    data object Delete : TemplateDetailEvent()
    data object NavigateBack : TemplateDetailEvent()
    data object ConfirmDiscardChanges : TemplateDetailEvent()
    data object DismissUnsavedChangesDialog : TemplateDetailEvent()
    data object ShowDeleteConfirmDialog : TemplateDetailEvent()
    data object DismissDeleteConfirmDialog : TemplateDetailEvent()
    data object ConfirmDelete : TemplateDetailEvent()
}
