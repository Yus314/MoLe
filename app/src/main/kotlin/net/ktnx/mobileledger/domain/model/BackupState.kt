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

/**
 * バックアップ/リストアのUI状態
 */
sealed class BackupState {
    /**
     * アイドル状態
     */
    data object Idle : BackupState()

    /**
     * 処理中
     */
    data class InProgress(
        val message: String
    ) : BackupState()

    /**
     * 処理完了
     */
    data object Completed : BackupState()

    /**
     * 処理失敗
     */
    data class Failed(
        val error: SyncError
    ) : BackupState()
}
