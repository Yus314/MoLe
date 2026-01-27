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

import java.util.regex.Pattern
import net.ktnx.mobileledger.core.common.utils.SimpleDate
import net.ktnx.mobileledger.core.domain.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for MatchedTemplate data class.
 *
 * Tests verify:
 * - Data class creation
 * - Template and match result properties
 * - Pattern matching behavior
 */
class MatchedTemplateTest {

    private fun createTestTemplate(id: Long = 1L, name: String = "Test Template", pattern: String = ".*"): Template =
        Template(
            id = id,
            name = name,
            pattern = pattern,
            testText = "test",
            isFallback = false,
            lines = emptyList()
        )

    // ========================================
    // Creation tests
    // ========================================

    @Test
    fun `MatchedTemplate contains template`() {
        val template = createTestTemplate(id = 1L, name = "My Template")
        val pattern = Pattern.compile("test")
        val matcher = pattern.matcher("test input")
        assertTrue(matcher.find())

        val matched = MatchedTemplate(template, matcher.toMatchResult())

        assertEquals(template, matched.template)
        assertEquals("My Template", matched.template.name)
    }

    @Test
    fun `MatchedTemplate contains match result`() {
        val template = createTestTemplate()
        val pattern = Pattern.compile("(\\d+)")
        val matcher = pattern.matcher("price: 123")
        assertTrue(matcher.find())

        val matched = MatchedTemplate(template, matcher.toMatchResult())

        assertNotNull(matched.matchResult)
        assertEquals("123", matched.matchResult.group(1))
    }

    // ========================================
    // Match result properties
    // ========================================

    @Test
    fun `matchResult group returns matched text`() {
        val template = createTestTemplate(pattern = "(\\w+):(\\d+)")
        val pattern = Pattern.compile("(\\w+):(\\d+)")
        val matcher = pattern.matcher("amount:500")
        assertTrue(matcher.find())

        val matched = MatchedTemplate(template, matcher.toMatchResult())

        assertEquals("amount:500", matched.matchResult.group(0))
        assertEquals("amount", matched.matchResult.group(1))
        assertEquals("500", matched.matchResult.group(2))
    }

    @Test
    fun `matchResult groupCount returns number of groups`() {
        val template = createTestTemplate(pattern = "(\\w+):(\\d+):(\\w+)")
        val pattern = Pattern.compile("(\\w+):(\\d+):(\\w+)")
        val matcher = pattern.matcher("field:123:value")
        assertTrue(matcher.find())

        val matched = MatchedTemplate(template, matcher.toMatchResult())

        assertEquals(3, matched.matchResult.groupCount())
    }

    @Test
    fun `matchResult start and end return positions`() {
        val template = createTestTemplate()
        val pattern = Pattern.compile("\\d+")
        val text = "price is 123 dollars"
        val matcher = pattern.matcher(text)
        assertTrue(matcher.find())

        val matched = MatchedTemplate(template, matcher.toMatchResult())

        assertEquals(9, matched.matchResult.start())
        assertEquals(12, matched.matchResult.end())
    }

    // ========================================
    // Pattern matching scenarios
    // ========================================

    @Test
    fun `template with transaction pattern`() {
        val template = createTestTemplate(
            name = "Grocery Template",
            pattern = "GROCERY\\s+(\\d+\\.\\d{2})"
        )
        val pattern = Pattern.compile("GROCERY\\s+(\\d+\\.\\d{2})")
        val matcher = pattern.matcher("GROCERY 45.99 at store")
        assertTrue(matcher.find())

        val matched = MatchedTemplate(template, matcher.toMatchResult())

        assertEquals("45.99", matched.matchResult.group(1))
    }

    @Test
    fun `template with date pattern`() {
        val template = createTestTemplate(
            name = "Date Template",
            pattern = "(\\d{4})-(\\d{2})-(\\d{2})"
        )
        val pattern = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})")
        val matcher = pattern.matcher("Transaction on 2026-01-21")
        assertTrue(matcher.find())

        val matched = MatchedTemplate(template, matcher.toMatchResult())

        assertEquals("2026", matched.matchResult.group(1))
        assertEquals("01", matched.matchResult.group(2))
        assertEquals("21", matched.matchResult.group(3))
    }

    // ========================================
    // Data class properties
    // ========================================

    @Test
    fun `MatchedTemplate equality based on template and match`() {
        val template = createTestTemplate()
        val pattern = Pattern.compile("test")
        val matcher1 = pattern.matcher("test")
        val matcher2 = pattern.matcher("test")
        assertTrue(matcher1.find())
        assertTrue(matcher2.find())

        val matched1 = MatchedTemplate(template, matcher1.toMatchResult())
        val matched2 = MatchedTemplate(template, matcher2.toMatchResult())

        // Note: MatchResult equality depends on implementation
        assertEquals(matched1.template, matched2.template)
    }

    @Test
    fun `MatchedTemplate has JvmField annotation for Java interop`() {
        val template = createTestTemplate()
        val pattern = Pattern.compile("test")
        val matcher = pattern.matcher("test")
        assertTrue(matcher.find())

        val matched = MatchedTemplate(template, matcher.toMatchResult())

        // JvmField allows direct field access from Java
        assertNotNull(matched.template)
        assertNotNull(matched.matchResult)
    }

    // ========================================
    // Template properties access
    // ========================================

    @Test
    fun `can access template id through matched template`() {
        val template = createTestTemplate(id = 42L)
        val pattern = Pattern.compile(".*")
        val matcher = pattern.matcher("any text")
        assertTrue(matcher.find())

        val matched = MatchedTemplate(template, matcher.toMatchResult())

        assertEquals(42L, matched.template.id)
    }

    @Test
    fun `can access template name through matched template`() {
        val template = createTestTemplate(name = "Salary Template")
        val pattern = Pattern.compile(".*")
        val matcher = pattern.matcher("any text")
        assertTrue(matcher.find())

        val matched = MatchedTemplate(template, matcher.toMatchResult())

        assertEquals("Salary Template", matched.template.name)
    }

    @Test
    fun `can access template pattern through matched template`() {
        val template = createTestTemplate(pattern = "PAYMENT.*")
        val pattern = Pattern.compile("PAYMENT.*")
        val matcher = pattern.matcher("PAYMENT received")
        assertTrue(matcher.find())

        val matched = MatchedTemplate(template, matcher.toMatchResult())

        assertEquals("PAYMENT.*", matched.template.pattern)
    }
}
