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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for BackupState sealed class.
 *
 * Tests verify:
 * - All state variants
 * - State properties
 * - When expression exhaustiveness
 */
class BackupStateTest {

    // ========================================
    // Idle state tests
    // ========================================

    @Test
    fun `Idle is data object`() {
        val state1 = BackupState.Idle
        val state2 = BackupState.Idle
        assertEquals(state1, state2)
        assertTrue(state1 === state2)
    }

    @Test
    fun `Idle is instance of BackupState`() {
        val state: BackupState = BackupState.Idle
        assertTrue(state is BackupState.Idle)
    }

    // ========================================
    // InProgress state tests
    // ========================================

    @Test
    fun `InProgress contains message`() {
        val state = BackupState.InProgress("Backing up profiles...")
        assertEquals("Backing up profiles...", state.message)
    }

    @Test
    fun `InProgress with empty message`() {
        val state = BackupState.InProgress("")
        assertEquals("", state.message)
    }

    @Test
    fun `InProgress equality based on message`() {
        val state1 = BackupState.InProgress("test")
        val state2 = BackupState.InProgress("test")
        val state3 = BackupState.InProgress("different")
        assertEquals(state1, state2)
        assertTrue(state1 != state3)
    }

    // ========================================
    // Completed state tests
    // ========================================

    @Test
    fun `Completed is data object`() {
        val state1 = BackupState.Completed
        val state2 = BackupState.Completed
        assertEquals(state1, state2)
        assertTrue(state1 === state2)
    }

    @Test
    fun `Completed is instance of BackupState`() {
        val state: BackupState = BackupState.Completed
        assertTrue(state is BackupState.Completed)
    }

    // ========================================
    // Failed state tests
    // ========================================

    @Test
    fun `Failed contains SyncError`() {
        val error = SyncError.NetworkError("Connection failed", null)
        val state = BackupState.Failed(error)
        assertEquals(error, state.error)
    }

    @Test
    fun `Failed with different error types`() {
        val networkError = SyncError.NetworkError("Network issue", null)
        val serverError = SyncError.ServerError("Internal Server Error", 500)

        val state1 = BackupState.Failed(networkError)
        val state2 = BackupState.Failed(serverError)

        assertTrue(state1.error is SyncError.NetworkError)
        assertTrue(state2.error is SyncError.ServerError)
    }

    @Test
    fun `Failed equality based on error`() {
        val error1 = SyncError.NetworkError("test", null)
        val error2 = SyncError.NetworkError("test", null)
        val error3 = SyncError.NetworkError("different", null)

        val state1 = BackupState.Failed(error1)
        val state2 = BackupState.Failed(error2)
        val state3 = BackupState.Failed(error3)

        assertEquals(state1, state2)
        assertTrue(state1 != state3)
    }

    // ========================================
    // State transition scenarios
    // ========================================

    @Test
    fun `typical backup state progression`() {
        var state: BackupState = BackupState.Idle

        // Start backup
        state = BackupState.InProgress("Starting backup...")
        assertTrue(state is BackupState.InProgress)

        // Update progress
        state = BackupState.InProgress("Backing up templates...")
        assertEquals("Backing up templates...", (state as BackupState.InProgress).message)

        // Complete
        state = BackupState.Completed
        assertTrue(state is BackupState.Completed)
    }

    @Test
    fun `backup failure state progression`() {
        var state: BackupState = BackupState.Idle
        state = BackupState.InProgress("Backing up...")
        state = BackupState.Failed(SyncError.NetworkError("Connection lost", null))

        assertTrue(state is BackupState.Failed)
        assertEquals("Connection lost", (state as BackupState.Failed).error.message)
    }

    // ========================================
    // When expression exhaustiveness
    // ========================================

    @Test
    fun `when expression handles all states`() {
        val states = listOf(
            BackupState.Idle,
            BackupState.InProgress("test"),
            BackupState.Completed,
            BackupState.Failed(SyncError.NetworkError("test", null))
        )

        states.forEach { state ->
            val description = when (state) {
                is BackupState.Idle -> "idle"
                is BackupState.InProgress -> "in progress: ${state.message}"
                is BackupState.Completed -> "completed"
                is BackupState.Failed -> "failed: ${state.error.message}"
            }
            assertTrue(description.isNotEmpty())
        }
    }
}
