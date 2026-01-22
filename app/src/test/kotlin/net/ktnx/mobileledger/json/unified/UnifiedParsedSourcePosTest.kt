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

package net.ktnx.mobileledger.json.unified

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [UnifiedParsedSourcePos].
 *
 * Tests verify:
 * - Default values
 * - Property accessors
 * - JSON deserialization for v1_32+ format
 */
class UnifiedParsedSourcePosTest {

    // ========================================
    // Default values tests
    // ========================================

    @Test
    fun `default sourceName is empty string`() {
        val sourcePos = UnifiedParsedSourcePos()
        assertEquals("", sourcePos.sourceName)
    }

    @Test
    fun `default sourceLine is 1`() {
        val sourcePos = UnifiedParsedSourcePos()
        assertEquals(1, sourcePos.sourceLine)
    }

    @Test
    fun `default sourceColumn is 1`() {
        val sourcePos = UnifiedParsedSourcePos()
        assertEquals(1, sourcePos.sourceColumn)
    }

    // ========================================
    // Property setter tests
    // ========================================

    @Test
    fun `sourceName can be set`() {
        val sourcePos = UnifiedParsedSourcePos()
        sourcePos.sourceName = "myfile.journal"
        assertEquals("myfile.journal", sourcePos.sourceName)
    }

    @Test
    fun `sourceLine can be set`() {
        val sourcePos = UnifiedParsedSourcePos()
        sourcePos.sourceLine = 42
        assertEquals(42, sourcePos.sourceLine)
    }

    @Test
    fun `sourceColumn can be set`() {
        val sourcePos = UnifiedParsedSourcePos()
        sourcePos.sourceColumn = 15
        assertEquals(15, sourcePos.sourceColumn)
    }

    // ========================================
    // JSON deserialization tests
    // ========================================

    @Test
    fun `deserialize with sourceName and sourceLine`() {
        val mapper = ObjectMapper()
        val json = """
            {
                "sourceName": "accounts.journal",
                "sourceLine": 100,
                "sourceColumn": 5
            }
        """.trimIndent()

        val sourcePos = mapper.readValue(json, UnifiedParsedSourcePos::class.java)

        assertEquals("accounts.journal", sourcePos.sourceName)
        assertEquals(100, sourcePos.sourceLine)
        assertEquals(5, sourcePos.sourceColumn)
    }

    @Test
    fun `deserialize ignores unknown properties`() {
        val mapper = ObjectMapper()
        val json = """
            {
                "sourceName": "test.journal",
                "sourceLine": 1,
                "unknownField": "value",
                "anotherUnknown": 123
            }
        """.trimIndent()

        // Should not throw exception
        val sourcePos = mapper.readValue(json, UnifiedParsedSourcePos::class.java)
        assertEquals("test.journal", sourcePos.sourceName)
    }
}
