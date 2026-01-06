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

package net.ktnx.mobileledger.ui.activity

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import net.ktnx.mobileledger.BackupsActivity
import net.ktnx.mobileledger.R
import net.ktnx.mobileledger.async.RetrieveTransactionsTask
import net.ktnx.mobileledger.async.TransactionAccumulator
import net.ktnx.mobileledger.databinding.ActivityMainBinding
import net.ktnx.mobileledger.db.DB
import net.ktnx.mobileledger.db.Option
import net.ktnx.mobileledger.db.Profile
import net.ktnx.mobileledger.db.TransactionWithAccounts
import net.ktnx.mobileledger.model.Data
import net.ktnx.mobileledger.model.LedgerTransaction
import net.ktnx.mobileledger.ui.FabManager
import net.ktnx.mobileledger.ui.MainModel
import net.ktnx.mobileledger.ui.account_summary.AccountSummaryFragment
import net.ktnx.mobileledger.ui.new_transaction.NewTransactionActivity
import net.ktnx.mobileledger.ui.profiles.ProfileDetailActivity
import net.ktnx.mobileledger.ui.profiles.ProfilesRecyclerViewAdapter
import net.ktnx.mobileledger.ui.templates.TemplatesActivity
import net.ktnx.mobileledger.ui.transaction_list.TransactionListFragment
import net.ktnx.mobileledger.utils.Colors
import net.ktnx.mobileledger.utils.Logger
import net.ktnx.mobileledger.utils.Misc
import java.util.Date
import java.util.Locale

/*
 * TODO: reports
 *  */

@AndroidEntryPoint
class MainActivity : ProfileThemedActivity(), FabManager.FabHandler {
    private var converterThread: ConverterThread? = null
    private var mSectionsPagerAdapter: SectionsPagerAdapter? = null
    private var mProfileListAdapter: ProfilesRecyclerViewAdapter? = null
    private var mCurrentPage = 0
    private var mBackMeansToAccountList = false
    private var drawerListener: DrawerLayout.SimpleDrawerListener? = null
    private var barDrawerToggle: ActionBarDrawerToggle? = null
    private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null
    private var profile: Profile? = null
    private val mainModel: MainModel by viewModels()
    private lateinit var b: ActivityMainBinding
    private var fabVerticalOffset = 0
    private lateinit var fabManager: FabManager

    override fun onStart() {
        super.onStart()

        Logger.debug(TAG, "onStart()")

        b.mainPager.setCurrentItem(mCurrentPage, false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_CURRENT_PAGE, b.mainPager.currentItem)
        mainModel.getAccountFilter().value?.let { filter ->
            outState.putString(STATE_ACC_FILTER, filter)
        }
    }

