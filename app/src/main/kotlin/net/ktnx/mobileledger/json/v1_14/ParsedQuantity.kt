/*
 * Copyright © 2020 Damyan Ivanov.
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

package net.ktnx.mobileledger.json.v1_14

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import kotlin.math.pow

@JsonIgnoreProperties(ignoreUnknown = true)
open class ParsedQuantity() {
    var decimalMantissa: Long = 0
    var decimalPlaces: Int = 0

    constructor(input: String) : this() {
        parseString(input)
    }

    fun asFloat(): Float = (decimalMantissa * 10.0.pow(-decimalPlaces.toDouble())).toFloat()

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
