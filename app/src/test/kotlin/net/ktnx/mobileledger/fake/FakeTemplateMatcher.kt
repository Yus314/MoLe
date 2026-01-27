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

package net.ktnx.mobileledger.fake

import net.ktnx.mobileledger.core.domain.model.MatchedTemplate
import net.ktnx.mobileledger.core.domain.model.Template
import net.ktnx.mobileledger.domain.usecase.TemplateMatcher

/**
 * Fake implementation of [TemplateMatcher] for testing.
 *
 * Provides controllable behavior for tests:
 * - Configure match result via [matchResult]
 * - Configure extracted transaction via [extractedTransaction]
 * - Configure pattern validation via [patternError]
 * - Track method calls via [findMatchCallCount], [extractTransactionCallCount]
 * - Reset state between tests via [reset]
 */
class FakeTemplateMatcher : TemplateMatcher {

    /**
     * Custom match result to return. If null, no match is found.
     */
    var matchResult: MatchedTemplate? = null

    /**
     * Custom extracted transaction to return.
     */
    var extractedTransaction: TemplateMatcher.ExtractedTransaction? = null

    /**
     * Pattern validation error to return. If null, pattern is valid.
     */
    var patternError: String? = null

    /**
     * Number of times [findMatch] was called.
     */
    var findMatchCallCount = 0
        private set

    /**
     * Number of times [extractTransaction] was called.
     */
    var extractTransactionCallCount = 0
        private set

    /**
     * Number of times [validatePattern] was called.
     */
    var validatePatternCallCount = 0
        private set

    /**
     * The text passed to the last [findMatch] call.
     */
    var lastMatchText: String? = null
        private set

    /**
     * The templates passed to the last [findMatch] call.
     */
    var lastTemplates: List<Template>? = null
        private set

    /**
     * The matched template passed to the last [extractTransaction] call.
     */
    var lastMatched: MatchedTemplate? = null
        private set

    /**
     * The pattern passed to the last [validatePattern] call.
     */
    var lastPattern: String? = null
        private set

    override fun findMatch(text: String, templates: List<Template>): MatchedTemplate? {
        findMatchCallCount++
        lastMatchText = text
        lastTemplates = templates
        return matchResult
    }

    override fun extractTransaction(
        matched: MatchedTemplate,
        defaultCurrency: String
    ): TemplateMatcher.ExtractedTransaction {
        extractTransactionCallCount++
        lastMatched = matched

        return extractedTransaction ?: TemplateMatcher.ExtractedTransaction(
            description = matched.template.transactionDescription ?: "",
            comment = matched.template.transactionComment,
            date = null,
            lines = emptyList()
        )
    }

    override fun validatePattern(pattern: String): String? {
        validatePatternCallCount++
        lastPattern = pattern
        return patternError
    }

    /**
     * Reset all state to initial values.
     */
    fun reset() {
        matchResult = null
        extractedTransaction = null
        patternError = null
        findMatchCallCount = 0
        extractTransactionCallCount = 0
        validatePatternCallCount = 0
        lastMatchText = null
        lastTemplates = null
        lastMatched = null
        lastPattern = null
    }

    /**
     * Configure an extracted transaction with the given values.
     */
    fun setExtractedTransaction(
        description: String,
        comment: String? = null,
        lines: List<TemplateMatcher.ExtractedLine> = emptyList()
    ) {
        extractedTransaction = TemplateMatcher.ExtractedTransaction(
            description = description,
            comment = comment,
            date = null,
            lines = lines
        )
    }
}
