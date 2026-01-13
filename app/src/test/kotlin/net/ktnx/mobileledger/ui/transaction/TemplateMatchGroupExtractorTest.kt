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

import java.util.regex.Pattern
import net.ktnx.mobileledger.db.TemplateHeader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TemplateMatchGroupExtractorTest {

    // Helper to create a MatchResult from a pattern and input
    private fun createMatchResult(pattern: String, input: String): java.util.regex.MatchResult {
        val compiledPattern = Pattern.compile(pattern)
        val matcher = compiledPattern.matcher(input)
        matcher.find()
        return matcher.toMatchResult()
    }

    // ========== extractFromMatchGroup tests ==========

    @Test
    fun `extractFromMatchGroup returns value from valid group`() {
        val matchResult = createMatchResult("PAY:(\\d+):(.+)", "PAY:1500:コンビニ")

        val amount = TemplateMatchGroupExtractor.extractFromMatchGroup(matchResult, 1, null)
        val description = TemplateMatchGroupExtractor.extractFromMatchGroup(matchResult, 2, null)

        assertEquals("1500", amount)
        assertEquals("コンビニ", description)
    }

    @Test
    fun `extractFromMatchGroup returns fallback when group is null`() {
        val matchResult = createMatchResult("PAY:(\\d+)", "PAY:1500")

        val result = TemplateMatchGroupExtractor.extractFromMatchGroup(matchResult, null, "default")

        assertEquals("default", result)
    }

    @Test
    fun `extractFromMatchGroup returns fallback when group is zero`() {
        val matchResult = createMatchResult("PAY:(\\d+)", "PAY:1500")

        val result = TemplateMatchGroupExtractor.extractFromMatchGroup(matchResult, 0, "default")

        assertEquals("default", result)
    }

    @Test
    fun `extractFromMatchGroup returns fallback when group exceeds count`() {
        val matchResult = createMatchResult("PAY:(\\d+)", "PAY:1500")

        // Pattern has only 1 group, requesting group 5
        val result = TemplateMatchGroupExtractor.extractFromMatchGroup(matchResult, 5, "fallback")

        assertEquals("fallback", result)
    }

    @Test
    fun `extractFromMatchGroup returns fallback when group is negative`() {
        val matchResult = createMatchResult("PAY:(\\d+)", "PAY:1500")

        val result = TemplateMatchGroupExtractor.extractFromMatchGroup(matchResult, -1, "negative")

        assertEquals("negative", result)
    }

    // ========== parseAmount tests ==========

    @Test
    fun `parseAmount handles simple number`() {
        val result = TemplateMatchGroupExtractor.parseAmount("1500", false)

        assertEquals(1500f, result)
    }

    @Test
    fun `parseAmount handles comma-separated number`() {
        val result = TemplateMatchGroupExtractor.parseAmount("1,500", false)

        assertEquals(1500f, result)
    }

    @Test
    fun `parseAmount handles number with spaces`() {
        val result = TemplateMatchGroupExtractor.parseAmount("1 500", false)

        assertEquals(1500f, result)
    }

    @Test
    fun `parseAmount negates value when negate is true`() {
        val result = TemplateMatchGroupExtractor.parseAmount("1500", true)

        assertEquals(-1500f, result)
    }

    @Test
    fun `parseAmount returns null for null input`() {
        val result = TemplateMatchGroupExtractor.parseAmount(null, false)

        assertNull(result)
    }

    @Test
    fun `parseAmount returns null for blank input`() {
        val result = TemplateMatchGroupExtractor.parseAmount("  ", false)

        assertNull(result)
    }

    @Test
    fun `parseAmount returns null for invalid input`() {
        val result = TemplateMatchGroupExtractor.parseAmount("abc", false)

        assertNull(result)
    }

    @Test
    fun `parseAmount handles decimal number`() {
        val result = TemplateMatchGroupExtractor.parseAmount("1500.50", false)

        assertEquals(1500.5f, result)
    }

    @Test
    fun `parseAmount handles negative input string`() {
        val result = TemplateMatchGroupExtractor.parseAmount("-1500", false)

        assertEquals(-1500f, result)
    }

    @Test
    fun `parseAmount handles large comma-separated number`() {
        val result = TemplateMatchGroupExtractor.parseAmount("1,234,567", false)

        assertEquals(1234567f, result)
    }

    // ========== extractDate tests ==========

    @Test
    fun `extractDate returns SimpleDate when all parts from match groups`() {
        val matchResult = createMatchResult(
            "(\\d{4})-(\\d{2})-(\\d{2})",
            "2024-01-15"
        )
        val header = TemplateHeader(0, "test", "").apply {
            dateYearMatchGroup = 1
            dateMonthMatchGroup = 2
            dateDayMatchGroup = 3
        }

        val result = TemplateMatchGroupExtractor.extractDate(matchResult, header)

        assertNotNull(result)
        assertEquals(2024, result!!.year)
        assertEquals(1, result.month)
        assertEquals(15, result.day)
    }

    @Test
    fun `extractDate returns SimpleDate when year from group and others from static`() {
        val matchResult = createMatchResult("YEAR:(\\d{4})", "YEAR:2024")
        val header = TemplateHeader(0, "test", "").apply {
            dateYearMatchGroup = 1
            dateMonth = 6
            dateDay = 20
        }

        val result = TemplateMatchGroupExtractor.extractDate(matchResult, header)

        assertNotNull(result)
        assertEquals(2024, result!!.year)
        assertEquals(6, result.month)
        assertEquals(20, result.day)
    }

    @Test
    fun `extractDate returns null when year is missing`() {
        val matchResult = createMatchResult("(\\d{2})-(\\d{2})", "01-15")
        val header = TemplateHeader(0, "test", "").apply {
            // No year match group and no static year
            dateMonthMatchGroup = 1
            dateDayMatchGroup = 2
        }

        val result = TemplateMatchGroupExtractor.extractDate(matchResult, header)

        assertNull(result)
    }

    @Test
    fun `extractDate uses static year when no match group`() {
        val matchResult = createMatchResult("(\\d{2})-(\\d{2})", "01-15")
        val header = TemplateHeader(0, "test", "").apply {
            dateYear = 2024
            dateMonthMatchGroup = 1
            dateDayMatchGroup = 2
        }

        val result = TemplateMatchGroupExtractor.extractDate(matchResult, header)

        assertNotNull(result)
        assertEquals(2024, result!!.year)
        assertEquals(1, result.month)
        assertEquals(15, result.day)
    }

    @Test
    fun `extractDate uses today for missing month and day`() {
        val matchResult = createMatchResult("YEAR:(\\d{4})", "YEAR:2024")
        val header = TemplateHeader(0, "test", "").apply {
            dateYearMatchGroup = 1
            // No month or day - should use today's values
        }

        val result = TemplateMatchGroupExtractor.extractDate(matchResult, header)

        assertNotNull(result)
        assertEquals(2024, result!!.year)
        // Month and day should be from today - we can't assert exact values
        // but we can check they're valid
        assert(result.month in 1..12)
        assert(result.day in 1..31)
    }
}
