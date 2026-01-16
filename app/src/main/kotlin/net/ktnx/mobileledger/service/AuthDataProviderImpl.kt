/*
 * Copyright © 2026 Damyan Ivanov.
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

package net.ktnx.mobileledger.service

import android.app.backup.BackupManager
import javax.inject.Inject
import javax.inject.Singleton
import net.ktnx.mobileledger.App
import net.ktnx.mobileledger.BuildConfig
import net.ktnx.mobileledger.TemporaryAuthData
import net.ktnx.mobileledger.domain.model.Profile
import net.ktnx.mobileledger.utils.Colors

/**
 * Production implementation of AuthDataProvider.
 * Delegates to App.setTemporaryAuthData() and App.resetAuthenticationData().
 */
@Singleton
class AuthDataProviderImpl @Inject constructor() : AuthDataProvider {
    override fun setTemporaryAuthData(authData: TemporaryAuthData?) {
        App.setTemporaryAuthData(authData)
    }

    override fun resetAuthenticationData() {
        App.resetAuthenticationData()
    }

    override fun notifyBackupDataChanged() {
        BackupManager.dataChanged(BuildConfig.APPLICATION_ID)
    }

    override fun getDefaultThemeHue(): Int = Colors.DEFAULT_HUE_DEG

    override fun getNewProfileThemeHue(existingProfiles: List<Profile>?): Int =
        Colors.getNewProfileThemeHue(existingProfiles)
}
