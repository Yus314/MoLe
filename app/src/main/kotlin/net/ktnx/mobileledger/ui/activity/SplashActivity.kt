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

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import dagger.hilt.android.AndroidEntryPoint
import net.ktnx.mobileledger.App
import net.ktnx.mobileledger.R
import net.ktnx.mobileledger.ui.components.CrashReportDialog
import net.ktnx.mobileledger.ui.splash.SplashEffect
import net.ktnx.mobileledger.ui.splash.SplashScreen
import net.ktnx.mobileledger.ui.splash.SplashViewModel
import net.ktnx.mobileledger.ui.theme.MoLeTheme
import net.ktnx.mobileledger.utils.Logger

@AndroidEntryPoint
class SplashActivity : CrashReportingActivity() {
    private val viewModel: SplashViewModel by viewModels()
    private var running = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme_default)

        val savedHue = App.getStartupTheme()
        setContent {
            MoLeTheme(profileHue = savedHue.toFloat()) {
                SplashScreen()

                crashReportText?.let { text ->
                    CrashReportDialog(
                        crashReportText = text,
                        onDismiss = { dismissCrashReport() }
                    )
                }

                // Handle navigation effects
                LaunchedEffect(Unit) {
                    viewModel.effects.collect { effect ->
                        when (effect) {
                            SplashEffect.NavigateToMain -> startMainActivity()
                        }
                    }
                }
            }
        }

        Logger.debug("splash", "onCreate()")
    }

    override fun onStart() {
        super.onStart()
        Logger.debug("splash", "onStart()")
        running = true
    }

    override fun onPause() {
        super.onPause()
        Logger.debug("splash", "onPause()")
        running = false
    }

    override fun onResume() {
        super.onResume()
        Logger.debug("splash", "onResume()")
        running = true
    }

    private fun startMainActivity() {
        if (running) {
            Logger.debug("splash", "still running, launching main activity")
            val intent = Intent(this, MainActivityCompose::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                        Intent.FLAG_ACTIVITY_NEW_TASK
                )
            }
            startActivity(intent)
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.fade_in_slowly, R.anim.fade_out_slowly)
        } else {
            Logger.debug("splash", "Not running, finish and go away")
            finish()
        }
    }
}
