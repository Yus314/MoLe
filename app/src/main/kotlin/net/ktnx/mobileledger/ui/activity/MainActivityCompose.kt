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

package net.ktnx.mobileledger.ui.activity

import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.text.format.DateUtils
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.launch
import net.ktnx.mobileledger.BackupsActivity
import net.ktnx.mobileledger.R
import net.ktnx.mobileledger.data.repository.OptionRepository
import net.ktnx.mobileledger.db.Option
import net.ktnx.mobileledger.db.Profile
import net.ktnx.mobileledger.service.TaskState
import net.ktnx.mobileledger.ui.components.CrashReportDialog
import net.ktnx.mobileledger.ui.main.MainEffect
import net.ktnx.mobileledger.ui.main.MainScreen
import net.ktnx.mobileledger.ui.main.MainViewModel
import net.ktnx.mobileledger.ui.profiles.ProfileDetailActivity
import net.ktnx.mobileledger.ui.templates.TemplatesActivity
import net.ktnx.mobileledger.ui.theme.MoLeTheme
import net.ktnx.mobileledger.utils.Colors
import net.ktnx.mobileledger.utils.Logger

/**
 * Main activity using Jetpack Compose for the UI.
 */
@AndroidEntryPoint
class MainActivityCompose : ProfileThemedActivity() {

    @Inject
    lateinit var optionRepository: OptionRepository

    // profileRepository is inherited from ProfileThemedActivity

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        Logger.debug(TAG, "onCreate()/entry")
        super.onCreate(savedInstanceState)
        Logger.debug(TAG, "onCreate()/after super")

