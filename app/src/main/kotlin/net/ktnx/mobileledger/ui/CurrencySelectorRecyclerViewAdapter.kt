/*
 * Copyright © 2019 Damyan Ivanov.
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
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.ktnx.mobileledger.R

/**
 * [RecyclerView.Adapter] that can display a Currency and makes a call to the
 * specified [OnCurrencySelectedListener].
 */
class CurrencySelectorRecyclerViewAdapter :
    ListAdapter<String, CurrencySelectorRecyclerViewAdapter.ViewHolder>(DIFF_CALLBACK) {

    private var currencySelectedListener: OnCurrencySelectedListener? = null
    private var currencyLongClickListener: OnCurrencyLongClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_currency_selector, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindTo(getItem(position))
    }

    fun setCurrencySelectedListener(listener: OnCurrencySelectedListener?) {
        currencySelectedListener = listener
    }

    fun resetCurrencySelectedListener() {
        currencySelectedListener = null
    }

    fun notifyCurrencySelected(currency: String?) {
        currency?.let { currencySelectedListener?.onCurrencySelected(it) }
    }

    fun setCurrencyLongClickListener(listener: OnCurrencyLongClickListener?) {
        currencyLongClickListener = listener
    }

    fun resetCurrencyLockClickListener() {
        currencyLongClickListener = null
    }

    private fun notifyCurrencyLongClicked(item: String) {
        currencyLongClickListener?.onCurrencyLongClick(item)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val mNameView: TextView = view.findViewById(R.id.content)
        private var mItem: String? = null

        init {
            view.setOnClickListener { mItem?.let { notifyCurrencySelected(it) } }
            view.setOnLongClickListener {
                mItem?.let { notifyCurrencyLongClicked(it) }
                false
            }
        }

        override fun toString(): String = super.toString() + " '" + mNameView.text + "'"

        fun bindTo(item: String) {
            mItem = item
            mNameView.text = item
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem

            override fun areContentsTheSame(oldItem: String, newItem: String): Boolean = true
        }
    }
}
