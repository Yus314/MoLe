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

package net.ktnx.mobileledger.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.ktnx.mobileledger.databinding.FragmentTemplateDetailSourceSelectorBinding
import net.ktnx.mobileledger.model.TemplateDetailSource

/**
 * [RecyclerView.Adapter] that can display a [TemplateDetailSource] and makes a call
 * to the specified [OnSourceSelectedListener].
 */
class TemplateDetailSourceSelectorRecyclerViewAdapter :
    ListAdapter<TemplateDetailSource, TemplateDetailSourceSelectorRecyclerViewAdapter.ViewHolder>(
        TemplateDetailSource.DIFF_CALLBACK
    ) {

    private var sourceSelectedListener: OnSourceSelectedListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val b = FragmentTemplateDetailSourceSelectorBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(b)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindTo(getItem(position))
    }

    fun setSourceSelectedListener(listener: OnSourceSelectedListener?) {
        sourceSelectedListener = listener
    }

    fun resetSourceSelectedListener() {
        sourceSelectedListener = null
    }

    fun notifySourceSelected(item: TemplateDetailSource) {
        sourceSelectedListener?.onSourceSelected(false, item.groupNumber)
    }

    fun notifyLiteralSelected() {
        sourceSelectedListener?.onSourceSelected(true, -1)
    }

    inner class ViewHolder(
        private val b: FragmentTemplateDetailSourceSelectorBinding
    ) : RecyclerView.ViewHolder(b.root) {
        private var mItem: TemplateDetailSource? = null

        init {
            b.root.setOnClickListener { mItem?.let { notifySourceSelected(it) } }
        }

        override fun toString(): String {
            return super.toString() + " " + b.groupNumber.text + ": '" +
                    b.matchedText.text + "'"
        }

        fun bindTo(item: TemplateDetailSource) {
            mItem = item
            b.groupNumber.text = item.groupNumber.toString()
            b.matchedText.text = item.matchedText
        }
    }
}
