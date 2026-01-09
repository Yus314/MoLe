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
import android.content.pm.PackageManager
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
import dagger.hilt.android.AndroidEntryPoint
import java.util.Date
import java.util.Locale
import net.ktnx.mobileledger.BackupsActivity
import net.ktnx.mobileledger.R
import net.ktnx.mobileledger.db.DB
import net.ktnx.mobileledger.db.Option
import net.ktnx.mobileledger.db.Profile
import net.ktnx.mobileledger.model.Data
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

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        Logger.debug(TAG, "onCreate()/entry")
        super.onCreate(savedInstanceState)
        Logger.debug(TAG, "onCreate()/after super")

        // Observe profile changes from Data singleton
        Data.observeProfile(this) { newProfile -> onProfileChanged(newProfile) }
        Data.profiles.observe(this) { profiles -> onProfileListChanged(profiles) }
        Data.backgroundTaskProgress.observe(this) { progress ->
            viewModel.updateBackgroundTaskProgress(progress)
            // Reset the task reference when finished so new refreshes can start
            if (progress?.state == net.ktnx.mobileledger.async.RetrieveTransactionsTask.ProgressState.FINISHED) {
                viewModel.transactionRetrievalDone()
            }
        }
        Data.backgroundTasksRunning.observe(this) { running ->
            viewModel.updateBackgroundTasksRunning(running)
        }
        Data.lastUpdateDate.observe(this) { date ->
            viewModel.updateLastUpdateInfo(
                date,
                Data.lastUpdateTransactionCount.value,
                Data.lastUpdateAccountCount.value
            )
            updateLastUpdateText()
        }
        Data.lastUpdateAccountCount.observe(this) { _ ->
            updateLastUpdateText()
        }
        Data.lastUpdateTotalAccountCount.observe(this) { _ ->
            updateLastUpdateText()
        }
        Data.lastUpdateTransactionCount.observe(this) { _ ->
            updateLastUpdateText()
        }
        Data.lastAccountsUpdateText.observe(this) { text ->
            viewModel.updateHeaderTexts(text, null)
        }
        Data.lastTransactionsUpdateText.observe(this) { text ->
            viewModel.updateHeaderTexts(null, text)
        }

        setContent {
            val mainUiState by viewModel.mainUiState.collectAsState()
            val accountSummaryUiState by viewModel.accountSummaryUiState.collectAsState()
            val transactionListUiState by viewModel.transactionListUiState.collectAsState()

            // Handle one-shot effects
            LaunchedEffect(Unit) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        is MainEffect.NavigateToNewTransaction -> {
                            navigateToNewTransaction(effect.profileId, effect.theme)
                        }

                        is MainEffect.NavigateToProfileDetail -> {
                            if (effect.profileId != null) {
                                val profile = Data.profiles.value?.find { it.id == effect.profileId }
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
                            val profile = Data.profiles.value?.find { it.id == profileId }
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

        if (newList.isNotEmpty() && replacementProfile == null) {
            Logger.debug(TAG, "Switching profile because the current is no longer available")
            Data.setCurrentProfile(newList[0])
        } else if (replacementProfile != null) {
            Data.setCurrentProfile(replacementProfile)
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

    private fun updateLastUpdateText() {
        val formatFlags = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME
        val templateForTransactions = resources.getString(R.string.transaction_count_summary)
        val templateForAccounts = resources.getString(R.string.account_count_summary)
        val templateForAccountsFiltered = resources.getString(R.string.account_count_summary_filtered)
        val displayedAccountCount = Data.lastUpdateAccountCount.value ?: 0
        val totalAccountCount = Data.lastUpdateTotalAccountCount.value ?: 0
        val transactionCount = Data.lastUpdateTransactionCount.value
        val lastUpdate = Data.lastUpdateDate.value
        val locale = Data.locale.value ?: Locale.getDefault()

        if (lastUpdate == null) {
            Data.lastTransactionsUpdateText.value = "----"
            Data.lastAccountsUpdateText.value = "----"
        } else {
            val dateTimeText = DateUtils.formatDateTime(this, lastUpdate.time, formatFlags)

            Data.lastTransactionsUpdateText.value = String.format(
                locale,
                templateForTransactions,
                transactionCount ?: 0,
                dateTimeText
            )

            // Use hybrid format when filtered (displayed != total)
            Data.lastAccountsUpdateText.value = if (displayedAccountCount == totalAccountCount) {
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
        }
    }

    private fun updateLastUpdateTextFromDB() {
        val currentProfile = Data.getProfile() ?: return

        DB.get()
            .getOptionDAO()
            .load(currentProfile.id, Option.OPT_LAST_SCRAPE)
            .observe(this) { opt: Option? ->
                var lastUpdate = 0L
                if (opt != null) {
                    try {
                        lastUpdate = opt.value?.toLong() ?: 0L
                    } catch (ex: NumberFormatException) {
                        Logger.debug(TAG, "Error parsing '${opt.value}' as long", ex)
                    }
                }

                if (lastUpdate == 0L) {
                    Data.lastUpdateDate.postValue(null)
                } else {
                    Data.lastUpdateDate.postValue(Date(lastUpdate))
                }
            }
    }

    private fun profileThemeChanged() {
        Data.removeProfileObservers(this)
        Data.profiles.removeObservers(this)
        Data.lastUpdateTransactionCount.removeObservers(this)
        Data.lastUpdateAccountCount.removeObservers(this)
        Data.lastUpdateTotalAccountCount.removeObservers(this)
        Data.lastUpdateDate.removeObservers(this)

        Logger.debug(TAG, "profileThemeChanged(): recreating activity")
        recreate()
    }

    companion object {
        const val TAG = "main-compose"
    }
}
