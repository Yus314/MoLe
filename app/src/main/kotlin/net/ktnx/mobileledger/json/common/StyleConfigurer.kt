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

import net.ktnx.mobileledger.json.unified.UnifiedParsedStyle

/**
 * Strategy interface for version-specific style configuration.
 *
 * Configures amount formatting style for hledger API v1_32+.
 */
sealed interface StyleConfigurer {
    /**
     * Configure style parameters on the given style object.
     * @param style The ParsedStyle-like object to configure
     * @param precision The decimal precision to set
     */
    fun configureStyle(style: Any, precision: Int)

    /**
     * v1_32, v1_40, v1_50 - uses asdecimalmark (String) + asrounding
     */
    data object DecimalMarkString : StyleConfigurer {
        override fun configureStyle(style: Any, precision: Int) {
            if (style is UnifiedParsedStyle) {
                style.asprecision = precision
                style.asdecimalmark = "."
                style.asrounding = "NoRounding"
            }
        }
    }
}
