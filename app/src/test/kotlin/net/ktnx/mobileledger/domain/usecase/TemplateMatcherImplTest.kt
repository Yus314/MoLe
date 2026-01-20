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

import net.ktnx.mobileledger.domain.model.Template
import net.ktnx.mobileledger.domain.model.TemplateLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [TemplateMatcherImpl].
 */
class TemplateMatcherImplTest {

    private lateinit var matcher: TemplateMatcherImpl

    @Before
    fun setup() {
        matcher = TemplateMatcherImpl()
    }

    private fun template(
        id: Long = 1L,
        name: String = "Test Template",
        pattern: String = "",
        transactionDescription: String? = null,
        transactionDescriptionMatchGroup: Int? = null,
        transactionComment: String? = null,
        transactionCommentMatchGroup: Int? = null,
        dateYear: Int? = null,
        dateYearMatchGroup: Int? = null,
        dateMonth: Int? = null,
        dateMonthMatchGroup: Int? = null,
        dateDay: Int? = null,
        dateDayMatchGroup: Int? = null,
        lines: List<TemplateLine> = emptyList()
    ) = Template(
        id = id,
        name = name,
        pattern = pattern,
        transactionDescription = transactionDescription,
        transactionDescriptionMatchGroup = transactionDescriptionMatchGroup,
        transactionComment = transactionComment,
        transactionCommentMatchGroup = transactionCommentMatchGroup,
        dateYear = dateYear,
        dateYearMatchGroup = dateYearMatchGroup,
        dateMonth = dateMonth,
        dateMonthMatchGroup = dateMonthMatchGroup,
        dateDay = dateDay,
        dateDayMatchGroup = dateDayMatchGroup,
        lines = lines
    )

    private fun line(
        accountName: String? = null,
        accountNameGroup: Int? = null,
        amount: Float? = null,
        amountGroup: Int? = null,
        currencyName: String? = null,
        currencyGroup: Int? = null,
        comment: String? = null,
        commentGroup: Int? = null,
        negateAmount: Boolean = false
    ) = TemplateLine(
        accountName = accountName,
        accountNameGroup = accountNameGroup,
        amount = amount,
        amountGroup = amountGroup,
        currencyName = currencyName,
        currencyGroup = currencyGroup,
        comment = comment,
        commentGroup = commentGroup,
        negateAmount = negateAmount
    )

    // ========== findMatch tests ==========

    @Test
    fun `findMatch returns null for empty templates list`() {
        val result = matcher.findMatch("some text", emptyList())
        assertNull(result)
    }

    @Test
    fun `findMatch returns null when no template matches`() {
        val templates = listOf(
            template(pattern = "foo"),
            template(pattern = "bar")
        )
        val result = matcher.findMatch("baz", templates)
        assertNull(result)
    }

    @Test
    fun `findMatch returns first matching template`() {
        val templates = listOf(
            template(id = 1, name = "First", pattern = "hello"),
            template(id = 2, name = "Second", pattern = "hello")
        )
        val result = matcher.findMatch("hello world", templates)

        assertNotNull(result)
        assertEquals(1L, result!!.template.id)
        assertEquals("First", result.template.name)
    }

    @Test
    fun `findMatch skips templates with blank pattern`() {
        val templates = listOf(
            template(id = 1, pattern = ""),
            template(id = 2, pattern = "hello")
        )
        val result = matcher.findMatch("hello", templates)

        assertNotNull(result)
        assertEquals(2L, result!!.template.id)
    }

    @Test
    fun `findMatch skips templates with invalid regex`() {
        val templates = listOf(
            template(id = 1, pattern = "[invalid"),
            template(id = 2, pattern = "valid")
        )
        val result = matcher.findMatch("valid text", templates)

        assertNotNull(result)
        assertEquals(2L, result!!.template.id)
    }

    @Test
    fun `findMatch captures groups correctly`() {
        val templates = listOf(
            template(pattern = "(\\d+)-(\\d+)-(\\d+)")
        )
        val result = matcher.findMatch("Date: 2025-06-15", templates)

        assertNotNull(result)
        assertEquals("2025", result!!.matchResult.group(1))
        assertEquals("06", result.matchResult.group(2))
        assertEquals("15", result.matchResult.group(3))
    }

    // ========== extractTransaction tests ==========

    @Test
    fun `extractTransaction uses literal description`() {
        val template = template(transactionDescription = "Fixed Description")
        val matched = matcher.findMatch("anything", listOf(template.copy(pattern = ".*")))!!

        val result = matcher.extractTransaction(matched, "USD")

        assertEquals("Fixed Description", result.description)
    }

