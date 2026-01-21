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
 * Unit tests for [PostingFieldDelegate].
 */
class PostingFieldDelegateTest {

    @Test
    fun `default values are set correctly`() {
        // Given
        val delegate = PostingFieldDelegate()

        // Then
        assertNull(delegate.pbalanceassertion)
        assertEquals("Unmarked", delegate.pstatus)
        assertNull(delegate.paccount)
        assertNull(delegate.pdate)
        assertNull(delegate.pdate2)
        assertEquals("RegularPosting", delegate.ptype)
        assertNull(delegate.poriginal)
        assertEquals("", delegate.pcomment)
        assertTrue(delegate.ptags.isEmpty())
    }

    @Test
    fun `pcomment setter trims whitespace`() {
        // Given
        val delegate = PostingFieldDelegate()

        // When
        delegate.pcomment = "  test comment  "

        // Then
        assertEquals("test comment", delegate.pcomment)
    }

    @Test
    fun `pcomment setter handles empty string`() {
        // Given
        val delegate = PostingFieldDelegate()

        // When
        delegate.pcomment = ""

        // Then
        assertEquals("", delegate.pcomment)
    }

    @Test
    fun `pcomment setter handles whitespace only`() {
        // Given
        val delegate = PostingFieldDelegate()

        // When
        delegate.pcomment = "   "

        // Then
        assertEquals("", delegate.pcomment)
    }

    @Test
    fun `paccount can be set and retrieved`() {
        // Given
        val delegate = PostingFieldDelegate()

        // When
        delegate.paccount = "assets:cash"

        // Then
        assertEquals("assets:cash", delegate.paccount)
    }

    @Test
    fun `pstatus can be changed`() {
        // Given
        val delegate = PostingFieldDelegate()

        // When
        delegate.pstatus = "Cleared"

        // Then
        assertEquals("Cleared", delegate.pstatus)
    }

    @Test
    fun `ptags is mutable list`() {
        // Given
        val delegate = PostingFieldDelegate()

        // When
        delegate.ptags.add(listOf("key", "value"))

        // Then
        assertEquals(1, delegate.ptags.size)
        assertEquals("key", delegate.ptags[0][0])
        assertEquals("value", delegate.ptags[0][1])
    }
}
