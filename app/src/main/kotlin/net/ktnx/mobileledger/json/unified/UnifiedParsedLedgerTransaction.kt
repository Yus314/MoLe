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

import java.text.ParseException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.ktnx.mobileledger.domain.model.Transaction
import net.ktnx.mobileledger.json.MoLeJson
import net.ktnx.mobileledger.json.config.ApiVersionConfig
import net.ktnx.mobileledger.utils.Globals
import net.ktnx.mobileledger.utils.Misc

/**
 * サロゲートクラス - tsourcepos の型変換対応
 */
@Serializable
private data class UnifiedParsedLedgerTransactionSurrogate(
    val tdate: String? = null,
    val tdate2: String? = null,
    val tdescription: String? = null,
    val tcomment: String? = null,
    val tcode: String = "",
    val tstatus: String = "Unmarked",
    val tprecedingcomment: String = "",
    val ttags: List<List<String>> = emptyList(),
    val tpostings: List<UnifiedParsedPosting>? = null,
    val tsourcepos: JsonElement? = null,
    val tindex: Int = 0
)

/**
 * tsourcepos のカスタムシリアライザ
 *
 * v1_32-v1_40: 単一オブジェクト
 * v1_50: リスト
 */
object TsourceposSerializer : KSerializer<List<UnifiedParsedSourcePos>> {
    override val descriptor: SerialDescriptor =
        JsonElement.serializer().descriptor

    override fun serialize(encoder: Encoder, value: List<UnifiedParsedSourcePos>) {
        val jsonEncoder = encoder as JsonEncoder
        val jsonArray = buildJsonArray {
            value.forEach { pos ->
                add(MoLeJson.encodeToJsonElement(UnifiedParsedSourcePos.serializer(), pos))
            }
        }
        jsonEncoder.encodeJsonElement(jsonArray)
    }

    override fun deserialize(decoder: Decoder): List<UnifiedParsedSourcePos> {
        val jsonDecoder = decoder as JsonDecoder
        val element = jsonDecoder.decodeJsonElement()
        return parseSourcePosElement(element)
    }

    private fun parseSourcePosElement(element: JsonElement): List<UnifiedParsedSourcePos> = when (element) {
        is JsonArray -> {
            element.mapNotNull { item ->
                when (item) {
                    is JsonObject -> parseSourcePosObject(item)
                    else -> null
                }
            }
        }

        is JsonObject -> {
            listOfNotNull(parseSourcePosObject(element))
        }

        else -> listOf(UnifiedParsedSourcePos())
    }

    private fun parseSourcePosObject(obj: JsonObject): UnifiedParsedSourcePos = UnifiedParsedSourcePos(
        sourceName = obj["sourceName"]?.jsonPrimitive?.content ?: "",
        sourceLine = obj["sourceLine"]?.jsonPrimitive?.intOrNull ?: 1,
        sourceColumn = obj["sourceColumn"]?.jsonPrimitive?.intOrNull ?: 1
    )
}

/**
 * UnifiedParsedLedgerTransaction 用のカスタムシリアライザ
 */
object UnifiedParsedLedgerTransactionSerializer : KSerializer<UnifiedParsedLedgerTransaction> {
    override val descriptor: SerialDescriptor =
        UnifiedParsedLedgerTransactionSurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: UnifiedParsedLedgerTransaction) {
        val surrogate = UnifiedParsedLedgerTransactionSurrogate(
            tdate = value.tdate,
            tdate2 = value.tdate2,
            tdescription = value.tdescription,
            tcomment = value.tcomment,
            tcode = value.tcode,
            tstatus = value.tstatus,
            tprecedingcomment = value.tprecedingcomment,
            ttags = value.ttags,
            tpostings = value.tpostings,
            tsourcepos = buildJsonArray {
                value.tsourcepos.forEach { pos ->
                    add(MoLeJson.encodeToJsonElement(UnifiedParsedSourcePos.serializer(), pos))
                }
            },
            tindex = value.tindex
        )
        encoder.encodeSerializableValue(UnifiedParsedLedgerTransactionSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): UnifiedParsedLedgerTransaction {
        val surrogate = decoder.decodeSerializableValue(UnifiedParsedLedgerTransactionSurrogate.serializer())

        // tsourcepos を解析
        val sourcePosList = surrogate.tsourcepos?.let { element ->
            TsourceposSerializer.run {
                when (element) {
                    is JsonArray -> element.mapNotNull { item ->
                        if (item is JsonObject) parseSourcePosObject(item) else null
                    }

                    is JsonObject -> listOf(parseSourcePosObject(element))

                    else -> listOf(UnifiedParsedSourcePos())
                }
            }
        } ?: listOf(UnifiedParsedSourcePos())

        return UnifiedParsedLedgerTransaction(
            tdate = surrogate.tdate,
            tdate2 = surrogate.tdate2,
            tdescription = surrogate.tdescription,
            tcomment = surrogate.tcomment,
            tcode = surrogate.tcode,
            tstatus = surrogate.tstatus,
            tprecedingcomment = surrogate.tprecedingcomment,
            ttags = surrogate.ttags,
            tpostings = surrogate.tpostings,
            tsourcepos = sourcePosList,
            tindex = surrogate.tindex
        )
    }

    private fun parseSourcePosObject(obj: JsonObject): UnifiedParsedSourcePos = UnifiedParsedSourcePos(
        sourceName = obj["sourceName"]?.jsonPrimitive?.content ?: "",
        sourceLine = obj["sourceLine"]?.jsonPrimitive?.intOrNull ?: 1,
        sourceColumn = obj["sourceColumn"]?.jsonPrimitive?.intOrNull ?: 1
    )
}

