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

import android.net.Uri
import net.ktnx.mobileledger.domain.usecase.ConfigBackup

class FakeConfigBackup : ConfigBackup {
    var shouldSucceed: Boolean = true
    var backupError: Exception? = null
    var restoreError: Exception? = null
    var backupCallCount = 0
        private set
    var restoreCallCount = 0
        private set
    var lastBackupUri: Uri? = null
        private set
    var lastRestoreUri: Uri? = null
        private set

    override suspend fun backup(uri: Uri): Result<Unit> {
        backupCallCount++
        lastBackupUri = uri
        return if (shouldSucceed && backupError == null) {
            Result.success(Unit)
        } else {
            Result.failure(backupError ?: Exception("Backup failed"))
        }
    }

    override suspend fun restore(uri: Uri): Result<Unit> {
        restoreCallCount++
        lastRestoreUri = uri
        return if (shouldSucceed && restoreError == null) {
            Result.success(Unit)
        } else {
            Result.failure(restoreError ?: Exception("Restore failed"))
        }
    }

    fun reset() {
        shouldSucceed = true
        backupError = null
        restoreError = null
        backupCallCount = 0
        restoreCallCount = 0
        lastBackupUri = null
        lastRestoreUri = null
    }
}
