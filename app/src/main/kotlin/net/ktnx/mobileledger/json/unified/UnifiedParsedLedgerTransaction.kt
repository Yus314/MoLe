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
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.databind.JsonNode
import java.text.ParseException
import net.ktnx.mobileledger.domain.model.Transaction
import net.ktnx.mobileledger.json.config.ApiVersionConfig
import net.ktnx.mobileledger.utils.Globals
import net.ktnx.mobileledger.utils.Misc

/**
 * 統合 ParsedLedgerTransaction - API バージョン v1_32+ の差分を吸収
 *
 * バージョン間の差分:
 * - v1_32-v1_40: tsourcepos は単一オブジェクト
 * - v1_50: tsourcepos はリスト（開始位置と終了位置）
 * - v1_32+: ptransaction_ は String
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class UnifiedParsedLedgerTransaction {
    var tdate: String? = null
    var tdate2: String? = null
    var tdescription: String? = null
    var tcomment: String? = null
    var tcode: String = ""
    var tstatus: String = "Unmarked"
    var tprecedingcomment: String = ""
    var ttags: MutableList<List<String>> = mutableListOf()
    var tpostings: MutableList<UnifiedParsedPosting>? = null

    /**
     * ソース位置（リストとして正規化）
     *
     * v1_32-v1_40: 単一オブジェクト → 1要素のリストに変換
     * v1_50: リスト → そのまま
     */
    var tsourcepos: MutableList<UnifiedParsedSourcePos> = mutableListOf()

    private var _tindex: Int = 0

    /**
     * トランザクションインデックス
     *
     * setter で全ての posting の ptransaction_ も更新する
     */
    var tindex: Int
        get() = _tindex
        set(value) {
            _tindex = value
            tpostings?.forEach { it.setTransactionIdAsString(value.toString()) }
        }

    /**
     * tsourcepos を JSON から設定（v1_32+ 形式）
     */
    @JsonSetter("tsourcepos")
    fun setTsourceposFromJson(value: Any?) {
        tsourcepos.clear()
        when (value) {
            is List<*> -> {
                value.filterNotNull().forEach { item ->
                    val pos = parseSourcePos(item)
                    if (pos != null) {
                        tsourcepos.add(pos)
                    }
                }
            }

            is Map<*, *> -> {
                val pos = parseSourcePos(value)
                if (pos != null) {
                    tsourcepos.add(pos)
                }
            }

            is JsonNode -> {
                if (value.isArray) {
                    value.forEach { item ->
                        val pos = UnifiedParsedSourcePos().apply {
                            if (item.has("sourceName")) sourceName = item.get("sourceName").asText()
                            if (item.has("sourceLine")) sourceLine = item.get("sourceLine").asInt()
                            if (item.has("sourceColumn")) sourceColumn = item.get("sourceColumn").asInt()
                        }
                        tsourcepos.add(pos)
                    }
                } else if (value.isObject) {
                    val pos = UnifiedParsedSourcePos().apply {
                        if (value.has("sourceName")) sourceName = value.get("sourceName").asText()
                        if (value.has("sourceLine")) sourceLine = value.get("sourceLine").asInt()
                        if (value.has("sourceColumn")) sourceColumn = value.get("sourceColumn").asInt()
                    }
                    tsourcepos.add(pos)
                }
            }
        }

        // デフォルト値を設定（空の場合）
        if (tsourcepos.isEmpty()) {
            tsourcepos.add(UnifiedParsedSourcePos())
        }
    }

    private fun parseSourcePos(item: Any?): UnifiedParsedSourcePos? = when (item) {
        is Map<*, *> -> UnifiedParsedSourcePos().apply {
            sourceName = (item["sourceName"] as? String) ?: ""
            sourceLine = (item["sourceLine"] as? Number)?.toInt() ?: 1
            sourceColumn = (item["sourceColumn"] as? Number)?.toInt() ?: 1
        }

        else -> null
    }

    /**
     * Posting を追加
     */
    fun addPosting(posting: UnifiedParsedPosting) {
        posting.setTransactionIdAsString(tindex.toString())
        if (tpostings == null) {
            tpostings = mutableListOf()
        }
        tpostings?.add(posting)
    }

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

    companion object {
        /**
         * ドメインモデルから Transaction を生成
         *
         * @param tr 変換元のトランザクション
         * @param config API バージョン設定
         * @return 生成した UnifiedParsedLedgerTransaction
         */
        fun fromDomain(tr: Transaction, config: ApiVersionConfig): UnifiedParsedLedgerTransaction =
            UnifiedParsedLedgerTransaction().apply {
                tcomment = Misc.nullIsEmpty(tr.comment)
                tprecedingcomment = ""
                tpostings = tr.lines
                    .filter { it.accountName.isNotEmpty() }
                    .map { UnifiedParsedPosting.fromDomain(it, config) }
                    .toMutableList()
                tdate = Globals.formatIsoDate(tr.date)
                tdate2 = null
                tindex = 1
                tdescription = tr.description

                // tsourcepos の初期化（v1_50 用に2要素）
                tsourcepos.clear()
                tsourcepos.add(UnifiedParsedSourcePos())
                if (config == ApiVersionConfig.V1_50) {
                    val endPos = UnifiedParsedSourcePos()
                    tsourcepos.add(endPos)
                }
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

    /**
     * JSON シリアライズ用: ptransaction_ の型を取得
     *
     * v1_32+ では常に String 型を使用
     */
    fun getTransactionIdForPostingSerialization(): (Int) -> Any = { value -> value.toString() }
}
