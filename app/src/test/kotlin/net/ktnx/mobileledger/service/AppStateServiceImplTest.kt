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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [AppStateServiceImpl].
 *
 * Tests verify:
 * - Sync info management
 * - Drawer state management
 * - Data version signaling
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppStateServiceImplTest {

    private lateinit var service: AppStateServiceImpl

    @Before
    fun setup() {
        service = AppStateServiceImpl()
    }

    // ========================================
    // Sync info tests
    // ========================================

    @Test
    fun `lastSyncInfo starts with EMPTY`() = runTest {
        // Then
        val info = service.lastSyncInfo.first()
        assertNull(info.date)
        assertEquals(0, info.transactionCount)
        assertEquals(0, info.accountCount)
        assertEquals(0, info.totalAccountCount)
    }

    @Test
    fun `updateSyncInfo updates the state`() = runTest {
        // Given
        val newInfo = SyncInfo(
            date = Date(),
            transactionCount = 100,
            accountCount = 50,
            totalAccountCount = 75
        )

        // When
        service.updateSyncInfo(newInfo)

        // Then
        val result = service.lastSyncInfo.first()
        assertEquals(100, result.transactionCount)
        assertEquals(50, result.accountCount)
        assertEquals(75, result.totalAccountCount)
    }

    @Test
    fun `clearSyncInfo resets to EMPTY`() = runTest {
        // Given
        service.updateSyncInfo(SyncInfo(Date(), 100, 50, 75))

        // When
        service.clearSyncInfo()

        // Then
        val result = service.lastSyncInfo.first()
        assertNull(result.date)
        assertEquals(0, result.transactionCount)
    }

    // ========================================
    // Drawer state tests
    // ========================================

    @Test
    fun `drawerOpen starts as false`() = runTest {
        // Then
        assertFalse(service.drawerOpen.first())
    }

    @Test
    fun `setDrawerOpen sets drawer to open`() = runTest {
        // When
        service.setDrawerOpen(true)

        // Then
        assertTrue(service.drawerOpen.first())
    }

    @Test
    fun `setDrawerOpen sets drawer to closed`() = runTest {
        // Given
        service.setDrawerOpen(true)

        // When
        service.setDrawerOpen(false)

        // Then
        assertFalse(service.drawerOpen.first())
    }

    @Test
    fun `toggleDrawer opens when closed`() = runTest {
        // Given - drawer starts closed
        assertFalse(service.drawerOpen.first())

        // When
        service.toggleDrawer()

        // Then
        assertTrue(service.drawerOpen.first())
    }

    @Test
    fun `toggleDrawer closes when open`() = runTest {
        // Given
        service.setDrawerOpen(true)

        // When
        service.toggleDrawer()

        // Then
        assertFalse(service.drawerOpen.first())
    }

    @Test
    fun `toggleDrawer multiple times works correctly`() = runTest {
        // Given
        assertFalse(service.drawerOpen.first())

        // When/Then
        service.toggleDrawer()
        assertTrue(service.drawerOpen.first())

        service.toggleDrawer()
        assertFalse(service.drawerOpen.first())

        service.toggleDrawer()
        assertTrue(service.drawerOpen.first())
    }

    // ========================================
    // Data version tests
    // ========================================

    @Test
    fun `dataVersion starts at 0`() = runTest {
        // Then
        assertEquals(0L, service.dataVersion.first())
    }

    @Test
    fun `signalDataChanged increments version`() = runTest {
        // When
        service.signalDataChanged()

        // Then
        assertEquals(1L, service.dataVersion.first())
    }

    @Test
    fun `signalDataChanged increments multiple times`() = runTest {
        // When
        service.signalDataChanged()
        service.signalDataChanged()
        service.signalDataChanged()

        // Then
        assertEquals(3L, service.dataVersion.first())
    }

    // ========================================
    // StateFlow behavior tests
    // ========================================

    @Test
    fun `lastSyncInfo StateFlow emits updates`() = runTest {
        // Given
        val info1 = SyncInfo(Date(), 10, 5, 10)
        val info2 = SyncInfo(Date(), 20, 10, 20)

        // When
        service.updateSyncInfo(info1)
        val result1 = service.lastSyncInfo.first()

        service.updateSyncInfo(info2)
        val result2 = service.lastSyncInfo.first()

        // Then
        assertEquals(10, result1.transactionCount)
        assertEquals(20, result2.transactionCount)
    }
}
