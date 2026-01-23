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

package net.ktnx.mobileledger.utils

import android.app.Activity
import android.content.res.ColorStateList
import android.content.res.Resources
import androidx.annotation.ColorInt
import net.ktnx.mobileledger.di.ThemeServiceEntryPoint
import net.ktnx.mobileledger.domain.model.Profile
import net.ktnx.mobileledger.service.ThemeService

/**
 * Legacy theme color utilities.
 *
 * @deprecated Use [ThemeService] via Hilt injection instead.
 * This object delegates to ThemeService for backward compatibility.
 *
 * For DI-accessible classes (ViewModels, Services, Repositories):
 * ```kotlin
 * @Inject constructor(private val themeService: ThemeService)
 * ```
 *
 * For Activities/base classes, use BackupEntryPoint:
 * ```kotlin
 * protected val themeService: ThemeService by lazy {
 *     BackupEntryPoint.get(this).themeService()
 * }
 * ```
 */
@Deprecated("Use ThemeService via Hilt injection instead")
object Colors {
    /**
     * Default theme hue (purple).
     * @deprecated Use [ThemeService.DEFAULT_HUE_DEG] instead.
     */
    @Deprecated(
        "Use ThemeService.DEFAULT_HUE_DEG instead",
        ReplaceWith("ThemeService.DEFAULT_HUE_DEG", "net.ktnx.mobileledger.service.ThemeService")
    )
    const val DEFAULT_HUE_DEG = ThemeService.DEFAULT_HUE_DEG

    /**
     * Primary color extracted from current theme.
     * @deprecated Use [ThemeService.primaryColor] instead.
     */
    @JvmStatic
    @get:ColorInt
    @Deprecated(
        "Use ThemeService.primaryColor instead",
        ReplaceWith("themeService.primaryColor")
    )
    val primary: Int
        get() = ThemeServiceEntryPoint.get().primaryColor

    /**
     * Table row dark background color.
     * @deprecated Use [ThemeService.tableRowDarkBG] instead.
     */
    @JvmStatic
    @get:ColorInt
    @Deprecated(
        "Use ThemeService.tableRowDarkBG instead",
        ReplaceWith("themeService.tableRowDarkBG")
    )
    val tableRowDarkBG: Int
        get() = ThemeServiceEntryPoint.get().tableRowDarkBG

    /**
     * Current profile theme hue.
     * @deprecated Use [ThemeService.currentThemeHue] instead.
     */
    @JvmStatic
    @Deprecated(
        "Use ThemeService.currentThemeHue instead",
        ReplaceWith("themeService.currentThemeHue.value")
    )
    var profileThemeId: Int
        get() = ThemeServiceEntryPoint.get().currentThemeHue.value
        set(value) = ThemeServiceEntryPoint.get().setCurrentThemeHue(value)

    /**
     * Refresh cached color values from the current theme.
     * @deprecated Use [ThemeService.refreshColors] instead.
     */
    @JvmStatic
    @Deprecated(
        "Use ThemeService.refreshColors instead",
        ReplaceWith("themeService.refreshColors(theme)")
    )
    fun refreshColors(theme: Resources.Theme) {
        ThemeServiceEntryPoint.get().refreshColors(theme)
    }

    /**
     * Get the primary color for a specific hue.
     * @deprecated Use [ThemeService.getPrimaryColorForHue] instead.
     */
    @JvmStatic
    @ColorInt
    @Deprecated(
        "Use ThemeService.getPrimaryColorForHue instead",
        ReplaceWith("themeService.getPrimaryColorForHue(hueDegrees)")
    )
    fun getPrimaryColorForHue(hueDegrees: Int): Int = ThemeServiceEntryPoint.get().getPrimaryColorForHue(hueDegrees)

    /**
     * Get the theme resource ID for a specific hue.
     * @deprecated Use [ThemeService.getThemeIdForHue] instead.
     */
    @JvmStatic
    @Deprecated(
        "Use ThemeService.getThemeIdForHue instead",
        ReplaceWith("themeService.getThemeIdForHue(themeHue)")
    )
    fun getThemeIdForHue(themeHue: Int): Int = ThemeServiceEntryPoint.get().getThemeIdForHue(themeHue)

    /**
     * Set up theme for an activity.
     * @deprecated Use [ThemeService.setupTheme] instead.
     */
    @JvmStatic
    @Deprecated(
        "Use ThemeService.setupTheme instead",
        ReplaceWith("themeService.setupTheme(activity, themeHue)")
    )
    fun setupTheme(activity: Activity, themeHue: Int) {
        ThemeServiceEntryPoint.get().setupTheme(activity, themeHue)
    }

    /**
     * Get a ColorStateList for swipe circle colors using current theme hue.
     * @deprecated Use [ThemeService.getColorStateList] instead.
     */
    @JvmStatic
    @Deprecated(
        "Use ThemeService.getColorStateList instead",
        ReplaceWith("themeService.getColorStateList()")
    )
    fun getColorStateList(): ColorStateList = ThemeServiceEntryPoint.get().getColorStateList()

    /**
     * Get a ColorStateList for swipe circle colors.
     * @deprecated Use [ThemeService.getColorStateList] instead.
     */
    @JvmStatic
    @Deprecated(
        "Use ThemeService.getColorStateList instead",
        ReplaceWith("themeService.getColorStateList(hue)")
    )
    fun getColorStateList(hue: Int): ColorStateList = ThemeServiceEntryPoint.get().getColorStateList(hue)

    /**
     * Get an array of colors for swipe circles using current theme hue.
     * @deprecated Use [ThemeService.getSwipeCircleColors] instead.
     */
    @JvmStatic
    @Deprecated(
        "Use ThemeService.getSwipeCircleColors instead",
        ReplaceWith("themeService.getSwipeCircleColors()")
    )
    fun getSwipeCircleColors(): IntArray = ThemeServiceEntryPoint.get().getSwipeCircleColors()

    /**
     * Get an array of colors for swipe circles.
     * @deprecated Use [ThemeService.getSwipeCircleColors] instead.
     */
    @JvmStatic
    @Deprecated(
        "Use ThemeService.getSwipeCircleColors instead",
        ReplaceWith("themeService.getSwipeCircleColors(hue)")
    )
    fun getSwipeCircleColors(hue: Int): IntArray = ThemeServiceEntryPoint.get().getSwipeCircleColors(hue)

    /**
     * Suggest a new profile theme hue that contrasts with existing profiles.
     * @deprecated Use [ThemeService.getNewProfileThemeHue] instead.
     */
    @JvmStatic
    @Deprecated(
        "Use ThemeService.getNewProfileThemeHue instead",
        ReplaceWith("themeService.getNewProfileThemeHue(profiles)")
    )
    fun getNewProfileThemeHue(profiles: List<Profile>?): Int {
        val service = ThemeServiceEntryPoint.get()
        return service.getNewProfileThemeHue(profiles)
    }
}
