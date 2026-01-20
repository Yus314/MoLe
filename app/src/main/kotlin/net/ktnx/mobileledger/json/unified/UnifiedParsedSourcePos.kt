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
 * 統合 ParsedSourcePos - 全 API バージョンの差分を吸収
 *
 * バージョン間の差分:
 * - v1_14-v1_40: tag + contents 形式 (contents は ["filename", [line, column]])
 * - v1_50: sourceName + sourceLine + sourceColumn 形式
 *
 * 両方の形式をサポートし、Jackson が自動的に該当フィールドをマッピングする。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class UnifiedParsedSourcePos {
    // v1_14-v1_40 形式
    /** タグ（通常は "JournalSourcePos"） */
    var tag: String = "JournalSourcePos"

    /** ソース位置情報 ["filename", [line, column]] */
    var contents: MutableList<Any> = mutableListOf("", arrayOf(1, 1))

    // v1_50 形式

    /** ソースファイル名 (v1_50+) */
    var sourceName: String = ""

    /** ソース行番号 (v1_50+) */
    var sourceLine: Int = 1

    /** ソース列番号 (v1_50+) */
    var sourceColumn: Int = 1
}
