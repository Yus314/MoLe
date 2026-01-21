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

package net.ktnx.mobileledger.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateTest {

    @Test
    fun `new template has null id`() {
        val template = Template(
            name = "Test Template",
            pattern = ".*"
        )
        assertNull(template.id)
    }

    @Test
    fun `existing template has id`() {
        val template = Template(
            id = 123L,
            name = "Test Template",
            pattern = ".*"
        )
        assertEquals(123L, template.id)
    }

    @Test
    fun `template with lines`() {
        val lines = listOf(
            TemplateLine(accountName = "Assets:Cash"),
            TemplateLine(accountName = "Expenses:Food", amount = 100f)
        )
        val template = Template(
            name = "Test Template",
            pattern = ".*",
            lines = lines
        )
        assertEquals(2, template.lines.size)
    }

    @Test
    fun `template defaults`() {
        val template = Template(
            name = "Test Template",
            pattern = ".*"
        )
        assertNull(template.testText)
        assertNull(template.transactionDescription)
        assertNull(template.transactionComment)
        assertNull(template.dateYearMatchGroup)
        assertNull(template.dateMonthMatchGroup)
        assertNull(template.dateDayMatchGroup)
        assertTrue(template.lines.isEmpty())
        assertFalse(template.isFallback)
    }

    @Test
    fun `template with all fields`() {
        val template = Template(
            id = 1L,
            name = "Full Template",
            pattern = "(\\d+)-(\\d+)-(\\d+)",
            testText = "2026-01-16",
            transactionDescription = "Test",
            transactionComment = "Comment",
            dateYearMatchGroup = 1,
            dateMonthMatchGroup = 2,
            dateDayMatchGroup = 3,
            isFallback = true
        )
        assertEquals(1L, template.id)
        assertEquals("Full Template", template.name)
        assertEquals("(\\d+)-(\\d+)-(\\d+)", template.pattern)
        assertEquals("2026-01-16", template.testText)
        assertEquals("Test", template.transactionDescription)
        assertEquals("Comment", template.transactionComment)
        assertEquals(1, template.dateYearMatchGroup)
        assertEquals(2, template.dateMonthMatchGroup)
        assertEquals(3, template.dateDayMatchGroup)
        assertTrue(template.isFallback)
    }

    @Test
    fun `template is data class with copy`() {
        val original = Template(
            id = 1L,
            name = "Original",
            pattern = ".*"
        )
        val modified = original.copy(name = "Modified")
        assertEquals("Original", original.name)
        assertEquals("Modified", modified.name)
        assertEquals(original.id, modified.id)
    }

    // ========================================
    // Match group properties
    // ========================================

    @Test
    fun `template with description match group`() {
        val template = Template(
            name = "Test",
            pattern = "(.+)",
            transactionDescriptionMatchGroup = 1
        )
        assertEquals(1, template.transactionDescriptionMatchGroup)
        assertNull(template.transactionDescription)
    }

    @Test
    fun `template with comment match group`() {
        val template = Template(
            name = "Test",
            pattern = "(.+)",
            transactionCommentMatchGroup = 2
        )
        assertEquals(2, template.transactionCommentMatchGroup)
        assertNull(template.transactionComment)
    }

    // ========================================
    // Date literal properties
    // ========================================

    @Test
    fun `template with date literals`() {
        val template = Template(
            name = "Test",
            pattern = ".*",
            dateYear = 2026,
            dateMonth = 1,
            dateDay = 21
        )
        assertEquals(2026, template.dateYear)
        assertEquals(1, template.dateMonth)
        assertEquals(21, template.dateDay)
    }

    @Test
    fun `template date literals default to null`() {
        val template = Template(
            name = "Test",
            pattern = ".*"
        )
        assertNull(template.dateYear)
        assertNull(template.dateMonth)
        assertNull(template.dateDay)
    }

    @Test
    fun `template with mixed date literals and groups`() {
        val template = Template(
            name = "Test",
            pattern = "(\\d{4})-(\\d{2})",
            dateYear = 2026,
            dateMonthMatchGroup = 1,
            dateDayMatchGroup = 2
        )
        assertEquals(2026, template.dateYear)
        assertNull(template.dateYearMatchGroup)
        assertEquals(1, template.dateMonthMatchGroup)
        assertNull(template.dateMonth)
        assertEquals(2, template.dateDayMatchGroup)
        assertNull(template.dateDay)
    }
}

class TemplateLineTest {

    @Test
    fun `new template line has null id`() {
        val line = TemplateLine(accountName = "Assets:Cash")
        assertNull(line.id)
    }

    @Test
    fun `existing template line has id`() {
        val line = TemplateLine(id = 123L, accountName = "Assets:Cash")
        assertEquals(123L, line.id)
    }

    @Test
    fun `template line defaults`() {
        val line = TemplateLine(accountName = "Assets:Cash")
        assertNull(line.accountNameGroup)
        assertNull(line.amount)
        assertNull(line.amountGroup)
        assertNull(line.currencyId)
        assertNull(line.currencyName)
        assertNull(line.currencyGroup)
        assertNull(line.comment)
        assertNull(line.commentGroup)
        assertFalse(line.negateAmount)
    }

    @Test
    fun `template line with literal values`() {
        val line = TemplateLine(
            accountName = "Assets:Cash",
            amount = 100f,
            currencyId = 5L,
            currencyName = "USD",
            comment = "Payment"
        )
        assertEquals("Assets:Cash", line.accountName)
        assertEquals(100f, line.amount)
        assertEquals(5L, line.currencyId)
        assertEquals("USD", line.currencyName)
        assertEquals("Payment", line.comment)
    }

    @Test
    fun `template line with match groups`() {
        val line = TemplateLine(
            accountNameGroup = 1,
            amountGroup = 2,
            currencyGroup = 3,
            commentGroup = 4,
            negateAmount = true
        )
        assertEquals(1, line.accountNameGroup)
        assertEquals(2, line.amountGroup)
        assertEquals(3, line.currencyGroup)
        assertEquals(4, line.commentGroup)
        assertTrue(line.negateAmount)
    }

    @Test
    fun `template line is data class with copy`() {
        val original = TemplateLine(accountName = "Assets:Cash", amount = 100f)
        val modified = original.copy(amount = 200f)
        assertEquals(100f, original.amount)
        assertEquals(200f, modified.amount)
    }
}
