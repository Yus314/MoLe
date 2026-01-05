/*
 * Copyright © 2025 Damyan Ivanov.
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

package net.ktnx.mobileledger.model

import net.ktnx.mobileledger.utils.Misc

/**
 * Represents the display style for currency amounts.
 * Holds information about currency symbol position, spacing, decimal precision, and decimal mark.
 */
class AmountStyle(
    val commodityPosition: Position,
    val isCommoditySpaced: Boolean,
    val precision: Int,
    val decimalMark: String
) {
    /**
     * Position of the currency symbol relative to the amount
     */
    enum class Position {
        BEFORE,  // Currency symbol before the amount (e.g., "$100")
        AFTER,   // Currency symbol after the amount (e.g., "100円")
        NONE     // No currency symbol
    }

    /**
     * Serializes the AmountStyle to a string for database storage
     */
    fun serialize(): String {
        val posStr = when (commodityPosition) {
            Position.BEFORE -> "BEFORE"
            Position.AFTER -> "AFTER"
            Position.NONE -> "NONE"
        }
        val mark = if (Misc.emptyIsNull(decimalMark) == null) "." else decimalMark
        return String.format("%s:%b:%d:%s", posStr, isCommoditySpaced, precision, mark)
    }

    companion object {
        /**
         * Creates an AmountStyle from hledger's ParsedStyle JSON object (v1.50+)
         */
        @JvmStatic
        fun fromParsedStyle(
            parsedStyle: net.ktnx.mobileledger.json.v1_50.ParsedStyle?,
            currency: String?
        ): AmountStyle? {
            if (parsedStyle == null) return null

            val position = determinePosition(parsedStyle.ascommodityside, currency)
            val spaced = parsedStyle.isAscommodityspaced
            val precision = parsedStyle.asprecision
            val decimalMark = parsedStyle.asdecimalmark

            return AmountStyle(position, spaced, precision, decimalMark)
        }

        /**
         * Creates an AmountStyle from hledger's ParsedStyle JSON object (v1.40)
         */
        @JvmStatic
        fun fromParsedStyle(
            parsedStyle: net.ktnx.mobileledger.json.v1_40.ParsedStyle?,
            currency: String?
        ): AmountStyle? {
            if (parsedStyle == null) return null

            val position = determinePosition(parsedStyle.ascommodityside, currency)
            val spaced = parsedStyle.isAscommodityspaced
            val precision = parsedStyle.asprecision
            val decimalMark = parsedStyle.asdecimalmark

            return AmountStyle(position, spaced, precision, decimalMark)
        }

        /**
         * Creates an AmountStyle from hledger's ParsedStyle JSON object (v1.32)
         */
        @JvmStatic
        fun fromParsedStyle(
            parsedStyle: net.ktnx.mobileledger.json.v1_32.ParsedStyle?,
            currency: String?
        ): AmountStyle? {
            if (parsedStyle == null) return null

            val position = determinePosition(parsedStyle.ascommodityside, currency)
            val spaced = parsedStyle.isAscommodityspaced
            val precision = parsedStyle.asprecision
            val decimalMark = parsedStyle.asdecimalmark

            return AmountStyle(position, spaced, precision, decimalMark)
        }

        /**
         * Creates an AmountStyle from hledger's ParsedStyle JSON object (base/old versions)
         */
        @JvmStatic
        fun fromParsedStyle(
            parsedStyle: net.ktnx.mobileledger.json.ParsedStyle?,
            currency: String?
        ): AmountStyle? {
            if (parsedStyle == null) return null

            val position = determinePosition(parsedStyle.ascommodityside, currency)
            val spaced = parsedStyle.isAscommodityspaced

            // Handle decimal mark from ParsedStyle
            val decimalMark = when (parsedStyle.asdecimalpoint) {
                ',' -> ","
                '.' -> "."
                else -> "."
            }

            // Get precision - default to 2 if not specified in older versions
            val precision = 2

            return AmountStyle(position, spaced, precision, decimalMark)
        }

        /**
         * Helper method to determine currency position from side character
         */
        private fun determinePosition(side: Char, currency: String?): Position {
            return when {
                currency.isNullOrEmpty() -> Position.NONE
                side == 'L' -> Position.BEFORE
                side == 'R' -> Position.AFTER
                else -> Position.NONE
            }
        }

        /**
         * Gets the default AmountStyle based on global settings
         */
        @JvmStatic
        fun getDefault(currency: String?): AmountStyle {
            val globalPos = Data.currencySymbolPosition.value

            val position = when {
                currency.isNullOrEmpty() -> Position.NONE
                globalPos == Currency.Position.before -> Position.BEFORE
                globalPos == Currency.Position.after -> Position.AFTER
                else -> Position.NONE
            }

            val gap = Data.currencyGap.value
            val spaced = gap ?: true

            // Default precision is 2 decimal places
            val precision = 2

            // Default decimal mark is period
            val decimalMark = "."

            return AmountStyle(position, spaced, precision, decimalMark)
        }

        /**
         * Deserializes an AmountStyle from a database string
         */
        @JvmStatic
        fun deserialize(serialized: String?): AmountStyle? {
            if (serialized.isNullOrEmpty()) return null

            return try {
                val parts = serialized.split(":")
                if (parts.size != 4) return null

                val position = when (parts[0]) {
                    "BEFORE" -> Position.BEFORE
                    "AFTER" -> Position.AFTER
                    "NONE" -> Position.NONE
                    else -> return null
                }

                val spaced = parts[1].toBoolean()
                val precision = parts[2].toInt()
                val decimalMark = parts[3]

                AmountStyle(position, spaced, precision, decimalMark)
            } catch (e: Exception) {
                null
            }
        }
    }
}
