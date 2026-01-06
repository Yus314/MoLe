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

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel

class CurrencySelectorModel : ViewModel() {
    private val positionAndPaddingVisible = MutableLiveData(true)
    private var selectionListener: OnCurrencySelectedListener? = null

    fun showPositionAndPadding() {
        positionAndPaddingVisible.postValue(true)
    }

    fun hidePositionAndPadding() {
        positionAndPaddingVisible.postValue(false)
    }

    fun observePositionAndPaddingVisible(activity: LifecycleOwner, observer: Observer<Boolean>) {
        positionAndPaddingVisible.observe(activity, observer)
    }

    fun setOnCurrencySelectedListener(listener: OnCurrencySelectedListener?) {
        selectionListener = listener
    }

    fun resetOnCurrencySelectedListener() {
        selectionListener = null
    }

    fun triggerOnCurrencySelectedListener(c: String) {
        selectionListener?.onCurrencySelected(c)
    }
}
