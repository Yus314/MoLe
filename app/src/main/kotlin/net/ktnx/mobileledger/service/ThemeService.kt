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

import android.app.Activity
import android.content.res.ColorStateList
import android.content.res.Resources
import androidx.annotation.ColorInt
import kotlinx.coroutines.flow.StateFlow
import net.ktnx.mobileledger.core.domain.model.Profile
import net.ktnx.mobileledger.core.domain.repository.PreferencesRepository

/**
 * Theme management service interface.
 *
 * Provides theme color calculations and current theme state.
 * Replaces the static Colors object with an injectable service.
 *
 * ## Usage in ViewModel
 *
 * ```kotlin
 * @HiltViewModel
 * class ProfileViewModel @Inject constructor(
 *     private val themeService: ThemeService
 * ) : ViewModel() {
 *     fun getProfileColor(hue: Int): Int =
 *         themeService.getPrimaryColorForHue(hue)
 * }
 * ```
 *
 * ## Usage in Activity
 *
 * For base classes that cannot use @AndroidEntryPoint, use BackupEntryPoint:
 *
 * ```kotlin
 * protected val themeService: ThemeService by lazy {
 *     BackupEntryPoint.get(this).themeService()
 * }
 * ```
 */
interface ThemeService {
    /**
     * Current profile theme hue (0-360 degrees).
     * Used to track the active profile's theme color.
     */
    val currentThemeHue: StateFlow<Int>

    /**
     * Current primary color extracted from theme.
     */
    @get:ColorInt
    val primaryColor: Int

    /**
     * Current table row dark background color.
     */
    @get:ColorInt
    val tableRowDarkBG: Int

    /**
     * Set up theme for an activity.
     *
     * @param activity The activity to apply theme to
     * @param themeHue The hue value (0-360) for the theme
     */
    fun setupTheme(activity: Activity, themeHue: Int)

    /**
     * Refresh cached color values from the current theme.
     *
     * @param theme The current Resources.Theme
     */
    fun refreshColors(theme: Resources.Theme)

    /**
     * Update the current profile theme hue.
     *
     * @param hue The new hue value (0-360)
     */
    fun setCurrentThemeHue(hue: Int)

    /**
     * Get the primary color for a specific hue.
     *
     * @param hueDegrees Hue value in degrees (0-360)
     * @return The primary color for that hue
     */
    @ColorInt
    fun getPrimaryColorForHue(hueDegrees: Int): Int

    /**
     * Get the theme resource ID for a specific hue.
     *
     * @param themeHue Hue value in degrees (0-360)
     * @return The theme resource ID (R.style.AppTheme_xxx)
     */
    fun getThemeIdForHue(themeHue: Int): Int

    /**
     * Suggest a new profile theme hue that contrasts with existing profiles.
     *
     * @param profiles List of existing profiles
     * @return Suggested hue value that maximizes contrast
     */
    fun getNewProfileThemeHue(profiles: List<Profile>?): Int

    /**
     * Get a ColorStateList for swipe circle colors.
     *
     * @param hue Starting hue value (default: current theme hue)
     * @return ColorStateList with swipe circle colors
     */
    fun getColorStateList(hue: Int? = null): ColorStateList

    /**
     * Get an array of colors for swipe circles.
     *
     * @param hue Starting hue value (default: current theme hue)
     * @return Array of color values
     */
    fun getSwipeCircleColors(hue: Int? = null): IntArray

    companion object {
        /**
         * Default theme hue (purple).
         * Delegates to PreferencesRepository to ensure consistency.
         */
        const val DEFAULT_HUE_DEG = PreferencesRepository.DEFAULT_HUE_DEG
    }
}
