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

import android.content.Context.INPUT_METHOD_SERVICE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.util.Locale
import net.ktnx.mobileledger.R
import net.ktnx.mobileledger.async.TransactionDateFinder
import net.ktnx.mobileledger.databinding.TransactionListFragmentBinding
import net.ktnx.mobileledger.db.AccountAutocompleteAdapter
import net.ktnx.mobileledger.db.Profile
import net.ktnx.mobileledger.model.Data
import net.ktnx.mobileledger.ui.DatePickerFragment
import net.ktnx.mobileledger.ui.FabManager
import net.ktnx.mobileledger.ui.MainModel
import net.ktnx.mobileledger.ui.MobileLedgerListFragment
import net.ktnx.mobileledger.utils.Colors
import net.ktnx.mobileledger.utils.Globals
import net.ktnx.mobileledger.utils.Logger
import net.ktnx.mobileledger.utils.SimpleDate

@Suppress("DEPRECATION")
class TransactionListFragment : MobileLedgerListFragment(), DatePickerFragment.DatePickedListener {
    private var menuTransactionListFilter: MenuItem? = null
    private var menuGoToDate: MenuItem? = null
    private lateinit var model: MainModel
    private var fragmentActive = false
    private var _binding: TransactionListFragmentBinding? = null
    private val b get() = _binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = TransactionListFragmentBinding.inflate(inflater, container, false)
        return b?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        fragmentActive = true
        toggleMenuItems()
        Logger.debug("flow", "TransactionListFragment.onResume()")
    }

    private fun toggleMenuItems() {
        menuGoToDate?.isVisible = fragmentActive
        menuTransactionListFilter?.let { filter ->
            val filterVisibility = b?.transactionListAccountNameFilter?.visibility
            filter.isVisible = fragmentActive && filterVisibility != View.VISIBLE
        }
    }

    override fun onStop() {
        super.onStop()
        fragmentActive = false
        toggleMenuItems()
        Logger.debug("flow", "TransactionListFragment.onStop()")
    }

    override fun onPause() {
        super.onPause()
        fragmentActive = false
        toggleMenuItems()
        Logger.debug("flow", "TransactionListFragment.onPause()")
    }

    override fun getRefreshLayout(): SwipeRefreshLayout? {
        return b?.transactionSwipe
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Logger.debug("flow", "TransactionListFragment.onActivityCreated called")
        super.onViewCreated(view, savedInstanceState)
        Data.backgroundTasksRunning.observe(viewLifecycleOwner, this::onBackgroundTaskRunningChanged)

        val mainActivity = getMainActivity()

        model = ViewModelProvider(requireActivity())[MainModel::class.java]

        modelAdapter = TransactionListAdapter()
        b?.transactionRoot?.adapter = modelAdapter

        mainActivity.fabShouldShow()

        if (mainActivity is FabManager.FabHandler) {
            b?.transactionRoot?.let { FabManager.handle(mainActivity, it) }
        }

        val llm = LinearLayoutManager(mainActivity)
        llm.orientation = RecyclerView.VERTICAL
        b?.transactionRoot?.layoutManager = llm

        b?.transactionSwipe?.setOnRefreshListener {
            Logger.debug("ui", "refreshing transactions via swipe")
            model.scheduleTransactionListRetrieval()
        }

        Colors.themeWatch.observe(viewLifecycleOwner, this::themeChanged)

        Data.observeProfile(viewLifecycleOwner, this::onProfileChanged)

        b?.transactionFilterAccountName?.setOnItemClickListener { parent, _, position, _ ->
            model.getAccountFilter().value = parent.getItemAtPosition(position).toString()
            Globals.hideSoftKeyboard(mainActivity)
        }

        model.getAccountFilter().observe(viewLifecycleOwner, this::onAccountNameFilterChanged)

        model.getUpdatingFlag().observe(viewLifecycleOwner) { flag ->
            b?.transactionSwipe?.isRefreshing = flag
        }

        model.getDisplayedTransactions().observe(viewLifecycleOwner) { list ->
            modelAdapter?.setTransactions(list)
        }

        view.findViewById<View>(R.id.clearAccountNameFilter).setOnClickListener {
            model.getAccountFilter().value = null
            Globals.hideSoftKeyboard(mainActivity)
        }

        model.foundTransactionItemIndex.observe(viewLifecycleOwner) { pos ->
            Logger.debug("go-to-date", String.format(Locale.US, "Found pos %d", pos ?: -1))
            if (pos != null) {
                b?.transactionRoot?.scrollToPosition(pos)
                // reset the value to avoid re-notification upon reconfiguration or app restart
                model.foundTransactionItemIndex.value = null
            }
        }
    }

    private fun onProfileChanged(profile: Profile?) {
        if (profile == null) return
        val ctx = context ?: return

        b?.transactionFilterAccountName?.setAdapter(
            AccountAutocompleteAdapter(ctx, profile)
        )
    }

    private fun onAccountNameFilterChanged(accName: String?) {
        b?.transactionFilterAccountName?.setText(accName, false)

        val filterActive = !accName.isNullOrEmpty()
        b?.transactionListAccountNameFilter?.visibility = if (filterActive) View.VISIBLE else View.GONE
        menuTransactionListFilter?.isVisible = !filterActive
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.transaction_list, menu)

        menuTransactionListFilter = menu.findItem(R.id.menu_transaction_list_filter)
            ?: throw AssertionError()
        menuGoToDate = menu.findItem(R.id.menu_go_to_date)
            ?: throw AssertionError()

        model.getAccountFilter().observe(this) { v ->
            menuTransactionListFilter?.isVisible = v == null
        }

        super.onCreateOptionsMenu(menu, inflater)

        menuTransactionListFilter?.setOnMenuItemClickListener {
            b?.transactionListAccountNameFilter?.visibility = View.VISIBLE
            menuTransactionListFilter?.isVisible = false
            b?.transactionFilterAccountName?.requestFocus()
            val imm = getMainActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            b?.transactionFilterAccountName?.let { editText ->
                imm.showSoftInput(editText, 0)
            }
            true
        }

        menuGoToDate?.setOnMenuItemClickListener {
            val picker = DatePickerFragment()
            picker.setOnDatePickedListener(this)
            picker.setDateRange(model.firstTransactionDate, model.lastTransactionDate)
            picker.show(requireActivity().supportFragmentManager, null)
            true
        }

        toggleMenuItems()
    }

    override fun onDatePicked(year: Int, month: Int, day: Int) {
        val finder = TransactionDateFinder(model, SimpleDate(year, month + 1, day))
        finder.start()
    }
}
