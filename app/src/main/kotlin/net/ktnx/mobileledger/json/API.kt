/*
 * Copyright © 2020, 2024 Damyan Ivanov.
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

package net.ktnx.mobileledger.json

/**
 * Supported hledger-web API versions.
 *
 * As of this version, only API versions v1_32 and later are supported.
 * Earlier versions (v1_14, v1_15, v1_19_1, v1_23) and HTML form fallback
 * have been removed.
 *
 * Note: This class is Android-free to support future core module extraction.
 * Use [net.ktnx.mobileledger.ui.util.ApiDescriptions] for localized UI strings.
 */
enum class API(private val value: Int) {
    auto(0),
    v1_32(-6),
    v1_40(-7),
    v1_50(-8);

    fun toInt(): Int = value

    /**
     * Short description for logging/debugging (non-localized).
     */
    val description: String
        get() = when (this) {
            auto -> "(automatic)"
            v1_32 -> "1.32"
            v1_40 -> "1.40"
            v1_50 -> "1.50"
        }

    companion object {
        private val map: Map<Int, API> = entries.associateBy { it.value }

        @JvmField
        val allVersions = arrayOf(v1_50, v1_40, v1_32)

        /**
         * Convert integer value to API enum.
         *
         * Legacy values (html, v1_14, v1_15, v1_19_1, v1_23) are mapped to auto
         * for backward compatibility after database migration.
         */
        @JvmStatic
        fun valueOf(i: Int): API = map[i] ?: auto
    }
}
