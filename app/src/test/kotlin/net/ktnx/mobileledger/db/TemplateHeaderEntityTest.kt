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

package net.ktnx.mobileledger.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [TemplateHeader] Room entity.
 */
class TemplateHeaderEntityTest {

    @Test
    fun `primary constructor sets id, name, and regularExpression`() {
        val template = TemplateHeader(1L, "Test Template", ".*pattern.*")

        assertEquals(1L, template.id)
        assertEquals("Test Template", template.name)
        assertEquals(".*pattern.*", template.regularExpression)
        assertNotNull(template.uuid)
    }

    @Test
    fun `primary constructor generates new uuid`() {
        val template1 = TemplateHeader(1L, "Test 1", "pattern1")
        val template2 = TemplateHeader(2L, "Test 2", "pattern2")

        assertNotEquals(template1.uuid, template2.uuid)
    }

    @Test
    fun `copy constructor copies all fields`() {
        val original = TemplateHeader(1L, "Original", "pattern")
        original.testText = "test input"
        original.transactionDescription = "description"
        original.transactionDescriptionMatchGroup = 1
        original.transactionComment = "comment"
        original.transactionCommentMatchGroup = 2
        original.dateYear = 2026
        original.dateYearMatchGroup = 3
        original.dateMonth = 1
        original.dateMonthMatchGroup = 4
        original.dateDay = 21
        original.dateDayMatchGroup = 5
        original.isFallback = true

        val copy = TemplateHeader(original)

        assertEquals(original.id, copy.id)
        assertEquals(original.name, copy.name)
        assertEquals(original.uuid, copy.uuid)
        assertEquals(original.regularExpression, copy.regularExpression)
        assertEquals(original.testText, copy.testText)
        assertEquals(original.transactionDescription, copy.transactionDescription)
        assertEquals(original.transactionDescriptionMatchGroup, copy.transactionDescriptionMatchGroup)
        assertEquals(original.transactionComment, copy.transactionComment)
        assertEquals(original.transactionCommentMatchGroup, copy.transactionCommentMatchGroup)
        assertEquals(original.dateYear, copy.dateYear)
        assertEquals(original.dateYearMatchGroup, copy.dateYearMatchGroup)
        assertEquals(original.dateMonth, copy.dateMonth)
        assertEquals(original.dateMonthMatchGroup, copy.dateMonthMatchGroup)
        assertEquals(original.dateDay, copy.dateDay)
        assertEquals(original.dateDayMatchGroup, copy.dateDayMatchGroup)
        assertEquals(original.isFallback, copy.isFallback)
    }

    @Test
    fun `createDuplicate creates copy with id 0 and new uuid`() {
        val original = TemplateHeader(42L, "Original", "pattern")
        original.transactionDescription = "description"
        original.dateYear = 2026

        val duplicate = original.createDuplicate()

        assertEquals(0L, duplicate.id)
        assertNotEquals(original.uuid, duplicate.uuid)
        assertEquals(original.name, duplicate.name)
        assertEquals(original.regularExpression, duplicate.regularExpression)
        assertEquals(original.transactionDescription, duplicate.transactionDescription)
        assertEquals(original.dateYear, duplicate.dateYear)
    }

    @Test
    fun `equals returns true for same values`() {
        val template1 = TemplateHeader(1L, "Test", "pattern")
        template1.transactionDescription = "desc"
        template1.dateYear = 2026

        val template2 = TemplateHeader(1L, "Test", "pattern")
        template2.transactionDescription = "desc"
        template2.dateYear = 2026

        assertEquals(template1, template2)
    }

    @Test
    fun `equals returns false for different ids`() {
        val template1 = TemplateHeader(1L, "Test", "pattern")
        val template2 = TemplateHeader(2L, "Test", "pattern")

        assertNotEquals(template1, template2)
    }

    @Test
    fun `equals returns false for different names`() {
        val template1 = TemplateHeader(1L, "Name 1", "pattern")
        val template2 = TemplateHeader(1L, "Name 2", "pattern")

        assertNotEquals(template1, template2)
    }

    @Test
    fun `equals returns false for different regularExpression`() {
        val template1 = TemplateHeader(1L, "Test", "pattern1")
        val template2 = TemplateHeader(1L, "Test", "pattern2")

        assertNotEquals(template1, template2)
    }

    @Test
    fun `equals returns false for different transactionDescription`() {
        val template1 = TemplateHeader(1L, "Test", "pattern")
        template1.transactionDescription = "desc1"

        val template2 = TemplateHeader(1L, "Test", "pattern")
        template2.transactionDescription = "desc2"

        assertNotEquals(template1, template2)
    }

    @Test
    fun `equals returns false for different transactionDescriptionMatchGroup`() {
        val template1 = TemplateHeader(1L, "Test", "pattern")
        template1.transactionDescriptionMatchGroup = 1

        val template2 = TemplateHeader(1L, "Test", "pattern")
        template2.transactionDescriptionMatchGroup = 2

        assertNotEquals(template1, template2)
    }

