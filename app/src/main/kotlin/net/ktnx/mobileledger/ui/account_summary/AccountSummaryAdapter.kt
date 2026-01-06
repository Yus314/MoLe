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

package net.ktnx.mobileledger.ui.account_summary

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale
import net.ktnx.mobileledger.R
import net.ktnx.mobileledger.dao.BaseDAO
import net.ktnx.mobileledger.databinding.AccountListRowBinding
import net.ktnx.mobileledger.databinding.AccountListSummaryRowBinding
import net.ktnx.mobileledger.db.DB
import net.ktnx.mobileledger.model.AccountListItem
import net.ktnx.mobileledger.model.LedgerAccount
import net.ktnx.mobileledger.ui.activity.MainActivity
import net.ktnx.mobileledger.utils.Logger
import net.ktnx.mobileledger.utils.Logger.debug
import net.ktnx.mobileledger.utils.Misc

class AccountSummaryAdapter : RecyclerView.Adapter<AccountSummaryAdapter.RowHolder>() {
    private val listDiffer: AsyncListDiffer<AccountListItem>

    init {
        setHasStableIds(true)

        listDiffer = AsyncListDiffer(
            this,
            object : DiffUtil.ItemCallback<AccountListItem>() {
            override fun getChangePayload(oldItem: AccountListItem, newItem: AccountListItem): Any? {
                val changes = Change()

                val oldAcc = oldItem.toAccount().account
                val newAcc = newItem.toAccount().account

                if (!Misc.equalStrings(oldAcc.name, newAcc.name)) {
                    changes.add(Change.NAME)
                }

                if (oldAcc.level != newAcc.level) {
                    changes.add(Change.LEVEL)
                }

                if (oldAcc.isExpanded != newAcc.isExpanded) {
                    changes.add(Change.EXPANDED)
                }

                if (oldAcc.amountsExpanded() != newAcc.amountsExpanded()) {
                    changes.add(Change.EXPANDED_AMOUNTS)
                }

                if (oldAcc.getAmountsString() != newAcc.getAmountsString()) {
                    changes.add(Change.AMOUNTS)
                }

                return changes.toPayload()
            }

            override fun areItemsTheSame(oldItem: AccountListItem, newItem: AccountListItem): Boolean {
                val oldType = oldItem.type
                val newType = newItem.type
                if (oldType != newType) return false
                if (oldType == AccountListItem.Type.HEADER) return true

                return oldItem.toAccount().account.id == newItem.toAccount().account.id
            }

            override fun areContentsTheSame(oldItem: AccountListItem, newItem: AccountListItem): Boolean = oldItem.sameContent(newItem)
        }
        )
    }

    override fun getItemId(position: Int): Long {
        if (position == 0) return 0
        return listDiffer.currentList[position]
            .toAccount()
            .account
            .id
    }

    override fun onBindViewHolder(holder: RowHolder, position: Int, payloads: MutableList<Any>) {
        holder.bind(listDiffer.currentList[position], payloads)
        super.onBindViewHolder(holder, position, payloads)
    }

