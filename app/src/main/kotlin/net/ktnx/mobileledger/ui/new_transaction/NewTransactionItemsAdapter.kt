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

package net.ktnx.mobileledger.ui.new_transaction

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale
import java.util.Objects
import net.ktnx.mobileledger.databinding.NewTransactionAccountRowBinding
import net.ktnx.mobileledger.databinding.NewTransactionHeaderRowBinding
import net.ktnx.mobileledger.db.Profile
import net.ktnx.mobileledger.utils.Logger

internal class NewTransactionItemsAdapter(@JvmField val model: NewTransactionModel, profile: Profile) :
    RecyclerView.Adapter<NewTransactionItemViewHolder>() {

    private val touchHelper: ItemTouchHelper
    private val differ: AsyncListDiffer<NewTransactionModel.Item>

    private var mProfile: Profile = profile
    private var checkHoldCounter = 0

    init {
        setHasStableIds(true)

        differ = AsyncListDiffer(
            this,
            object : DiffUtil.ItemCallback<NewTransactionModel.Item>() {
                override fun areItemsTheSame(
                    oldItem: NewTransactionModel.Item,
                    newItem: NewTransactionModel.Item
                ): Boolean = oldItem.id == newItem.id

                override fun areContentsTheSame(
                    oldItem: NewTransactionModel.Item,
                    newItem: NewTransactionModel.Item
                ): Boolean = oldItem.equalContents(newItem)
            }
        )

        touchHelper = ItemTouchHelper(object : ItemTouchHelper.Callback() {
            override fun isLongPressDragEnabled(): Boolean = true

            override fun canDropOver(
                recyclerView: RecyclerView,
                current: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val adapterPosition = target.bindingAdapterPosition
                // first item is immovable
                if (adapterPosition == 0) return false
                return super.canDropOver(recyclerView, current, target)
            }

            override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                var flags = makeFlag(ItemTouchHelper.ACTION_STATE_IDLE, ItemTouchHelper.END)
                // the top (date and description) and the bottom (padding) items are always there
                val adapterPosition = viewHolder.bindingAdapterPosition
                if (adapterPosition > 0) {
                    flags = flags or
                        makeFlag(
                            ItemTouchHelper.ACTION_STATE_DRAG,
                            ItemTouchHelper.UP or ItemTouchHelper.DOWN
                        ) or
                        makeFlag(
                            ItemTouchHelper.ACTION_STATE_SWIPE,
                            ItemTouchHelper.START or ItemTouchHelper.END
                        )
                }
                return flags
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                model.moveItem(
                    viewHolder.bindingAdapterPosition,
                    target.bindingAdapterPosition
                )
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.bindingAdapterPosition
                model.removeItem(pos)
            }
        })
    }

    override fun getItemViewType(position: Int): Int = when (val type = differ.currentList[position].type) {
        ItemType.generalData -> ITEM_VIEW_TYPE_HEADER
        ItemType.transactionRow -> ITEM_VIEW_TYPE_ACCOUNT
        else -> throw RuntimeException("Can't handle $type")
    }

    override fun getItemId(position: Int): Long = differ.currentList[position].id.toLong()

    fun setProfile(profile: Profile) {
        mProfile = profile
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewTransactionItemViewHolder = when (viewType) {
        ITEM_VIEW_TYPE_HEADER -> {
            val headerBinding = NewTransactionHeaderRowBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            val headerHolder = NewTransactionHeaderItemHolder(headerBinding, this)
            Logger.debug(
                "new-trans",
                "Creating new Header ViewHolder ${Integer.toHexString(headerHolder.hashCode())}"
            )
            headerHolder
        }

        ITEM_VIEW_TYPE_ACCOUNT -> {
            val accBinding = NewTransactionAccountRowBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            val accHolder = NewTransactionAccountRowItemHolder(accBinding, this)
            Logger.debug(
                "new-trans",
                "Creating new AccountRow ViewHolder ${Integer.toHexString(accHolder.hashCode())}"
            )
            accHolder
        }

        else -> throw RuntimeException("Can't handle view type $viewType")
    }

    override fun onBindViewHolder(holder: NewTransactionItemViewHolder, position: Int) {
        Logger.debug(
            "bind",
            String.format(
                Locale.US,
                "Binding item at position %d, holder %s",
                position,
                Integer.toHexString(holder.hashCode())
            )
        )
        val item = Objects.requireNonNull(differ.currentList[position])
        holder.bind(item)
        Logger.debug(
            "bind",
            String.format(
                Locale.US,
                "Bound %s item at position %d",
                item.type.toString(),
                position
            )
        )
    }

    override fun getItemCount(): Int = differ.currentList.size

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        touchHelper.attachToRecyclerView(recyclerView)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        touchHelper.attachToRecyclerView(null)
        super.onDetachedFromRecyclerView(recyclerView)
    }

    internal fun noteFocusIsOnAccount(position: Int) {
        model.noteFocusChanged(position, FocusedElement.Account)
    }

    internal fun noteFocusIsOnAmount(position: Int) {
        model.noteFocusChanged(position, FocusedElement.Amount)
    }

    internal fun noteFocusIsOnComment(position: Int) {
        model.noteFocusChanged(position, FocusedElement.Comment)
    }

    internal fun noteFocusIsOnTransactionComment(position: Int) {
        model.noteFocusChanged(position, FocusedElement.TransactionComment)
    }

    fun noteFocusIsOnDescription(pos: Int) {
        model.noteFocusChanged(pos, FocusedElement.Description)
    }

    private fun holdSubmittableChecks() {
        checkHoldCounter++
    }

    private fun releaseSubmittableChecks() {
        if (checkHoldCounter == 0) {
            throw RuntimeException("Asymmetrical call to releaseSubmittableChecks")
        }
        checkHoldCounter--
    }

    internal fun setItemCurrency(position: Int, newCurrency: String?) {
        model.setItemCurrency(position, newCurrency ?: "")
    }

    fun setItems(newList: List<NewTransactionModel.Item>) {
        Logger.debug("new-trans", "adapter: submitting new item list")
        differ.submitList(newList)
    }

    fun getItem(position: Int): NewTransactionModel.Item = differ.currentList[position]

    companion object {
        private const val ITEM_VIEW_TYPE_HEADER = 1
        private const val ITEM_VIEW_TYPE_ACCOUNT = 2
    }
}
