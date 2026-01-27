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

package net.ktnx.mobileledger.core.network.json.unified

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import net.ktnx.mobileledger.core.network.json.MoLeJson

/**
 * 単一の期間エントリ: [date, balanceData]
 */
@Serializable
data class PeriodEntry(
    val date: String? = null,
    val balanceData: UnifiedParsedBalanceData? = null
)

/**
 * pdperiods のカスタムシリアライザ
 * 異種配列 [["0000-01-01", { balanceData }], ...] を処理する
 */
object PdPeriodsSerializer : KSerializer<List<PeriodEntry>> {
    override val descriptor: SerialDescriptor =
        ListSerializer(JsonElement.serializer()).descriptor

    override fun serialize(encoder: Encoder, value: List<PeriodEntry>) {
        val jsonEncoder = encoder as JsonEncoder
        val jsonArray = buildJsonArray {
            value.forEach { entry ->
                add(
                    buildJsonArray {
                        add(kotlinx.serialization.json.JsonPrimitive(entry.date ?: ""))
                        add(
                            MoLeJson.encodeToJsonElement(
                                UnifiedParsedBalanceData.serializer(),
                                entry.balanceData ?: UnifiedParsedBalanceData()
                            )
                        )
                    }
                )
            }
        }
        jsonEncoder.encodeJsonElement(jsonArray)
    }

    override fun deserialize(decoder: Decoder): List<PeriodEntry> {
        val jsonDecoder = decoder as JsonDecoder
        val jsonArray = jsonDecoder.decodeJsonElement().jsonArray
        val result = mutableListOf<PeriodEntry>()

        for (entryElement in jsonArray) {
            val entryArray = entryElement.jsonArray
            if (entryArray.size >= 2) {
                val date = entryArray[0].jsonPrimitive.content
                val balanceData = MoLeJson.decodeFromJsonElement(
                    UnifiedParsedBalanceData.serializer(),
                    entryArray[1]
                )
                result.add(PeriodEntry(date, balanceData))
            }
        }

        return result
    }
}

/**
 * v1_50 専用: アカウントデータ構造
 *
 * hledger-web 1.50+ で追加された adata フィールドの構造を表す。
 *
 * JSON 構造:
 * {
 *   "pdperiods": [["0000-01-01", { "bdincludingsubs": [...], "bdexcludingsubs": [...], "bdnumpostings": 1 }]],
 *   "pdpre": { "bdincludingsubs": [], "bdexcludingsubs": [], "bdnumpostings": 0 }
 * }
 */
@Serializable
data class UnifiedParsedAccountData(
    @Serializable(with = PdPeriodsSerializer::class)
    val pdperiods: List<PeriodEntry>? = null,
    val pdpre: UnifiedParsedBalanceData? = null
) {
    /**
     * 最初の期間エントリから残高データを取得
     * 通常、日付 "0000-01-01" の単一エントリが存在する
     */
    fun getFirstPeriodBalance(): UnifiedParsedBalanceData? = pdperiods?.firstOrNull()?.balanceData
}

/**
 * v1_50 専用: 残高データ構造
 *
 * pdperiods エントリと pdpre フィールドの両方で使用される。
 *
 * JSON 構造:
 * {
 *   "bdincludingsubs": [...],
 *   "bdexcludingsubs": [...],
 *   "bdnumpostings": 1
 * }
 */
@Serializable
data class UnifiedParsedBalanceData(
    val bdincludingsubs: List<UnifiedParsedBalance>? = null,
    val bdexcludingsubs: List<UnifiedParsedBalance>? = null,
    val bdnumpostings: Int = 0
)
