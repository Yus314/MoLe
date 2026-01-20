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

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.databind.JsonNode

/**
 * 統合 ParsedStyle - 全 API バージョンの差分を吸収
 *
 * バージョン間の差分:
 * - v1_14-v1_23: asdecimalpoint (Char)
 * - v1_32+: asdecimalmark (String)
 * - v1_19_1: asprecision が ParsedPrecision オブジェクト
 * - v1_32+: asrounding フィールド追加
 *
 * @JsonAlias でフィールド名の違いを吸収し、String に正規化して保持する。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class UnifiedParsedStyle {
    /** 通貨記号の位置（'L'=左, 'R'=右） */
    var ascommodityside: Char = '\u0000'

    /** 通貨記号と数値の間にスペースを入れるか */
    @get:JvmName("isAscommodityspaced")
    @JsonProperty("ascommodityspaced")
    var isAscommodityspaced: Boolean = false

    /** 桁グループ */
    var digitgroups: Int = 0

    /**
     * 小数点記号（String に正規化）
     *
     * v1_14-v1_23: asdecimalpoint (Char) → String に変換
     * v1_32+: asdecimalmark (String) → そのまま
     */
    @JsonAlias("asdecimalpoint")
    var asdecimalmark: String = "."

    /**
     * 小数点精度
     *
     * v1_19_1 では ParsedPrecision オブジェクトだが、Int に平坦化する
     */
    var asprecision: Int = 0

    /**
     * 丸めモード（v1_32+ で追加）
     */
    var asrounding: String? = null

    /**
     * asdecimalpoint (Char) から設定する場合のセッター
     *
     * v1_14-v1_23 では asdecimalpoint は Char 型だが、
     * Jackson が String として解析する場合に対応。
     */
    @JsonSetter("asdecimalpoint")
    fun setAsdecimalpointFromJson(value: Any?) {
        asdecimalmark = when (value) {
            is Char -> value.toString()
            is String -> if (value.isNotEmpty()) value else "."
            else -> "."
        }
    }

    /**
     * asprecision を JSON から設定（Int または ParsedPrecision オブジェクト対応）
     *
     * v1_19_1 では asprecision が {"tag": "Precision", "contents": N} 形式のオブジェクト。
     * 他のバージョンでは単純な Int。
     */
    @JsonSetter("asprecision")
    fun setAsprecisionFromJson(value: Any?) {
        asprecision = when (value) {
            is Int -> value

            is Number -> value.toInt()

            is JsonNode -> {
                // v1_19_1: { "tag": "Precision", "contents": N }
                if (value.isObject && value.has("contents")) {
                    value.get("contents")?.asInt() ?: 0
                } else if (value.isInt) {
                    value.asInt()
                } else {
                    0
                }
            }

            is Map<*, *> -> {
                // Map として来た場合（v1_19_1 の ParsedPrecision）
                (value["contents"] as? Number)?.toInt() ?: 0
            }

            else -> 0
        }
    }
}
