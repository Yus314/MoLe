/*
 * Copyright © 2021 Damyan Ivanov.
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

package net.ktnx.mobileledger.model

import android.content.res.Resources
import android.util.SparseArray
import net.ktnx.mobileledger.R

enum class FutureDates(private val value: Int) {
    None(0),
    OneWeek(7),
    TwoWeeks(14),
    OneMonth(30),
    TwoMonths(60),
    ThreeMonths(90),
    SixMonths(180),
    OneYear(365),
    All(-1);

    fun toInt(): Int = value

    fun getText(resources: Resources): String = when (value) {
        7 -> resources.getString(R.string.future_dates_7)
        14 -> resources.getString(R.string.future_dates_14)
        30 -> resources.getString(R.string.future_dates_30)
        60 -> resources.getString(R.string.future_dates_60)
        90 -> resources.getString(R.string.future_dates_90)
        180 -> resources.getString(R.string.future_dates_180)
        365 -> resources.getString(R.string.future_dates_365)
        -1 -> resources.getString(R.string.future_dates_all)
        else -> resources.getString(R.string.future_dates_none)
    }

    companion object {
        private val map = SparseArray<FutureDates>()

        init {
            for (item in values()) {
                map.put(item.value, item)
            }
        }

        @JvmStatic
        fun valueOf(i: Int): FutureDates = map.get(i, None)
    }
}
