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
 * 統合 ParsedPrice - 全 API バージョンで同一構造
 *
 * 価格/為替レート情報を表す。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class UnifiedParsedPrice {
    /** 価格タグ（"NoPrice", "UnitPrice", "TotalPrice"） */
    var tag: String = "NoPrice"

    /** 価格の詳細内容 */
    internal var contents: Contents? = null

    @JsonIgnoreProperties(ignoreUnknown = true)
    internal class Contents {
        var aprice: UnifiedParsedPrice? = null
        var aquantity: UnifiedParsedQuantity? = null
        var acommodity: String = ""
        var aismultiplier: Boolean = false
        var astyle: UnifiedParsedStyle? = null
    }
}
