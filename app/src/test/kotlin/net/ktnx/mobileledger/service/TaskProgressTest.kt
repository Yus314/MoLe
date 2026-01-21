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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [TaskProgress] and [TaskState].
 *
 * Tests verify:
 * - Progress fraction calculation
 * - Progress percent calculation
 * - isCompleted computed property
 * - TaskState enum values
 */
class TaskProgressTest {

    // ========================================
    // TaskState enum tests
    // ========================================

    @Test
    fun `TaskState has all expected values`() {
        // Verify all enum values exist
        assertEquals(4, TaskState.entries.size)
        assertTrue(TaskState.entries.contains(TaskState.STARTING))
        assertTrue(TaskState.entries.contains(TaskState.RUNNING))
        assertTrue(TaskState.entries.contains(TaskState.FINISHED))
        assertTrue(TaskState.entries.contains(TaskState.ERROR))
    }

    // ========================================
    // progressFraction tests
    // ========================================

    @Test
    fun `progressFraction returns 0 when total is 0`() {
        // Given
        val progress = TaskProgress(
            taskId = "test",
            state = TaskState.RUNNING,
            message = "Testing",
            current = 5,
            total = 0
        )

        // Then
        assertEquals(0f, progress.progressFraction, 0.001f)
    }

    @Test
    fun `progressFraction returns correct fraction`() {
        // Given
        val progress = TaskProgress(
            taskId = "test",
            state = TaskState.RUNNING,
            message = "Testing",
            current = 50,
            total = 100
        )

        // Then
        assertEquals(0.5f, progress.progressFraction, 0.001f)
    }

    @Test
    fun `progressFraction clamps to 1 when current exceeds total`() {
        // Given
        val progress = TaskProgress(
            taskId = "test",
            state = TaskState.RUNNING,
            message = "Testing",
            current = 150,
            total = 100
        )

        // Then
        assertEquals(1.0f, progress.progressFraction, 0.001f)
    }

    @Test
    fun `progressFraction returns 1 when complete`() {
        // Given
        val progress = TaskProgress(
            taskId = "test",
            state = TaskState.FINISHED,
            message = "Done",
            current = 100,
            total = 100
        )

        // Then
        assertEquals(1.0f, progress.progressFraction, 0.001f)
    }

    // ========================================
    // progressPercent tests
    // ========================================

    @Test
    fun `progressPercent returns 0 when total is 0`() {
        // Given
        val progress = TaskProgress(
            taskId = "test",
            state = TaskState.RUNNING,
            message = "Testing",
            current = 0,
            total = 0
        )

        // Then
        assertEquals(0, progress.progressPercent)
    }

    @Test
    fun `progressPercent returns correct percentage`() {
        // Given
        val progress = TaskProgress(
            taskId = "test",
            state = TaskState.RUNNING,
            message = "Testing",
            current = 75,
            total = 100
        )

        // Then
        assertEquals(75, progress.progressPercent)
    }

    @Test
    fun `progressPercent truncates decimal`() {
        // Given
        val progress = TaskProgress(
            taskId = "test",
            state = TaskState.RUNNING,
            message = "Testing",
            current = 1,
            total = 3
        )

        // Then
        assertEquals(33, progress.progressPercent) // 33.33% truncated
    }

    // ========================================
    // isCompleted tests
    // ========================================

    @Test
    fun `isCompleted returns false for STARTING state`() {
        // Given
        val progress = TaskProgress(
            taskId = "test",
            state = TaskState.STARTING,
            message = "Starting"
        )

        // Then
        assertFalse(progress.isCompleted)
    }

    @Test
    fun `isCompleted returns false for RUNNING state`() {
        // Given
        val progress = TaskProgress(
            taskId = "test",
            state = TaskState.RUNNING,
            message = "Running"
        )

        // Then
        assertFalse(progress.isCompleted)
    }

    @Test
    fun `isCompleted returns true for FINISHED state`() {
        // Given
        val progress = TaskProgress(
            taskId = "test",
            state = TaskState.FINISHED,
            message = "Finished"
        )

        // Then
        assertTrue(progress.isCompleted)
    }

    @Test
    fun `isCompleted returns true for ERROR state`() {
        // Given
        val progress = TaskProgress(
            taskId = "test",
            state = TaskState.ERROR,
            message = "Error occurred"
        )

        // Then
        assertTrue(progress.isCompleted)
    }

    // ========================================
    // Validation tests
    // ========================================

    @Test(expected = IllegalArgumentException::class)
    fun `constructor throws when taskId is blank`() {
        TaskProgress(
            taskId = "",
            state = TaskState.RUNNING,
            message = "Testing"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `constructor throws when taskId is whitespace`() {
        TaskProgress(
            taskId = "   ",
            state = TaskState.RUNNING,
            message = "Testing"
        )
    }

    // ========================================
    // Data class tests
    // ========================================

    @Test
    fun `data class equals works correctly`() {
        // Given
        val progress1 = TaskProgress(
            taskId = "test",
            state = TaskState.RUNNING,
            message = "Testing",
            current = 50,
            total = 100
        )
        val progress2 = TaskProgress(
            taskId = "test",
            state = TaskState.RUNNING,
            message = "Testing",
            current = 50,
            total = 100
        )

        // Then
        assertEquals(progress1, progress2)
    }

    @Test
    fun `data class copy works correctly`() {
        // Given
        val progress = TaskProgress(
            taskId = "test",
            state = TaskState.RUNNING,
            message = "Testing",
            current = 50,
            total = 100
        )

        // When
        val updated = progress.copy(current = 75)

        // Then
        assertEquals(75, updated.current)
        assertEquals(100, updated.total)
        assertEquals("test", updated.taskId)
    }

    @Test
    fun `default values for current and total are 0`() {
        // Given
        val progress = TaskProgress(
            taskId = "test",
            state = TaskState.STARTING,
            message = "Starting"
        )

        // Then
        assertEquals(0, progress.current)
        assertEquals(0, progress.total)
    }
}
