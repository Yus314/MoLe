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

/**
 * 統合 ParsedStyle - hledger API v1_32+ 用
 *
 * v1_32+ で使用されるフィールド:
 * - asdecimalmark (String): 小数点記号
 * - asrounding: 丸めモード
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
     * 小数点記号（v1_32+）
     */
    var asdecimalmark: String = "."

    /**
     * 小数点精度
     */
    var asprecision: Int = 0

    /**
     * 丸めモード（v1_32+）
     */
    var asrounding: String? = null

    /**
     * asprecision を JSON から設定
     */
    @JsonSetter("asprecision")
    fun setAsprecisionFromJson(value: Any?) {
        asprecision = when (value) {
            is Int -> value
            is Number -> value.toInt()
            else -> 0
        }
    }
}
