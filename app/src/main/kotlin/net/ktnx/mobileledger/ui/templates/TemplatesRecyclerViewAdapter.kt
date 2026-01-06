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

package net.ktnx.mobileledger.ui.templates

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import net.ktnx.mobileledger.databinding.TemplateListTemplateItemBinding
import net.ktnx.mobileledger.databinding.TemplatesFallbackDividerBinding
import net.ktnx.mobileledger.db.TemplateHeader

class TemplatesRecyclerViewAdapter : RecyclerView.Adapter<BaseTemplateViewHolder>() {

    private val listDiffer: AsyncListDiffer<BaseTemplateItem>

    init {
        listDiffer = AsyncListDiffer(
            this,
            object : DiffUtil.ItemCallback<BaseTemplateItem>() {
                override fun areItemsTheSame(
                    oldItem: BaseTemplateItem,
                    newItem: BaseTemplateItem
                ): Boolean = oldItem.id == newItem.id

                override fun areContentsTheSame(
                    oldItem: BaseTemplateItem,
                    newItem: BaseTemplateItem
                ): Boolean = oldItem == newItem
            }
        )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseTemplateViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            ITEM_TYPE_TEMPLATE -> {
                val binding = TemplateListTemplateItemBinding.inflate(inflater, parent, false)
                BaseTemplateViewHolder.TemplateViewHolder(binding)
            }

            ITEM_TYPE_DIVIDER -> {
                val binding = TemplatesFallbackDividerBinding.inflate(inflater, parent, false)
                BaseTemplateViewHolder.TemplateDividerViewHolder(binding)
            }

            else -> throw RuntimeException("Can't handle $viewType")
        }
    }

    override fun onBindViewHolder(holder: BaseTemplateViewHolder, position: Int) {
        holder.bindToItem(listDiffer.currentList[position])
    }

    override fun getItemViewType(position: Int): Int = when (val item = getItem(position)) {
        is TemplateItem -> ITEM_TYPE_TEMPLATE
        is TemplateDivider -> ITEM_TYPE_DIVIDER
        else -> throw RuntimeException("Can't handle $item")
    }

    override fun getItemCount(): Int = listDiffer.currentList.size

    fun setTemplates(newList: List<TemplateHeader>) {
        val itemList = mutableListOf<BaseTemplateItem>()
        var reachedFallbackItems = false

        for (item in newList) {
            if (!reachedFallbackItems && item.isFallback) {
                itemList.add(TemplateDivider)
                reachedFallbackItems = true
            }
            itemList.add(TemplateItem(item))
        }

        listDiffer.submitList(itemList)
    }

    fun getItem(position: Int): BaseTemplateItem = listDiffer.currentList[position]

    sealed class BaseTemplateItem {
        abstract val id: Long
    }

    data class TemplateItem(val template: TemplateHeader) : BaseTemplateItem() {
        override val id: Long
            get() = template.id
    }

    data object TemplateDivider : BaseTemplateItem() {
        override val id: Long
            get() = -1L
    }

    companion object {
        const val ITEM_TYPE_TEMPLATE = 1
        const val ITEM_TYPE_DIVIDER = 2
    }
}
