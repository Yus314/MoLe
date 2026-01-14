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
 * 同期処理のUI状態
 */
sealed class SyncState {
    /**
     * アイドル状態（同期処理なし）
     */
    data object Idle : SyncState()

    /**
     * 同期処理実行中
     */
    data class InProgress(
        val progress: SyncProgress
    ) : SyncState()

    /**
     * 同期処理完了
     */
    data class Completed(
        val result: SyncResult
    ) : SyncState()

    /**
     * 同期処理失敗
     */
    data class Failed(
        val error: SyncError
    ) : SyncState()

    /**
     * 同期処理キャンセル済み
     */
    data object Cancelled : SyncState()
}
