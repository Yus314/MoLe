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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [TemplateDataMapperImpl].
 *
 * Tests verify:
 * - Conversion from UI state to Template domain model
 * - Conversion from Template to account rows
 * - MatchableValue extraction logic
 */
class TemplateDataMapperImplTest {

    private lateinit var mapper: TemplateDataMapperImpl

    @Before
    fun setup() {
        mapper = TemplateDataMapperImpl()
    }

    // ========================================
    // toTemplate tests
    // ========================================

    @Test
    fun `toTemplate converts basic fields`() {
        // Given
        val state = TemplateDetailUiState(
            templateId = 1L,
            name = "Test Template",
            pattern = "\\d+",
            testText = "123",
            isFallback = true
        )

        // When
        val template = mapper.toTemplate(state)

        // Then
        assertEquals(1L, template.id)
        assertEquals("Test Template", template.name)
        assertEquals("\\d+", template.pattern)
        assertEquals("123", template.testText)
        assertTrue(template.isFallback)
    }

    @Test
    fun `toTemplate converts literal description`() {
        // Given
        val state = TemplateDetailUiState(
            name = "Test",
            pattern = ".*",
            transactionDescription = MatchableValue.Literal("Payment")
        )

        // When
        val template = mapper.toTemplate(state)

        // Then
        assertEquals("Payment", template.transactionDescription)
        assertNull(template.transactionDescriptionMatchGroup)
    }

    @Test
    fun `toTemplate converts match group description`() {
        // Given
        val state = TemplateDetailUiState(
            name = "Test",
            pattern = ".*",
            transactionDescription = MatchableValue.MatchGroup(2)
        )

        // When
        val template = mapper.toTemplate(state)

        // Then
        assertNull(template.transactionDescription)
        assertEquals(2, template.transactionDescriptionMatchGroup)
    }

    @Test
    fun `toTemplate converts date fields from literal`() {
        // Given
        val state = TemplateDetailUiState(
            name = "Test",
            pattern = ".*",
            dateYear = MatchableValue.Literal("2024"),
            dateMonth = MatchableValue.Literal("12"),
            dateDay = MatchableValue.Literal("25")
        )

        // When
        val template = mapper.toTemplate(state)

        // Then
        assertEquals(2024, template.dateYear)
        assertEquals(12, template.dateMonth)
        assertEquals(25, template.dateDay)
    }

    @Test
    fun `toTemplate converts date fields from match groups`() {
        // Given
        val state = TemplateDetailUiState(
            name = "Test",
            pattern = ".*",
            dateYear = MatchableValue.MatchGroup(1),
            dateMonth = MatchableValue.MatchGroup(2),
            dateDay = MatchableValue.MatchGroup(3)
        )

        // When
        val template = mapper.toTemplate(state)

        // Then
        assertNull(template.dateYear)
        assertEquals(1, template.dateYearMatchGroup)
        assertNull(template.dateMonth)
        assertEquals(2, template.dateMonthMatchGroup)
        assertNull(template.dateDay)
        assertEquals(3, template.dateDayMatchGroup)
    }

    @Test
    fun `toTemplate converts account rows`() {
        // Given
        val state = TemplateDetailUiState(
            name = "Test",
            pattern = ".*",
            accounts = listOf(
                TemplateAccountRow(
                    id = 1,
                    position = 0,
                    accountName = MatchableValue.Literal("Assets:Bank"),
                    amount = MatchableValue.Literal("100.0"),
                    negateAmount = false
                ),
                TemplateAccountRow(
                    id = 2,
                    position = 1,
                    accountName = MatchableValue.Literal("Expenses:Food"),
                    amount = MatchableValue.Literal("100.0"),
                    negateAmount = true
                )
            )
        )

        // When
        val template = mapper.toTemplate(state)

        // Then
        assertEquals(2, template.lines.size)
        assertEquals("Assets:Bank", template.lines[0].accountName)
        assertEquals("Expenses:Food", template.lines[1].accountName)
        assertFalse(template.lines[0].negateAmount)
        assertTrue(template.lines[1].negateAmount)
    }

    @Test
    fun `toTemplate filters empty rows after minimum`() {
        // Given
        val state = TemplateDetailUiState(
            name = "Test",
            pattern = ".*",
            accounts = listOf(
                TemplateAccountRow(id = 1, accountName = MatchableValue.Literal("Assets:Bank")),
                TemplateAccountRow(id = 2), // empty row at position 1 - kept (minimum 2)
                TemplateAccountRow(id = 3) // empty row at position 2 - filtered
            )
        )

        // When
        val template = mapper.toTemplate(state)

        // Then
        assertEquals(2, template.lines.size)
    }

    @Test
    fun `toTemplate trims whitespace from name`() {
        // Given
        val state = TemplateDetailUiState(
            name = "  Test Template  ",
            pattern = ".*"
        )

        // When
        val template = mapper.toTemplate(state)

        // Then
        assertEquals("Test Template", template.name)
    }

