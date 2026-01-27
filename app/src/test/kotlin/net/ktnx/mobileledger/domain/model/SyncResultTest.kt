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
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for SyncResult data class.
 *
 * Tests verify:
 * - Data class properties
 * - summaryMessage generation
 * - Equality and copy
 */
class SyncResultTest {

    // ========================================
    // Property tests
    // ========================================

    @Test
    fun `SyncResult has transactionCount`() {
        val result = SyncResult(transactionCount = 100, accountCount = 50, duration = 5000L)
        assertEquals(100, result.transactionCount)
    }

    @Test
    fun `SyncResult has accountCount`() {
        val result = SyncResult(transactionCount = 100, accountCount = 50, duration = 5000L)
        assertEquals(50, result.accountCount)
    }

    @Test
    fun `SyncResult has duration`() {
        val result = SyncResult(transactionCount = 100, accountCount = 50, duration = 5000L)
        assertEquals(5000L, result.duration)
    }

    // ========================================
    // summaryMessage tests
    // ========================================

    @Test
    fun `summaryMessage with typical counts`() {
        val result = SyncResult(1000, 50, 5000L)
        assertEquals("1000件の取引、50件の勘定科目を同期しました", result.summaryMessage)
    }

    @Test
    fun `summaryMessage with zero counts`() {
        val result = SyncResult(0, 0, 1000L)
        assertEquals("0件の取引、0件の勘定科目を同期しました", result.summaryMessage)
    }

    @Test
    fun `summaryMessage with single count`() {
        val result = SyncResult(1, 1, 500L)
        assertEquals("1件の取引、1件の勘定科目を同期しました", result.summaryMessage)
    }

    @Test
    fun `summaryMessage with large counts`() {
        val result = SyncResult(100000, 5000, 60000L)
        assertEquals("100000件の取引、5000件の勘定科目を同期しました", result.summaryMessage)
    }

    // ========================================
    // Equality tests
    // ========================================

    @Test
    fun `SyncResult equality based on all fields`() {
        val result1 = SyncResult(100, 50, 5000L)
        val result2 = SyncResult(100, 50, 5000L)
        assertEquals(result1, result2)
    }

    @Test
    fun `SyncResult not equal with different transactionCount`() {
        val result1 = SyncResult(100, 50, 5000L)
        val result2 = SyncResult(101, 50, 5000L)
        assertTrue(result1 != result2)
    }

    @Test
    fun `SyncResult not equal with different accountCount`() {
        val result1 = SyncResult(100, 50, 5000L)
        val result2 = SyncResult(100, 51, 5000L)
        assertTrue(result1 != result2)
    }

    @Test
    fun `SyncResult not equal with different duration`() {
        val result1 = SyncResult(100, 50, 5000L)
        val result2 = SyncResult(100, 50, 5001L)
        assertTrue(result1 != result2)
    }

    // ========================================
    // Copy tests
    // ========================================

    @Test
    fun `SyncResult copy with modified transactionCount`() {
        val original = SyncResult(100, 50, 5000L)
        val copied = original.copy(transactionCount = 200)
        assertEquals(200, copied.transactionCount)
        assertEquals(50, copied.accountCount)
        assertEquals(5000L, copied.duration)
    }

    @Test
    fun `SyncResult copy with modified accountCount`() {
        val original = SyncResult(100, 50, 5000L)
        val copied = original.copy(accountCount = 75)
        assertEquals(100, copied.transactionCount)
        assertEquals(75, copied.accountCount)
        assertEquals(5000L, copied.duration)
    }

    @Test
    fun `SyncResult copy with modified duration`() {
        val original = SyncResult(100, 50, 5000L)
        val copied = original.copy(duration = 10000L)
        assertEquals(100, copied.transactionCount)
        assertEquals(50, copied.accountCount)
        assertEquals(10000L, copied.duration)
    }

    // ========================================
    // Hash code tests
    // ========================================

    @Test
    fun `equal SyncResults have same hashCode`() {
        val result1 = SyncResult(100, 50, 5000L)
        val result2 = SyncResult(100, 50, 5000L)
        assertEquals(result1.hashCode(), result2.hashCode())
    }

    // ========================================
    // toString tests
    // ========================================

    @Test
    fun `SyncResult toString contains all fields`() {
        val result = SyncResult(100, 50, 5000L)
        val str = result.toString()
        assertTrue(str.contains("100"))
        assertTrue(str.contains("50"))
        assertTrue(str.contains("5000"))
    }
}
