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
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSetter
import net.ktnx.mobileledger.di.CurrencyFormatterEntryPoint
import net.ktnx.mobileledger.domain.model.CurrencyPosition
import net.ktnx.mobileledger.domain.model.TransactionLine
import net.ktnx.mobileledger.json.config.ApiVersionConfig

/**
 * 統合 ParsedPosting - API バージョン v1_32+ の差分を吸収
 *
 * v1_32+ では ptransaction_ は String 型を使用
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class UnifiedParsedPosting {
    var pbalanceassertion: Void? = null
    var pstatus: String = "Unmarked"
    var paccount: String? = null
    var pamount: MutableList<UnifiedParsedAmount>? = null
    var pdate: String? = null
    var pdate2: String? = null
    var ptype: String = "RegularPosting"
    var ptags: MutableList<List<String>> = mutableListOf()
    var poriginal: String? = null

    private var _pcomment: String = ""
    var pcomment: String
        get() = _pcomment
        set(value) {
            _pcomment = value.trim()
        }

    /**
     * トランザクションID（String として正規化）
     *
     * v1_32+ では常に String 型
     */
    @JsonProperty("ptransaction_")
    var ptransaction_: String = "0"
        private set

    /**
     * ptransaction_ を JSON から設定（Int または String 対応）
     */
    @JsonSetter("ptransaction_")
    fun setPtransactionFromJson(value: Any?) {
        ptransaction_ = when (value) {
            is Int -> value.toString()
            is Number -> value.toInt().toString()
            is String -> value
            else -> "0"
        }
    }

    /**
     * String として ptransaction_ を設定
     */
    fun setTransactionIdAsString(value: String) {
        ptransaction_ = value
    }

    /**
     * ドメインモデルに変換
     */
    fun toDomain(): TransactionLine {
        val amt = pamount?.firstOrNull()
        return TransactionLine(
            id = null,
            accountName = paccount ?: "",
            amount = amt?.aquantity?.asFloat(),
            currency = amt?.acommodity ?: "",
            comment = pcomment.takeIf { it.isNotEmpty() }
        )
    }

    companion object {
        /**
         * ドメインモデルから Posting を生成
         *
         * @param line 変換元のトランザクション行
         * @param config API バージョン設定
         * @return 生成した UnifiedParsedPosting
         */
        @Suppress("UNUSED_PARAMETER")
        fun fromDomain(line: TransactionLine, config: ApiVersionConfig): UnifiedParsedPosting =
            UnifiedParsedPosting().apply {
                paccount = line.accountName
                pcomment = line.comment ?: ""
                pamount = mutableListOf(
                    UnifiedParsedAmount().apply {
                        acommodity = line.currency
                        aismultiplier = false
                        aquantity = UnifiedParsedQuantity().apply {
                            decimalPlaces = 2
                            decimalMantissa = Math.round((line.amount ?: 0f) * 100).toLong()
                        }
                        astyle = UnifiedParsedStyle().apply {
                            ascommodityside = getCommoditySide()
                            isAscommodityspaced = getCommoditySpaced()
                            configureStyleForVersion(this, 2)
                        }
                    }
                )
            }

        /**
         * API バージョン v1_32+ 用スタイル設定
         */
        private fun configureStyleForVersion(style: UnifiedParsedStyle, precision: Int) {
            style.asprecision = precision
            style.asdecimalmark = "."
            style.asrounding = "NoRounding"
        }

        private fun getCommoditySpaced(): Boolean = CurrencyFormatterEntryPoint.getOrNull()?.currencyGap?.value ?: false

        private fun getCommoditySide(): Char {
            val formatter = CurrencyFormatterEntryPoint.getOrNull() ?: return 'L'
            return if (formatter.currencySymbolPosition.value == CurrencyPosition.AFTER) {
                'R'
            } else {
                'L'
            }
        }
    }

    /**
     * JSON シリアライズ用: ptransaction_ を取得
     *
     * v1_32+ では常に String 型を使用
     */
    fun getTransactionIdForSerialization(): String = ptransaction_
}
