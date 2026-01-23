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
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

// Thread-local date formatters for thread-safe parsing/formatting
private val ledgerDateFormatter: ThreadLocal<SimpleDateFormat> = ThreadLocal.withInitial {
    SimpleDateFormat("yyyy/MM/dd", Locale.US)
}

private val isoDateFormatter: ThreadLocal<SimpleDateFormat> = ThreadLocal.withInitial {
    SimpleDateFormat("yyyy-MM-dd", Locale.US)
}

private val reLedgerDate = Pattern.compile("^(?:(?:(\\d+)/)??(\\d\\d?)/)?(\\d\\d?)$")

/**
 * Parse a ledger date string (yyyy/MM/dd format) to SimpleDate.
 * Supports partial dates like "15" (day only) or "3/15" (month/day).
 */
@Throws(ParseException::class)
fun String.parseLedgerDate(): SimpleDate {
    val m = reLedgerDate.matcher(this)
    if (!m.matches()) {
        throw ParseException("'$this' does not match expected ledger date pattern", 0)
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

/**
 * Parse a ledger date string to Calendar.
 */
@Throws(ParseException::class)
fun String.parseLedgerDateAsCalendar(): Calendar = this.parseLedgerDate().toCalendar()

/**
 * Parse an ISO date string (yyyy-MM-dd format) to SimpleDate.
 */
@Throws(ParseException::class)
fun String.parseIsoDate(): SimpleDate {
    val date = isoDateFormatter.get()?.parse(this)
        ?: throw ParseException("Failed to parse ISO date: $this", 0)
    return SimpleDate.fromDate(date)
}

/**
 * Format SimpleDate to ledger date string (yyyy/MM/dd).
 */
fun SimpleDate.formatLedgerDate(): String = ledgerDateFormatter.get()?.format(toDate()) ?: ""

/**
 * Format SimpleDate to ISO date string (yyyy-MM-dd).
 */
fun SimpleDate.formatIsoDate(): String = isoDateFormatter.get()?.format(toDate()) ?: ""

/**
 * Hide the soft keyboard for this activity.
 */
fun Activity.hideSoftKeyboard() {
    val v = currentFocus
    if (v != null) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(v.windowToken, 0)
    }
}

/**
 * Global constants for the application.
 */
object Globals {
    const val developerEmail = "dam+mole-crash@ktnx.net"
}
