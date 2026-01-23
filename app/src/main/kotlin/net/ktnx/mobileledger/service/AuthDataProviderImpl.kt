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
import net.ktnx.mobileledger.BuildConfig
import net.ktnx.mobileledger.TemporaryAuthData
import net.ktnx.mobileledger.domain.model.Profile

/**
 * Production implementation of AuthDataProvider.
 * Holds temporary authentication data internally instead of delegating to App singleton.
 */
@Singleton
class AuthDataProviderImpl @Inject constructor(
    private val themeService: ThemeService
) : AuthDataProvider {
    @Volatile
    private var temporaryAuthData: TemporaryAuthData? = null

    override fun setTemporaryAuthData(authData: TemporaryAuthData?) {
        temporaryAuthData = authData
    }

    override fun getTemporaryAuthData(): TemporaryAuthData? = temporaryAuthData

    override fun resetAuthenticationData() {
        temporaryAuthData = null
    }

    override fun notifyBackupDataChanged() {
        BackupManager.dataChanged(BuildConfig.APPLICATION_ID)
    }

    override fun getDefaultThemeHue(): Int = ThemeService.DEFAULT_HUE_DEG

    override fun getNewProfileThemeHue(existingProfiles: List<Profile>?): Int =
        themeService.getNewProfileThemeHue(existingProfiles)
}
