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

package net.ktnx.mobileledger.core.domain.model

/**
 * 通貨フォーマット設定のインターフェース。
 *
 * JSON シリアライズ時のデフォルト値を提供。
 * CurrencyFormatter が実装し、Gateway/UseCase からテスト可能な形で
 * 通貨設定を渡すために使用。
 */
interface CurrencySettings {
    /**
     * 通貨記号の表示位置
     */
    val symbolPosition: CurrencyPosition

    /**
     * 通貨記号と金額の間にスペースがあるかどうか
     */
    val hasGap: Boolean

    companion object {
        /**
         * デフォルト設定（通貨記号なし、スペースなし）
         *
         * テストやフォールバック用のデフォルト実装
         */
        val DEFAULT: CurrencySettings = object : CurrencySettings {
            override val symbolPosition: CurrencyPosition = CurrencyPosition.BEFORE
            override val hasGap: Boolean = false
        }
    }
}