        // Observe profile changes from ProfileRepository (via ViewModel)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.currentProfile.collect { newProfile ->
                        onProfileChanged(newProfile)
                    }
                }
                launch {
                    viewModel.allProfiles.collect { profiles ->
                        onProfileListChanged(profiles)
                    }
                }
                // Observe task progress from BackgroundTaskManager via ViewModel
                launch {
                    viewModel.taskProgress.collect { progress ->
                        val isRunning = viewModel.isTaskRunning.value
                        viewModel.updateBackgroundTasksRunning(isRunning)
                        // Reset the task reference when finished so new refreshes can start
                        if (progress?.state == TaskState.FINISHED || progress?.state == TaskState.ERROR) {
                            viewModel.transactionRetrievalDone()
                        }
                    }
                }
                // Observe sync info from AppStateService via ViewModel
                launch {
                    viewModel.lastSyncInfo.collect { syncInfo ->
                        if (syncInfo != null && syncInfo.date != null) {
                            viewModel.updateLastUpdateInfo(
                                syncInfo.date,
                                syncInfo.transactionCount,
                                syncInfo.accountCount
                            )
                            updateLastUpdateText(
                                syncInfo.date,
                                syncInfo.transactionCount,
                                syncInfo.accountCount,
                                syncInfo.totalAccountCount
                            )
                        }
                    }
                }
            }
        }

        setContent {
            val mainUiState by viewModel.mainUiState.collectAsState()
            val accountSummaryUiState by viewModel.accountSummaryUiState.collectAsState()
            val transactionListUiState by viewModel.transactionListUiState.collectAsState()
            val drawerOpen by viewModel.drawerOpen.collectAsState()

            // Handle one-shot effects
            LaunchedEffect(Unit) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        is MainEffect.NavigateToNewTransaction -> {
                            navigateToNewTransaction(effect.profileId, effect.theme)
                        }

                        is MainEffect.NavigateToProfileDetail -> {
                            if (effect.profileId != null) {
                                val profile = viewModel.allProfiles.value.find { it.id == effect.profileId }
                                ProfileDetailActivity.start(this@MainActivityCompose, profile)
                            } else {
                                ProfileDetailActivity.start(this@MainActivityCompose, null)
                            }
                        }

                        is MainEffect.NavigateToTemplates -> {
                            startActivity(
                                Intent(this@MainActivityCompose, TemplatesActivity::class.java)
                            )
                        }

                        is MainEffect.NavigateToBackups -> {
                            BackupsActivity.start(this@MainActivityCompose)
                        }

                        is MainEffect.ShowError -> {
                            // TODO: Show snackbar
                        }
                    }
                }
            }

            MoLeTheme(
                profileHue = if (mainUiState.currentProfileTheme >= 0) {
                    mainUiState.currentProfileTheme.toFloat()
                } else {
                    null
                }
            ) {
                MainScreen(
                    mainUiState = mainUiState,
                    accountSummaryUiState = accountSummaryUiState,
                    transactionListUiState = transactionListUiState,
                    drawerOpen = drawerOpen,
                    onMainEvent = viewModel::onMainEvent,
                    onAccountSummaryEvent = viewModel::onAccountSummaryEvent,
                    onTransactionListEvent = viewModel::onTransactionListEvent,
                    onNavigateToNewTransaction = {
                        mainUiState.currentProfileId?.let { profileId ->
                            navigateToNewTransaction(profileId, mainUiState.currentProfileTheme)
                        }
                    },
                    onNavigateToProfileSettings = { profileId ->
                        if (profileId == -1L) {
                            ProfileDetailActivity.start(this, null)
                        } else {
                            val profile = viewModel.allProfiles.value.find { it.id == profileId }
                            ProfileDetailActivity.start(this, profile)
                        }
                    },
                    onNavigateToTemplates = {
                        startActivity(Intent(this, TemplatesActivity::class.java))
                    },
                    onNavigateToBackups = {
                        BackupsActivity.start(this)
                    }
                )

                crashReportText?.let { text ->
                    CrashReportDialog(
                        crashReportText = text,
                        onDismiss = { dismissCrashReport() }
                    )
                }
            }
        }
    }

    private fun navigateToNewTransaction(profileId: Long, theme: Int) {
        val intent = Intent(this, NewTransactionActivityCompose::class.java)
        intent.putExtra(ProfileThemedActivity.PARAM_PROFILE_ID, profileId)
        intent.putExtra(ProfileThemedActivity.PARAM_THEME, theme)
        startActivity(intent)
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.slide_in_up, R.anim.dummy)
    }

    private fun onProfileChanged(newProfile: Profile?) {
        val newProfileTheme = newProfile?.theme ?: Colors.DEFAULT_HUE_DEG
        if (newProfileTheme != Colors.profileThemeId) {
            Logger.debug(
                "profiles",
                "profile theme ${Colors.profileThemeId} → $newProfileTheme"
            )
            Colors.profileThemeId = newProfileTheme
            profileThemeChanged()
            return
        }

        viewModel.updateProfile(newProfile)
        updateLastUpdateTextFromDB()
    }

    private fun onProfileListChanged(newList: List<Profile>) {
        createShortcuts(newList)
        viewModel.updateProfiles(newList)

        val currentProfile = profileRepository.currentProfile.value
        var replacementProfile: Profile? = null
        if (currentProfile != null) {
            for (p in newList) {
                if (p.id == currentProfile.id) {
                    replacementProfile = p
                    break
                }
            }
        }

        if (newList.isNotEmpty() && replacementProfile == null) {
            Logger.debug(TAG, "Switching profile because the current is no longer available")
            profileRepository.setCurrentProfile(newList[0])
        } else if (replacementProfile != null) {
            profileRepository.setCurrentProfile(replacementProfile)
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
                    Intent(Intent.ACTION_VIEW, null, this, NewTransactionActivityCompose::class.java)
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

    private fun updateLastUpdateText(
        lastUpdate: Date?,
        transactionCount: Int,
        displayedAccountCount: Int,
        totalAccountCount: Int
    ) {
        val formatFlags = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME
        val templateForTransactions = resources.getString(R.string.transaction_count_summary)
        val templateForAccounts = resources.getString(R.string.account_count_summary)
        val templateForAccountsFiltered = resources.getString(R.string.account_count_summary_filtered)
        val locale = Locale.getDefault()

        if (lastUpdate == null) {
            viewModel.updateHeaderTexts("----", "----")
        } else {
            val dateTimeText = DateUtils.formatDateTime(this, lastUpdate.time, formatFlags)

            val transactionsText = String.format(
                locale,
                templateForTransactions,
                transactionCount,
                dateTimeText
            )

            // Use hybrid format when filtered (displayed != total)
            val accountsText = if (displayedAccountCount == totalAccountCount) {
                String.format(locale, templateForAccounts, displayedAccountCount, dateTimeText)
            } else {
                String.format(
                    locale,
                    templateForAccountsFiltered,
                    displayedAccountCount,
                    totalAccountCount,
                    dateTimeText
                )
            }
            viewModel.updateHeaderTexts(accountsText, transactionsText)
        }
    }

    private fun updateLastUpdateTextFromDB() {
        val currentProfile = profileRepository.currentProfile.value ?: return

        lifecycleScope.launch {
            optionRepository.getOption(currentProfile.id, Option.OPT_LAST_SCRAPE)
                .collect { opt: Option? ->
                    var lastUpdate = 0L
                    if (opt != null) {
                        try {
                            lastUpdate = opt.value?.toLong() ?: 0L
                        } catch (ex: NumberFormatException) {
                            Logger.debug(TAG, "Error parsing '${opt.value}' as long", ex)
                        }
                    }

                    val syncInfo = viewModel.lastSyncInfo.value
                    if (lastUpdate == 0L) {
                        viewModel.updateLastUpdateInfo(null, 0, 0)
                    } else {
                        val date = Date(lastUpdate)
                        viewModel.updateLastUpdateInfo(
                            date,
                            syncInfo?.transactionCount ?: 0,
                            syncInfo?.accountCount ?: 0
                        )
                        updateLastUpdateText(
                            date,
                            syncInfo?.transactionCount ?: 0,
                            syncInfo?.accountCount ?: 0,
                            syncInfo?.totalAccountCount ?: 0
                        )
                    }
                }
        }
    }

    private fun profileThemeChanged() {
        Logger.debug(TAG, "profileThemeChanged(): recreating activity")
        recreate()
    }

    companion object {
        const val TAG = "main-compose"
    }
}
