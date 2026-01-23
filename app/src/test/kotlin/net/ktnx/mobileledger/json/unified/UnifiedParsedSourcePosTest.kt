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

import net.ktnx.mobileledger.json.MoLeJson
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [UnifiedParsedSourcePos].
 *
 * Tests verify:
 * - Default values
 * - Constructor initialization
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
    // Constructor initialization tests
    // ========================================

    @Test
    fun `can create sourcePos with sourceName`() {
        val sourcePos = UnifiedParsedSourcePos(sourceName = "myfile.journal")
        assertEquals("myfile.journal", sourcePos.sourceName)
    }

    @Test
    fun `can create sourcePos with sourceLine`() {
        val sourcePos = UnifiedParsedSourcePos(sourceLine = 42)
        assertEquals(42, sourcePos.sourceLine)
    }

    @Test
    fun `can create sourcePos with sourceColumn`() {
        val sourcePos = UnifiedParsedSourcePos(sourceColumn = 15)
        assertEquals(15, sourcePos.sourceColumn)
    }

    @Test
    fun `can create complete sourcePos`() {
        val sourcePos = UnifiedParsedSourcePos(
            sourceName = "accounts.journal",
            sourceLine = 100,
            sourceColumn = 5
        )

        assertEquals("accounts.journal", sourcePos.sourceName)
        assertEquals(100, sourcePos.sourceLine)
        assertEquals(5, sourcePos.sourceColumn)
    }

    // ========================================
    // JSON deserialization tests
    // ========================================

    @Test
    fun `deserialize with sourceName and sourceLine`() {
        val json = """
            {
                "sourceName": "accounts.journal",
                "sourceLine": 100,
                "sourceColumn": 5
            }
        """.trimIndent()

        val sourcePos = MoLeJson.decodeFromString<UnifiedParsedSourcePos>(json)

        assertEquals("accounts.journal", sourcePos.sourceName)
        assertEquals(100, sourcePos.sourceLine)
        assertEquals(5, sourcePos.sourceColumn)
    }

    @Test
    fun `deserialize ignores unknown properties`() {
        val json = """
            {
                "sourceName": "test.journal",
                "sourceLine": 1,
                "unknownField": "value",
                "anotherUnknown": 123
            }
        """.trimIndent()

        // Should not throw exception
        val sourcePos = MoLeJson.decodeFromString<UnifiedParsedSourcePos>(json)
        assertEquals("test.journal", sourcePos.sourceName)
    }
}
