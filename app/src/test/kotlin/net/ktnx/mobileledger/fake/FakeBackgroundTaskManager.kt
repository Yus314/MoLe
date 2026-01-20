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

package net.ktnx.mobileledger.fake

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.ktnx.mobileledger.service.BackgroundTaskManager
import net.ktnx.mobileledger.service.TaskProgress

/**
 * Fake BackgroundTaskManager for ViewModel testing.
 */
class FakeBackgroundTaskManager : BackgroundTaskManager {
    private val runningTasks = mutableSetOf<String>()
    private val _isRunning = MutableStateFlow(false)
    private val _progress = MutableStateFlow<TaskProgress?>(null)

    override val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    override val progress: StateFlow<TaskProgress?> = _progress.asStateFlow()
    override val runningTaskCount: Int get() = runningTasks.size

    override fun taskStarted(taskId: String) {
        runningTasks.add(taskId)
        _isRunning.value = runningTasks.isNotEmpty()
    }

    override fun taskFinished(taskId: String) {
        runningTasks.remove(taskId)
        _isRunning.value = runningTasks.isNotEmpty()
        if (runningTasks.isEmpty()) {
            _progress.value = null
        }
    }

    override fun updateProgress(progress: TaskProgress) {
        _progress.value = progress
    }
}