    override fun onBindViewHolder(holder: RowHolder, position: Int) {
        holder.bind(listDiffer.currentList[position], null)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            ITEM_TYPE_HEADER -> HeaderRowHolder(
                AccountListSummaryRowBinding.inflate(inflater, parent, false)
            )

            ITEM_TYPE_ACCOUNT -> AccountRowHolder(
                AccountListRowBinding.inflate(inflater, parent, false)
            )

            else -> throw IllegalStateException("Unexpected value: $viewType")
        }
    }

    override fun getItemCount(): Int = listDiffer.currentList.size

    override fun getItemViewType(position: Int): Int = if (position == 0) ITEM_TYPE_HEADER else ITEM_TYPE_ACCOUNT

    fun setAccounts(newList: List<AccountListItem>) {
        Misc.onMainThread { listDiffer.submitList(newList) }
    }

    class Change(private var value: Int = 0) {
        fun add(bits: Int) {
            value = value or bits
        }

        fun add(change: Change) {
            value = value or change.value
        }

        fun toPayload(): Change? = if (value == 0) null else this

        fun has(bits: Int): Boolean = value == 0 || (value and bits) == bits

        companion object {
            const val NAME = 1
            const val EXPANDED = 1 shl 1
            const val LEVEL = 1 shl 2
            const val EXPANDED_AMOUNTS = 1 shl 3
            const val AMOUNTS = 1 shl 4
        }
    }

    abstract class RowHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(accountListItem: AccountListItem, payloads: List<Any>?)
    }

    class HeaderRowHolder(private val b: AccountListSummaryRowBinding) : RowHolder(b.root) {
        override fun bind(item: AccountListItem, payloads: List<Any>?) {
            (item as AccountListItem.Header).text
                .observe(itemView.context as LifecycleOwner) { text ->
                    b.lastUpdateText.text = text
                }
        }
    }

    inner class AccountRowHolder(private val b: AccountListRowBinding) : RowHolder(b.root) {
        init {
            itemView.setOnLongClickListener { onItemLongClick(it) }
            b.accountRowAccName.setOnLongClickListener { onItemLongClick(it) }
            b.accountRowAccAmounts.setOnLongClickListener { onItemLongClick(it) }
            b.accountExpanderContainer.setOnLongClickListener { onItemLongClick(it) }
            b.accountExpander.setOnLongClickListener { onItemLongClick(it) }

            b.accountRowAccName.setOnClickListener { toggleAccountExpanded() }
            b.accountExpanderContainer.setOnClickListener { toggleAccountExpanded() }
            b.accountExpander.setOnClickListener { toggleAccountExpanded() }
            b.accountRowAccAmounts.setOnClickListener { toggleAmountsExpanded() }
        }

        private fun toggleAccountExpanded() {
            val account = getAccount()
            if (!account.hasSubAccounts()) return
            debug("accounts", "Account expander clicked")

            BaseDAO.runAsync {
                val dbo = account.toDBO()
                dbo.expanded = !dbo.expanded
                Logger.debug(
                    "accounts",
                    String.format(
                        Locale.ROOT,
                        "%s (%d) → %s",
                        account.name,
                        dbo.id,
                        if (dbo.expanded) "expanded" else "collapsed"
                    )
                )
                DB.get()
                    .getAccountDAO()
                    .updateSync(dbo)
            }
        }

        private fun getAccount(): LedgerAccount = listDiffer.currentList[bindingAdapterPosition]
                .toAccount()
                .account

        private fun toggleAmountsExpanded() {
            val account = getAccount()
            if (account.amountCount <= AMOUNT_LIMIT) return

            account.toggleAmountsExpanded()
            if (account.amountsExpanded()) {
                b.accountRowAccAmounts.text = account.getAmountsString()
                b.accountRowAmountsExpanderContainer.visibility = View.GONE
            } else {
                b.accountRowAccAmounts.text = account.getAmountsString(AMOUNT_LIMIT)
                b.accountRowAmountsExpanderContainer.visibility = View.VISIBLE
            }

            BaseDAO.runAsync {
                val dbo = account.toDBO()
                DB.get()
                    .getAccountDAO()
                    .updateSync(dbo)
            }
        }

        private fun onItemLongClick(v: View): Boolean {
            val activity = v.context as MainActivity
            val builder = AlertDialog.Builder(activity)
            val accountName = getAccount().name
            builder.setTitle(accountName)
            builder.setItems(R.array.acc_ctx_menu) { dialog, which ->
                if (which == 0) { // show transactions
                    activity.showAccountTransactions(accountName)
                } else {
                    throw RuntimeException(String.format("Unknown menu item id (%d)", which))
                }
                dialog.dismiss()
            }
            builder.show()
            return true
        }

        override fun bind(item: AccountListItem, payloads: List<Any>?) {
            val acc = item.toAccount().account

            val changes = Change()
            payloads?.forEach { p ->
                if (p is Change) {
                    changes.add(p)
                }
            }

            val rm = b.root.context.resources

            if (changes.has(Change.NAME)) {
                b.accountRowAccName.text = acc.shortName
            }

            if (changes.has(Change.LEVEL)) {
                val lp = b.flowWrapper.layoutParams as ConstraintLayout.LayoutParams
                lp.marginStart = acc.level * rm.getDimensionPixelSize(R.dimen.thumb_row_height) / 3
            }

            if (acc.hasSubAccounts()) {
                b.accountExpanderContainer.visibility = View.VISIBLE

                if (changes.has(Change.EXPANDED)) {
                    val wantedRotation = if (acc.isExpanded) 0f else 180f
                    if (b.accountExpanderContainer.rotation != wantedRotation) {
                        b.accountExpanderContainer.animate().rotation(wantedRotation)
                    }
                }
            } else {
                b.accountExpanderContainer.visibility = View.GONE
            }

            if (changes.has(Change.EXPANDED_AMOUNTS)) {
                val amounts = acc.amountCount
                if ((amounts > AMOUNT_LIMIT) && !acc.amountsExpanded()) {
                    b.accountRowAccAmounts.text = acc.getAmountsString(AMOUNT_LIMIT)
                    b.accountRowAmountsExpanderContainer.visibility = View.VISIBLE
                } else {
                    b.accountRowAccAmounts.text = acc.getAmountsString()
                    b.accountRowAmountsExpanderContainer.visibility = View.GONE
                }
            }
        }
    }

    companion object {
        const val AMOUNT_LIMIT = 3
        private const val ITEM_TYPE_HEADER = 1
        private const val ITEM_TYPE_ACCOUNT = 2
    }
}
