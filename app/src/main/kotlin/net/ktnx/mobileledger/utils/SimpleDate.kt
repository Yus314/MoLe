/*
 * Copyright © 2020 Damyan Ivanov.
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

package net.ktnx.mobileledger.utils

import java.util.Calendar
import java.util.Date
import java.util.Locale

data class SimpleDate(@JvmField val year: Int, @JvmField val month: Int, @JvmField val day: Int) :
    Comparable<SimpleDate> {

    fun toCalendar(): Calendar = Calendar.getInstance().apply {
        set(year, month - 1, day)
    }

    fun toDate(): Date = toCalendar().time

    fun earlierThan(date: SimpleDate): Boolean {
        if (year < date.year) return true
        if (year > date.year) return false
        if (month < date.month) return true
        if (month > date.month) return false
        return day < date.day
    }

    fun laterThan(date: SimpleDate): Boolean {
        if (year > date.year) return true
        if (year < date.year) return false
        if (month > date.month) return true
        if (month < date.month) return false
        return day > date.day
    }

    override fun compareTo(other: SimpleDate): Int {
        var res = year.compareTo(other.year)
        if (res != 0) return res

        res = month.compareTo(other.month)
        if (res != 0) return res

        return day.compareTo(other.day)
    }

    fun asCalendar(): Calendar = Calendar.getInstance().apply {
        set(year, month, day)
    }

    override fun toString(): String = String.format(Locale.US, "%d-%02d-%02d", year, month, day)

    companion object {
        @JvmStatic
        fun fromDate(date: Date): SimpleDate {
            val calendar = Calendar.getInstance().apply { time = date }
            return fromCalendar(calendar)
        }

        @JvmStatic
        fun fromCalendar(calendar: Calendar): SimpleDate = SimpleDate(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DATE)
        )

        @JvmStatic
        fun today(): SimpleDate = fromCalendar(Calendar.getInstance())
    }
}
