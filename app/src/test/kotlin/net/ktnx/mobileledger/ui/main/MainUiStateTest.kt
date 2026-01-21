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

package net.ktnx.mobileledger.ui.main

import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for MainUiState and related data classes.
 */
class MainUiStateTest {

    // ========================================
    // MainTab enum tests
    // ========================================

    @Test
    fun `MainTab has expected values`() {
        val values = MainTab.values()
        assertEquals(2, values.size)
        assertTrue(values.contains(MainTab.Accounts))
        assertTrue(values.contains(MainTab.Transactions))
    }

    @Test
    fun `MainTab valueOf returns correct enum`() {
        assertEquals(MainTab.Accounts, MainTab.valueOf("Accounts"))
        assertEquals(MainTab.Transactions, MainTab.valueOf("Transactions"))
    }

    // ========================================
    // MainUiState tests
    // ========================================

    @Test
    fun `MainUiState default values are correct`() {
        val state = MainUiState()
        assertNull(state.currentProfileId)
        assertEquals("", state.currentProfileName)
        assertEquals(-1, state.currentProfileTheme)
        assertFalse(state.currentProfileCanPost)
        assertTrue(state.profiles.isEmpty())
        assertEquals(MainTab.Accounts, state.selectedTab)
        assertFalse(state.isDrawerOpen)
        assertFalse(state.isRefreshing)
        assertNull(state.lastUpdateDate)
        assertEquals(0, state.lastUpdateTransactionCount)
        assertEquals(0, state.lastUpdateAccountCount)
        assertEquals(0f, state.backgroundTaskProgress, 0.001f)
        assertFalse(state.backgroundTasksRunning)
        assertNull(state.updateError)
    }

    @Test
    fun `MainUiState can be customized`() {
        val date = Date()
        val profiles = listOf(
            ProfileListItem(1, "Test Profile", 180, true)
        )
        val state = MainUiState(
            currentProfileId = 1L,
            currentProfileName = "Test Profile",
            currentProfileTheme = 180,
            currentProfileCanPost = true,
            profiles = profiles,
            selectedTab = MainTab.Transactions,
            isDrawerOpen = true,
            isRefreshing = true,
            lastUpdateDate = date,
            lastUpdateTransactionCount = 100,
            lastUpdateAccountCount = 50,
            backgroundTaskProgress = 0.5f,
            backgroundTasksRunning = true,
            updateError = "Network error"
        )
        assertEquals(1L, state.currentProfileId)
        assertEquals("Test Profile", state.currentProfileName)
        assertEquals(180, state.currentProfileTheme)
        assertTrue(state.currentProfileCanPost)
        assertEquals(profiles, state.profiles)
        assertEquals(MainTab.Transactions, state.selectedTab)
        assertTrue(state.isDrawerOpen)
        assertTrue(state.isRefreshing)
        assertEquals(date, state.lastUpdateDate)
        assertEquals(100, state.lastUpdateTransactionCount)
        assertEquals(50, state.lastUpdateAccountCount)
        assertEquals(0.5f, state.backgroundTaskProgress, 0.001f)
        assertTrue(state.backgroundTasksRunning)
        assertEquals("Network error", state.updateError)
    }

    // ========================================
    // ProfileListItem tests
    // ========================================

    @Test
    fun `ProfileListItem constructor sets all fields`() {
        val item = ProfileListItem(1L, "Profile Name", 180, true)
        assertEquals(1L, item.id)
        assertEquals("Profile Name", item.name)
        assertEquals(180, item.theme)
        assertTrue(item.canPost)
    }

    @Test
    fun `ProfileListItem with canPost false`() {
        val item = ProfileListItem(2L, "Read Only", 90, false)
        assertEquals(2L, item.id)
        assertFalse(item.canPost)
    }

    @Test
    fun `ProfileListItem with default theme`() {
        val item = ProfileListItem(3L, "Default", -1, true)
        assertEquals(-1, item.theme)
    }

    // ========================================
    // MainEffect tests
    // ========================================

