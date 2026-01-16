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
 * 取引行のドメインモデル
 *
 * 取引内の1行（勘定科目、金額、通貨、コメント）を表す。
 * データベースのorderNoや外部キーを隠蔽する。
 */
data class TransactionLine(
    /** データベースID。新規行の場合はnull */
    val id: Long? = null,

    /** 勘定科目名 */
    val accountName: String,

    /** 金額。自動計算行の場合はnull */
    val amount: Float? = null,

    /** 通貨。空文字はデフォルト通貨 */
    val currency: String = "",

    /** 行のコメント（オプション） */
    val comment: String? = null
) {
    /**
     * 金額が設定されているかどうか
     */
    val hasAmount: Boolean get() = amount != null

    /**
     * 金額を設定した新しいTransactionLineを返す
     */
    fun withAmount(amount: Float): TransactionLine = copy(amount = amount)

    /**
     * 金額をクリアした新しいTransactionLineを返す
     */
    fun withoutAmount(): TransactionLine = copy(amount = null)
}
