/*
 * Copyright © 2024 Damyan Ivanov.
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

package net.ktnx.mobileledger.ui.transaction_list

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import java.text.DateFormat
import java.util.GregorianCalendar
import java.util.TimeZone
import net.ktnx.mobileledger.App
import net.ktnx.mobileledger.databinding.TransactionDelimiterBinding
import net.ktnx.mobileledger.model.TransactionListItem
import net.ktnx.mobileledger.utils.DimensionUtils
import net.ktnx.mobileledger.utils.Globals

internal class TransactionListDelimiterRowHolder(private val b: TransactionDelimiterBinding) :
    TransactionRowHolderBase(b.root) {

    fun bind(item: TransactionListItem) {
        val date = item.date
        b.transactionDelimiterDate.text = DateFormat.getDateInstance()
            .format(date.toDate())

        if (item.isMonthShown) {
            val cal = GregorianCalendar(TimeZone.getDefault())
            cal.time = date.toDate()
            App.prepareMonthNames()
            b.transactionDelimiterMonth.text = Globals.monthNames?.get(cal.get(GregorianCalendar.MONTH))
            b.transactionDelimiterMonth.visibility = View.VISIBLE
            b.transactionDelimiterThick.visibility = View.VISIBLE
            val lp = b.transactionDelimiterThick.layoutParams as ConstraintLayout.LayoutParams
            lp.height = DimensionUtils.dp2px(b.root.context, 4f)
            b.transactionDelimiterThick.layoutParams = lp
        } else {
            b.transactionDelimiterMonth.visibility = View.GONE
            val lp = b.transactionDelimiterThick.layoutParams as ConstraintLayout.LayoutParams
            lp.height = DimensionUtils.dp2px(b.root.context, 1.3f)
            b.transactionDelimiterThick.layoutParams = lp
            b.transactionDelimiterThick.visibility = View.VISIBLE
        }
    }
}
