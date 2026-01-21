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

package net.ktnx.mobileledger.fake

import net.ktnx.mobileledger.TemporaryAuthData
import net.ktnx.mobileledger.domain.model.Profile
import net.ktnx.mobileledger.service.AuthDataProvider

/**
 * Fake AuthDataProvider for testing.
 *
 * This fake allows tests to verify that operations correctly call
 * AuthDataProvider methods without depending on Android framework.
 */
class FakeAuthDataProvider : AuthDataProvider {

    private var temporaryAuthData: TemporaryAuthData? = null
    private var defaultThemeHue = 0

    /**
     * Track how many times notifyBackupDataChanged was called.
     */
    var backupDataChangedCallCount = 0
        private set

    /**
     * Track how many times resetAuthenticationData was called.
     */
    var resetAuthenticationCallCount = 0
        private set

    override fun setTemporaryAuthData(authData: TemporaryAuthData?) {
        temporaryAuthData = authData
    }

    override fun getTemporaryAuthData(): TemporaryAuthData? = temporaryAuthData

    override fun resetAuthenticationData() {
        temporaryAuthData = null
        resetAuthenticationCallCount++
    }

    override fun notifyBackupDataChanged() {
        backupDataChangedCallCount++
    }

    override fun getDefaultThemeHue(): Int = defaultThemeHue

    override fun getNewProfileThemeHue(existingProfiles: List<Profile>?): Int {
        if (existingProfiles.isNullOrEmpty()) {
            return defaultThemeHue
        }
        // Simple implementation: increment by 60 degrees for each profile
        return (existingProfiles.size * 60) % 360
    }

    /**
     * Set the default theme hue for testing.
     */
    fun setDefaultThemeHue(hue: Int) {
        defaultThemeHue = hue
    }

    /**
     * Reset all tracking state.
     */
    fun reset() {
        temporaryAuthData = null
        defaultThemeHue = 0
        backupDataChangedCallCount = 0
        resetAuthenticationCallCount = 0
    }
}
