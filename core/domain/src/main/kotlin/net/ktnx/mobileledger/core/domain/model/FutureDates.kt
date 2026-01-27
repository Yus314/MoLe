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
 * 将来日付の許可設定
 */
enum class FutureDates(private val value: Int) {
    /** 将来日付を許可しない */
    None(0),

    /** 1週間先まで許可 */
    OneWeek(7),

    /** 2週間先まで許可 */
    TwoWeeks(14),

    /** 1ヶ月先まで許可 */
    OneMonth(30),

    /** 2ヶ月先まで許可 */
    TwoMonths(60),

    /** 3ヶ月先まで許可 */
    ThreeMonths(90),

    /** 6ヶ月先まで許可 */
    SixMonths(180),

    /** 1年先まで許可 */
    OneYear(365),

    /** 全ての将来日付を許可 */
    All(-1);

    /**
     * enum を Int (日数) に変換
     */
    fun toInt(): Int = value

    companion object {
        private val map: Map<Int, FutureDates> = entries.associateBy { it.value }

        /**
         * Int (日数) から enum に変換
         * 無効な値の場合は None を返す
         */
        fun fromInt(value: Int): FutureDates = map[value] ?: None
    }
}
