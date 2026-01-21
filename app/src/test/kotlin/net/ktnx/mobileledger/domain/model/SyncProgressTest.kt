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
 * Unit tests for [SyncProgress].
 *
 * Tests verify:
 * - Sealed class instantiation
 * - Progress calculations in Running state
 */
class SyncProgressTest {

    // ========================================
    // Starting tests
    // ========================================

    @Test
    fun `Starting with default message`() {
        val progress = SyncProgress.Starting()
        assertEquals("接続中...", progress.message)
    }

    @Test
    fun `Starting with custom message`() {
        val progress = SyncProgress.Starting("Custom message")
        assertEquals("Custom message", progress.message)
    }

    @Test
    fun `Starting is SyncProgress`() {
        val progress: SyncProgress = SyncProgress.Starting()
        assertTrue(progress is SyncProgress.Starting)
    }

    // ========================================
    // Running tests
    // ========================================

    @Test
    fun `Running stores current and total`() {
        val progress = SyncProgress.Running(5, 10, "Loading")
        assertEquals(5, progress.current)
        assertEquals(10, progress.total)
        assertEquals("Loading", progress.message)
    }

    @Test
    fun `Running progressFraction returns correct value`() {
        val progress = SyncProgress.Running(5, 10, "")
        assertEquals(0.5f, progress.progressFraction, 0.001f)
    }

    @Test
    fun `Running progressFraction returns negative when total is zero`() {
        val progress = SyncProgress.Running(0, 0, "")
        assertEquals(-1f, progress.progressFraction, 0.001f)
    }

    @Test
    fun `Running progressFraction returns 0 for start`() {
        val progress = SyncProgress.Running(0, 100, "")
        assertEquals(0f, progress.progressFraction, 0.001f)
    }

    @Test
    fun `Running progressFraction returns 1 for complete`() {
        val progress = SyncProgress.Running(100, 100, "")
        assertEquals(1f, progress.progressFraction, 0.001f)
    }

    @Test
    fun `Running progressPercent returns correct value`() {
        val progress = SyncProgress.Running(50, 100, "")
        assertEquals(50, progress.progressPercent)
    }

    @Test
    fun `Running progressPercent returns negative when total is zero`() {
        val progress = SyncProgress.Running(0, 0, "")
        assertEquals(-1, progress.progressPercent)
    }

    @Test
    fun `Running progressPercent returns 0 for start`() {
        val progress = SyncProgress.Running(0, 100, "")
        assertEquals(0, progress.progressPercent)
    }

    @Test
    fun `Running progressPercent returns 100 for complete`() {
        val progress = SyncProgress.Running(100, 100, "")
        assertEquals(100, progress.progressPercent)
    }

    @Test
    fun `Running progressPercent truncates decimals`() {
        val progress = SyncProgress.Running(1, 3, "")
        assertEquals(33, progress.progressPercent) // 33.33% -> 33
    }

    // ========================================
    // Indeterminate tests
    // ========================================

    @Test
    fun `Indeterminate stores message`() {
        val progress = SyncProgress.Indeterminate("Waiting...")
        assertEquals("Waiting...", progress.message)
    }

    @Test
    fun `Indeterminate is SyncProgress`() {
        val progress: SyncProgress = SyncProgress.Indeterminate("Test")
        assertTrue(progress is SyncProgress.Indeterminate)
    }

    // ========================================
    // Sealed class behavior
    // ========================================

    @Test
    fun `when expression handles all variants`() {
        val progress: SyncProgress = SyncProgress.Running(1, 2, "test")
        val result = when (progress) {
            is SyncProgress.Starting -> "starting"
            is SyncProgress.Running -> "running"
            is SyncProgress.Indeterminate -> "indeterminate"
        }
        assertEquals("running", result)
    }
}
