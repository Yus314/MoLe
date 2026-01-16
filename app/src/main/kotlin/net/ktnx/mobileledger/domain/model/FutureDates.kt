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
 * 将来日付の許可設定
 */
enum class FutureDates {
    /** 将来日付を許可しない */
    None,

    /** 1週間先まで許可 */
    OneWeek,

    /** 2週間先まで許可 */
    TwoWeeks,

    /** 1ヶ月先まで許可 */
    OneMonth,

    /** 全ての将来日付を許可 */
    All;

    /**
     * enum を Int に変換
     */
    fun toInt(): Int = ordinal

    companion object {
        /**
         * Int から enum に変換
         * 無効な値の場合は None を返す
         */
        fun fromInt(value: Int): FutureDates = entries.getOrNull(value) ?: None
    }
}
