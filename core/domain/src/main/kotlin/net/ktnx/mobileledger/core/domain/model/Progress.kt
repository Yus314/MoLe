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

package net.ktnx.mobileledger.core.domain.model

/**
 * 進捗情報（イミュータブル）
 *
 * バックグラウンド処理の進捗状況を表す。
 *
 * @param current 現在の処理数
 * @param total 総処理数（未知の場合はnull）
 * @param message 現在の処理内容を示すメッセージ
 */
data class Progress(
    val current: Int = 0,
    val total: Int? = null,
    val message: String = ""
) {
    init {
        require(current >= 0) { "current must be non-negative" }
        require(total == null || total >= 0) { "total must be non-negative if specified" }
        require(total == null || current <= total) { "current must not exceed total" }
    }

    /**
     * 進捗率（0.0〜1.0、totalが未知の場合はnull）
     */
    val percentage: Float?
        get() = total?.let { if (it > 0) current.toFloat() / it else null }

    /**
     * 進捗パーセント（0〜100、totalが未知の場合はnull）
     */
    val percentInt: Int?
        get() = percentage?.let { (it * 100).toInt() }

    /**
     * 進捗が完了しているか
     */
    val isComplete: Boolean
        get() = total != null && current == total
}
