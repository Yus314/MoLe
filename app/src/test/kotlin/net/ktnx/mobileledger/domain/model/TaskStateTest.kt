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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for TaskState sealed class.
 *
 * Tests verify:
 * - State transitions
 * - isTerminal and isRunning properties
 * - Error state with cause
 */
class TaskStateTest {

    // ========================================
    // Pending state tests
    // ========================================

    @Test
    fun `Pending is not terminal`() {
        val state: TaskState = TaskState.Pending
        assertFalse(state.isTerminal)
    }

    @Test
    fun `Pending is not running`() {
        val state: TaskState = TaskState.Pending
        assertFalse(state.isRunning)
    }

    // ========================================
    // Running state tests
    // ========================================

    @Test
    fun `Running is not terminal`() {
        val state: TaskState = TaskState.Running(Progress())
        assertFalse(state.isTerminal)
    }

    @Test
    fun `Running is running`() {
        val state: TaskState = TaskState.Running(Progress())
        assertTrue(state.isRunning)
    }

    @Test
    fun `Running contains progress information`() {
        val progress = Progress(50, 100, "Processing")
        val state = TaskState.Running(progress)
        assertEquals(progress, state.progress)
    }

    @Test
    fun `Running with indeterminate progress`() {
        val progress = Progress(0, null, "Waiting...")
        val state = TaskState.Running(progress)
        assertEquals(progress, state.progress)
        assertNull(state.progress.total)
    }

    // ========================================
    // Completed state tests
    // ========================================

    @Test
    fun `Completed is terminal`() {
        val state: TaskState = TaskState.Completed
        assertTrue(state.isTerminal)
    }

    @Test
    fun `Completed is not running`() {
        val state: TaskState = TaskState.Completed
        assertFalse(state.isRunning)
    }

    // ========================================
    // Cancelled state tests
    // ========================================

    @Test
    fun `Cancelled is terminal`() {
        val state: TaskState = TaskState.Cancelled
        assertTrue(state.isTerminal)
    }

    @Test
    fun `Cancelled is not running`() {
        val state: TaskState = TaskState.Cancelled
        assertFalse(state.isRunning)
    }

    // ========================================
    // Error state tests
    // ========================================

    @Test
    fun `Error is terminal`() {
        val state: TaskState = TaskState.Error("Something went wrong")
        assertTrue(state.isTerminal)
    }

    @Test
    fun `Error is not running`() {
        val state: TaskState = TaskState.Error("Something went wrong")
        assertFalse(state.isRunning)
    }

    @Test
    fun `Error contains message`() {
        val state = TaskState.Error("Database connection failed")
        assertEquals("Database connection failed", state.message)
    }

    @Test
    fun `Error can have cause`() {
        val cause = RuntimeException("Original error")
        val state = TaskState.Error("Wrapped error", cause)
        assertEquals("Wrapped error", state.message)
        assertEquals(cause, state.cause)
    }

    @Test
    fun `Error cause is optional`() {
        val state = TaskState.Error("No cause")
        assertNull(state.cause)
    }

    // ========================================
    // State transition scenarios
    // ========================================

    @Test
    fun `typical state progression Pending to Running to Completed`() {
        var state: TaskState = TaskState.Pending
        assertFalse(state.isTerminal)
        assertFalse(state.isRunning)

        state = TaskState.Running(Progress(0, null, ""))
        assertFalse(state.isTerminal)
        assertTrue(state.isRunning)

        state = TaskState.Completed
        assertTrue(state.isTerminal)
        assertFalse(state.isRunning)
    }

    @Test
    fun `state progression with error`() {
        var state: TaskState = TaskState.Pending
        state = TaskState.Running(Progress(0, 100, "Starting"))
        state = TaskState.Error("Failed", RuntimeException())
        assertTrue(state.isTerminal)
    }

    @Test
    fun `state progression with cancellation`() {
        var state: TaskState = TaskState.Pending
        state = TaskState.Running(Progress())
        state = TaskState.Cancelled
        assertTrue(state.isTerminal)
    }

    // ========================================
    // When expression exhaustiveness
    // ========================================

    @Test
    fun `when expression handles all states`() {
        val states = listOf(
            TaskState.Pending,
            TaskState.Running(Progress()),
            TaskState.Completed,
            TaskState.Cancelled,
            TaskState.Error("test")
        )

        states.forEach { state ->
            val description = when (state) {
                is TaskState.Pending -> "pending"
                is TaskState.Running -> "running"
                is TaskState.Completed -> "completed"
                is TaskState.Cancelled -> "cancelled"
                is TaskState.Error -> "error"
            }
            assertTrue(description.isNotEmpty())
        }
    }
}
