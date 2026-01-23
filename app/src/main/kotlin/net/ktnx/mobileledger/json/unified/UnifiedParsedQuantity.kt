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

import kotlin.math.pow
import kotlinx.serialization.Serializable

/**
 * 統合 ParsedQuantity - 全 API バージョンで同一構造
 *
 * hledger の金額数値を表現する。整数の仮数部と小数点以下桁数で構成される。
 * 例: 1234.56 → decimalMantissa=123456, decimalPlaces=2
 */
@Serializable
data class UnifiedParsedQuantity(
    /** 仮数部（整数表現） */
    val decimalMantissa: Long = 0,
    /** 小数点以下の桁数 */
    val decimalPlaces: Int = 0
) {
    /**
     * Float 値に変換
     */
    fun asFloat(): Float = (decimalMantissa * 10.0.pow(-decimalPlaces.toDouble())).toFloat()

    companion object {
        /**
         * 文字列から UnifiedParsedQuantity を生成
         */
        fun fromString(input: String): UnifiedParsedQuantity {
            val pointPos = input.indexOf('.')
            return if (pointPos >= 0) {
                val integral = input.replace(".", "")
                UnifiedParsedQuantity(
                    decimalMantissa = integral.toLong(),
                    decimalPlaces = input.length - pointPos - 1
                )
            } else {
                UnifiedParsedQuantity(
                    decimalMantissa = input.toLong(),
                    decimalPlaces = 0
                )
            }
        }
    }
}
