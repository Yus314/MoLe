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
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

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
@JsonIgnoreProperties(ignoreUnknown = true)
class UnifiedParsedAccountData {
    @JsonDeserialize(using = PdPeriodsDeserializer::class)
    var pdperiods: List<PeriodEntry>? = null
    var pdpre: UnifiedParsedBalanceData? = null

    /**
     * 最初の期間エントリから残高データを取得
     * 通常、日付 "0000-01-01" の単一エントリが存在する
     */
    fun getFirstPeriodBalance(): UnifiedParsedBalanceData? = pdperiods?.firstOrNull()?.balanceData

    /**
     * 単一の期間エントリ: [date, balanceData]
     */
    data class PeriodEntry(
        var date: String? = null,
        var balanceData: UnifiedParsedBalanceData? = null
    )

    /**
     * pdperiods のカスタムデシリアライザ
     * 異種配列 [["0000-01-01", { balanceData }], ...] を処理する
     */
    class PdPeriodsDeserializer : JsonDeserializer<List<PeriodEntry>>() {
        override fun deserialize(p: JsonParser, ctxt: DeserializationContext): List<PeriodEntry> {
            val result = mutableListOf<PeriodEntry>()
            val mapper = p.codec as ObjectMapper
            val arrayNode: JsonNode = mapper.readTree(p)

            if (arrayNode.isArray) {
                for (entryNode in arrayNode) {
                    if (entryNode.isArray && entryNode.size() >= 2) {
                        val date = entryNode.get(0).asText()
                        val balanceData = mapper.treeToValue(
                            entryNode.get(1),
                            UnifiedParsedBalanceData::class.java
                        )
                        result.add(PeriodEntry(date, balanceData))
                    }
                }
            }

            return result
        }
    }
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
@JsonIgnoreProperties(ignoreUnknown = true)
class UnifiedParsedBalanceData {
    var bdincludingsubs: List<UnifiedParsedBalance>? = null
    var bdexcludingsubs: List<UnifiedParsedBalance>? = null
    var bdnumpostings: Int = 0
}
