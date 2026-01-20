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

package net.ktnx.mobileledger.json.unified

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import net.ktnx.mobileledger.domain.model.Account
import net.ktnx.mobileledger.domain.model.AccountAmount
import net.ktnx.mobileledger.domain.model.AmountStyle

/**
 * 統合 ParsedLedgerAccount - 全 API バージョンの差分を吸収
 *
 * バージョン間の差分:
 * - v1_14-v1_40: aibalance フィールドで直接残高取得
 * - v1_50: adata.pdperiods[0][1].bdincludingsubs で残高取得
 * - v1_32+: adeclarationinfo フィールドが追加
 *
 * このクラスは両方の構造に対応し、getSimpleBalance() で統一的に残高を取得できる。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class UnifiedParsedLedgerAccount {
    /** アカウント名 */
    var aname: String = ""

    /**
     * ポスティング数
     *
     * v1_14-v1_40: 直接フィールド
     * v1_50: adata.getFirstPeriodBalance().bdnumpostings から取得
     */
    private var _anumpostings: Int = 0
    var anumpostings: Int
        get() = adata?.getFirstPeriodBalance()?.bdnumpostings ?: _anumpostings
        set(value) {
            _anumpostings = value
        }

    /**
     * v1_14-v1_40: 勘定残高（サブ勘定を含む）
     */
    var aibalance: List<UnifiedParsedBalance>? = null

    /**
     * v1_14-v1_40: 勘定残高（サブ勘定を除く）
     */
    var aebalance: List<UnifiedParsedBalance>? = null

    /**
     * v1_32+: 勘定宣言情報
     */
    var adeclarationinfo: UnifiedParsedDeclarationInfo? = null

    /**
     * v1_50: アカウントデータ（新しい構造）
     */
    var adata: UnifiedParsedAccountData? = null

    /**
     * 残高を取得（全バージョン対応）
     *
     * v1_50: adata から取得
     * v1_14-v1_40: aibalance から取得
     */
    fun getSimpleBalance(): List<SimpleBalance> {
        val result = mutableListOf<SimpleBalance>()

        // v1_50: adata から取得
        adata?.let { accountData ->
            accountData.getFirstPeriodBalance()?.let { balanceData ->
                balanceData.bdincludingsubs?.forEach { b ->
                    val style = AmountStyle.fromParsedStyle(b.astyle, b.acommodity)
                    result.add(SimpleBalance(b.acommodity, b.aquantity?.asFloat() ?: 0f, style))
                }
            }
        }

        // v1_14-v1_40: aibalance から取得（adata がない場合）
        if (result.isEmpty() && aibalance != null) {
            aibalance?.forEach { b ->
                val style = AmountStyle.fromParsedStyle(b.astyle, b.acommodity)
                result.add(SimpleBalance(b.acommodity, b.aquantity?.asFloat() ?: 0f, style))
            }
        }

        return result
    }

    /**
     * ドメインモデルに変換
     *
     * 注意: 親アカウントは作成しない。親アカウントの作成は呼び出し元で行う。
     */
    fun toDomain(): Account {
        val level = aname.count { it == ':' }
        val balances = getSimpleBalance()
        return Account(
            id = null,
            name = aname,
            level = level,
            isExpanded = false,
            isVisible = true,
            amounts = aggregateBalances(balances)
        )
    }

    private fun aggregateBalances(balances: List<SimpleBalance>): List<AccountAmount> = balances
        .groupBy { it.commodity }
        .map { (commodity, items) ->
            AccountAmount(
                currency = commodity,
                amount = items.sumOf { it.amount.toDouble() }.toFloat()
            )
        }

    /**
     * 簡易残高データ
     */
    data class SimpleBalance(
        var commodity: String,
        var amount: Float,
        var amountStyle: AmountStyle? = null
    ) {
        constructor(commodity: String, amount: Float) : this(commodity, amount, null)
    }
}

/**
 * v1_32+ 専用: 勘定宣言情報
 *
 * ジャーナルファイル内での勘定科目宣言位置を示す。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class UnifiedParsedDeclarationInfo(
    var file: String? = null,
    var line: Int = 0
) {
    override fun toString(): String = "${file ?: "unknown"}:$line"
}
