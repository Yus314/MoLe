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

package net.ktnx.mobileledger.ui.transaction_list

import android.app.Activity
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import net.ktnx.mobileledger.R
import net.ktnx.mobileledger.databinding.TransactionListRowBinding
import net.ktnx.mobileledger.model.TransactionListItem
import net.ktnx.mobileledger.utils.Colors
import net.ktnx.mobileledger.utils.Misc

internal class TransactionRowHolder(
    private val b: TransactionListRowBinding
) : TransactionRowHolderBase(b.root) {

    var lastType: TransactionListItem.Type? = null

    fun bind(item: TransactionListItem, boldAccountName: String?) {
        val tr = item.getTransaction()
        b.transactionRowDescription.text = tr.description
        val trComment = Misc.emptyIsNull(tr.comment)
        if (trComment == null) {
            b.transactionComment.visibility = View.GONE
        } else {
            b.transactionComment.text = trComment
            b.transactionComment.visibility = View.VISIBLE
        }

        if (Misc.emptyIsNull(item.runningTotal) != null) {
            b.transactionRunningTotal.text = item.runningTotal
            b.transactionRunningTotal.visibility = View.VISIBLE
            b.transactionRunningTotalDivider.visibility = View.VISIBLE
        } else {
            b.transactionRunningTotal.visibility = View.GONE
            b.transactionRunningTotalDivider.visibility = View.GONE
        }

        var rowIndex = 0
        val ctx = b.root.context
        val inflater = (ctx as Activity).layoutInflater
        for (acc in tr.accounts) {
            var row = b.transactionRowAccAmounts.getChildAt(rowIndex) as? LinearLayout
            if (row == null) {
                row = LinearLayout(ctx)
                inflater.inflate(R.layout.transaction_list_row_accounts_table_row, row)
                b.transactionRowAccAmounts.addView(row)
            }

            val dummyText = row.findViewById<TextView>(R.id.dummy_text)
            val accName = row.findViewById<TextView>(R.id.transaction_list_acc_row_acc_name)
            val accComment = row.findViewById<TextView>(R.id.transaction_list_acc_row_acc_comment)
            val accAmount = row.findViewById<TextView>(R.id.transaction_list_acc_row_acc_amount)

            if (boldAccountName != null && acc.accountName.startsWith(boldAccountName)) {
                accName.setTextColor(Colors.primary)
                accAmount.setTextColor(Colors.primary)

                val ss = SpannableString(Misc.addWrapHints(acc.accountName))
                val boldLength = Misc.addWrapHints(boldAccountName)?.length ?: 0
                ss.setSpan(
                    StyleSpan(Typeface.BOLD),
                    0,
                    boldLength,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                accName.text = ss
            } else {
                val textColor = dummyText.textColors.defaultColor
                accName.setTextColor(textColor)
                accAmount.setTextColor(textColor)
                accName.text = Misc.addWrapHints(acc.accountName)
            }

            val comment = acc.comment
            if (!comment.isNullOrEmpty()) {
                accComment.text = comment
                accComment.visibility = View.VISIBLE
            } else {
                accComment.visibility = View.GONE
            }
            accAmount.text = acc.toString()

            rowIndex++
        }

        if (b.transactionRowAccAmounts.childCount > rowIndex) {
            b.transactionRowAccAmounts.removeViews(
                rowIndex,
                b.transactionRowAccAmounts.childCount - rowIndex
            )
        }
    }
}
