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

package net.ktnx.mobileledger.domain.model

/**
 * 通貨の表示位置
 */
enum class CurrencyPosition {
    /** 金額の前に表示 ($100) */
    BEFORE,

    /** 金額の後に表示 (100円) */
    AFTER;

    /**
     * enum を Int に変換
     */
    fun toInt(): Int = ordinal

    companion object {
        /**
         * Int から enum に変換
         * 無効な値の場合は AFTER を返す
         */
        fun fromInt(value: Int): CurrencyPosition = entries.getOrNull(value) ?: AFTER
    }
}
