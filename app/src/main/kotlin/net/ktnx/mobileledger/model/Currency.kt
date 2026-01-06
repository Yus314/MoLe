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

package net.ktnx.mobileledger.model

import net.ktnx.mobileledger.utils.Misc

class Currency(
    val id: Int,
    var name: String?,
    var position: Position = Position.after,
    var hasGap: Boolean = true
) {
    constructor(id: Int, name: String?) : this(id, name, Position.after, true)

    enum class Position {
        before,
        after,
        unknown,
        none
    }

    companion object {
        @JvmStatic
        fun equal(left: Currency?, right: Currency?): Boolean = if (left == null) {
            right == null
        } else {
            left == right
        }

        @JvmStatic
        fun equal(left: Currency?, right: String?): Boolean {
            val normalizedRight = Misc.emptyIsNull(right)
            return if (left == null) {
                normalizedRight == null
            } else {
                val leftName = Misc.emptyIsNull(left.name)
                if (leftName == null) {
                    normalizedRight == null
                } else {
                    leftName == normalizedRight
                }
            }
        }
    }
}
