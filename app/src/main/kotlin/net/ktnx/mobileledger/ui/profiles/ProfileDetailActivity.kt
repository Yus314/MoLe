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

package net.ktnx.mobileledger.ui.profiles

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject
import logcat.logcat
import net.ktnx.mobileledger.service.ThemeService
import net.ktnx.mobileledger.ui.activity.CrashReportingActivity
import net.ktnx.mobileledger.ui.components.CrashReportDialog
import net.ktnx.mobileledger.ui.profile.ProfileDetailScreen
import net.ktnx.mobileledger.ui.theme.MoLeTheme

/**
 * An activity representing a single Profile detail screen.
 * Rebuilt with Jetpack Compose.
 */
@AndroidEntryPoint
class ProfileDetailActivity : CrashReportingActivity() {

    @Inject
    lateinit var themeService: ThemeService

    private var profileId: Long = -1L
    private var themeHue: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        profileId = intent.getLongExtra(ARG_ITEM_ID, -1)
        themeHue = intent.getIntExtra(ARG_HUE, -1)

        if (themeHue == -1) {
            // Generate a random theme hue for new profiles (0-360)
            themeHue = (Math.random() * 360).toInt()
        }

        super.onCreate(savedInstanceState)

        // Set up the theme for the activity (after Hilt injection)
        themeService.setupTheme(this, themeHue)

        setContent {
            MoLeTheme(profileHue = themeHue.toFloat()) {
                ProfileDetailScreen(
                    profileId = if (profileId > 0) profileId else 0L,
                    initialThemeHue = themeHue,
                    onNavigateBack = { finish() }
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

    companion object {
        const val ARG_ITEM_ID = "item_id"
        const val ARG_HUE = "hue"

        @JvmStatic
        fun start(context: Context, profileId: Long?, themeHue: Int?) {
            val starter = Intent(context, ProfileDetailActivity::class.java)
            if (profileId != null && profileId > 0) {
                starter.putExtra(ARG_ITEM_ID, profileId)
                if (themeHue != null) {
                    starter.putExtra(ARG_HUE, themeHue)
                }
                logcat { "Starting profile editor for profile $profileId, theme $themeHue" }
            } else {
                logcat { "Starting empty profile editor" }
            }
            context.startActivity(starter)
        }
    }
}
