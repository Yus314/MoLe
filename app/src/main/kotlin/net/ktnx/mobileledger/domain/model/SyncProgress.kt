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
 * 同期処理の進捗情報
 */
sealed class SyncProgress {
    /**
     * 処理開始
     */
    data class Starting(
        val message: String = "接続中..."
    ) : SyncProgress()

    /**
     * 処理実行中（進捗計算可能）
     *
     * @param current 現在の処理済み件数
     * @param total 総件数（0の場合は不明）
     * @param message 進捗メッセージ
     */
    data class Running(
        val current: Int,
        val total: Int,
        val message: String
    ) : SyncProgress() {
        /**
         * 進捗率（0.0-1.0）
         * totalが0の場合は-1.0（不確定）
         */
        val progressFraction: Float
            get() = if (total > 0) current.toFloat() / total else -1f

        /**
         * 進捗パーセント（0-100）
         * totalが0の場合は-1（不確定）
         */
        val progressPercent: Int
            get() = if (total > 0) (current * 100 / total) else -1
    }

    /**
     * 処理実行中（進捗計算不可）
     * サーバー応答待ちなど、総件数が不明な場合
     */
    data class Indeterminate(
        val message: String
    ) : SyncProgress()
}
