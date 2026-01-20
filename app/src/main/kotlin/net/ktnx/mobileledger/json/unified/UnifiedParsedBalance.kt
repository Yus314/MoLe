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
 * 統合 ParsedBalance - 全 API バージョンで同一構造
 *
 * 勘定科目の残高を表す基本構造。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class UnifiedParsedBalance {
    /** 金額数値 */
    var aquantity: UnifiedParsedQuantity? = null

    /** 通貨/商品コード */
    private var _acommodity: String? = null
    var acommodity: String
        get() = _acommodity ?: ""
        set(value) {
            _acommodity = value
        }

    /** 金額スタイル */
    var astyle: UnifiedParsedStyle? = null
}