/**
 * 統合 ParsedLedgerTransaction - API バージョン v1_32+ の差分を吸収
 *
 * バージョン間の差分:
 * - v1_32-v1_40: tsourcepos は単一オブジェクト
 * - v1_50: tsourcepos はリスト（開始位置と終了位置）
 * - v1_32+: ptransaction_ は String
 */
@Serializable(with = UnifiedParsedLedgerTransactionSerializer::class)
data class UnifiedParsedLedgerTransaction(
    val tdate: String? = null,
    val tdate2: String? = null,
    val tdescription: String? = null,
    val tcomment: String? = null,
    val tcode: String = "",
    val tstatus: String = "Unmarked",
    val tprecedingcomment: String = "",
    val ttags: List<List<String>> = emptyList(),
    val tpostings: List<UnifiedParsedPosting>? = null,
    /**
     * ソース位置（リストとして正規化）
     *
     * v1_32-v1_40: 単一オブジェクト → 1要素のリストに変換
     * v1_50: リスト → そのまま
     */
    val tsourcepos: List<UnifiedParsedSourcePos> = listOf(UnifiedParsedSourcePos()),
    /**
     * トランザクションインデックス
     */
    val tindex: Int = 0
) {
    /**
     * ドメインモデルに変換
     */
    @Throws(ParseException::class)
    fun toDomain(): Transaction {
        val date = tdate?.let { Globals.parseIsoDate(it) }
            ?: throw ParseException("Transaction date is required", 0)

        return Transaction(
            id = null,
            ledgerId = tindex.toLong(),
            date = date,
            description = tdescription ?: "",
            comment = tcomment?.trim()?.takeIf { it.isNotEmpty() },
            lines = tpostings?.map { it.toDomain() } ?: emptyList()
        )
    }

    /**
     * JSON シリアライズ用: ptransaction_ の型を取得
     *
     * v1_32+ では常に String 型を使用
     */
    fun getTransactionIdForPostingSerialization(): (Int) -> Any = { value -> value.toString() }

    companion object {
        /**
         * ドメインモデルから Transaction を生成
         *
         * @param tr 変換元のトランザクション
         * @param config API バージョン設定
         * @return 生成した UnifiedParsedLedgerTransaction
         */
        fun fromDomain(tr: Transaction, config: ApiVersionConfig): UnifiedParsedLedgerTransaction {
            val postings = tr.lines
                .filter { it.accountName.isNotEmpty() }
                .map { UnifiedParsedPosting.fromDomain(it, config) }

            // tsourcepos の初期化（v1_50 用に2要素）
            val sourcePosList = if (config == ApiVersionConfig.V1_50) {
                listOf(UnifiedParsedSourcePos(), UnifiedParsedSourcePos())
            } else {
                listOf(UnifiedParsedSourcePos())
            }

            return UnifiedParsedLedgerTransaction(
                tcomment = Misc.nullIsEmpty(tr.comment),
                tprecedingcomment = "",
                tpostings = postings,
                tdate = Globals.formatIsoDate(tr.date),
                tdate2 = null,
                tindex = 1,
                tdescription = tr.description,
                tsourcepos = sourcePosList
            )
        }

        /**
         * JSON シリアライズ時に tsourcepos を API バージョンに応じた形式で取得
         */
        fun getSourcePosForSerialization(transaction: UnifiedParsedLedgerTransaction, config: ApiVersionConfig): Any =
            if (config == ApiVersionConfig.V1_50) {
                // v1_50: リスト形式
                transaction.tsourcepos
            } else {
                // v1_32-v1_40: 単一オブジェクト
                transaction.tsourcepos.firstOrNull() ?: UnifiedParsedSourcePos()
            }
    }
}
