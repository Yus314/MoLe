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

/**
 * Background task state.
 */
enum class TaskState {
    /** Task is starting up */
    STARTING,

    /** Task is executing */
    RUNNING,

    /** Task completed successfully */
    FINISHED,

    /** Task failed with error */
    ERROR
}

/**
 * Background task progress information.
 *
 * @property taskId Unique identifier for the task
 * @property state Current task state
 * @property message User-facing progress message
 * @property current Current progress value (optional)
 * @property total Total progress value (optional)
 */
data class TaskProgress(
    val taskId: String,
    val state: TaskState,
    val message: String,
    val current: Int = 0,
    val total: Int = 0
) {
    init {
        require(taskId.isNotBlank()) { "taskId must not be blank" }
    }

    /**
     * Progress fraction between 0.0 and 1.0.
     * Returns 0.0 if total is 0.
     */
    val progressFraction: Float
        get() = if (total > 0) current.coerceAtMost(total).toFloat() / total else 0f

    /**
     * Progress as percentage (0-100).
     */
    val progressPercent: Int
        get() = (progressFraction * 100).toInt()

    /**
     * Whether the task has completed (either successfully or with error).
     */
    val isCompleted: Boolean
        get() = state == TaskState.FINISHED || state == TaskState.ERROR
}
