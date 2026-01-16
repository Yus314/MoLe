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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import logcat.logcat
import net.ktnx.mobileledger.R
import net.ktnx.mobileledger.db.Profile
import net.ktnx.mobileledger.ui.QR
import net.ktnx.mobileledger.ui.theme.MoLeTheme
import net.ktnx.mobileledger.ui.transaction.AccountRowsEvent
import net.ktnx.mobileledger.ui.transaction.AccountRowsViewModel
import net.ktnx.mobileledger.ui.transaction.NewTransactionScreen
import net.ktnx.mobileledger.ui.transaction.TemplateApplicatorEffect
import net.ktnx.mobileledger.ui.transaction.TemplateApplicatorEvent
import net.ktnx.mobileledger.ui.transaction.TemplateApplicatorViewModel
import net.ktnx.mobileledger.ui.transaction.TransactionFormViewModel

/**
 * New Transaction Activity using Jetpack Compose for the UI.
 *
 * Provides a form for creating new transactions with:
 * - Date selection
 * - Description with autocomplete
 * - Multiple account rows with amount, currency, and comment fields
 * - Template support via QR code scanning
 *
 * Uses three specialized ViewModels:
 * - TransactionFormViewModel: Form fields (date, description, comment) and submission
 * - AccountRowsViewModel: Account row CRUD, amount calculation, currency selection
 * - TemplateApplicatorViewModel: Template search and application
 */
@AndroidEntryPoint
class NewTransactionActivityCompose :
    ProfileThemedActivity(),
    QR.QRScanResultReceiver {

    private val formViewModel: TransactionFormViewModel by viewModels()
    private val accountRowsViewModel: AccountRowsViewModel by viewModels()
    private val templateApplicatorViewModel: TemplateApplicatorViewModel by viewModels()
    private lateinit var qrScanLauncher: ActivityResultLauncher<Void?>
    private var profileTheme: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // intentから直接profileIdを取得して即座に設定（非同期を待たない）
        val profileId = intent.getLongExtra(PARAM_PROFILE_ID, 0)
        if (profileId > 0) {
            formViewModel.setProfile(profileId)
        }

        qrScanLauncher = QR.registerLauncher(this, this)

        // Observe profile changes (プロファイル変更時の対応)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                profileRepository.currentProfile.collect { profile ->
                    if (profile == null) {
                        logcat { "No active profile. Redirecting to SplashActivity" }
                        val intent = Intent(this@NewTransactionActivityCompose, SplashActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_TASK_ON_HOME or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        val profileId = profile.id ?: return@collect
                        formViewModel.setProfile(profileId)
                    }
                }
            }
        }

        // Observe template apply effects and coordinate between ViewModels
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                templateApplicatorViewModel.effects.collect { effect ->
                    when (effect) {
                        is TemplateApplicatorEffect.ApplyTemplate -> {
                            // Apply template data to form and account rows
                            formViewModel.applyTemplateData(
                                description = effect.description,
                                transactionComment = effect.transactionComment,
                                date = effect.date
                            )
                            accountRowsViewModel.onEvent(
                                AccountRowsEvent.SetRows(effect.accounts)
                            )
                        }
                    }
                }
            }
        }

        setContent {
            MoLeTheme(
                profileHue = if (profileTheme >= 0) profileTheme.toFloat() else null
            ) {
                NewTransactionScreen(
                    formViewModel = formViewModel,
                    accountRowsViewModel = accountRowsViewModel,
                    templateApplicatorViewModel = templateApplicatorViewModel,
                    onNavigateBack = { finish() }
                )
            }
        }
    }

    override fun initProfile() {
        val profileId = intent.getLongExtra(PARAM_PROFILE_ID, 0)
        profileTheme = intent.getIntExtra(PARAM_THEME, -1)

        if (profileTheme < 0) {
            logcat { "Started with invalid/missing theme; quitting" }
            finish()
            return
        }

        if (profileId <= 0) {
            logcat { "Started with invalid/missing profile_id; quitting" }
            finish()
            return
        }

        setupProfileColors(profileTheme)
        initProfile(profileId)
    }

    override fun finish() {
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.dummy, R.anim.slide_out_down)
    }

    override fun onQRScanResult(text: String?) {
        logcat { "Got QR scan result [$text]" }
        // QR result handling is now done through the TemplateApplicatorViewModel
        if (!text.isNullOrBlank()) {
            templateApplicatorViewModel.onEvent(TemplateApplicatorEvent.ApplyTemplateFromQr(text))
        }
    }

    fun triggerQRScan() {
        qrScanLauncher.launch(null)
    }

    companion object {

        /**
         * Start the new transaction activity for the given profile.
         */
        fun start(context: Context, profile: Profile) {
            val intent = Intent(context, NewTransactionActivityCompose::class.java)
            intent.putExtra(PARAM_PROFILE_ID, profile.id)
            intent.putExtra(PARAM_THEME, profile.theme)
            context.startActivity(intent)
        }

        /**
         * Create an intent for the new transaction activity.
         */
        fun createIntent(context: Context, profileId: Long, theme: Int): Intent =
            Intent(context, NewTransactionActivityCompose::class.java).apply {
                putExtra(PARAM_PROFILE_ID, profileId)
                putExtra(PARAM_THEME, theme)
            }
    }
}
