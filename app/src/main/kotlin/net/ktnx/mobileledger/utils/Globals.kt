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

import android.app.Activity
import android.content.Context
import android.view.inputmethod.InputMethodManager
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.regex.Pattern

object Globals {
    private val dateFormatter = ThreadLocal<SimpleDateFormat>().apply {
        set(SimpleDateFormat("yyyy/MM/dd", Locale.US))
    }

    private val isoDateFormatter = ThreadLocal<SimpleDateFormat>().apply {
        set(SimpleDateFormat("yyyy-MM-dd", Locale.US))
    }

    @JvmField
    var monthNames: Array<String>? = null

    @JvmField
    val developerEmail = "dam+mole-crash@ktnx.net"

    private val reLedgerDate = Pattern.compile("^(?:(?:(\\d+)/)??(\\d\\d?)/)?(\\d\\d?)$")

    @JvmStatic
    fun hideSoftKeyboard(act: Activity) {
        val v = act.currentFocus
        if (v != null) {
            val imm = act.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(v.windowToken, 0)
        }
    }

    @JvmStatic
    @Throws(ParseException::class)
    fun parseLedgerDate(dateString: String): SimpleDate {
        val m = reLedgerDate.matcher(dateString)
        if (!m.matches()) {
            throw ParseException(
                "'$dateString' does not match expected pattern '$reLedgerDate'", 0
            )
        }

        val yearStr = m.group(1)
        val monthStr = m.group(2)
        val dayStr = m.group(3)

        val year: Int
        val month: Int

        if (yearStr == null) {
            val today = SimpleDate.today()
            year = today.year
            month = if (monthStr == null) {
                today.month
            } else {
                monthStr.toInt()
            }
        } else {
            year = yearStr.toInt()
            requireNotNull(monthStr)
            month = monthStr.toInt()
        }

        requireNotNull(dayStr)
        val day = dayStr.toInt()

        return SimpleDate(year, month, day)
    }

    @JvmStatic
    @Throws(ParseException::class)
    fun parseLedgerDateAsCalendar(dateString: String) = parseLedgerDate(dateString).toCalendar()

    @JvmStatic
    @Throws(ParseException::class)
    fun parseIsoDate(dateString: String): SimpleDate {
        val date = isoDateFormatter.get()?.parse(dateString)
            ?: throw ParseException("Failed to parse ISO date: $dateString", 0)
        return SimpleDate.fromDate(date)
    }

    @JvmStatic
    fun formatLedgerDate(date: SimpleDate): String {
        return dateFormatter.get()?.format(date.toDate()) ?: ""
    }

    @JvmStatic
    fun formatIsoDate(date: SimpleDate): String {
        return isoDateFormatter.get()?.format(date.toDate()) ?: ""
    }
}
