/*
 * Copyright © 2026 Damyan Ivanov.
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

package net.ktnx.mobileledger.json.unified

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * 統合 ParsedSourcePos - hledger API v1_32+ 用
 *
 * v1_50 形式: sourceName + sourceLine + sourceColumn
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class UnifiedParsedSourcePos {
    /** ソースファイル名 */
    var sourceName: String = ""

    /** ソース行番号 */
    var sourceLine: Int = 1

    /** ソース列番号 */
    var sourceColumn: Int = 1
}
