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
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [UnifiedParsedSourcePos].
 *
 * Tests verify:
 * - Default values
 * - v1_14-v1_40 format (tag + contents)
 * - v1_50 format (sourceName + sourceLine + sourceColumn)
 * - JSON deserialization
 */
class UnifiedParsedSourcePosTest {

    // ========================================
    // Default values tests
    // ========================================

    @Test
    fun `default tag is JournalSourcePos`() {
        val sourcePos = UnifiedParsedSourcePos()
        assertEquals("JournalSourcePos", sourcePos.tag)
    }

    @Test
    fun `default contents has two elements`() {
        val sourcePos = UnifiedParsedSourcePos()
        assertEquals(2, sourcePos.contents.size)
    }

    @Test
    fun `default contents first element is empty string`() {
        val sourcePos = UnifiedParsedSourcePos()
        assertEquals("", sourcePos.contents[0])
    }

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
    // Property setter tests (v1_14-v1_40 format)
    // ========================================

    @Test
    fun `tag can be set`() {
        val sourcePos = UnifiedParsedSourcePos()
        sourcePos.tag = "CustomTag"
        assertEquals("CustomTag", sourcePos.tag)
    }

    @Test
    fun `contents can be set`() {
        val sourcePos = UnifiedParsedSourcePos()
        sourcePos.contents = mutableListOf("test.journal", arrayOf(10, 5))
        assertEquals("test.journal", sourcePos.contents[0])
    }

    // ========================================
    // Property setter tests (v1_50 format)
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
    // JSON deserialization tests (v1_14-v1_40 format)
    // ========================================

    @Test
    fun `deserialize v1_14_40 format with tag and contents`() {
        val mapper = ObjectMapper()
        val json = """
            {
                "tag": "JournalSourcePos",
                "contents": ["ledger.journal", [5, 1]]
            }
        """.trimIndent()

        val sourcePos = mapper.readValue(json, UnifiedParsedSourcePos::class.java)

        assertEquals("JournalSourcePos", sourcePos.tag)
        assertEquals(2, sourcePos.contents.size)
        assertEquals("ledger.journal", sourcePos.contents[0])
    }

    @Test
    fun `deserialize with GenericSourcePos tag`() {
        val mapper = ObjectMapper()
        val json = """
            {
                "tag": "GenericSourcePos"
            }
        """.trimIndent()

        val sourcePos = mapper.readValue(json, UnifiedParsedSourcePos::class.java)

        assertEquals("GenericSourcePos", sourcePos.tag)
    }

    // ========================================
    // JSON deserialization tests (v1_50 format)
    // ========================================

    @Test
    fun `deserialize v1_50 format with sourceName and sourceLine`() {
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
                "tag": "JournalSourcePos",
                "unknownField": "value",
                "anotherUnknown": 123
            }
        """.trimIndent()

        // Should not throw exception
        val sourcePos = mapper.readValue(json, UnifiedParsedSourcePos::class.java)
        assertEquals("JournalSourcePos", sourcePos.tag)
    }

    // ========================================
    // Mixed format tests
    // ========================================

    @Test
    fun `deserialize mixed format with both styles`() {
        val mapper = ObjectMapper()
        val json = """
            {
                "tag": "JournalSourcePos",
                "contents": ["main.journal", [1, 1]],
                "sourceName": "main.journal",
                "sourceLine": 1,
                "sourceColumn": 1
            }
        """.trimIndent()

        val sourcePos = mapper.readValue(json, UnifiedParsedSourcePos::class.java)

        assertEquals("JournalSourcePos", sourcePos.tag)
        assertEquals("main.journal", sourcePos.sourceName)
        assertEquals(1, sourcePos.sourceLine)
    }

    // ========================================
    // Edge case tests
    // ========================================

    @Test
    fun `contents can hold nested array`() {
        val sourcePos = UnifiedParsedSourcePos()
        val lineCol = arrayOf(99, 15)
        sourcePos.contents = mutableListOf("file.journal", lineCol)

        assertEquals(2, sourcePos.contents.size)
        assertTrue(sourcePos.contents[1] is Array<*>)
    }

    @Test
    fun `can modify contents list`() {
        val sourcePos = UnifiedParsedSourcePos()
        sourcePos.contents.clear()
        sourcePos.contents.add("new-file.journal")
        sourcePos.contents.add(arrayOf(50, 10))

        assertEquals(2, sourcePos.contents.size)
        assertEquals("new-file.journal", sourcePos.contents[0])
    }
}
