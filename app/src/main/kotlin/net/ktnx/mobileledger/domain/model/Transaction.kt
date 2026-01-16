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

import kotlin.math.abs
import net.ktnx.mobileledger.utils.SimpleDate

/**
 * 取引のドメインモデル
 *
 * データベースの内部構造（年・月・日の分離、dataHash等）を隠蔽し、
 * 画面開発者がビジネス概念のみを扱えるようにする。
 */
data class Transaction(
    /** データベースID。新規取引の場合はnull */
    val id: Long? = null,

    /** サーバー上の取引ID（ledger ID） */
    val ledgerId: Long = 0,

    /** 取引日 */
    val date: SimpleDate,

    /** 取引の説明（摘要） */
    val description: String,

    /** 取引のコメント（オプション） */
    val comment: String? = null,

    /** 取引行のリスト */
    val lines: List<TransactionLine> = emptyList()
) {
    /**
     * 取引の金額バランスを検証する
     *
     * @return バリデーション結果
     */
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()

        if (description.isBlank()) {
            errors.add("説明は必須です")
        }

        if (lines.isEmpty()) {
            errors.add("少なくとも1つの取引行が必要です")
        }

        // 通貨ごとの金額バランスをチェック
        val balanceByCurrency = lines
            .filter { it.amount != null }
            .groupBy { it.currency }
            .mapValues { (_, lines) -> lines.sumOf { it.amount?.toDouble() ?: 0.0 } }

        for ((currency, balance) in balanceByCurrency) {
            if (abs(balance) > 0.0001) {
                val currencyLabel = currency.ifEmpty { "デフォルト通貨" }
                errors.add("$currencyLabel の金額が不均衡です（差額: $balance）")
            }
        }

        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }

    /**
     * 指定した勘定科目名を含むかどうか
     */
    fun hasAccountNamed(name: String): Boolean {
        val upperName = name.uppercase()
        return lines.any { it.accountName.uppercase().contains(upperName) }
    }

    /**
     * 取引行を追加した新しいTransactionを返す
     */
    fun withLine(line: TransactionLine): Transaction = copy(lines = lines + line)

    /**
     * 取引行を更新した新しいTransactionを返す
     */
    fun withUpdatedLine(index: Int, line: TransactionLine): Transaction =
        copy(lines = lines.toMutableList().apply { set(index, line) })
}
