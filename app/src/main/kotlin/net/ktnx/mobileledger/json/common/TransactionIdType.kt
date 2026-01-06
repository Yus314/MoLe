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
 * Sealed interface abstracting the different transaction ID types across API versions.
 *
 * Group A (v1_14, v1_15, v1_19_1, v1_23): ptransaction_ is Int
 * Group B (v1_32, v1_40, v1_50): ptransaction_ is String
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
     * Integer transaction ID type (Group A: v1_14 - v1_23)
     */
    object IntType : TransactionIdType {
        override val defaultValue: Int = 0
        override fun fromIndex(index: Int): Int = index
    }

    /**
     * String transaction ID type (Group B: v1_32 - v1_50)
     */
    object StringType : TransactionIdType {
        override val defaultValue: String = "1"
        override fun fromIndex(index: Int): String = index.toString()
    }
}
