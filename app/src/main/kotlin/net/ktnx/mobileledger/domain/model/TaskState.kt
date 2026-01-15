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
 * バックグラウンド処理の状態を表すsealed class
 *
 * 単方向データフロー: Pending -> Running -> (Completed | Cancelled | Error)
 * 終端状態（Completed, Cancelled, Error）からの遷移は許可されない。
 */
sealed class TaskState {
    /**
     * 開始前（初期状態）
     */
    object Pending : TaskState()

    /**
     * 実行中（進捗情報を含む）
     */
    data class Running(val progress: Progress) : TaskState()

    /**
     * 正常完了
     */
    object Completed : TaskState()

    /**
     * キャンセル済み
     */
    object Cancelled : TaskState()

    /**
     * エラー発生
     */
    data class Error(val message: String, val cause: Throwable? = null) : TaskState()

    /**
     * 終端状態かどうか（Completed, Cancelled, Error）
     */
    val isTerminal: Boolean
        get() = this is Completed || this is Cancelled || this is Error

    /**
     * 実行中かどうか
     */
    val isRunning: Boolean
        get() = this is Running
}
