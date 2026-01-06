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

package net.ktnx.mobileledger.json.v1_32

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Account declaration information (hledger-web v1.32+)
 * Indicates where in the journal file the account was declared
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class ParsedDeclarationInfo(
    var file: String? = null,
    var line: Int = 0
) {
    override fun toString(): String = "${file ?: "unknown"}:$line"
}
