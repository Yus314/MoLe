/*
 * Copyright © 2024 Damyan Ivanov.
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

package net.ktnx.mobileledger.json.common

/**
 * Strategy interface for version-specific style configuration.
 *
 * Different hledger API versions use different JSON field names and structures
 * for amount formatting. This sealed interface abstracts those differences.
 */
sealed interface StyleConfigurer {
    /**
     * Configure style parameters on the given style object.
     * @param style The ParsedStyle-like object to configure (accepts Any to support different version classes)
     * @param precision The decimal precision to set
     */
    fun configureStyle(style: Any, precision: Int)

    /**
     * Group A: v1_14, v1_15 - uses asdecimalpoint (Char)
     */
    object DecimalPointChar : StyleConfigurer {
        override fun configureStyle(style: Any, precision: Int) {
            when (style) {
                is net.ktnx.mobileledger.json.v1_14.ParsedStyle -> {
                    style.asprecision = precision
                    style.asdecimalpoint = '.'
                }
                is net.ktnx.mobileledger.json.v1_15.ParsedStyle -> {
                    style.asprecision = precision
                    style.asdecimalpoint = '.'
                }
            }
        }
    }

    /**
     * v1_19_1 only - uses asdecimalpoint (Char) with ParsedPrecision object
     */
    object DecimalPointCharWithParsedPrecision : StyleConfigurer {
        override fun configureStyle(style: Any, precision: Int) {
            when (style) {
                is net.ktnx.mobileledger.json.v1_19_1.ParsedStyle -> {
                    style.asprecision = net.ktnx.mobileledger.json.v1_19_1.ParsedPrecision(precision)
                    style.asdecimalpoint = '.'
                }
            }
        }
    }

    /**
     * v1_23 - uses asdecimalpoint (Char) with Int precision
     */
    object DecimalPointCharIntPrecision : StyleConfigurer {
        override fun configureStyle(style: Any, precision: Int) {
            when (style) {
                is net.ktnx.mobileledger.json.v1_23.ParsedStyle -> {
                    style.asprecision = precision
                    style.asdecimalpoint = '.'
                }
            }
        }
    }

    /**
     * Group B: v1_32, v1_40, v1_50 - uses asdecimalmark (String) + asrounding
     */
    object DecimalMarkString : StyleConfigurer {
        override fun configureStyle(style: Any, precision: Int) {
            when (style) {
                is net.ktnx.mobileledger.json.v1_32.ParsedStyle -> {
                    style.asprecision = precision
                    style.asdecimalmark = "."
                    style.asrounding = "NoRounding"
                }
                is net.ktnx.mobileledger.json.v1_40.ParsedStyle -> {
                    style.asprecision = precision
                    style.asdecimalmark = "."
                    style.asrounding = "NoRounding"
                }
                is net.ktnx.mobileledger.json.v1_50.ParsedStyle -> {
                    style.asprecision = precision
                    style.asdecimalmark = "."
                    style.asrounding = "NoRounding"
                }
            }
        }
    }
}