    @Test
    fun `NavigateToNewTransaction contains profileId and theme`() {
        val effect = MainEffect.NavigateToNewTransaction(1L, 180)
        assertEquals(1L, effect.profileId)
        assertEquals(180, effect.theme)
    }

    @Test
    fun `NavigateToProfileDetail contains profileId`() {
        val effect = MainEffect.NavigateToProfileDetail(5L)
        assertEquals(5L, effect.profileId)
    }

    @Test
    fun `NavigateToProfileDetail can have null profileId for new profile`() {
        val effect = MainEffect.NavigateToProfileDetail(null)
        assertNull(effect.profileId)
    }

    @Test
    fun `ShowError contains message`() {
        val effect = MainEffect.ShowError("Test error message")
        assertEquals("Test error message", effect.message)
    }

    @Test
    fun `MainEffect NavigateToTemplates is singleton object`() {
        val effect1 = MainEffect.NavigateToTemplates
        val effect2 = MainEffect.NavigateToTemplates
        assertEquals(effect1, effect2)
    }

    @Test
    fun `MainEffect NavigateToBackups is singleton object`() {
        val effect1 = MainEffect.NavigateToBackups
        val effect2 = MainEffect.NavigateToBackups
        assertEquals(effect1, effect2)
    }

    // ========================================
    // MainEvent tests
    // ========================================

    @Test
    fun `SelectTab contains tab`() {
        val event = MainEvent.SelectTab(MainTab.Transactions)
        assertEquals(MainTab.Transactions, event.tab)
    }

    @Test
    fun `SelectProfile contains profileId`() {
        val event = MainEvent.SelectProfile(3L)
        assertEquals(3L, event.profileId)
    }

    @Test
    fun `EditProfile contains profileId`() {
        val event = MainEvent.EditProfile(2L)
        assertEquals(2L, event.profileId)
    }

    @Test
    fun `ReorderProfiles contains orderedProfiles`() {
        val profiles = listOf(
            ProfileListItem(2L, "Second", 90, true),
            ProfileListItem(1L, "First", 180, false)
        )
        val event = MainEvent.ReorderProfiles(profiles)
        assertEquals(profiles, event.orderedProfiles)
        assertEquals(2, event.orderedProfiles.size)
        assertEquals("Second", event.orderedProfiles[0].name)
    }

    @Test
    fun `OpenDrawer is singleton object`() {
        val event1 = MainEvent.OpenDrawer
        val event2 = MainEvent.OpenDrawer
        assertEquals(event1, event2)
    }

    @Test
    fun `CloseDrawer is singleton object`() {
        val event1 = MainEvent.CloseDrawer
        val event2 = MainEvent.CloseDrawer
        assertEquals(event1, event2)
    }

    @Test
    fun `RefreshData is singleton object`() {
        val event1 = MainEvent.RefreshData
        val event2 = MainEvent.RefreshData
        assertEquals(event1, event2)
    }

    @Test
    fun `CancelRefresh is singleton object`() {
        val event1 = MainEvent.CancelRefresh
        val event2 = MainEvent.CancelRefresh
        assertEquals(event1, event2)
    }

    @Test
    fun `AddNewTransaction is singleton object`() {
        val event1 = MainEvent.AddNewTransaction
        val event2 = MainEvent.AddNewTransaction
        assertEquals(event1, event2)
    }

    @Test
    fun `CreateNewProfile is singleton object`() {
        val event1 = MainEvent.CreateNewProfile
        val event2 = MainEvent.CreateNewProfile
        assertEquals(event1, event2)
    }

    @Test
    fun `MainEvent NavigateToTemplates is singleton object`() {
        val event1 = MainEvent.NavigateToTemplates
        val event2 = MainEvent.NavigateToTemplates
        assertEquals(event1, event2)
    }

    @Test
    fun `MainEvent NavigateToBackups is singleton object`() {
        val event1 = MainEvent.NavigateToBackups
        val event2 = MainEvent.NavigateToBackups
        assertEquals(event1, event2)
    }

    @Test
    fun `ClearUpdateError is singleton object`() {
        val event1 = MainEvent.ClearUpdateError
        val event2 = MainEvent.ClearUpdateError
        assertEquals(event1, event2)
    }
}
