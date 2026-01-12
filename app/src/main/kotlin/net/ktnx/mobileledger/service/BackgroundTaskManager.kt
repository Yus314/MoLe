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

import kotlinx.coroutines.flow.StateFlow

/**
 * Background task management service interface.
 *
 * Manages background task (data sync, etc.) execution state and progress
 * across the application. @Singleton scoped to maintain state across
 * Activity transitions.
 *
 * ## Usage in ViewModel
 *
 * ```kotlin
 * @HiltViewModel
 * class MainViewModel @Inject constructor(
 *     private val backgroundTaskManager: BackgroundTaskManager
 * ) : ViewModel() {
 *     val isTaskRunning = backgroundTaskManager.isRunning
 *         .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
 * }
 * ```
 *
 * ## Usage in Task
 *
 * ```kotlin
 * class RetrieveTransactionsTask @Inject constructor(
 *     private val backgroundTaskManager: BackgroundTaskManager
 * ) {
 *     fun run() {
 *         val taskId = "sync-${System.currentTimeMillis()}"
 *         backgroundTaskManager.taskStarted(taskId)
 *         try {
 *             // processing...
 *             backgroundTaskManager.updateProgress(
 *                 TaskProgress(taskId, TaskState.RUNNING, "Syncing...", current, total)
 *             )
 *             backgroundTaskManager.taskFinished(taskId)
 *         } catch (e: Exception) {
 *             backgroundTaskManager.updateProgress(
 *                 TaskProgress(taskId, TaskState.ERROR, e.message ?: "Error")
 *             )
 *             backgroundTaskManager.taskFinished(taskId)
 *         }
 *     }
 * }
 * ```
 */
interface BackgroundTaskManager {
    /**
     * Whether any background task is currently running.
     *
     * Returns true if one or more tasks are running.
     */
    val isRunning: StateFlow<Boolean>

    /**
     * Progress of the currently running task.
     *
     * Returns null if no task is running.
     * If multiple tasks are running, returns the most recently updated one.
     */
    val progress: StateFlow<TaskProgress?>

    /**
     * Number of currently running tasks.
     */
    val runningTaskCount: Int

    /**
     * Notify that a task has started.
     *
     * @param taskId Unique task identifier
     */
    fun taskStarted(taskId: String)

    /**
     * Notify that a task has finished.
     *
     * @param taskId Unique task identifier
     */
    fun taskFinished(taskId: String)

    /**
     * Update task progress.
     *
     * @param progress Progress information
     */
    fun updateProgress(progress: TaskProgress)
}
