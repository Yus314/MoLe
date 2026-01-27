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

import java.util.regex.MatchResult
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import javax.inject.Inject
import logcat.logcat
import net.ktnx.mobileledger.core.common.utils.SimpleDate
import net.ktnx.mobileledger.core.domain.model.MatchedTemplate
import net.ktnx.mobileledger.core.domain.model.Template
import net.ktnx.mobileledger.core.domain.model.TemplateLine

/**
 * Implementation of [TemplateMatcher].
 *
 * Provides template matching and data extraction logic:
 * - Pattern matching against text
 * - Extraction of values from regex match groups
 * - Date parsing from match groups
 * - Amount parsing with negation support
 */
class TemplateMatcherImpl @Inject constructor() : TemplateMatcher {

    companion object {
        const val ERROR_INVALID_PATTERN = "無効な正規表現パターンです"
    }

    override fun findMatch(text: String, templates: List<Template>): MatchedTemplate? {
        for (template in templates) {
            val regex = template.pattern
            if (regex.isBlank()) continue

            try {
                val pattern = Pattern.compile(regex)
                val matcher = pattern.matcher(text)
                if (matcher.find()) {
                    return MatchedTemplate(template, matcher.toMatchResult())
                }
            } catch (e: PatternSyntaxException) {
                logcat { "Invalid regex in template '${template.name}': $regex - ${e.message}" }
            }
        }
        return null
    }

    override fun extractTransaction(
        matched: MatchedTemplate,
        defaultCurrency: String
    ): TemplateMatcher.ExtractedTransaction {
        val template = matched.template
        val matchResult = matched.matchResult

        val description = extractFromMatchGroup(
            matchResult,
            template.transactionDescriptionMatchGroup,
            template.transactionDescription
        ) ?: ""

        val comment = extractFromMatchGroup(
            matchResult,
            template.transactionCommentMatchGroup,
            template.transactionComment
        )

        val date = extractDate(matchResult, template)

        val lines = template.lines.map { line ->
            extractLine(matchResult, line, defaultCurrency)
        }

        return TemplateMatcher.ExtractedTransaction(
            description = description,
            comment = comment,
            date = date,
            lines = lines
        )
    }

    override fun validatePattern(pattern: String): String? {
        if (pattern.isBlank()) {
            return null // Empty pattern is valid (will not match anything)
        }

        return try {
            Pattern.compile(pattern)
            null // Valid pattern
        } catch (e: PatternSyntaxException) {
            ERROR_INVALID_PATTERN
        }
    }

    /**
     * Extract a value from a regex match group.
     */
    private fun extractFromMatchGroup(matchResult: MatchResult, groupNumber: Int?, fallback: String?): String? {
        if (groupNumber == null || groupNumber <= 0) {
            return fallback
        }

        return try {
            if (groupNumber <= matchResult.groupCount()) {
                matchResult.group(groupNumber) ?: fallback
            } else {
                logcat { "Group $groupNumber exceeds count ${matchResult.groupCount()}" }
                fallback
            }
        } catch (e: Exception) {
            logcat { "Failed to extract group $groupNumber: ${e.message}" }
            fallback
        }
    }

    /**
     * Extract a transaction line from match groups.
     */
    private fun extractLine(
        matchResult: MatchResult,
        line: TemplateLine,
        defaultCurrency: String
    ): TemplateMatcher.ExtractedLine {
        val accountName = extractFromMatchGroup(
            matchResult,
            line.accountNameGroup,
            line.accountName
        ) ?: ""

        val amountStr = extractFromMatchGroup(
            matchResult,
            line.amountGroup,
            line.amount?.toString()
        )
        val amount = parseAmount(amountStr, line.negateAmount)

        val currencyName = if ((line.currencyGroup ?: 0) > 0) {
            extractFromMatchGroup(matchResult, line.currencyGroup, null)
        } else {
            line.currencyName
        } ?: defaultCurrency

        val comment = extractFromMatchGroup(
            matchResult,
            line.commentGroup,
            line.comment
        ) ?: ""

        return TemplateMatcher.ExtractedLine(
            accountName = accountName,
            amount = amount,
            currency = currencyName,
            comment = comment
        )
    }

    /**
     * Extract date from match groups or static values in the template.
     */
    private fun extractDate(matchResult: MatchResult, template: Template): SimpleDate? {
        val today = SimpleDate.today()

        // Year is required - without it, we can't construct a valid date
        val year = extractFromMatchGroup(
            matchResult,
            template.dateYearMatchGroup,
            template.dateYear?.toString()
        )?.toIntOrNull() ?: return null

        val month = extractFromMatchGroup(
            matchResult,
            template.dateMonthMatchGroup,
            template.dateMonth?.toString()
        )?.toIntOrNull() ?: today.month

        val day = extractFromMatchGroup(
            matchResult,
            template.dateDayMatchGroup,
            template.dateDay?.toString()
        )?.toIntOrNull() ?: today.day

        return try {
            SimpleDate(year, month, day)
        } catch (e: Exception) {
            logcat { "Failed to construct date: $year-$month-$day" }
            null
        }
    }

    /**
     * Parse an amount string to Float, handling comma separators and optional negation.
     */
    private fun parseAmount(amountStr: String?, negate: Boolean): Float? {
        if (amountStr.isNullOrBlank()) return null
        val cleaned = amountStr.replace(",", "").replace(" ", "").trim()
        val value = cleaned.toFloatOrNull() ?: return null
        return if (negate) -value else value
    }
}
