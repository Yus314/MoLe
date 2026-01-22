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
 * Sealed interface abstracting the transaction ID type for hledger API v1_32+.
 *
 * All supported API versions (v1_32, v1_40, v1_50) use String transaction IDs.
 */
sealed interface TransactionIdType {
    /**
     * Get the default value for the transaction ID.
     */
    val defaultValue: Any

    /**
     * Convert an Int index to the appropriate type.
     */
    fun fromIndex(index: Int): Any

    /**
     * String transaction ID type (v1_32+)
     */
    data object StringType : TransactionIdType {
        override val defaultValue: String = "1"
        override fun fromIndex(index: Int): String = index.toString()
    }
}
