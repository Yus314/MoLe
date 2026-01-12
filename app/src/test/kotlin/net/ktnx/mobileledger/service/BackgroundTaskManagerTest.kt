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

@OptIn(ExperimentalCoroutinesApi::class)
class BackgroundTaskManagerTest {

    private lateinit var taskManager: BackgroundTaskManagerImpl

    @Before
    fun setup() {
        taskManager = BackgroundTaskManagerImpl()
    }

    @Test
    fun `initial state is not running`() = runTest {
        assertFalse(taskManager.isRunning.value)
        assertNull(taskManager.progress.value)
        assertEquals(0, taskManager.runningTaskCount)
    }

    @Test
    fun `taskStarted sets running state to true`() = runTest {
        taskManager.taskStarted("task1")

        assertTrue(taskManager.isRunning.value)
        assertEquals(1, taskManager.runningTaskCount)
        assertNotNull(taskManager.progress.value)
        assertEquals("task1", taskManager.progress.value?.taskId)
        assertEquals(TaskState.STARTING, taskManager.progress.value?.state)
    }

    @Test
    fun `taskFinished sets running state to false when no tasks remain`() = runTest {
        taskManager.taskStarted("task1")
        assertTrue(taskManager.isRunning.value)

        taskManager.taskFinished("task1")
        assertFalse(taskManager.isRunning.value)
        assertEquals(0, taskManager.runningTaskCount)
        assertNull(taskManager.progress.value)
    }

    @Test
    fun `multiple tasks keep running state true until all finish`() = runTest {
        taskManager.taskStarted("task1")
        taskManager.taskStarted("task2")

        assertTrue(taskManager.isRunning.value)
        assertEquals(2, taskManager.runningTaskCount)

        taskManager.taskFinished("task1")
        assertTrue(taskManager.isRunning.value)
        assertEquals(1, taskManager.runningTaskCount)

        taskManager.taskFinished("task2")
        assertFalse(taskManager.isRunning.value)
        assertEquals(0, taskManager.runningTaskCount)
    }

    @Test
    fun `updateProgress updates progress for running task`() = runTest {
        taskManager.taskStarted("task1")

        val progress = TaskProgress(
            taskId = "task1",
            state = TaskState.RUNNING,
            message = "Processing...",
            current = 50,
            total = 100
        )
        taskManager.updateProgress(progress)

        val currentProgress = taskManager.progress.value
        assertNotNull(currentProgress)
        assertEquals("task1", currentProgress?.taskId)
        assertEquals(TaskState.RUNNING, currentProgress?.state)
        assertEquals("Processing...", currentProgress?.message)
        assertEquals(50, currentProgress?.current)
        assertEquals(100, currentProgress?.total)
    }

    @Test
    fun `updateProgress ignores updates for non-running tasks`() = runTest {
        taskManager.taskStarted("task1")

        val progress = TaskProgress(
            taskId = "nonexistent",
            state = TaskState.RUNNING,
            message = "Should be ignored",
            current = 50,
            total = 100
        )
        taskManager.updateProgress(progress)

        val currentProgress = taskManager.progress.value
        assertEquals("task1", currentProgress?.taskId)
        assertEquals(TaskState.STARTING, currentProgress?.state)
    }

    @Test
    fun `taskFinished for non-started task is safe`() = runTest {
        taskManager.taskFinished("nonexistent")

        assertFalse(taskManager.isRunning.value)
        assertEquals(0, taskManager.runningTaskCount)
    }

    @Test
    fun `duplicate taskStarted calls are handled`() = runTest {
        taskManager.taskStarted("task1")
        taskManager.taskStarted("task1")

        assertTrue(taskManager.isRunning.value)
        assertEquals(1, taskManager.runningTaskCount)
    }

    @Test
    fun `progress fraction is calculated correctly`() {
        val progress = TaskProgress(
            taskId = "task1",
            state = TaskState.RUNNING,
            message = "Processing...",
            current = 25,
            total = 100
        )

        assertEquals(0.25f, progress.progressFraction, 0.001f)
        assertEquals(25, progress.progressPercent)
    }

    @Test
    fun `progress fraction handles zero total`() {
        val progress = TaskProgress(
            taskId = "task1",
            state = TaskState.RUNNING,
            message = "Processing...",
            current = 10,
            total = 0
        )

        assertEquals(0f, progress.progressFraction, 0.001f)
        assertEquals(0, progress.progressPercent)
    }

    @Test
    fun `isRunning flow emits correct sequence`() = runTest {
        assertFalse(taskManager.isRunning.first())

        taskManager.taskStarted("task1")
        assertTrue(taskManager.isRunning.first())

        taskManager.taskFinished("task1")
        assertFalse(taskManager.isRunning.first())
    }

    /**
     * Edge Case 3 from spec: Sync-during-profile-switch.
     *
     * Tests that when a profile switch occurs while a background task is running:
     * 1. The old task can be identified and finished
     * 2. A new task for the new profile can be started
     * 3. Task state is correctly tracked throughout
     */
    @Test
    fun `sync during profile switch is handled correctly`() = runTest {
        // Start sync task for profile A
        val taskA = "sync-profile-A"
        taskManager.taskStarted(taskA)
        assertTrue(taskManager.isRunning.value)
        assertEquals(taskA, taskManager.progress.value?.taskId)

        // Simulate profile switch - profile A's task finishes (cancelled or completed)
        taskManager.taskFinished(taskA)
        assertFalse(taskManager.isRunning.value)
        assertNull(taskManager.progress.value)

        // Start sync task for profile B
        val taskB = "sync-profile-B"
        taskManager.taskStarted(taskB)
        assertTrue(taskManager.isRunning.value)
        assertEquals(taskB, taskManager.progress.value?.taskId)

        // Complete profile B's task
        taskManager.taskFinished(taskB)
        assertFalse(taskManager.isRunning.value)
    }

    /**
     * Edge Case 3 variant: Task for old profile continues while new profile's task starts.
     *
     * Tests concurrent tasks from different profiles.
     */
    @Test
    fun `concurrent tasks from different profiles are tracked`() = runTest {
        val taskA = "sync-profile-A"
        val taskB = "sync-profile-B"

        // Profile A sync starts
        taskManager.taskStarted(taskA)
        assertTrue(taskManager.isRunning.value)
        assertEquals(1, taskManager.runningTaskCount)

        // Profile B sync starts while A is still running
        taskManager.taskStarted(taskB)
        assertTrue(taskManager.isRunning.value)
        assertEquals(2, taskManager.runningTaskCount)

        // Profile A sync completes
        taskManager.taskFinished(taskA)
        assertTrue(taskManager.isRunning.value) // B still running
        assertEquals(1, taskManager.runningTaskCount)

        // Profile B sync completes
        taskManager.taskFinished(taskB)
        assertFalse(taskManager.isRunning.value)
        assertEquals(0, taskManager.runningTaskCount)
    }
}
