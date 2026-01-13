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

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.ktnx.mobileledger.utils.Logger

/**
 * Implementation of [BackgroundTaskManager].
 *
 * Thread-safe implementation that tracks multiple concurrent tasks
 * and reports aggregate running state.
 */
@Singleton
class BackgroundTaskManagerImpl @Inject constructor() : BackgroundTaskManager {

    private val runningTasks = mutableSetOf<String>()
    private val lock = Any()

    private val _isRunning = MutableStateFlow(false)
    override val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _progress = MutableStateFlow<TaskProgress?>(null)
    override val progress: StateFlow<TaskProgress?> = _progress.asStateFlow()

    override val runningTaskCount: Int
        get() = synchronized(lock) { runningTasks.size }

    override fun taskStarted(taskId: String) {
        // Update data structure under lock, but emit StateFlow OUTSIDE lock
        // to avoid deadlock with Compose recomposition on main thread
        val newProgress: TaskProgress
        synchronized(lock) {
            runningTasks.add(taskId)
            Logger.debug(
                "BackgroundTaskManager",
                "Task started: $taskId, running count: ${runningTasks.size}"
            )
            newProgress = TaskProgress(
                taskId = taskId,
                state = TaskState.STARTING,
                message = "Starting..."
            )
        }
        // StateFlow updates outside synchronized block
        _isRunning.value = true
        _progress.value = newProgress
    }

    override fun taskFinished(taskId: String) {
        // Update data structure under lock, but emit StateFlow OUTSIDE lock
        // to avoid deadlock with Compose recomposition on main thread
        val newIsRunning: Boolean
        val shouldClearProgress: Boolean
        synchronized(lock) {
            runningTasks.remove(taskId)
            Logger.debug(
                "BackgroundTaskManager",
                "Task finished: $taskId, running count: ${runningTasks.size}"
            )
            newIsRunning = runningTasks.isNotEmpty()
            shouldClearProgress = runningTasks.isEmpty()
        }
        // StateFlow updates outside synchronized block
        _isRunning.value = newIsRunning
        if (shouldClearProgress) {
            _progress.value = null
        }
    }

    override fun updateProgress(progress: TaskProgress) {
        // Check under lock, but emit StateFlow OUTSIDE lock
        // to avoid deadlock with Compose recomposition on main thread
        val shouldUpdate: Boolean
        synchronized(lock) {
            // Only update if the task is still running
            shouldUpdate = runningTasks.contains(progress.taskId)
        }
        // StateFlow update outside synchronized block
        if (shouldUpdate) {
            _progress.value = progress
        }
    }
}
