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

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import net.ktnx.mobileledger.R
import net.ktnx.mobileledger.db.DB
import net.ktnx.mobileledger.model.Currency
import net.ktnx.mobileledger.model.Data
import net.ktnx.mobileledger.utils.Misc

/**
 * A fragment representing a list of Items.
 *
 * Activities containing this fragment MUST implement the [OnCurrencySelectedListener]
 * interface.
 */
class CurrencySelectorFragment :
    AppCompatDialogFragment(),
    OnCurrencySelectedListener,
    OnCurrencyLongClickListener {

    private var mColumnCount = DEFAULT_COLUMN_COUNT
    private var model: CurrencySelectorModel? = null
    private var onCurrencySelectedListener: OnCurrencySelectedListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            mColumnCount = it.getInt(ARG_COLUMN_COUNT, DEFAULT_COLUMN_COUNT)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val csd = Dialog(context)
        csd.setContentView(R.layout.fragment_currency_selector_list)
        csd.setTitle(R.string.choose_currency_label)

        val recyclerView = csd.findViewById<RecyclerView>(R.id.list)

        if (mColumnCount <= 1) {
            recyclerView.layoutManager = LinearLayoutManager(context)
        } else {
            recyclerView.layoutManager = GridLayoutManager(context, mColumnCount)
        }

        model = ViewModelProvider(this)[CurrencySelectorModel::class.java]
        onCurrencySelectedListener?.let { model?.setOnCurrencySelectedListener(it) }

        val adapter = CurrencySelectorRecyclerViewAdapter()
        DB.get()
            .getCurrencyDAO()
            .getAll()
            .observe(this) { list ->
                val strings = list.map { it.name }
                adapter.submitList(strings)
            }

        recyclerView.adapter = adapter
        adapter.setCurrencySelectedListener(this)
        adapter.setCurrencyLongClickListener(this)

        val tvNewCurrName = csd.findViewById<TextView>(R.id.new_currency_name)
        val tvNoCurrBtn = csd.findViewById<TextView>(R.id.btn_no_currency)
        val tvAddCurrOkBtn = csd.findViewById<TextView>(R.id.btn_add_currency)
        val tvAddCurrBtn = csd.findViewById<TextView>(R.id.btn_add_new)
        val gap = csd.findViewById<SwitchMaterial>(R.id.currency_gap)
        val rgPosition = csd.findViewById<RadioGroup>(R.id.position_radio_group)

        tvNewCurrName.visibility = View.GONE
        tvAddCurrOkBtn.visibility = View.GONE
        tvNoCurrBtn.visibility = View.VISIBLE
        tvAddCurrBtn.visibility = View.VISIBLE

        tvAddCurrBtn.setOnClickListener {
            tvNewCurrName.visibility = View.VISIBLE
            tvAddCurrOkBtn.visibility = View.VISIBLE

            tvNoCurrBtn.visibility = View.GONE
            tvAddCurrBtn.visibility = View.GONE

            tvNewCurrName.text = null
            tvNewCurrName.requestFocus()
            Misc.showSoftKeyboard(this)
        }

        tvAddCurrOkBtn.setOnClickListener {
            val currName = tvNewCurrName.text.toString()
            if (currName.isNotEmpty()) {
                DB.get()
                    .getCurrencyDAO()
                    .insert(
                        net.ktnx.mobileledger.db.Currency(
                            0,
                            tvNewCurrName.text.toString(),
                            if (rgPosition.checkedRadioButtonId == R.id.currency_position_left) {
                                "before"
                            } else {
                                "after"
                            },
                            gap.isChecked
                        )
                    )
            }

            tvNewCurrName.visibility = View.GONE
            tvAddCurrOkBtn.visibility = View.GONE

            tvNoCurrBtn.visibility = View.VISIBLE
            tvAddCurrBtn.visibility = View.VISIBLE
        }

        tvNoCurrBtn.setOnClickListener {
            adapter.notifyCurrencySelected(null)
            dismiss()
        }

        val rbPositionLeft = csd.findViewById<View>(R.id.currency_position_left)
        val rbPositionRight = csd.findViewById<View>(R.id.currency_position_right)

        if (Data.currencySymbolPosition.value == Currency.Position.before) {
            rgPosition.check(R.id.currency_position_left)
        } else {
            rgPosition.check(R.id.currency_position_right)
        }

        rgPosition.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.currency_position_left) {
                Data.currencySymbolPosition.setValue(Currency.Position.before)
            } else {
                Data.currencySymbolPosition.setValue(Currency.Position.after)
            }
        }

        gap.isChecked = Data.currencyGap.value ?: false

        gap.setOnCheckedChangeListener { _, checked ->
            Data.currencyGap.setValue(checked)
        }

        model?.observePositionAndPaddingVisible(this) { visible ->
            csd.findViewById<View>(R.id.params_panel).visibility =
                if (visible) View.VISIBLE else View.GONE
        }

        val showParams = arguments?.getBoolean(ARG_SHOW_PARAMS, DEFAULT_SHOW_PARAMS)
            ?: DEFAULT_SHOW_PARAMS

        if (showParams) {
            model?.showPositionAndPadding()
        } else {
            model?.hidePositionAndPadding()
        }

        return csd
    }

    fun setOnCurrencySelectedListener(listener: OnCurrencySelectedListener?) {
        onCurrencySelectedListener = listener
        model?.setOnCurrencySelectedListener(listener)
    }

    fun resetOnCurrencySelectedListener() {
        model?.resetOnCurrencySelectedListener()
    }

    override fun onCurrencySelected(item: String) {
        model?.triggerOnCurrencySelectedListener(item)
        dismiss()
    }

    override fun onCurrencyLongClick(item: String) {
        val dao = DB.get().getCurrencyDAO()
        dao.getByName(item).observe(this) { currency ->
            currency?.let { dao.deleteSync(it) }
        }
    }

    fun showPositionAndPadding() {
        model?.showPositionAndPadding()
    }

    fun hidePositionAndPadding() {
        model?.hidePositionAndPadding()
    }

    companion object {
        const val DEFAULT_COLUMN_COUNT = 2
        const val ARG_COLUMN_COUNT = "column-count"
        const val ARG_SHOW_PARAMS = "show-params"
        const val DEFAULT_SHOW_PARAMS = true

        @JvmStatic
        @Suppress("unused")
        fun newInstance(): CurrencySelectorFragment = newInstance(DEFAULT_COLUMN_COUNT, DEFAULT_SHOW_PARAMS)

        @JvmStatic
        fun newInstance(columnCount: Int, showParams: Boolean): CurrencySelectorFragment =
            CurrencySelectorFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_COLUMN_COUNT, columnCount)
                    putBoolean(ARG_SHOW_PARAMS, showParams)
                }
            }
    }
}