    @Test
    fun `equals returns false for different transactionComment`() {
        val template1 = TemplateHeader(1L, "Test", "pattern")
        template1.transactionComment = "comment1"

        val template2 = TemplateHeader(1L, "Test", "pattern")
        template2.transactionComment = "comment2"

        assertNotEquals(template1, template2)
    }

    @Test
    fun `equals returns false for different dateYear`() {
        val template1 = TemplateHeader(1L, "Test", "pattern")
        template1.dateYear = 2025

        val template2 = TemplateHeader(1L, "Test", "pattern")
        template2.dateYear = 2026

        assertNotEquals(template1, template2)
    }

    @Test
    fun `equals returns false for different dateMonth`() {
        val template1 = TemplateHeader(1L, "Test", "pattern")
        template1.dateMonth = 1

        val template2 = TemplateHeader(1L, "Test", "pattern")
        template2.dateMonth = 2

        assertNotEquals(template1, template2)
    }

    @Test
    fun `equals returns false for different dateDay`() {
        val template1 = TemplateHeader(1L, "Test", "pattern")
        template1.dateDay = 1

        val template2 = TemplateHeader(1L, "Test", "pattern")
        template2.dateDay = 2

        assertNotEquals(template1, template2)
    }

    @Test
    fun `equals returns false for null`() {
        val template = TemplateHeader(1L, "Test", "pattern")

        assertFalse(template.equals(null))
    }

    @Test
    fun `equals returns false for non-TemplateHeader object`() {
        val template = TemplateHeader(1L, "Test", "pattern")

        assertFalse(template.equals("not a template"))
    }

    @Test
    fun `hashCode is consistent for equal objects`() {
        val template1 = TemplateHeader(1L, "Test", "pattern")
        template1.uuid = "same-uuid"

        val template2 = TemplateHeader(1L, "Test", "pattern")
        template2.uuid = "same-uuid"

        assertEquals(template1.hashCode(), template2.hashCode())
    }

    @Test
    fun `hashCode differs for different objects`() {
        val template1 = TemplateHeader(1L, "Test 1", "pattern1")
        val template2 = TemplateHeader(2L, "Test 2", "pattern2")

        assertNotEquals(template1.hashCode(), template2.hashCode())
    }

    @Test
    fun `default values for optional fields are null`() {
        val template = TemplateHeader(1L, "Test", "pattern")

        assertNull(template.testText)
        assertNull(template.transactionDescription)
        assertNull(template.transactionDescriptionMatchGroup)
        assertNull(template.transactionComment)
        assertNull(template.transactionCommentMatchGroup)
        assertNull(template.dateYear)
        assertNull(template.dateYearMatchGroup)
        assertNull(template.dateMonth)
        assertNull(template.dateMonthMatchGroup)
        assertNull(template.dateDay)
        assertNull(template.dateDayMatchGroup)
        assertFalse(template.isFallback)
    }

    @Test
    fun `all optional fields can be set`() {
        val template = TemplateHeader(1L, "Test", "pattern")

        template.testText = "test input"
        template.transactionDescription = "description"
        template.transactionDescriptionMatchGroup = 1
        template.transactionComment = "comment"
        template.transactionCommentMatchGroup = 2
        template.dateYear = 2026
        template.dateYearMatchGroup = 3
        template.dateMonth = 1
        template.dateMonthMatchGroup = 4
        template.dateDay = 21
        template.dateDayMatchGroup = 5
        template.isFallback = true

        assertEquals("test input", template.testText)
        assertEquals("description", template.transactionDescription)
        assertEquals(1, template.transactionDescriptionMatchGroup)
        assertEquals("comment", template.transactionComment)
        assertEquals(2, template.transactionCommentMatchGroup)
        assertEquals(2026, template.dateYear)
        assertEquals(3, template.dateYearMatchGroup)
        assertEquals(1, template.dateMonth)
        assertEquals(4, template.dateMonthMatchGroup)
        assertEquals(21, template.dateDay)
        assertEquals(5, template.dateDayMatchGroup)
        assertTrue(template.isFallback)
    }

    @Test
    fun `equals handles null transactionDescription correctly`() {
        val template1 = TemplateHeader(1L, "Test", "pattern")
        template1.transactionDescription = null

        val template2 = TemplateHeader(1L, "Test", "pattern")
        template2.transactionDescription = null

        assertEquals(template1, template2)
    }

    @Test
    fun `equals handles null match groups correctly`() {
        val template1 = TemplateHeader(1L, "Test", "pattern")
        template1.transactionDescriptionMatchGroup = null
        template1.dateYearMatchGroup = null

        val template2 = TemplateHeader(1L, "Test", "pattern")
        template2.transactionDescriptionMatchGroup = null
        template2.dateYearMatchGroup = null

        assertEquals(template1, template2)
    }
}
