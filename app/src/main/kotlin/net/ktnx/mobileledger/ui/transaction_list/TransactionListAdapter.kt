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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale
import net.ktnx.mobileledger.databinding.LastUpdateLayoutBinding
import net.ktnx.mobileledger.databinding.TransactionDelimiterBinding
import net.ktnx.mobileledger.databinding.TransactionListRowBinding
import net.ktnx.mobileledger.model.TransactionListItem
import net.ktnx.mobileledger.utils.Logger
import net.ktnx.mobileledger.utils.Misc

class TransactionListAdapter : RecyclerView.Adapter<TransactionRowHolderBase>() {

    private val listDiffer: AsyncListDiffer<TransactionListItem>

    init {
        setHasStableIds(true)

        listDiffer = AsyncListDiffer(
            this,
            object : DiffUtil.ItemCallback<TransactionListItem>() {
            override fun areItemsTheSame(
                oldItem: TransactionListItem,
                newItem: TransactionListItem
            ): Boolean {
                if (oldItem.type != newItem.type) {
                    return false
                }
                return when (oldItem.type) {
                    TransactionListItem.Type.DELIMITER ->
                        oldItem.date == newItem.date

                    TransactionListItem.Type.TRANSACTION ->
                        oldItem.getTransaction().ledgerId == newItem.getTransaction().ledgerId

                    TransactionListItem.Type.HEADER ->
                        true // there can be only one header
                }
            }

            override fun areContentsTheSame(
                oldItem: TransactionListItem,
                newItem: TransactionListItem
            ): Boolean = when (oldItem.type) {
                    TransactionListItem.Type.DELIMITER ->
                        oldItem.isMonthShown == newItem.isMonthShown

                    TransactionListItem.Type.TRANSACTION ->
                        oldItem.getTransaction() == newItem.getTransaction() &&
                                Misc.equalStrings(oldItem.boldAccountName, newItem.boldAccountName) &&
                                Misc.equalStrings(oldItem.runningTotal, newItem.runningTotal)

                    TransactionListItem.Type.HEADER ->
                        // headers don't differ in their contents. they observe the last update
                        // date and react to its changes
                        true
                }
        }
        )
    }

    override fun getItemId(position: Int): Long {
        val item = listDiffer.currentList[position]
        return when (item.type) {
            TransactionListItem.Type.HEADER -> -1L
            TransactionListItem.Type.TRANSACTION -> item.getTransaction().ledgerId
            TransactionListItem.Type.DELIMITER -> -item.date.toDate().time
        }
    }

    override fun getItemViewType(position: Int): Int = listDiffer.currentList[position].type.ordinal

    override fun onBindViewHolder(holder: TransactionRowHolderBase, position: Int) {
        val item = listDiffer.currentList.getOrNull(position)

        // in a race when transaction value is reduced, but the model hasn't been notified yet
        // the view will disappear when the notifications reaches the model, so by simply omitting
        // the out-of-range get() call nothing bad happens - just a to-be-deleted view remains
        // a bit longer
        if (item == null) {
            return
        }

        val newType = item.type

        when (newType) {
            TransactionListItem.Type.TRANSACTION ->
                holder.asTransaction().bind(item, item.boldAccountName)

            TransactionListItem.Type.DELIMITER ->
                holder.asDelimiter().bind(item)

            TransactionListItem.Type.HEADER ->
                holder.asHeader().bind()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionRowHolderBase {
        val inflater = LayoutInflater.from(parent.context)

        return when (TransactionListItem.Type.valueOf(viewType)) {
            TransactionListItem.Type.TRANSACTION ->
                TransactionRowHolder(
                    TransactionListRowBinding.inflate(inflater, parent, false)
                )

            TransactionListItem.Type.DELIMITER ->
                TransactionListDelimiterRowHolder(
                    TransactionDelimiterBinding.inflate(inflater, parent, false)
                )

            TransactionListItem.Type.HEADER ->
                TransactionListLastUpdateRowHolder(
                    LastUpdateLayoutBinding.inflate(inflater, parent, false)
                )
        }
    }

    override fun getItemCount(): Int = listDiffer.currentList.size

    fun setTransactions(newList: List<TransactionListItem>) {
        Logger.debug(
            "transactions",
            String.format(Locale.US, "Got new transaction list (%d items)", newList.size)
        )
        listDiffer.submitList(newList)
    }
}
