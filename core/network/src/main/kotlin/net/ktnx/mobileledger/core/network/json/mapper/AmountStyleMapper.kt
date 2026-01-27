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

package net.ktnx.mobileledger.core.network.json.mapper

import net.ktnx.mobileledger.core.domain.model.AmountStyle
import net.ktnx.mobileledger.core.network.json.unified.UnifiedParsedStyle

/**
 * 統合 ParsedStyle から AmountStyle ドメインモデルへの変換
 *
 * 全 API バージョンで UnifiedParsedStyle を使用する場合、
 * このマッパーで一元的にドメインモデルに変換する。
 */
object AmountStyleMapper {

    /**
     * UnifiedParsedStyle から AmountStyle に変換
     *
     * @param parsedStyle パース済みのスタイル（null の場合は null を返す）
     * @param currency 通貨コード（空の場合は NONE ポジション）
     * @return AmountStyle ドメインモデル、または null
     */
    fun toDomain(parsedStyle: UnifiedParsedStyle?, currency: String?): AmountStyle? {
        if (parsedStyle == null) return null

        val position = determinePosition(parsedStyle.ascommodityside, currency)
        val spaced = parsedStyle.isAscommodityspaced
        val precision = parsedStyle.asprecision
        val decimalMark = parsedStyle.asdecimalmark

        return AmountStyle(position, spaced, precision, decimalMark)
    }

    /**
     * 通貨位置を決定
     *
     * @param side 'L' = 左（前）, 'R' = 右（後）
     * @param currency 通貨コード
     * @return Position enum
     */
    private fun determinePosition(side: Char, currency: String?): AmountStyle.Position = when {
        currency.isNullOrEmpty() -> AmountStyle.Position.NONE
        side == 'L' -> AmountStyle.Position.BEFORE
        side == 'R' -> AmountStyle.Position.AFTER
        else -> AmountStyle.Position.NONE
    }
}