    @Test
    fun `extractTransaction extracts description from match group`() {
        val template = template(
            pattern = "Order: (.+)",
            transactionDescriptionMatchGroup = 1
        )
        val matched = matcher.findMatch("Order: Coffee", listOf(template))!!

        val result = matcher.extractTransaction(matched, "USD")

        assertEquals("Coffee", result.description)
    }

    @Test
    fun `extractTransaction uses literal comment`() {
        val template = template(
            pattern = ".*",
            transactionComment = "Fixed Comment"
        )
        val matched = matcher.findMatch("anything", listOf(template))!!

        val result = matcher.extractTransaction(matched, "USD")

        assertEquals("Fixed Comment", result.comment)
    }

    @Test
    fun `extractTransaction extracts date from match groups`() {
        val template = template(
            pattern = "(\\d{4})/(\\d{2})/(\\d{2})",
            dateYearMatchGroup = 1,
            dateMonthMatchGroup = 2,
            dateDayMatchGroup = 3
        )
        val matched = matcher.findMatch("2025/06/15", listOf(template))!!

        val result = matcher.extractTransaction(matched, "USD")

        assertNotNull(result.date)
        assertEquals(2025, result.date!!.year)
        assertEquals(6, result.date!!.month)
        assertEquals(15, result.date!!.day)
    }

    @Test
    fun `extractTransaction uses literal date values`() {
        val template = template(
            pattern = ".*",
            dateYear = 2024,
            dateMonth = 12,
            dateDay = 25
        )
        val matched = matcher.findMatch("anything", listOf(template))!!

        val result = matcher.extractTransaction(matched, "USD")

        assertNotNull(result.date)
        assertEquals(2024, result.date!!.year)
        assertEquals(12, result.date!!.month)
        assertEquals(25, result.date!!.day)
    }

    @Test
    fun `extractTransaction returns null date when year not available`() {
        val template = template(
            pattern = ".*",
            dateMonth = 6,
            dateDay = 15
        )
        val matched = matcher.findMatch("anything", listOf(template))!!

        val result = matcher.extractTransaction(matched, "USD")

        assertNull(result.date)
    }

    @Test
    fun `extractTransaction extracts line account from match group`() {
        val template = template(
            pattern = "Account: (.+)",
            lines = listOf(line(accountNameGroup = 1))
        )
        val matched = matcher.findMatch("Account: Assets:Bank", listOf(template))!!

        val result = matcher.extractTransaction(matched, "USD")

        assertEquals(1, result.lines.size)
        assertEquals("Assets:Bank", result.lines[0].accountName)
    }

    @Test
    fun `extractTransaction uses default currency when not specified`() {
        val template = template(
            pattern = ".*",
            lines = listOf(line(accountName = "Assets:Bank"))
        )
        val matched = matcher.findMatch("anything", listOf(template))!!

        val result = matcher.extractTransaction(matched, "USD")

        assertEquals("USD", result.lines[0].currency)
    }

    @Test
    fun `extractTransaction extracts amount and negates when specified`() {
        val template = template(
            pattern = "Amount: (\\d+)",
            lines = listOf(line(amountGroup = 1, negateAmount = true))
        )
        val matched = matcher.findMatch("Amount: 100", listOf(template))!!

        val result = matcher.extractTransaction(matched, "USD")

        assertEquals(-100f, result.lines[0].amount)
    }

    @Test
    fun `extractTransaction parses amount with comma separator`() {
        val template = template(
            pattern = "Amount: ([\\d,]+)",
            lines = listOf(line(amountGroup = 1))
        )
        val matched = matcher.findMatch("Amount: 1,234", listOf(template))!!

        val result = matcher.extractTransaction(matched, "USD")

        assertEquals(1234f, result.lines[0].amount)
    }

    // ========== validatePattern tests ==========

    @Test
    fun `validatePattern returns null for valid pattern`() {
        val result = matcher.validatePattern("\\d{4}-\\d{2}-\\d{2}")
        assertNull(result)
    }

    @Test
    fun `validatePattern returns null for empty pattern`() {
        val result = matcher.validatePattern("")
        assertNull(result)
    }

    @Test
    fun `validatePattern returns error for invalid pattern`() {
        val result = matcher.validatePattern("[unclosed")
        assertEquals(TemplateMatcherImpl.ERROR_INVALID_PATTERN, result)
    }

    @Test
    fun `validatePattern returns error for invalid group syntax`() {
        val result = matcher.validatePattern("(unclosed")
        assertEquals(TemplateMatcherImpl.ERROR_INVALID_PATTERN, result)
    }
}
