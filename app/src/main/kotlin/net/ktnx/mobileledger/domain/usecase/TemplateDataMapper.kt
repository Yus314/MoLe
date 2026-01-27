/*
 * Copyright © 2026 Damyan Ivanov.
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

package net.ktnx.mobileledger.domain.usecase

import net.ktnx.mobileledger.core.domain.model.Template
import net.ktnx.mobileledger.core.domain.model.TemplateLine
import net.ktnx.mobileledger.ui.templates.MatchableValue
import net.ktnx.mobileledger.ui.templates.TemplateAccountRow
import net.ktnx.mobileledger.ui.templates.TemplateDetailUiState

/**
 * Interface for mapping between Template domain models and UI state.
 *
 * Provides pure functions for converting between Template/TemplateLine domain models
 * and TemplateDetailUiState/TemplateAccountRow UI state representations.
 */
interface TemplateDataMapper {

    /**
     * Converts a TemplateDetailUiState to a Template domain model.
     *
     * @param state The UI state to convert
     * @return The Template domain model
     */
    fun toTemplate(state: TemplateDetailUiState): Template

    /**
     * Converts a Template domain model to account rows for UI display.
     *
     * @param template The template to convert
     * @param idGenerator Function to generate IDs for rows without existing IDs
     * @return List of TemplateAccountRow for UI display
     */
    fun toAccountRows(template: Template, idGenerator: () -> Long): List<TemplateAccountRow>

    /**
     * Extracts a MatchableValue from literal and match group values (String).
     *
     * @param literal The literal string value
     * @param matchGroup The match group number (if using regex matching)
     * @return MatchableValue representing the field
     */
    fun extractMatchableValue(literal: String?, matchGroup: Int?): MatchableValue

    /**
     * Extracts a MatchableValue from literal and match group values (Int).
     *
     * @param literal The literal integer value
     * @param matchGroup The match group number (if using regex matching)
     * @return MatchableValue representing the field
     */
    fun extractMatchableValueInt(literal: Int?, matchGroup: Int?): MatchableValue

    /**
     * Extracts a MatchableValue from literal and match group values (Float).
     *
     * @param literal The literal float value
     * @param matchGroup The match group number (if using regex matching)
     * @return MatchableValue representing the field
     */
    fun extractMatchableValueFloat(literal: Float?, matchGroup: Int?): MatchableValue

    /**
     * Extracts a MatchableValue from literal and match group values (Currency ID).
     *
     * @param currencyId The currency ID value
     * @param matchGroup The match group number (if using regex matching)
     * @return MatchableValue representing the field
     */
    fun extractMatchableValueCurrency(currencyId: Long?, matchGroup: Int?): MatchableValue
}