    @Test
    fun `toTemplate handles empty testText`() {
        // Given
        val state = TemplateDetailUiState(
            name = "Test",
            pattern = ".*",
            testText = ""
        )

        // When
        val template = mapper.toTemplate(state)

        // Then
        assertNull(template.testText)
    }

    // ========================================
    // toAccountRows tests
    // ========================================

    @Test
    fun `toAccountRows converts template lines`() {
        // Given
        var nextId = 1L
        val template = Template(
            name = "Test",
            pattern = ".*",
            lines = listOf(
                TemplateLine(id = 10L, accountName = "Assets:Bank", amount = 100.0f),
                TemplateLine(id = 20L, accountName = "Expenses:Food", amount = 100.0f)
            )
        )

        // When
        val rows = mapper.toAccountRows(template) { nextId++ }

        // Then
        assertEquals(2, rows.size)
        assertEquals(10L, rows[0].id)
        assertEquals("Assets:Bank", rows[0].accountName.getLiteralValue())
        assertEquals(20L, rows[1].id)
        assertEquals("Expenses:Food", rows[1].accountName.getLiteralValue())
    }

    @Test
    fun `toAccountRows generates minimum rows for empty template`() {
        // Given
        var nextId = 1L
        val template = Template(name = "Test", pattern = ".*", lines = emptyList())

        // When
        val rows = mapper.toAccountRows(template) { nextId++ }

        // Then
        assertEquals(2, rows.size) // Minimum 2 rows
    }

    @Test
    fun `toAccountRows generates ID for line without ID`() {
        // Given
        var nextId = 100L
        val template = Template(
            name = "Test",
            pattern = ".*",
            lines = listOf(TemplateLine(id = null, accountName = "Assets:Bank"))
        )

        // When
        val rows = mapper.toAccountRows(template) { nextId++ }

        // Then
        assertEquals(100L, rows[0].id)
    }

    @Test
    fun `toAccountRows sets position correctly`() {
        // Given
        var nextId = 1L
        val template = Template(
            name = "Test",
            pattern = ".*",
            lines = listOf(
                TemplateLine(accountName = "A"),
                TemplateLine(accountName = "B"),
                TemplateLine(accountName = "C")
            )
        )

        // When
        val rows = mapper.toAccountRows(template) { nextId++ }

        // Then
        assertEquals(0, rows[0].position)
        assertEquals(1, rows[1].position)
        assertEquals(2, rows[2].position)
    }

    // ========================================
    // extractMatchableValue tests
    // ========================================

    @Test
    fun `extractMatchableValue returns Literal for null matchGroup`() {
        // When
        val result = mapper.extractMatchableValue("test", null)

        // Then
        assertTrue(result.isLiteral())
        assertEquals("test", result.getLiteralValue())
    }

    @Test
    fun `extractMatchableValue returns Literal for zero matchGroup`() {
        // When
        val result = mapper.extractMatchableValue("test", 0)

        // Then
        assertTrue(result.isLiteral())
        assertEquals("test", result.getLiteralValue())
    }

    @Test
    fun `extractMatchableValue returns MatchGroup for positive matchGroup`() {
        // When
        val result = mapper.extractMatchableValue("test", 3)

        // Then
        assertTrue(result.isMatchGroup())
        assertEquals(3, result.getMatchGroup())
    }

    @Test
    fun `extractMatchableValue returns empty Literal for null literal and null matchGroup`() {
        // When
        val result = mapper.extractMatchableValue(null, null)

        // Then
        assertTrue(result.isLiteral())
        assertEquals("", result.getLiteralValue())
    }

    @Test
    fun `extractMatchableValueInt returns Literal for int value`() {
        // When
        val result = mapper.extractMatchableValueInt(42, null)

        // Then
        assertTrue(result.isLiteral())
        assertEquals("42", result.getLiteralValue())
    }

    @Test
    fun `extractMatchableValueFloat returns Literal for float value`() {
        // When
        val result = mapper.extractMatchableValueFloat(3.14f, null)

        // Then
        assertTrue(result.isLiteral())
        assertEquals("3.14", result.getLiteralValue())
    }

    @Test
    fun `extractMatchableValueCurrency returns Literal for currency ID`() {
        // When
        val result = mapper.extractMatchableValueCurrency(1L, null)

        // Then
        assertTrue(result.isLiteral())
        assertEquals("1", result.getLiteralValue())
    }

    @Test
    fun `extractMatchableValueCurrency returns MatchGroup when matchGroup set`() {
        // When
        val result = mapper.extractMatchableValueCurrency(1L, 5)

        // Then
        assertTrue(result.isMatchGroup())
        assertEquals(5, result.getMatchGroup())
    }
}
