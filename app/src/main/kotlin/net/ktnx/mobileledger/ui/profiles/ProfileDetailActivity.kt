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
import net.ktnx.mobileledger.db.Profile
import net.ktnx.mobileledger.ui.activity.CrashReportingActivity
import net.ktnx.mobileledger.ui.components.CrashReportDialog
import net.ktnx.mobileledger.ui.profile.ProfileDetailScreen
import net.ktnx.mobileledger.ui.theme.MoLeTheme
import net.ktnx.mobileledger.utils.Colors
import timber.log.Timber

/**
 * An activity representing a single Profile detail screen.
 * Rebuilt with Jetpack Compose.
 */
@AndroidEntryPoint
class ProfileDetailActivity : CrashReportingActivity() {

    private var profileId: Long = -1L
    private var themeHue: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        profileId = intent.getLongExtra(ARG_ITEM_ID, -1)
        themeHue = intent.getIntExtra(ARG_HUE, -1)

        if (themeHue == -1) {
            // Generate a random theme hue for new profiles (0-360)
            themeHue = (Math.random() * 360).toInt()
        }

        // Set up the theme for the activity
        Colors.setupTheme(this, themeHue)

        super.onCreate(savedInstanceState)

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
        fun start(context: Context, profile: Profile?) {
            val starter = Intent(context, ProfileDetailActivity::class.java)
            if (profile != null) {
                starter.putExtra(ARG_ITEM_ID, profile.id)
                starter.putExtra(ARG_HUE, profile.theme)
                Timber.d(
                    "Starting profile editor for profile %d, theme %d",
                    profile.id,
                    profile.theme
                )
            } else {
                Timber.d("Starting empty profile editor")
            }
            context.startActivity(starter)
        }
    }
}
