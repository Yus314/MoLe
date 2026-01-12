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
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for AppStateServiceImpl.
 *
 * Tests the behavior of the AppStateService implementation including:
 * - Navigation drawer state management
 * - Sync information tracking
 * - State reset functionality
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppStateServiceTest {

    private lateinit var appStateService: AppStateServiceImpl

    @Before
    fun setup() {
        appStateService = AppStateServiceImpl()
    }

    // ========================================
    // Drawer State Tests
    // ========================================

    @Test
    fun `drawerOpen initial state is false`() = runTest {
        assertFalse(appStateService.drawerOpen.value)
    }

    @Test
    fun `setDrawerOpen true opens drawer`() = runTest {
        appStateService.setDrawerOpen(true)

        assertTrue(appStateService.drawerOpen.value)
    }

    @Test
    fun `setDrawerOpen false closes drawer`() = runTest {
        appStateService.setDrawerOpen(true)
        assertTrue(appStateService.drawerOpen.value)

        appStateService.setDrawerOpen(false)

        assertFalse(appStateService.drawerOpen.value)
    }

    @Test
    fun `toggleDrawer opens closed drawer`() = runTest {
        assertFalse(appStateService.drawerOpen.value)

        appStateService.toggleDrawer()

        assertTrue(appStateService.drawerOpen.value)
    }

    @Test
    fun `toggleDrawer closes open drawer`() = runTest {
        appStateService.setDrawerOpen(true)
        assertTrue(appStateService.drawerOpen.value)

        appStateService.toggleDrawer()

        assertFalse(appStateService.drawerOpen.value)
    }

    @Test
    fun `toggleDrawer twice returns to original state`() = runTest {
        assertFalse(appStateService.drawerOpen.value)

        appStateService.toggleDrawer()
        appStateService.toggleDrawer()

        assertFalse(appStateService.drawerOpen.value)
    }

    // ========================================
    // Sync Info Tests
    // ========================================

    @Test
    fun `lastSyncInfo initial state is EMPTY`() = runTest {
        assertEquals(SyncInfo.EMPTY, appStateService.lastSyncInfo.value)
    }

    @Test
    fun `lastSyncInfo EMPTY has null date`() = runTest {
        assertNull(appStateService.lastSyncInfo.value.date)
    }

    @Test
    fun `lastSyncInfo EMPTY hasSynced is false`() = runTest {
        assertFalse(appStateService.lastSyncInfo.value.hasSynced)
    }

    @Test
    fun `updateSyncInfo updates the state`() = runTest {
        val now = Date()
        val syncInfo = SyncInfo(
            date = now,
            transactionCount = 150,
            accountCount = 25,
            totalAccountCount = 30
        )

        appStateService.updateSyncInfo(syncInfo)

        assertEquals(syncInfo, appStateService.lastSyncInfo.value)
        assertEquals(now, appStateService.lastSyncInfo.value.date)
        assertEquals(150, appStateService.lastSyncInfo.value.transactionCount)
        assertEquals(25, appStateService.lastSyncInfo.value.accountCount)
        assertEquals(30, appStateService.lastSyncInfo.value.totalAccountCount)
    }

    @Test
    fun `updateSyncInfo hasSynced is true after update`() = runTest {
        val syncInfo = SyncInfo(
            date = Date(),
            transactionCount = 100,
            accountCount = 20,
            totalAccountCount = 25
        )

        appStateService.updateSyncInfo(syncInfo)

        assertTrue(appStateService.lastSyncInfo.value.hasSynced)
    }

    @Test
    fun `clearSyncInfo resets to EMPTY`() = runTest {
        val syncInfo = SyncInfo(
            date = Date(),
            transactionCount = 100,
            accountCount = 20,
            totalAccountCount = 25
        )
        appStateService.updateSyncInfo(syncInfo)
        assertTrue(appStateService.lastSyncInfo.value.hasSynced)

        appStateService.clearSyncInfo()

        assertEquals(SyncInfo.EMPTY, appStateService.lastSyncInfo.value)
        assertFalse(appStateService.lastSyncInfo.value.hasSynced)
    }

    @Test
    fun `updateSyncInfo multiple times keeps latest`() = runTest {
        val first = SyncInfo(Date(1000), 10, 5, 10)
        val second = SyncInfo(Date(2000), 20, 10, 15)
        val third = SyncInfo(Date(3000), 30, 15, 20)

        appStateService.updateSyncInfo(first)
        appStateService.updateSyncInfo(second)
        appStateService.updateSyncInfo(third)

        assertEquals(third, appStateService.lastSyncInfo.value)
        assertEquals(30, appStateService.lastSyncInfo.value.transactionCount)
    }

    // ========================================
    // SyncInfo Data Class Tests
    // ========================================

    @Test
    fun `SyncInfo formatSummary returns correct format`() {
        val syncInfo = SyncInfo(
            date = Date(),
            transactionCount = 100,
            accountCount = 25,
            totalAccountCount = 30
        )

        val summary = syncInfo.formatSummary("transactions", "accounts")

        assertEquals("100 transactions, 25/30 accounts", summary)
    }

    @Test
    fun `SyncInfo EMPTY formatSummary returns zero counts`() {
        val summary = SyncInfo.EMPTY.formatSummary("txns", "accts")

        assertEquals("0 txns, 0/0 accts", summary)
    }

    // ========================================
    // State Independence Tests
    // ========================================

    @Test
    fun `drawer state and sync info are independent`() = runTest {
        // Setting drawer state should not affect sync info
        appStateService.setDrawerOpen(true)

        assertEquals(SyncInfo.EMPTY, appStateService.lastSyncInfo.value)

        // Setting sync info should not affect drawer state
        val syncInfo = SyncInfo(Date(), 100, 20, 25)
        appStateService.updateSyncInfo(syncInfo)

        assertTrue(appStateService.drawerOpen.value)
        assertEquals(syncInfo, appStateService.lastSyncInfo.value)
    }

    @Test
    fun `clearSyncInfo does not affect drawer state`() = runTest {
        appStateService.setDrawerOpen(true)
        appStateService.updateSyncInfo(SyncInfo(Date(), 100, 20, 25))

        appStateService.clearSyncInfo()

        assertTrue(appStateService.drawerOpen.value)
        assertEquals(SyncInfo.EMPTY, appStateService.lastSyncInfo.value)
    }
}
