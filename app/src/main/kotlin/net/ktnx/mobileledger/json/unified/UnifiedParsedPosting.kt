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

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.ktnx.mobileledger.di.CurrencyFormatterEntryPoint
import net.ktnx.mobileledger.domain.model.CurrencyPosition
import net.ktnx.mobileledger.domain.model.TransactionLine
import net.ktnx.mobileledger.json.MoLeJson
import net.ktnx.mobileledger.json.config.ApiVersionConfig

/**
 * サロゲートクラス - ptransaction_ の型変換対応
 */
@Serializable
private data class UnifiedParsedPostingSurrogate(
    val pbalanceassertion: JsonElement? = null,
    val pstatus: String = "Unmarked",
    val paccount: String? = null,
    val pamount: List<UnifiedParsedAmount>? = null,
    val pdate: String? = null,
    val pdate2: String? = null,
    val ptype: String = "RegularPosting",
    val ptags: List<List<String>> = emptyList(),
    val poriginal: String? = null,
    val pcomment: String = "",
    @SerialName("ptransaction_")
    val ptransaction: JsonPrimitive? = null
)

/**
 * UnifiedParsedPosting 用のカスタムシリアライザ
 *
 * ptransaction_ の Int/String 両方に対応
 */
object UnifiedParsedPostingSerializer : KSerializer<UnifiedParsedPosting> {
    override val descriptor: SerialDescriptor =
        UnifiedParsedPostingSurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: UnifiedParsedPosting) {
        val surrogate = UnifiedParsedPostingSurrogate(
            pbalanceassertion = null,
            pstatus = value.pstatus,
            paccount = value.paccount,
            pamount = value.pamount,
            pdate = value.pdate,
            pdate2 = value.pdate2,
            ptype = value.ptype,
            ptags = value.ptags,
            poriginal = value.poriginal,
            pcomment = value.pcomment,
            ptransaction = JsonPrimitive(value.ptransaction_)
        )
        encoder.encodeSerializableValue(UnifiedParsedPostingSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): UnifiedParsedPosting {
        val surrogate = decoder.decodeSerializableValue(UnifiedParsedPostingSurrogate.serializer())

        // ptransaction_ を Int または String から String に変換
        val transactionId = surrogate.ptransaction?.let { prim ->
            prim.intOrNull?.toString() ?: prim.contentOrNull ?: "0"
        } ?: "0"

        return UnifiedParsedPosting(
            pstatus = surrogate.pstatus,
            paccount = surrogate.paccount,
            pamount = surrogate.pamount,
            pdate = surrogate.pdate,
            pdate2 = surrogate.pdate2,
            ptype = surrogate.ptype,
            ptags = surrogate.ptags,
            poriginal = surrogate.poriginal,
            pcomment = surrogate.pcomment.trim(),
            ptransaction_ = transactionId
        )
    }
}

/**
 * 統合 ParsedPosting - API バージョン v1_32+ の差分を吸収
 *
 * v1_32+ では ptransaction_ は String 型を使用
 */
@Serializable(with = UnifiedParsedPostingSerializer::class)
data class UnifiedParsedPosting(
    val pstatus: String = "Unmarked",
    val paccount: String? = null,
    val pamount: List<UnifiedParsedAmount>? = null,
    val pdate: String? = null,
    val pdate2: String? = null,
    val ptype: String = "RegularPosting",
    val ptags: List<List<String>> = emptyList(),
    val poriginal: String? = null,
    val pcomment: String = "",
    /**
     * トランザクションID（String として正規化）
     *
     * v1_32+ では常に String 型
     */
    @SerialName("ptransaction_")
    val ptransaction_: String = "0"
) {
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

    /**
     * JSON シリアライズ用: ptransaction_ を取得
     *
     * v1_32+ では常に String 型を使用
     */
    fun getTransactionIdForSerialization(): String = ptransaction_

    companion object {
        /**
         * ドメインモデルから Posting を生成
         *
         * @param line 変換元のトランザクション行
         * @param config API バージョン設定
         * @return 生成した UnifiedParsedPosting
         */
        @Suppress("UNUSED_PARAMETER")
        fun fromDomain(line: TransactionLine, config: ApiVersionConfig): UnifiedParsedPosting {
            val (commoditySide, commoditySpaced) = getCommoditySettings()
            val precision = 2
            val mantissa = Math.round((line.amount ?: 0f) * 100).toLong()

            val amount = UnifiedParsedAmount(
                acommodity = line.currency,
                aismultiplier = false,
                aquantity = UnifiedParsedQuantity(
                    decimalPlaces = precision,
                    decimalMantissa = mantissa
                ),
                astyle = UnifiedParsedStyle(
                    ascommodityside = commoditySide,
                    isAscommodityspaced = commoditySpaced,
                    asprecision = precision,
                    asdecimalmark = ".",
                    asrounding = "NoRounding"
                )
            )

            return UnifiedParsedPosting(
                paccount = line.accountName,
                pcomment = line.comment ?: "",
                pamount = listOf(amount)
            )
        }

        private fun getCommoditySettings(): Pair<Char, Boolean> {
            val formatter = CurrencyFormatterEntryPoint.getOrNull()
            val side = if (formatter?.currencySymbolPosition?.value == CurrencyPosition.AFTER) {
                'R'
            } else {
                'L'
            }
            val spaced = formatter?.currencyGap?.value ?: false
            return Pair(side, spaced)
        }
    }
}
