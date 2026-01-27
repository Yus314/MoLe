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

import net.ktnx.mobileledger.core.common.utils.SimpleDate
import net.ktnx.mobileledger.core.domain.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [Progress].
 *
 * Tests verify:
 * - Constructor validation
 * - Percentage calculations
 * - isComplete property
 */
class ProgressTest {

    // ========================================
    // Constructor tests
    // ========================================

    @Test
    fun `default constructor creates zero progress`() {
        val progress = Progress()
        assertEquals(0, progress.current)
        assertNull(progress.total)
        assertEquals("", progress.message)
    }

    @Test
    fun `constructor with current only`() {
        val progress = Progress(current = 5)
        assertEquals(5, progress.current)
        assertNull(progress.total)
    }

    @Test
    fun `constructor with current and total`() {
        val progress = Progress(current = 5, total = 10)
        assertEquals(5, progress.current)
        assertEquals(10, progress.total)
    }

    @Test
    fun `constructor with message`() {
        val progress = Progress(message = "Loading...")
        assertEquals("Loading...", progress.message)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `constructor rejects negative current`() {
        Progress(current = -1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `constructor rejects negative total`() {
        Progress(current = 0, total = -1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `constructor rejects current exceeding total`() {
        Progress(current = 10, total = 5)
    }

    @Test
    fun `constructor allows current equal to total`() {
        val progress = Progress(current = 10, total = 10)
        assertEquals(10, progress.current)
        assertEquals(10, progress.total)
    }

    @Test
    fun `constructor allows zero total with zero current`() {
        val progress = Progress(current = 0, total = 0)
        assertEquals(0, progress.current)
        assertEquals(0, progress.total)
    }

    // ========================================
    // percentage tests
    // ========================================

    @Test
    fun `percentage returns null when total is null`() {
        val progress = Progress(current = 5)
        assertNull(progress.percentage)
    }

    @Test
    fun `percentage returns null when total is zero`() {
        val progress = Progress(current = 0, total = 0)
        assertNull(progress.percentage)
    }

    @Test
    fun `percentage returns 0 for 0 out of 10`() {
        val progress = Progress(current = 0, total = 10)
        assertEquals(0.0f, progress.percentage!!, 0.001f)
    }

    @Test
    fun `percentage returns 0_5 for 5 out of 10`() {
        val progress = Progress(current = 5, total = 10)
        assertEquals(0.5f, progress.percentage!!, 0.001f)
    }

    @Test
    fun `percentage returns 1 for complete progress`() {
        val progress = Progress(current = 10, total = 10)
        assertEquals(1.0f, progress.percentage!!, 0.001f)
    }

    @Test
    fun `percentage handles non-round fractions`() {
        val progress = Progress(current = 1, total = 3)
        assertEquals(0.333f, progress.percentage!!, 0.01f)
    }

    // ========================================
    // percentInt tests
    // ========================================

    @Test
    fun `percentInt returns null when total is null`() {
        val progress = Progress(current = 5)
        assertNull(progress.percentInt)
    }

    @Test
    fun `percentInt returns 0 for start`() {
        val progress = Progress(current = 0, total = 100)
        assertEquals(0, progress.percentInt)
    }

    @Test
    fun `percentInt returns 50 for half`() {
        val progress = Progress(current = 50, total = 100)
        assertEquals(50, progress.percentInt)
    }

    @Test
    fun `percentInt returns 100 for complete`() {
        val progress = Progress(current = 100, total = 100)
        assertEquals(100, progress.percentInt)
    }

    @Test
    fun `percentInt truncates decimals`() {
        val progress = Progress(current = 1, total = 3)
        assertEquals(33, progress.percentInt) // 33.33% -> 33
    }

    // ========================================
    // isComplete tests
    // ========================================

    @Test
    fun `isComplete returns false when total is null`() {
        val progress = Progress(current = 5)
        assertFalse(progress.isComplete)
    }

    @Test
    fun `isComplete returns false when current is less than total`() {
        val progress = Progress(current = 5, total = 10)
        assertFalse(progress.isComplete)
    }

    @Test
    fun `isComplete returns true when current equals total`() {
        val progress = Progress(current = 10, total = 10)
        assertTrue(progress.isComplete)
    }

    @Test
    fun `isComplete returns true for zero total and zero current`() {
        val progress = Progress(current = 0, total = 0)
        assertTrue(progress.isComplete)
    }
}
