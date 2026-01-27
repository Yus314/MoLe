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

import net.ktnx.mobileledger.core.common.utils.SimpleDate
import net.ktnx.mobileledger.core.domain.model.MatchedTemplate
import net.ktnx.mobileledger.core.domain.model.Template

/**
 * UseCase for matching text against templates and extracting transaction data.
 *
 * Provides centralized template matching logic including:
 * - Finding matching templates from a list
 * - Extracting transaction data from matched templates
 * - Pattern validation
 */
interface TemplateMatcher {

    /**
     * Extracted transaction data from a matched template.
     */
    data class ExtractedTransaction(
        val description: String,
        val comment: String?,
        val date: SimpleDate?,
        val lines: List<ExtractedLine>
    )

    /**
     * Extracted transaction line data.
     */
    data class ExtractedLine(
        val accountName: String,
        val amount: Float?,
        val currency: String,
        val comment: String
    )

    /**
     * Find the first template that matches the given text.
     *
     * @param text The text to match against template patterns
     * @param templates List of templates to check
     * @return [MatchedTemplate] if a match is found, null otherwise
     */
    fun findMatch(text: String, templates: List<Template>): MatchedTemplate?

    /**
     * Extract transaction data from a matched template.
     *
     * @param matched The matched template with its match result
     * @param defaultCurrency Currency to use when template doesn't specify one
     * @return [ExtractedTransaction] with extracted data
     */
    fun extractTransaction(matched: MatchedTemplate, defaultCurrency: String): ExtractedTransaction

    /**
     * Validate a regex pattern.
     *
     * @param pattern The regex pattern to validate
     * @return Error message if invalid, null if valid
     */
    fun validatePattern(pattern: String): String?
}
