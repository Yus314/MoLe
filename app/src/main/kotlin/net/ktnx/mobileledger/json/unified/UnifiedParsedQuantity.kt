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
import kotlin.math.pow

/**
 * 統合 ParsedQuantity - 全 API バージョンで同一構造
 *
 * hledger の金額数値を表現する。整数の仮数部と小数点以下桁数で構成される。
 * 例: 1234.56 → decimalMantissa=123456, decimalPlaces=2
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class UnifiedParsedQuantity() {
    /** 仮数部（整数表現） */
    var decimalMantissa: Long = 0

    /** 小数点以下の桁数 */
    var decimalPlaces: Int = 0

    /**
     * 文字列から初期化
     */
    constructor(input: String) : this() {
        parseString(input)
    }

    /**
     * Float 値に変換
     */
    fun asFloat(): Float = (decimalMantissa * 10.0.pow(-decimalPlaces.toDouble())).toFloat()

    /**
     * 文字列をパースして値を設定
     */
    fun parseString(input: String) {
        val pointPos = input.indexOf('.')
        if (pointPos >= 0) {
            val integral = input.replace(".", "")
            decimalMantissa = integral.toLong()
            decimalPlaces = input.length - pointPos - 1
        } else {
            decimalMantissa = input.toLong()
            decimalPlaces = 0
        }
    }
}
