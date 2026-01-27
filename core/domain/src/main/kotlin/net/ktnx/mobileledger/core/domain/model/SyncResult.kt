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
 * 同期処理の結果データ
 *
 * @param transactionCount 同期された取引数
 * @param accountCount 同期された勘定科目数
 * @param duration 処理時間（ミリ秒）
 */
data class SyncResult(
    val transactionCount: Int,
    val accountCount: Int,
    val duration: Long
) {
    /**
     * 結果サマリーメッセージ
     */
    val summaryMessage: String
        get() = "${transactionCount}件の取引、${accountCount}件の勘定科目を同期しました"
}
