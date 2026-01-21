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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for [BackgroundTaskManagerImpl].
 *
 * Tests verify:
 * - Task lifecycle (start, finish)
 * - Progress tracking
 * - Running state
 * - Concurrent task handling
 */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class BackgroundTaskManagerImplTest {

    private lateinit var manager: BackgroundTaskManagerImpl

    @Before
    fun setup() {
        manager = BackgroundTaskManagerImpl()
    }

    // ========================================
    // Initial state tests
    // ========================================

    @Test
    fun `isRunning starts as false`() = runTest {
        assertFalse(manager.isRunning.first())
    }

    @Test
    fun `progress starts as null`() = runTest {
        assertNull(manager.progress.first())
    }

    @Test
    fun `runningTaskCount starts at 0`() {
        assertEquals(0, manager.runningTaskCount)
    }

    // ========================================
    // taskStarted tests
    // ========================================

    @Test
    fun `taskStarted sets isRunning to true`() = runTest {
        // When
        manager.taskStarted("task1")

        // Then
        assertTrue(manager.isRunning.first())
    }

    @Test
    fun `taskStarted increments runningTaskCount`() {
        // When
        manager.taskStarted("task1")

        // Then
        assertEquals(1, manager.runningTaskCount)
    }

    @Test
    fun `taskStarted sets initial progress`() = runTest {
        // When
        manager.taskStarted("task1")

        // Then
        val progress = manager.progress.first()
        assertNotNull(progress)
        assertEquals("task1", progress?.taskId)
        assertEquals(TaskState.STARTING, progress?.state)
    }

    @Test
    fun `taskStarted handles multiple tasks`() {
        // When
        manager.taskStarted("task1")
        manager.taskStarted("task2")
        manager.taskStarted("task3")

        // Then
        assertEquals(3, manager.runningTaskCount)
    }

    // ========================================
    // taskFinished tests
    // ========================================

    @Test
    fun `taskFinished decrements runningTaskCount`() {
        // Given
        manager.taskStarted("task1")
        manager.taskStarted("task2")

        // When
        manager.taskFinished("task1")

        // Then
        assertEquals(1, manager.runningTaskCount)
    }

    @Test
    fun `taskFinished sets isRunning to false when last task finishes`() = runTest {
        // Given
        manager.taskStarted("task1")

        // When
        manager.taskFinished("task1")

        // Then
        assertFalse(manager.isRunning.first())
    }

    @Test
    fun `taskFinished keeps isRunning true when other tasks running`() = runTest {
        // Given
        manager.taskStarted("task1")
        manager.taskStarted("task2")

        // When
        manager.taskFinished("task1")

        // Then
        assertTrue(manager.isRunning.first())
    }

    @Test
    fun `taskFinished clears progress when last task finishes`() = runTest {
        // Given
        manager.taskStarted("task1")

        // When
        manager.taskFinished("task1")

        // Then
        assertNull(manager.progress.first())
    }

    @Test
    fun `taskFinished does not crash for unknown task`() {
        // When/Then - should not throw
        manager.taskFinished("unknown")
        assertEquals(0, manager.runningTaskCount)
    }

    // ========================================
    // updateProgress tests
    // ========================================

    @Test
    fun `updateProgress updates progress for running task`() = runTest {
        // Given
        manager.taskStarted("task1")
        val newProgress = TaskProgress(
            taskId = "task1",
            state = TaskState.RUNNING,
            message = "50% complete",
            current = 50,
            total = 100
        )

        // When
        manager.updateProgress(newProgress)

        // Then
        val result = manager.progress.first()
        assertEquals("50% complete", result?.message)
        assertEquals(50, result?.current)
    }

    @Test
    fun `updateProgress ignores progress for non-running task`() = runTest {
        // Given
        manager.taskStarted("task1")
        val initialProgress = manager.progress.first()

        val otherProgress = TaskProgress(
            taskId = "task2",
            state = TaskState.RUNNING,
            message = "Should be ignored"
        )

        // When
        manager.updateProgress(otherProgress)

        // Then - progress should still be for task1
        val result = manager.progress.first()
        assertEquals("task1", result?.taskId)
    }

    // ========================================
    // Concurrent access tests
    // ========================================

    @Test
    fun `handles rapid task start and finish`() = runTest {
        // When
        repeat(10) { i ->
            manager.taskStarted("task$i")
        }
        repeat(10) { i ->
            manager.taskFinished("task$i")
        }

        // Then
        assertEquals(0, manager.runningTaskCount)
        assertFalse(manager.isRunning.first())
    }

    @Test
    fun `same task can be started multiple times`() {
        // When
        manager.taskStarted("task1")
        manager.taskStarted("task1") // Duplicate - set ignores

        // Then
        assertEquals(1, manager.runningTaskCount)
    }
}
