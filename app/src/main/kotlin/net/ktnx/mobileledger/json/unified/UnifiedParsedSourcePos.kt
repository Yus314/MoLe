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
 * 統合 ParsedSourcePos - 全 API バージョンで同一構造
 *
 * hledger ジャーナルファイル内のソース位置を表す。
 * contents は ["ファイル名", [行番号, 列番号]] の形式。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class UnifiedParsedSourcePos {
    /** タグ（通常は "JournalSourcePos"） */
    var tag: String = "JournalSourcePos"

    /** ソース位置情報 ["filename", [line, column]] */
    var contents: MutableList<Any> = mutableListOf("", arrayOf(1, 1))
}
