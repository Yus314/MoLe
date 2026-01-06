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
 * Delegate class holding common transaction fields shared across all API versions.
 *
 * This class encapsulates the common properties that all ParsedLedgerTransaction
 * implementations share, enabling code reuse through delegation pattern.
 */
class TransactionFieldDelegate {
    var tdate: String? = null
    var tdate2: String? = null
    var tdescription: String? = null
    var tcomment: String? = null
    var tcode: String = ""
    var tstatus: String = "Unmarked"
    var tprecedingcomment: String = ""
    var ttags: MutableList<List<String>> = mutableListOf()
}