    override fun onDestroy() {
        mSectionsPagerAdapter = null
        b.navProfileList.adapter = null
        drawerListener?.let { b.drawerLayout.removeDrawerListener(it) }
        drawerListener = null
        barDrawerToggle?.let { b.drawerLayout.removeDrawerListener(it) }
        barDrawerToggle = null
        pageChangeCallback?.let { b.mainPager.unregisterOnPageChangeCallback(it) }
        pageChangeCallback = null
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        fabShouldShow()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Logger.debug(TAG, "onCreate()/entry")
        super.onCreate(savedInstanceState)
        Logger.debug(TAG, "onCreate()/after super")
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        // mainModel is now injected via Hilt using by viewModels()

        mSectionsPagerAdapter = SectionsPagerAdapter(this)

        var state = savedInstanceState
        val extra = intent.getBundleExtra(BUNDLE_SAVED_STATE)
        if (extra != null && savedInstanceState == null) {
            state = extra
        }

        setSupportActionBar(b.toolbar)

        Data.observeProfile(this) { newProfile -> onProfileChanged(newProfile) }

        Data.profiles.observe(this) { profiles -> onProfileListChanged(profiles) }

        Data.backgroundTaskProgress.observe(this) { progress -> onRetrieveProgress(progress) }
        Data.backgroundTasksRunning.observe(this) { running -> onRetrieveRunningChanged(running) }

        if (barDrawerToggle == null) {
            barDrawerToggle = ActionBarDrawerToggle(
                this, b.drawerLayout, b.toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close
            ).also { b.drawerLayout.addDrawerListener(it) }
        }
        barDrawerToggle?.syncState()

        try {
            val pm = applicationContext.packageManager
            val pi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, 0)
            }
            (b.navUpper.findViewById<View>(R.id.drawer_version_text) as TextView).text = pi.versionName
            (b.noProfilesLayout.findViewById<View>(R.id.drawer_version_text) as TextView).text = pi.versionName
        } catch (e: Exception) {
            e.printStackTrace()
        }

        markDrawerItemCurrent(R.id.nav_account_summary)

        b.mainPager.adapter = mSectionsPagerAdapter
        b.mainPager.offscreenPageLimit = 1

        if (pageChangeCallback == null) {
            pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    mCurrentPage = position
                    when (position) {
                        0 -> markDrawerItemCurrent(R.id.nav_account_summary)
                        1 -> markDrawerItemCurrent(R.id.nav_latest_transactions)
                        else -> Log.e(TAG, String.format("Unexpected page index %d", position))
                    }

                    super.onPageSelected(position)
                }
            }.also { b.mainPager.registerOnPageChangeCallback(it) }
        }

        mCurrentPage = 0
        state?.let {
            val currentPage = it.getInt(STATE_CURRENT_PAGE, -1)
            if (currentPage != -1) {
                mCurrentPage = currentPage
            }
            mainModel.getAccountFilter().value = it.getString(STATE_ACC_FILTER, null)
        }

        b.btnNoProfilesAdd.setOnClickListener { ProfileDetailActivity.start(this, null) }
        b.btnRestore.setOnClickListener { BackupsActivity.start(this) }

        b.btnAddTransaction.setOnClickListener { view -> fabNewTransactionClicked(view) }

        b.navNewProfileButton.setOnClickListener { ProfileDetailActivity.start(this, null) }

        b.transactionListCancelDownload.setOnClickListener { view -> onStopTransactionRefreshClick(view) }

        if (mProfileListAdapter == null) {
            mProfileListAdapter = ProfilesRecyclerViewAdapter()
        }
        b.navProfileList.adapter = mProfileListAdapter

        mProfileListAdapter?.editingProfiles?.observe(this) { newValue ->
            if (newValue) {
                b.navProfilesStartEdit.visibility = View.GONE
                b.navProfilesCancelEdit.visibility = View.VISIBLE
                b.navNewProfileButton.visibility = View.VISIBLE
                if (b.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    b.navProfilesStartEdit.startAnimation(
                        AnimationUtils.loadAnimation(this@MainActivity, R.anim.fade_out)
                    )
                    b.navProfilesCancelEdit.startAnimation(
                        AnimationUtils.loadAnimation(this@MainActivity, R.anim.fade_in)
                    )
                    b.navNewProfileButton.startAnimation(
                        AnimationUtils.loadAnimation(this@MainActivity, R.anim.fade_in)
                    )
                }
            } else {
                b.navProfilesCancelEdit.visibility = View.GONE
                b.navProfilesStartEdit.visibility = View.VISIBLE
                b.navNewProfileButton.visibility = View.GONE
                if (b.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    b.navProfilesCancelEdit.startAnimation(
                        AnimationUtils.loadAnimation(this@MainActivity, R.anim.fade_out)
                    )
                    b.navProfilesStartEdit.startAnimation(
                        AnimationUtils.loadAnimation(this@MainActivity, R.anim.fade_in)
                    )
                    b.navNewProfileButton.startAnimation(
                        AnimationUtils.loadAnimation(this@MainActivity, R.anim.fade_out)
                    )
                }
            }

            mProfileListAdapter?.notifyDataSetChanged()
        }

        fabManager = FabManager(b.btnAddTransaction)

        val llm = LinearLayoutManager(this)

        llm.orientation = RecyclerView.VERTICAL
        b.navProfileList.layoutManager = llm

        b.navProfilesStartEdit.setOnClickListener { mProfileListAdapter?.flipEditingProfiles() }
        b.navProfilesCancelEdit.setOnClickListener { mProfileListAdapter?.flipEditingProfiles() }
        b.navProfileListHeadButtons.setOnClickListener { mProfileListAdapter?.flipEditingProfiles() }
        if (drawerListener == null) {
            drawerListener = object : DrawerLayout.SimpleDrawerListener() {
                override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                    if (slideOffset > 0.2f) {
                        fabManager.hideFab()
                    }
                }

                override fun onDrawerClosed(drawerView: View) {
                    super.onDrawerClosed(drawerView)
                    mProfileListAdapter?.setAnimationsEnabled(false)
                    mProfileListAdapter?.editingProfiles?.value = false
                    Data.drawerOpen.value = false
                    fabShouldShow()
                }

                override fun onDrawerOpened(drawerView: View) {
                    super.onDrawerOpened(drawerView)
                    mProfileListAdapter?.setAnimationsEnabled(true)
                    Data.drawerOpen.value = true
                    fabManager.hideFab()
                }
            }.also { b.drawerLayout.addDrawerListener(it) }
        }

        Data.drawerOpen.observe(this) { open ->
            if (open) {
                b.drawerLayout.open()
            } else {
                b.drawerLayout.close()
            }
        }

        mainModel.getUpdateError().observe(this) { error ->
            if (error == null) return@observe

            Snackbar.make(b.mainPager, error, Snackbar.LENGTH_INDEFINITE).show()
            mainModel.clearUpdateError()
        }
        Data.locale.observe(this) { refreshLastUpdateInfo() }
        Data.lastUpdateDate.observe(this) { refreshLastUpdateInfo() }
        Data.lastUpdateTransactionCount.observe(this) { refreshLastUpdateInfo() }
        Data.lastUpdateAccountCount.observe(this) { refreshLastUpdateInfo() }
        b.navAccountSummary.setOnClickListener { view -> onAccountSummaryClicked(view) }
        b.navLatestTransactions.setOnClickListener { view -> onLatestTransactionsClicked(view) }
        b.navPatterns.setOnClickListener { onPatternsClick() }
        b.navBackupRestore.setOnClickListener { onBackupRestoreClick() }
    }

    private fun onBackupRestoreClick() {
        val intent = Intent(this, BackupsActivity::class.java)
        startActivity(intent)
    }

    private fun onPatternsClick() {
        val intent = Intent(this, TemplatesActivity::class.java)
        startActivity(intent)
    }

    private fun scheduleDataRetrievalIfStale(lastUpdate: Long) {
        val now = Date().time
        if (lastUpdate == 0L || now > lastUpdate + 24 * 3600 * 1000) {
            if (lastUpdate == 0L) {
                Logger.debug("db::", "WEB data never fetched. scheduling a fetch")
            } else {
                Logger.debug(
                    "db", String.format(
                        Locale.ENGLISH,
                        "WEB data last fetched at %1.3f and now is %1.3f. re-fetching",
                        lastUpdate / 1000f, now / 1000f
                    )
                )
            }

            mainModel.scheduleTransactionListRetrieval()
        }
    }

    private fun createShortcuts(list: List<Profile>) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return

        val sm = getSystemService(ShortcutManager::class.java)
        val shortcuts = ArrayList<ShortcutInfo>()
        var i = 0
        for (p in list) {
            if (shortcuts.size >= sm.maxShortcutCountPerActivity) break

            if (!p.canPost()) continue

            val builder = ShortcutInfo.Builder(this, "new_transaction_${p.id}")
            val si = builder.setShortLabel(p.name)
                .setIcon(Icon.createWithResource(this, R.drawable.thick_plus_icon))
                .setIntent(
                    Intent(Intent.ACTION_VIEW, null, this, NewTransactionActivity::class.java)
                        .putExtra(ProfileThemedActivity.PARAM_PROFILE_ID, p.id)
                        .putExtra(ProfileThemedActivity.PARAM_THEME, p.theme)
                )
                .setRank(i)
                .build()
            shortcuts.add(si)
            i++
        }
        sm.dynamicShortcuts = shortcuts
    }

    private fun onProfileListChanged(newList: List<Profile>) {
        createShortcuts(newList)

        if (newList.isEmpty()) {
            b.noProfilesLayout.visibility = View.VISIBLE
            b.mainAppLayout.visibility = View.GONE
            return
        }

        b.mainAppLayout.visibility = View.VISIBLE
        b.noProfilesLayout.visibility = View.GONE

        b.navProfileList.minimumHeight =
            (resources.getDimension(R.dimen.thumb_row_height) * newList.size).toInt()

        Logger.debug("profiles", "profile list changed")
        mProfileListAdapter?.setProfileList(newList)

        val currentProfile = Data.getProfile()
        var replacementProfile: Profile? = null
        if (currentProfile != null) {
            for (p in newList) {
                if (p.id == currentProfile.id) {
                    replacementProfile = p
                    break
                }
            }
        }

        if (replacementProfile == null) {
            Logger.debug(TAG, "Switching profile because the current is no longer available")
            Data.setCurrentProfile(newList[0])
        } else {
            Data.setCurrentProfile(replacementProfile)
        }
    }

    /**
     * called when the current profile has changed
     */
    private fun onProfileChanged(newProfile: Profile?) {
        if (this.profile != null) {
            if (this.profile == newProfile) return
        }

        if (newProfile != null) {
            setTitle(newProfile.name)
        } else {
            setTitle(R.string.app_name)
        }

        val newProfileTheme = newProfile?.theme ?: Colors.DEFAULT_HUE_DEG
        val haveProfile = newProfile != null
        if (newProfileTheme != Colors.profileThemeId) {
            Logger.debug(
                "profiles",
                String.format(
                    Locale.ENGLISH, "profile theme %d → %d", Colors.profileThemeId,
                    newProfileTheme
                )
            )
            Colors.profileThemeId = newProfileTheme
            profileThemeChanged()
            // profileThemeChanged would restart the activity, so no need to reload the
            // data sets below
            return
        }

        val sameProfileId = newProfile != null && this.profile?.id == newProfile.id

        this.profile = newProfile

        b.noProfilesLayout.visibility = if (haveProfile) View.GONE else View.VISIBLE
        b.pagerLayout.visibility = View.VISIBLE

        mProfileListAdapter?.notifyDataSetChanged()

        if (newProfile != null && newProfile.canPost()) {
            b.toolbar.subtitle = null
            b.btnAddTransaction.show()
        } else if (newProfile != null) {
            b.toolbar.setSubtitle(R.string.profile_subtitle_read_only)
            b.btnAddTransaction.hide()
        } else {
            b.toolbar.subtitle = null
            b.btnAddTransaction.hide()
        }

        updateLastUpdateTextFromDB()

        if (sameProfileId && newProfile != null) {
            Logger.debug(
                TAG, String.format(
                    Locale.ROOT, "Short-cut profile 'changed' to %d",
                    newProfile.id
                )
            )
            return
        }

        mainModel.getAccountFilter().observe(this) { accFilter -> onAccountFilterChanged(accFilter) }

        mainModel.stopTransactionsRetrieval()
        mainModel.clearTransactions()
    }

    private fun onAccountFilterChanged(accFilter: String?) {
        Logger.debug(TAG, "account filter changed, reloading transactions")
        val currentProfile = profile
        val transactions: LiveData<List<TransactionWithAccounts>> = if (currentProfile != null) {
            if (accFilter.isNullOrEmpty()) {
                DB.get().getTransactionDAO().getAllWithAccounts(currentProfile.id)
            } else {
                DB.get().getTransactionDAO().getAllWithAccountsFiltered(currentProfile.id, accFilter)
            }
        } else {
            MutableLiveData(ArrayList())
        }

        transactions.observe(this) { list ->
            Logger.debug(
                TAG,
                String.format(
                    Locale.ROOT, "got transaction list from DB (%d transactions)",
                    list.size
                )
            )

            converterThread?.interrupt()
            converterThread = ConverterThread(mainModel, list, accFilter)
            converterThread?.start()
        }
    }

    private fun profileThemeChanged() {
        // un-hook all observed LiveData
        Data.removeProfileObservers(this)
        Data.profiles.removeObservers(this)
        Data.lastUpdateTransactionCount.removeObservers(this)
        Data.lastUpdateAccountCount.removeObservers(this)
        Data.lastUpdateDate.removeObservers(this)

        Logger.debug(TAG, "profileThemeChanged(): recreating activity")
        recreate()
    }

    fun fabNewTransactionClicked(view: View) {
        val currentProfile = profile ?: return
        val intent = Intent(this, NewTransactionActivity::class.java)
        intent.putExtra(ProfileThemedActivity.PARAM_PROFILE_ID, currentProfile.id)
        intent.putExtra(ProfileThemedActivity.PARAM_THEME, currentProfile.theme)
        startActivity(intent)
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.slide_in_up, R.anim.dummy)
    }

    fun markDrawerItemCurrent(id: Int) {
        val item = b.drawerLayout.findViewById<TextView>(id)
        item.setBackgroundColor(Colors.tableRowDarkBG)

        for (i in 0 until b.navActions.childCount) {
            val view = b.navActions.getChildAt(i)
            if (view.id != id) {
                view.setBackgroundColor(Color.TRANSPARENT)
            }
        }
    }

    fun onAccountSummaryClicked(view: View) {
        b.drawerLayout.closeDrawers()

        showAccountSummaryFragment()
    }

    private fun showAccountSummaryFragment() {
        b.mainPager.setCurrentItem(0, true)
        mainModel.getAccountFilter().value = null
    }

    fun onLatestTransactionsClicked(view: View) {
        b.drawerLayout.closeDrawers()

        showTransactionsFragment(null)
    }

    fun showTransactionsFragment(accName: String?) {
        mainModel.getAccountFilter().value = accName
        b.mainPager.setCurrentItem(1, true)
    }

    fun showAccountTransactions(accountName: String?) {
        mBackMeansToAccountList = true
        showTransactionsFragment(accountName)
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (b.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            b.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            if (mBackMeansToAccountList && b.mainPager.currentItem == 1) {
                mainModel.getAccountFilter().value = null
                showAccountSummaryFragment()
                mBackMeansToAccountList = false
            } else {
                Logger.debug(
                    TAG, String.format(
                        Locale.ENGLISH, "manager stack: %d",
                        supportFragmentManager.backStackEntryCount
                    )
                )

                super.onBackPressed()
            }
        }
    }

    fun updateLastUpdateTextFromDB() {
        val currentProfile = profile ?: return

        DB.get()
            .getOptionDAO()
            .load(currentProfile.id, Option.OPT_LAST_SCRAPE)
            .observe(this) { opt: Option? ->
                var lastUpdate = 0L
                if (opt != null) {
                    try {
                        lastUpdate = opt.value?.toLong() ?: 0L
                    } catch (ex: NumberFormatException) {
                        Logger.debug(
                            TAG, String.format("Error parsing '%s' as long", opt.value),
                            ex
                        )
                    }
                }

                if (lastUpdate == 0L) {
                    Data.lastUpdateDate.postValue(null)
                } else {
                    Data.lastUpdateDate.postValue(Date(lastUpdate))
                }

                scheduleDataRetrievalIfStale(lastUpdate)
            }
    }

    private fun refreshLastUpdateInfo() {
        val formatFlags = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or
                DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_NUMERIC_DATE
        val templateForTransactions = resources.getString(R.string.transaction_count_summary)
        val templateForAccounts = resources.getString(R.string.account_count_summary)
        val accountCount = Data.lastUpdateAccountCount.value
        val transactionCount = Data.lastUpdateTransactionCount.value
        val lastUpdate = Data.lastUpdateDate.value
        val locale = Data.locale.value ?: Locale.getDefault()
        if (lastUpdate == null) {
            Data.lastTransactionsUpdateText.value = "----"
            Data.lastAccountsUpdateText.value = "----"
        } else {
            Data.lastTransactionsUpdateText.value = String.format(
                locale,
                templateForTransactions,
                transactionCount ?: 0,
                DateUtils.formatDateTime(this, lastUpdate.time, formatFlags)
            )
            Data.lastAccountsUpdateText.value = String.format(
                locale,
                templateForAccounts,
                accountCount ?: 0,
                DateUtils.formatDateTime(this, lastUpdate.time, formatFlags)
            )
        }
    }

    fun onStopTransactionRefreshClick(view: View) {
        Logger.debug(TAG, "Cancelling transactions refresh")
        mainModel.stopTransactionsRetrieval()
        b.transactionListCancelDownload.isEnabled = false
    }

    fun onRetrieveRunningChanged(running: Boolean) {
        if (running) {
            b.transactionListCancelDownload.isEnabled = true
            val csl = Colors.getColorStateList()
            b.transactionListProgressBar.indeterminateTintList = csl
            b.transactionListProgressBar.progressTintList = csl
            b.transactionListProgressBar.isIndeterminate = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                b.transactionListProgressBar.setProgress(0, false)
            } else {
                b.transactionListProgressBar.progress = 0
            }
            b.transactionProgressLayout.visibility = View.VISIBLE
        } else {
            b.transactionProgressLayout.visibility = View.GONE
        }
    }

    fun onRetrieveProgress(progress: RetrieveTransactionsTask.Progress?) {
        if (progress == null ||
            progress.state == RetrieveTransactionsTask.ProgressState.FINISHED
        ) {
            Logger.debug(TAG, "progress: Done")
            b.transactionProgressLayout.visibility = View.GONE

            mainModel.transactionRetrievalDone()

            val error = progress?.error
            if (error != null) {
                val displayError = if (error == RetrieveTransactionsTask.Result.ERR_JSON_PARSER_ERROR) {
                    resources.getString(R.string.err_json_parser_error)
                } else {
                    error
                }

                val builder = AlertDialog.Builder(this)
                builder.setMessage(displayError)
                builder.setPositiveButton(R.string.btn_profile_options) { _, _ ->
                    Logger.debug(TAG, "will start profile editor")
                    ProfileDetailActivity.start(this, profile)
                }
                builder.create().show()
                return
            }

            return
        }

        b.transactionListCancelDownload.isEnabled = true
        b.transactionProgressLayout.visibility = View.VISIBLE

        if (progress.isIndeterminate || progress.getTotal() <= 0) {
            b.transactionListProgressBar.isIndeterminate = true
            Logger.debug(TAG, "progress: indeterminate")
        } else {
            if (b.transactionListProgressBar.isIndeterminate) {
                b.transactionListProgressBar.isIndeterminate = false
            }
            b.transactionListProgressBar.max = progress.getTotal()
            // for some reason animation doesn't work - no progress is shown (stick at 0)
            // on lineageOS 14.1 (Nougat, 7.1.2)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                b.transactionListProgressBar.setProgress(progress.getProgress(), false)
            } else {
                b.transactionListProgressBar.progress = progress.getProgress()
            }
        }
    }

    fun fabShouldShow() {
        if (profile?.canPost() == true && !b.drawerLayout.isOpen) {
            fabManager.showFab()
        }
    }

    override fun getContext(): Context {
        return this
    }

    override fun showManagedFab() {
        fabShouldShow()
    }

    override fun hideManagedFab() {
        fabManager.hideFab()
    }

    class SectionsPagerAdapter(fragmentActivity: FragmentActivity) :
        FragmentStateAdapter(fragmentActivity) {

        override fun createFragment(position: Int): Fragment {
            Logger.debug(TAG, String.format(Locale.ENGLISH, "Switching to fragment %d", position))
            return when (position) {
                0 -> AccountSummaryFragment()
                1 -> TransactionListFragment()
                else -> throw IllegalStateException("Unexpected fragment index: $position")
            }
        }

        override fun getItemCount(): Int = 2
    }

    private class ConverterThread(
        private val model: MainModel,
        private val list: List<TransactionWithAccounts>,
        private val accFilter: String?
    ) : Thread() {

        override fun run() {
            val accumulator = TransactionAccumulator(accFilter, accFilter)

            for (tr in list) {
                if (isInterrupted) {
                    Logger.debug(TAG, "ConverterThread bailing out on interrupt")
                    return
                }
                accumulator.put(LedgerTransaction(tr))
            }

            if (isInterrupted) {
                Logger.debug(TAG, "ConverterThread bailing out on interrupt")
                return
            }

            Logger.debug(TAG, "ConverterThread publishing results")

            Misc.onMainThread { accumulator.publishResults(model) }
        }
    }

    companion object {
        const val TAG = "main-act"
        const val STATE_CURRENT_PAGE = "current_page"
        const val BUNDLE_SAVED_STATE = "bundle_savedState"
        const val STATE_ACC_FILTER = "account_filter"
        private const val FAB_HIDDEN = false
        private const val FAB_SHOWN = true
    }
}
