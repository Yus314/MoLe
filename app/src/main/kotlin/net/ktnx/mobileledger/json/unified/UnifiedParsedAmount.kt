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

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * 統合 ParsedAmount - 全 API バージョンの差分を吸収
 *
 * バージョン間の差分:
 * - v1_14-v1_40: aprice フィールド
 * - v1_50: acost フィールド（同じ意味）
 *
 * @JsonAlias で両方のフィールド名に対応する。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class UnifiedParsedAmount {
    /** 通貨/商品コード */
    var acommodity: String? = null

    /** 金額数値 */
    var aquantity: UnifiedParsedQuantity? = null

    /** 乗数かどうか */
    var aismultiplier: Boolean = false

    /** 金額スタイル */
    var astyle: UnifiedParsedStyle? = null

    /**
     * 価格情報
     *
     * v1_14-v1_40: aprice
     * v1_50: acost
     */
    @JsonAlias("acost")
    var aprice: UnifiedParsedPrice? = null
}
