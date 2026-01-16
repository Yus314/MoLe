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

import net.ktnx.mobileledger.TemporaryAuthData
import net.ktnx.mobileledger.domain.model.Profile

/**
 * Interface for managing temporary authentication data and system services.
 * Used during profile editing when testing connection to a server.
 *
 * This abstraction allows testing without Android framework dependencies.
 */
interface AuthDataProvider {
    /**
     * Set temporary authentication data for connection testing.
     */
    fun setTemporaryAuthData(authData: TemporaryAuthData?)

    /**
     * Get temporary authentication data for connection testing.
     */
    fun getTemporaryAuthData(): TemporaryAuthData?

    /**
     * Clear temporary authentication data after connection testing.
     */
    fun resetAuthenticationData()

    /**
     * Notify the system that backup data has changed.
     * Called after profile save/delete operations.
     */
    fun notifyBackupDataChanged()

    /**
     * Get the default theme hue for a new profile.
     */
    fun getDefaultThemeHue(): Int

    /**
     * Calculate a new theme hue based on existing profiles.
     * Returns a hue that's visually distinct from existing profile hues.
     */
    fun getNewProfileThemeHue(existingProfiles: List<Profile>?): Int
}
