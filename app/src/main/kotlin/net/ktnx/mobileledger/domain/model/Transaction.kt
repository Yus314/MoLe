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
     * 通貨別の残高を計算する。
     * 金額が設定されている行のみを対象とする。
     */
    val balancePerCurrency: Map<String, Float>
        get() = lines
            .filter { it.amount != null }
            .groupBy { it.currency }
            .mapValues { (_, currencyLines) ->
                currencyLines.sumOf { (it.amount ?: 0f).toDouble() }.toFloat()
            }

    /**
     * 取引がバランスしているかどうか。
     *
     * 以下のいずれかの条件を満たす場合、バランスしていると見なす：
     * - 各通貨の合計が BALANCE_EPSILON 以内でゼロに近い
     * - 各通貨で金額が空の行がちょうど1つ存在（自動バランス可能）
     *
     * 金額が設定されている行が1つもない通貨グループは入力途中としてスキップする。
     */
    val isBalanced: Boolean
        get() {
            val emptyAmountByCurrency = lines
                .filter { it.amount == null && it.accountName.isNotBlank() }
                .groupBy { it.currency }
                .mapValues { it.value.size }

            // 金額が設定されている通貨をチェック
            for ((currency, balance) in balancePerCurrency) {
                val emptyCount = emptyAmountByCurrency[currency] ?: 0
                val isBalancedForCurrency =
                    abs(balance) < BalanceConstants.BALANCE_EPSILON || emptyCount == 1
                if (!isBalancedForCurrency) {
                    return false
                }
            }

            // 金額が空の行のみ存在する通貨グループはスキップ（入力途中）

            return true
        }

    /**
     * 特定の通貨の自動バランス金額を取得する。
     * 空の金額の行が1つでない場合は null を返す。
     *
     * @param currency 通貨
     * @return 自動バランス金額。自動バランス不可の場合は null
     */
    fun getAutoBalanceAmount(currency: String): Float? {
        val emptyAmountLines = lines.filter {
            it.currency == currency && it.amount == null && it.accountName.isNotBlank()
        }
        if (emptyAmountLines.size != 1) return null

        val balance = balancePerCurrency[currency] ?: 0f
        return -balance
    }

    /**
     * 自動バランスを適用した新しい Transaction を返す。
     * 各通貨で空の金額の行が1つだけの場合、残高を埋める。
     */
    fun withAutoBalance(): Transaction {
        val emptyByCurrency = lines
            .filter { it.amount == null && it.accountName.isNotBlank() }
            .groupBy { it.currency }

        val newLines = lines.map { line ->
            if (line.amount == null && line.accountName.isNotBlank()) {
                val emptyCountForCurrency = emptyByCurrency[line.currency]?.size ?: 0
                if (emptyCountForCurrency == 1) {
                    val autoAmount = getAutoBalanceAmount(line.currency)
                    if (autoAmount != null) {
                        line.copy(amount = autoAmount)
                    } else {
                        line
                    }
                } else {
                    line
                }
            } else {
                line
            }
        }

        return copy(lines = newLines)
    }

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

        // 統一された isBalanced プロパティを使用
        if (!isBalanced) {
            for ((currency, balance) in balancePerCurrency) {
                if (abs(balance) > BalanceConstants.BALANCE_EPSILON) {
                    val emptyCount = lines.count {
                        it.currency == currency && it.amount == null && it.accountName.isNotBlank()
                    }
                    if (emptyCount != 1) {
                        val currencyLabel = currency.ifEmpty { "デフォルト通貨" }
                        errors.add("$currencyLabel の金額が不均衡です（差額: $balance）")
                    }
                }
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
