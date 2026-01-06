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
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.ViewModelProvider
import net.ktnx.mobileledger.R
import net.ktnx.mobileledger.db.DB
import net.ktnx.mobileledger.db.Profile
import net.ktnx.mobileledger.model.Data
import net.ktnx.mobileledger.ui.activity.CrashReportingActivity
import net.ktnx.mobileledger.utils.Colors
import net.ktnx.mobileledger.utils.Logger
import java.util.Locale

/**
 * An activity representing a single Profile detail screen. This
 * activity is only used on narrow width devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a ProfileListActivity (not really).
 */
class ProfileDetailActivity : CrashReportingActivity() {
    private var mFragment: ProfileDetailFragment? = null

    private fun getModel(): ProfileDetailModel {
        return ViewModelProvider(this)[ProfileDetailModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val id = intent.getLongExtra(ProfileDetailFragment.ARG_ITEM_ID, -1)

        DB.get()
            .getProfileDAO()
            .getById(id)
            .observe(this) { profile -> setProfile(profile) }

        var themeHue = intent.getIntExtra(ProfileDetailFragment.ARG_HUE, -1)

        super.onCreate(savedInstanceState)
        if (themeHue == -1) {
            themeHue = Colors.getNewProfileThemeHue(Data.profiles.value)
        }
        Colors.setupTheme(this, themeHue)
        val model = getModel()
        model.initialThemeHue = themeHue
        model.setThemeId(themeHue)
        setContentView(R.layout.activity_profile_detail)
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.detail_toolbar)
        setSupportActionBar(toolbar)

        // Show the Up button in the action bar.
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don't need to manually add it.
        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.
            val arguments = Bundle()
            arguments.putInt(ProfileDetailFragment.ARG_HUE, themeHue)
            mFragment = ProfileDetailFragment()
            mFragment?.arguments = arguments
            supportFragmentManager.beginTransaction()
                .add(R.id.profile_detail_container, mFragment!!)
                .commit()
        }
    }

    private fun setProfile(profile: Profile?) {
        val model = ViewModelProvider(this)[ProfileDetailModel::class.java]
        val appBarLayout = findViewById<com.google.android.material.appbar.CollapsingToolbarLayout>(
            R.id.toolbar_layout
        )
        appBarLayout?.title = profile?.name ?: resources.getString(R.string.new_profile_title)
        model.setValuesFromProfile(profile)
    }

    @Suppress("DEPRECATION")
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        Logger.debug("profiles", "[activity] Creating profile details options menu")
        mFragment?.onCreateOptionsMenu(menu, menuInflater)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val TAG = "profile-det-act"

        @JvmStatic
        fun start(context: Context, profile: Profile?) {
            val starter = Intent(context, ProfileDetailActivity::class.java)
            if (profile != null) {
                starter.putExtra(ProfileDetailFragment.ARG_ITEM_ID, profile.id)
                starter.putExtra(ProfileDetailFragment.ARG_HUE, profile.theme)
                Logger.debug(
                    TAG,
                    String.format(
                        Locale.ROOT,
                        "Starting profile editor for profile %d, theme %d",
                        profile.id,
                        profile.theme
                    )
                )
            } else {
                Logger.debug(TAG, "Starting empty profile editor")
            }
            context.startActivity(starter)
        }
    }
}
