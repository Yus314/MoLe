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
 * Unit tests for SyncState sealed class.
 *
 * Tests verify:
 * - All state variants
 * - State properties
 * - When expression exhaustiveness
 */
class SyncStateTest {

    // ========================================
    // Idle state tests
    // ========================================

    @Test
    fun `Idle is data object`() {
        val state1 = SyncState.Idle
        val state2 = SyncState.Idle
        assertEquals(state1, state2)
        assertTrue(state1 === state2)
    }

    @Test
    fun `Idle is instance of SyncState`() {
        val state: SyncState = SyncState.Idle
        assertTrue(state is SyncState.Idle)
    }

    // ========================================
    // InProgress state tests
    // ========================================

    @Test
    fun `InProgress with Starting contains SyncProgress`() {
        val progress = SyncProgress.Starting("接続中...")
        val state = SyncState.InProgress(progress)
        assertEquals(progress, state.progress)
    }

    @Test
    fun `InProgress with Running progress`() {
        val progress = SyncProgress.Running(50, 100, "Syncing accounts...")
        val state = SyncState.InProgress(progress)
        assertEquals(progress, state.progress)
        assertTrue(state.progress is SyncProgress.Running)
    }

    @Test
    fun `InProgress with Indeterminate progress`() {
        val progress = SyncProgress.Indeterminate("Waiting for server...")
        val state = SyncState.InProgress(progress)
        assertEquals(progress, state.progress)
        assertTrue(state.progress is SyncProgress.Indeterminate)
    }

    @Test
    fun `Running progress has progressFraction`() {
        val progress = SyncProgress.Running(50, 100, "Test")
        assertEquals(0.5f, progress.progressFraction)
    }

    @Test
    fun `Running progress has progressPercent`() {
        val progress = SyncProgress.Running(50, 100, "Test")
        assertEquals(50, progress.progressPercent)
    }

    @Test
    fun `Running progress with zero total returns negative fraction`() {
        val progress = SyncProgress.Running(0, 0, "Test")
        assertEquals(-1f, progress.progressFraction)
    }

    @Test
    fun `InProgress equality based on progress`() {
        val progress1 = SyncProgress.Running(50, 100, "Test")
        val progress2 = SyncProgress.Running(50, 100, "Test")
        val progress3 = SyncProgress.Running(51, 100, "Test")

        val state1 = SyncState.InProgress(progress1)
        val state2 = SyncState.InProgress(progress2)
        val state3 = SyncState.InProgress(progress3)

        assertEquals(state1, state2)
        assertTrue(state1 != state3)
    }

    // ========================================
    // Completed state tests
    // ========================================

    @Test
    fun `Completed contains SyncResult`() {
        val result = SyncResult(
            transactionCount = 1000,
            accountCount = 50,
            duration = 5000L
        )
        val state = SyncState.Completed(result)
        assertEquals(result, state.result)
    }

    @Test
    fun `Completed with different results`() {
        val result1 = SyncResult(100, 50, 1000L)
        val result2 = SyncResult(500, 100, 2000L)

        val state1 = SyncState.Completed(result1)
        val state2 = SyncState.Completed(result2)

        assertEquals(50, state1.result.accountCount)
        assertEquals(500, state2.result.transactionCount)
    }

    @Test
    fun `SyncResult summaryMessage`() {
        val result = SyncResult(1000, 50, 5000L)
        assertEquals("1000件の取引、50件の勘定科目を同期しました", result.summaryMessage)
    }

    @Test
    fun `Completed equality based on result`() {
        val result1 = SyncResult(100, 50, 1000L)
        val result2 = SyncResult(100, 50, 1000L)
        val result3 = SyncResult(101, 50, 1000L)

        val state1 = SyncState.Completed(result1)
        val state2 = SyncState.Completed(result2)
        val state3 = SyncState.Completed(result3)

        assertEquals(state1, state2)
        assertTrue(state1 != state3)
    }

    // ========================================
    // Failed state tests
    // ========================================

    @Test
    fun `Failed contains SyncError`() {
        val error = SyncError.NetworkError("Connection timeout", null)
        val state = SyncState.Failed(error)
        assertEquals(error, state.error)
    }

