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

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import net.ktnx.mobileledger.App
import net.ktnx.mobileledger.R
import net.ktnx.mobileledger.async.GeneralBackgroundTasks
import net.ktnx.mobileledger.databinding.AccountSummaryFragmentBinding
import net.ktnx.mobileledger.db.DB
import net.ktnx.mobileledger.db.Profile
import net.ktnx.mobileledger.model.AccountListItem
import net.ktnx.mobileledger.model.Data
import net.ktnx.mobileledger.model.LedgerAccount
import net.ktnx.mobileledger.ui.FabManager
import net.ktnx.mobileledger.ui.MainModel
import net.ktnx.mobileledger.ui.MobileLedgerListFragment
import net.ktnx.mobileledger.utils.Colors
import net.ktnx.mobileledger.utils.Logger.debug

class AccountSummaryFragment : MobileLedgerListFragment() {
    private var summaryAdapter: AccountSummaryAdapter? = null
    private var b: AccountSummaryFragmentBinding? = null
    private var menuShowZeroBalances: MenuItem? = null
    private var model: MainModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        debug("flow", "AccountSummaryFragment.onCreate()")
        @Suppress("DEPRECATION")
        setHasOptionsMenu(true)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        debug("flow", "AccountSummaryFragment.onAttach()")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        debug("flow", "AccountSummaryFragment.onCreateView()")
        b = AccountSummaryFragmentBinding.inflate(inflater, container, false)
        return b?.root
    }

    override fun getRefreshLayout(): SwipeRefreshLayout? = b?.accountSwipeRefreshLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        debug("flow", "AccountSummaryFragment.onActivityCreated()")
        super.onViewCreated(view, savedInstanceState)

        model = ViewModelProvider(requireActivity())[MainModel::class.java]

        Data.backgroundTasksRunning.observe(viewLifecycleOwner) { running ->
            onBackgroundTaskRunningChanged(running)
        }

        summaryAdapter = AccountSummaryAdapter()
        modelAdapter = null // Use summaryAdapter instead
        val mainActivity = getMainActivity()

        val llm = LinearLayoutManager(mainActivity)
        llm.orientation = RecyclerView.VERTICAL
        b?.accountRoot?.layoutManager = llm
        b?.accountRoot?.adapter = summaryAdapter
        val did = DividerItemDecoration(mainActivity, DividerItemDecoration.VERTICAL)
        b?.accountRoot?.addItemDecoration(did)

        mainActivity.fabShouldShow()

        b?.accountRoot?.let { FabManager.handle(mainActivity, it) }

        Colors.themeWatch.observe(viewLifecycleOwner) { counter -> themeChanged(counter) }
        b?.accountSwipeRefreshLayout?.setOnRefreshListener {
            debug("ui", "refreshing accounts via swipe")
            model?.scheduleTransactionListRetrieval()
        }

        Data.observeProfile(this) { profile ->
            onProfileChanged(
                profile,
                model?.getShowZeroBalanceAccounts()?.value == true
            )
        }
        model?.getShowZeroBalanceAccounts()?.setValue(App.getShowZeroBalanceAccounts())
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.account_list, menu)

        menuShowZeroBalances = menu.findItem(R.id.menu_account_list_show_zero_balances)
            ?: throw AssertionError()

        menuShowZeroBalances?.setOnMenuItemClickListener {
            model?.getShowZeroBalanceAccounts()?.setValue(
                model?.getShowZeroBalanceAccounts()?.value != true
            )
            true
        }

        model?.getShowZeroBalanceAccounts()?.observe(this) { v ->
            menuShowZeroBalances?.isChecked = v
            onProfileChanged(Data.getProfile(), v)
            App.storeShowZeroBalanceAccounts(v)
        }

        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun onProfileChanged(profile: Profile?, showZeroBalanceAccounts: Boolean) {
        if (profile == null) return

        DB.get()
            .getAccountDAO()
            .getAllWithAmounts(profile.id, showZeroBalanceAccounts)
            .observe(viewLifecycleOwner) { list ->
                GeneralBackgroundTasks.run {
                    val adapterList = mutableListOf<AccountListItem>()
                    adapterList.add(AccountListItem.Header(Data.lastAccountsUpdateText))
                    val accMap = HashMap<String, LedgerAccount>()
                    for (dbAcc in list) {
                        var parent: LedgerAccount? = null
                        val parentName = dbAcc.account.parentName
                        if (parentName != null) {
                            parent = accMap[parentName]
                        }
                        if (parent != null) {
                            parent.hasSubAccounts = true
                        }
                        val account = LedgerAccount.fromDBO(dbAcc, parent)
                        if (account.isVisible) {
                            adapterList.add(AccountListItem.Account(account))
                        }
                        accMap[dbAcc.account.name] = account
                    }

                    if (!showZeroBalanceAccounts) {
                        removeZeroAccounts(adapterList)
                    }
                    summaryAdapter?.setAccounts(adapterList)
                    Data.lastUpdateAccountCount.postValue(adapterList.size - 1)
                }
            }
    }

    private fun removeZeroAccounts(list: MutableList<AccountListItem>) {
        var removed = true

        while (removed) {
            var last: AccountListItem? = null
            removed = false
            val newList = mutableListOf<AccountListItem>()

            for (item in list) {
                if (last == null) {
                    last = item
                    continue
                }

                if (!last.isAccount() ||
                    !last.toAccount().allAmountsAreZero() ||
                    last.toAccount().account.isParentOf(item.toAccount().account)
                ) {
                    newList.add(last)
                } else {
                    removed = true
                }

                last = item
            }

            if (last != null) {
                if (!last.isAccount() || !last.toAccount().allAmountsAreZero()) {
                    newList.add(last)
                } else {
                    removed = true
                }
            }

            list.clear()
            list.addAll(newList)
        }
    }
}
