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

package net.ktnx.mobileledger.ui

import android.app.Dialog
import android.os.Bundle
import android.widget.CalendarView
import androidx.appcompat.app.AppCompatDialogFragment
import net.ktnx.mobileledger.R
import net.ktnx.mobileledger.model.FutureDates
import net.ktnx.mobileledger.utils.SimpleDate
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.regex.Pattern

class DatePickerFragment : AppCompatDialogFragment(), CalendarView.OnDateChangeListener {
    private val presentDate: Calendar = GregorianCalendar.getInstance()
    private var onDatePickedListener: DatePickedListener? = null
    private var minDate: Long = 0
    private var maxDate: Long = Long.MAX_VALUE

    fun setDateRange(minDate: SimpleDate?, maxDate: SimpleDate?) {
        this.minDate = minDate?.toDate()?.time ?: 0
        this.maxDate = maxDate?.toDate()?.time ?: Long.MAX_VALUE
    }

    fun setFutureDates(futureDates: FutureDates) {
        if (futureDates == FutureDates.All) {
            maxDate = Long.MAX_VALUE
        } else {
            val dateLimit = GregorianCalendar.getInstance()
            when (futureDates) {
                FutureDates.None -> { /* already there */ }
                FutureDates.OneWeek -> dateLimit.add(Calendar.DAY_OF_MONTH, 7)
                FutureDates.TwoWeeks -> dateLimit.add(Calendar.DAY_OF_MONTH, 14)
                FutureDates.OneMonth -> dateLimit.add(Calendar.MONTH, 1)
                FutureDates.TwoMonths -> dateLimit.add(Calendar.MONTH, 2)
                FutureDates.ThreeMonths -> dateLimit.add(Calendar.MONTH, 3)
                FutureDates.SixMonths -> dateLimit.add(Calendar.MONTH, 6)
                FutureDates.OneYear -> dateLimit.add(Calendar.YEAR, 1)
                else -> { /* no-op */ }
            }
            maxDate = dateLimit.time.time
        }
    }

    fun setCurrentDateFromText(present: CharSequence) {
        val now = GregorianCalendar.getInstance()
        var year = now.get(GregorianCalendar.YEAR)
        var month = now.get(GregorianCalendar.MONTH)
        var day = now.get(GregorianCalendar.DAY_OF_MONTH)

        var m = reYMD.matcher(present)
        if (m.matches()) {
            // Groups are guaranteed to exist when matches() returns true for these patterns
            year = m.group(1)?.toIntOrNull() ?: year
            month = (m.group(2)?.toIntOrNull() ?: (month + 1)) - 1   // month is 0-based
            day = m.group(3)?.toIntOrNull() ?: day
        } else {
            m = reMD.matcher(present)
            if (m.matches()) {
                month = (m.group(1)?.toIntOrNull() ?: (month + 1)) - 1
                day = m.group(2)?.toIntOrNull() ?: day
            } else {
                m = reD.matcher(present)
                if (m.matches()) {
                    day = m.group(1)?.toIntOrNull() ?: day
                }
            }
        }

        presentDate.set(year, month, day)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dpd = Dialog(requireActivity())
        dpd.setContentView(R.layout.date_picker_view)
        dpd.setTitle(null)
        val cv = dpd.findViewById<CalendarView>(R.id.calendarView)
        cv.date = presentDate.time.time

        cv.minDate = minDate
        cv.maxDate = maxDate

        cv.setOnDateChangeListener(this)

        return dpd
    }

    override fun onSelectedDayChange(view: CalendarView, year: Int, month: Int, dayOfMonth: Int) {
        dismiss()
        onDatePickedListener?.onDatePicked(year, month, dayOfMonth)
    }

    fun setOnDatePickedListener(listener: DatePickedListener?) {
        onDatePickedListener = listener
    }

    fun interface DatePickedListener {
        fun onDatePicked(year: Int, month: Int, day: Int)
    }

    companion object {
        @JvmField
        val reYMD: Pattern = Pattern.compile("^\\s*(\\d+)\\d*/\\s*(\\d+)\\s*/\\s*(\\d+)\\s*$")
        @JvmField
        val reMD: Pattern = Pattern.compile("^\\s*(\\d+)\\s*/\\s*(\\d+)\\s*$")
        @JvmField
        val reD: Pattern = Pattern.compile("\\s*(\\d+)\\s*$")
    }
}
