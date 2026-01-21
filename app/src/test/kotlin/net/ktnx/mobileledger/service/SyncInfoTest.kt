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

package net.ktnx.mobileledger.service

import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [SyncInfo].
 *
 * Tests verify:
 * - Empty state
 * - hasSynced computed property
 * - formatSummary method
 */
class SyncInfoTest {

    @Test
    fun `EMPTY has null date`() {
        // Then
        assertNull(SyncInfo.EMPTY.date)
    }

    @Test
    fun `EMPTY has zero counts`() {
        // Then
        assertEquals(0, SyncInfo.EMPTY.transactionCount)
        assertEquals(0, SyncInfo.EMPTY.accountCount)
        assertEquals(0, SyncInfo.EMPTY.totalAccountCount)
    }

    @Test
    fun `hasSynced returns false when date is null`() {
        // Given
        val syncInfo = SyncInfo(
            date = null,
            transactionCount = 10,
            accountCount = 5,
            totalAccountCount = 10
        )

        // Then
        assertFalse(syncInfo.hasSynced)
    }

    @Test
    fun `hasSynced returns true when date is set`() {
        // Given
        val syncInfo = SyncInfo(
            date = Date(),
            transactionCount = 10,
            accountCount = 5,
            totalAccountCount = 10
        )

        // Then
        assertTrue(syncInfo.hasSynced)
    }

    @Test
    fun `formatSummary returns correct format`() {
        // Given
        val syncInfo = SyncInfo(
            date = Date(),
            transactionCount = 100,
            accountCount = 25,
            totalAccountCount = 50
        )

        // When
        val summary = syncInfo.formatSummary("transactions", "accounts")

        // Then
        assertEquals("100 transactions, 25/50 accounts", summary)
    }

    @Test
    fun `formatSummary works with zero values`() {
        // Given
        val syncInfo = SyncInfo.EMPTY

        // When
        val summary = syncInfo.formatSummary("transactions", "accounts")

        // Then
        assertEquals("0 transactions, 0/0 accounts", summary)
    }

    @Test
    fun `formatSummary works with custom labels`() {
        // Given
        val syncInfo = SyncInfo(
            date = Date(),
            transactionCount = 50,
            accountCount = 10,
            totalAccountCount = 20
        )

        // When
        val summary = syncInfo.formatSummary("件", "アカウント")

        // Then
        assertEquals("50 件, 10/20 アカウント", summary)
    }

    @Test
    fun `data class equals works correctly`() {
        // Given
        val date = Date()
        val syncInfo1 = SyncInfo(date, 10, 5, 15)
        val syncInfo2 = SyncInfo(date, 10, 5, 15)

        // Then
        assertEquals(syncInfo1, syncInfo2)
    }

    @Test
    fun `data class copy works correctly`() {
        // Given
        val syncInfo = SyncInfo(Date(), 10, 5, 15)

        // When
        val updated = syncInfo.copy(transactionCount = 20)

        // Then
        assertEquals(20, updated.transactionCount)
        assertEquals(5, updated.accountCount)
    }
}
