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

import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import net.ktnx.mobileledger.R
import net.ktnx.mobileledger.databinding.TemplateListTemplateItemBinding
import net.ktnx.mobileledger.databinding.TemplatesFallbackDividerBinding

abstract class BaseTemplateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bindToItem(item: TemplatesRecyclerViewAdapter.BaseTemplateItem)

    class TemplateDividerViewHolder(binding: TemplatesFallbackDividerBinding) :
        BaseTemplateViewHolder(binding.root) {
        override fun bindToItem(item: TemplatesRecyclerViewAdapter.BaseTemplateItem) {
            // nothing
        }
    }

    class TemplateViewHolder(private val b: TemplateListTemplateItemBinding) :
        BaseTemplateViewHolder(b.root) {
        override fun bindToItem(baseItem: TemplatesRecyclerViewAdapter.BaseTemplateItem) {
            val item = (baseItem as TemplatesRecyclerViewAdapter.TemplateItem).template
            b.templateName.text = item.name
            b.templateName.setOnClickListener { v ->
                (v.context as TemplatesActivity).onEditTemplate(item.id)
            }
            b.templateName.setOnLongClickListener { v ->
                val activity = v.context as TemplatesActivity
                val builder = AlertDialog.Builder(activity)
                val templateName = item.name
                builder.setTitle(templateName)
                builder.setItems(R.array.templates_ctx_menu) { dialog, which ->
                    when (which) {
                        0 -> activity.onEditTemplate(item.id) // edit
                        1 -> activity.onDuplicateTemplate(item.id) // duplicate
                        2 -> activity.onDeleteTemplate(item.id) // delete
                        else -> throw RuntimeException("Unknown menu item id ($which)")
                    }
                    dialog.dismiss()
                }
                builder.show()
                true
            }
        }
    }
}
