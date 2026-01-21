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
 * Extended unit tests for SyncProgress sealed class.
 *
 * Tests verify:
 * - All SyncProgress variants
 * - Progress calculations
 * - Message handling
 */
class SyncProgressExtendedTest {

    // ========================================
    // Starting tests
    // ========================================

    @Test
    fun `Starting has default message`() {
        val progress = SyncProgress.Starting()
        assertEquals("接続中...", progress.message)
    }

    @Test
    fun `Starting with custom message`() {
        val progress = SyncProgress.Starting("Connecting to server...")
        assertEquals("Connecting to server...", progress.message)
    }

    @Test
    fun `Starting is instance of SyncProgress`() {
        val progress: SyncProgress = SyncProgress.Starting()
        assertTrue(progress is SyncProgress.Starting)
    }

    // ========================================
    // Running tests
    // ========================================

    @Test
    fun `Running has current total and message`() {
        val progress = SyncProgress.Running(50, 100, "Syncing accounts...")
        assertEquals(50, progress.current)
        assertEquals(100, progress.total)
        assertEquals("Syncing accounts...", progress.message)
    }

    @Test
    fun `Running progressFraction at 0 percent`() {
        val progress = SyncProgress.Running(0, 100, "Starting")
        assertEquals(0f, progress.progressFraction)
    }

    @Test
    fun `Running progressFraction at 50 percent`() {
        val progress = SyncProgress.Running(50, 100, "Halfway")
        assertEquals(0.5f, progress.progressFraction)
    }

    @Test
    fun `Running progressFraction at 100 percent`() {
        val progress = SyncProgress.Running(100, 100, "Complete")
        assertEquals(1.0f, progress.progressFraction)
    }

    @Test
    fun `Running progressFraction with zero total returns negative`() {
        val progress = SyncProgress.Running(0, 0, "Unknown total")
        assertEquals(-1f, progress.progressFraction)
    }

    @Test
    fun `Running progressPercent at 0 percent`() {
        val progress = SyncProgress.Running(0, 100, "Starting")
        assertEquals(0, progress.progressPercent)
    }

    @Test
    fun `Running progressPercent at 25 percent`() {
        val progress = SyncProgress.Running(25, 100, "Quarter")
        assertEquals(25, progress.progressPercent)
    }

    @Test
    fun `Running progressPercent at 75 percent`() {
        val progress = SyncProgress.Running(75, 100, "Three quarters")
        assertEquals(75, progress.progressPercent)
    }

    @Test
    fun `Running progressPercent at 100 percent`() {
        val progress = SyncProgress.Running(100, 100, "Done")
        assertEquals(100, progress.progressPercent)
    }

    @Test
    fun `Running progressPercent with zero total returns negative`() {
        val progress = SyncProgress.Running(10, 0, "Unknown")
        assertEquals(-1, progress.progressPercent)
    }

    @Test
    fun `Running with large numbers`() {
        val progress = SyncProgress.Running(5000, 10000, "Processing")
        assertEquals(0.5f, progress.progressFraction)
        assertEquals(50, progress.progressPercent)
    }

    @Test
    fun `Running progressPercent rounds down`() {
        // 33 / 100 = 0.33 -> 33%
        val progress = SyncProgress.Running(33, 100, "Progress")
        assertEquals(33, progress.progressPercent)

        // 66 / 100 = 0.66 -> 66%
        val progress2 = SyncProgress.Running(66, 100, "Progress")
        assertEquals(66, progress2.progressPercent)
    }

    // ========================================
    // Indeterminate tests
    // ========================================

    @Test
    fun `Indeterminate has message`() {
        val progress = SyncProgress.Indeterminate("Waiting for server response...")
        assertEquals("Waiting for server response...", progress.message)
    }

    @Test
    fun `Indeterminate is instance of SyncProgress`() {
        val progress: SyncProgress = SyncProgress.Indeterminate("Loading")
        assertTrue(progress is SyncProgress.Indeterminate)
    }

    @Test
    fun `Indeterminate with empty message`() {
        val progress = SyncProgress.Indeterminate("")
        assertEquals("", progress.message)
    }

    // ========================================
    // When expression exhaustiveness
    // ========================================

    @Test
    fun `when expression handles all progress types`() {
        val progressTypes: List<SyncProgress> = listOf(
            SyncProgress.Starting("Starting"),
            SyncProgress.Running(50, 100, "Running"),
            SyncProgress.Indeterminate("Indeterminate")
        )

        progressTypes.forEach { progress ->
            val description = when (progress) {
                is SyncProgress.Starting -> "starting: ${progress.message}"
                is SyncProgress.Running -> "running: ${progress.progressPercent}%"
                is SyncProgress.Indeterminate -> "indeterminate: ${progress.message}"
            }
            assertTrue(description.isNotEmpty())
        }
    }

    // ========================================
    // Data class equality
    // ========================================

    @Test
    fun `Starting equality based on message`() {
        val progress1 = SyncProgress.Starting("test")
        val progress2 = SyncProgress.Starting("test")
        val progress3 = SyncProgress.Starting("different")

        assertEquals(progress1, progress2)
        assertTrue(progress1 != progress3)
    }

    @Test
    fun `Running equality based on all fields`() {
        val progress1 = SyncProgress.Running(50, 100, "test")
        val progress2 = SyncProgress.Running(50, 100, "test")
        val progress3 = SyncProgress.Running(51, 100, "test")

        assertEquals(progress1, progress2)
        assertTrue(progress1 != progress3)
    }

    @Test
    fun `Indeterminate equality based on message`() {
        val progress1 = SyncProgress.Indeterminate("test")
        val progress2 = SyncProgress.Indeterminate("test")
        val progress3 = SyncProgress.Indeterminate("different")

        assertEquals(progress1, progress2)
        assertTrue(progress1 != progress3)
    }
}
