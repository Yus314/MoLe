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
 * Delegate class holding common posting fields shared across all API versions.
 *
 * This class encapsulates the common properties that all ParsedPosting implementations
 * share, enabling code reuse through delegation pattern.
 *
 * All API versions (v1_14 through v1_50) have identical fields except for:
 * - ptransaction_: Int in v1_14-v1_23, String in v1_32-v1_50
 * - Style configuration (handled separately by StyleConfigurer)
 */
class PostingFieldDelegate {
    var pbalanceassertion: Void? = null
    var pstatus: String = "Unmarked"
    var paccount: String? = null
    var pdate: String? = null
    var pdate2: String? = null
    var ptype: String = "RegularPosting"
    var poriginal: String? = null

    private var _pcomment: String = ""
    var pcomment: String
        get() = _pcomment
        set(value) {
            _pcomment = value.trim()
        }

    var ptags: MutableList<List<String>> = mutableListOf()
}
