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

import net.ktnx.mobileledger.db.TemplateHeader
import net.ktnx.mobileledger.utils.Logger
import net.ktnx.mobileledger.utils.SimpleDate

/**
 * Utility object for extracting values from regex match groups in templates.
 * Separated from ViewModel for testability.
 */
object TemplateMatchGroupExtractor {

    /**
     * Extract a value from a regex match group.
     * Returns fallback if groupNumber is null, 0, or out of range.
     *
     * @param matchResult The regex match result containing captured groups
     * @param groupNumber The 1-based group number to extract (null or 0 means use fallback)
     * @param fallback The value to return if group extraction fails
     * @return The extracted value or fallback
     */
    fun extractFromMatchGroup(matchResult: java.util.regex.MatchResult, groupNumber: Int?, fallback: String?): String? {
        if (groupNumber == null || groupNumber <= 0) {
            return fallback
        }
        return try {
            if (groupNumber <= matchResult.groupCount()) {
                matchResult.group(groupNumber) ?: fallback
            } else {
                Logger.debug("template", "Group $groupNumber exceeds count ${matchResult.groupCount()}")
                fallback
            }
        } catch (e: Exception) {
            Logger.debug("template", "Failed to extract group $groupNumber: ${e.message}")
            fallback
        }
    }

    /**
     * Parse an amount string to Float, handling comma separators and optional negation.
     *
     * @param amountStr The amount string to parse (may contain commas)
     * @param negate If true, the resulting value will be negated
     * @return The parsed Float value, or null if parsing fails
     */
    fun parseAmount(amountStr: String?, negate: Boolean): Float? {
        if (amountStr.isNullOrBlank()) return null
        val cleaned = amountStr.replace(",", "").replace(" ", "").trim()
        val value = cleaned.toFloatOrNull() ?: return null
        return if (negate) -value else value
    }

    /**
     * Extract date from match groups or static values in the template header.
     *
     * @param matchResult The regex match result
     * @param header The template header containing match group numbers and static values
     * @return SimpleDate if date can be constructed, null otherwise
     */
    fun extractDate(matchResult: java.util.regex.MatchResult, header: TemplateHeader): SimpleDate? {
        val today = SimpleDate.today()

        // Year is required - without it, we can't construct a valid date
        val year = extractFromMatchGroup(
            matchResult,
            header.dateYearMatchGroup,
            header.dateYear?.toString()
        )?.toIntOrNull() ?: return null

        val month = extractFromMatchGroup(
            matchResult,
            header.dateMonthMatchGroup,
            header.dateMonth?.toString()
        )?.toIntOrNull() ?: today.month

        val day = extractFromMatchGroup(
            matchResult,
            header.dateDayMatchGroup,
            header.dateDay?.toString()
        )?.toIntOrNull() ?: today.day

        return try {
            SimpleDate(year, month, day)
        } catch (e: Exception) {
            Logger.debug("template", "Failed to construct date: $year-$month-$day")
            null
        }
    }
}