    @Test
    fun `Failed with different error types`() {
        val networkError = SyncError.NetworkError("Network issue", null)
        val authError = SyncError.AuthenticationError("Invalid credentials")
        val serverError = SyncError.ServerError("Service unavailable", 503)

        val state1 = SyncState.Failed(networkError)
        val state2 = SyncState.Failed(authError)
        val state3 = SyncState.Failed(serverError)

        assertTrue(state1.error is SyncError.NetworkError)
        assertTrue(state2.error is SyncError.AuthenticationError)
        assertTrue(state3.error is SyncError.ServerError)
    }

    // ========================================
    // Cancelled state tests
    // ========================================

    @Test
    fun `Cancelled is data object`() {
        val state1 = SyncState.Cancelled
        val state2 = SyncState.Cancelled
        assertEquals(state1, state2)
        assertTrue(state1 === state2)
    }

    @Test
    fun `Cancelled is instance of SyncState`() {
        val state: SyncState = SyncState.Cancelled
        assertTrue(state is SyncState.Cancelled)
    }

    // ========================================
    // State transition scenarios
    // ========================================

    @Test
    fun `typical sync state progression success`() {
        var state: SyncState = SyncState.Idle

        // Start sync - starting phase
        state = SyncState.InProgress(SyncProgress.Starting("接続中..."))
        assertTrue(state is SyncState.InProgress)

        // Progress - running phase
        state = SyncState.InProgress(SyncProgress.Running(25, 50, "Syncing accounts"))
        assertTrue((state as SyncState.InProgress).progress is SyncProgress.Running)

        // Progress - transactions phase
        state = SyncState.InProgress(SyncProgress.Running(500, 1000, "Syncing transactions"))
        assertEquals(500, ((state as SyncState.InProgress).progress as SyncProgress.Running).current)

        // Complete
        state = SyncState.Completed(SyncResult(1000, 50, 5000L))
        assertTrue(state is SyncState.Completed)
    }

    @Test
    fun `sync state progression with failure`() {
        var state: SyncState = SyncState.Idle
        state = SyncState.InProgress(SyncProgress.Running(10, 50, "Syncing"))
        state = SyncState.Failed(SyncError.NetworkError("Connection reset", null))

        assertTrue(state is SyncState.Failed)
        assertEquals("Connection reset", (state as SyncState.Failed).error.message)
    }

    @Test
    fun `sync state progression with cancellation`() {
        var state: SyncState = SyncState.Idle
        state = SyncState.InProgress(SyncProgress.Running(500, 1000, "Syncing transactions"))
        state = SyncState.Cancelled

        assertTrue(state is SyncState.Cancelled)
    }

    // ========================================
    // When expression exhaustiveness
    // ========================================

    @Test
    fun `when expression handles all states`() {
        val states = listOf(
            SyncState.Idle,
            SyncState.InProgress(SyncProgress.Starting()),
            SyncState.Completed(SyncResult(100, 10, 1000L)),
            SyncState.Failed(SyncError.NetworkError("test", null)),
            SyncState.Cancelled
        )

        states.forEach { state ->
            val description = when (state) {
                is SyncState.Idle -> "idle"
                is SyncState.InProgress -> "in progress"
                is SyncState.Completed -> "done: ${state.result.accountCount} accounts"
                is SyncState.Failed -> "error: ${state.error.message}"
                is SyncState.Cancelled -> "cancelled"
            }
            assertTrue(description.isNotEmpty())
        }
    }

    // ========================================
    // SyncProgress variants tests
    // ========================================

    @Test
    fun `when expression handles all SyncProgress variants`() {
        val progressVariants: List<SyncProgress> = listOf(
            SyncProgress.Starting("test"),
            SyncProgress.Running(50, 100, "test"),
            SyncProgress.Indeterminate("test")
        )

        progressVariants.forEach { progress ->
            val description = when (progress) {
                is SyncProgress.Starting -> "starting: ${progress.message}"
                is SyncProgress.Running -> "running: ${progress.current}/${progress.total}"
                is SyncProgress.Indeterminate -> "indeterminate: ${progress.message}"
            }
            assertTrue(description.isNotEmpty())
        }
    }
}
