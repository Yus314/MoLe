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

package net.ktnx.mobileledger.json.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [TransactionFieldDelegate].
 */
class TransactionFieldDelegateTest {

    @Test
    fun `default values are correct`() {
        val delegate = TransactionFieldDelegate()

        assertNull(delegate.tdate)
        assertNull(delegate.tdate2)
        assertNull(delegate.tdescription)
        assertNull(delegate.tcomment)
        assertEquals("", delegate.tcode)
        assertEquals("Unmarked", delegate.tstatus)
        assertEquals("", delegate.tprecedingcomment)
        assertTrue(delegate.ttags.isEmpty())
    }

    @Test
    fun `can set and get tdate`() {
        val delegate = TransactionFieldDelegate()

        delegate.tdate = "2026-01-21"

        assertEquals("2026-01-21", delegate.tdate)
    }

    @Test
    fun `can set and get tdate2`() {
        val delegate = TransactionFieldDelegate()

        delegate.tdate2 = "2026-01-22"

        assertEquals("2026-01-22", delegate.tdate2)
    }

    @Test
    fun `can set and get tdescription`() {
        val delegate = TransactionFieldDelegate()

        delegate.tdescription = "Test Transaction"

        assertEquals("Test Transaction", delegate.tdescription)
    }

    @Test
    fun `can set and get tcomment`() {
        val delegate = TransactionFieldDelegate()

        delegate.tcomment = "A comment"

        assertEquals("A comment", delegate.tcomment)
    }

    @Test
    fun `can set and get tcode`() {
        val delegate = TransactionFieldDelegate()

        delegate.tcode = "TX001"

        assertEquals("TX001", delegate.tcode)
    }

    @Test
    fun `can set and get tstatus`() {
        val delegate = TransactionFieldDelegate()

        delegate.tstatus = "Cleared"

        assertEquals("Cleared", delegate.tstatus)
    }

    @Test
    fun `can set and get tprecedingcomment`() {
        val delegate = TransactionFieldDelegate()

        delegate.tprecedingcomment = "Preceding comment"

        assertEquals("Preceding comment", delegate.tprecedingcomment)
    }

    @Test
    fun `can set and get ttags`() {
        val delegate = TransactionFieldDelegate()
        val tags = mutableListOf(listOf("tag1", "value1"), listOf("tag2", "value2"))

        delegate.ttags = tags

        assertEquals(2, delegate.ttags.size)
        assertEquals("tag1", delegate.ttags[0][0])
        assertEquals("value1", delegate.ttags[0][1])
    }
}
