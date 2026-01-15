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
import android.util.TypedValue
import androidx.annotation.ColorInt
import androidx.lifecycle.MutableLiveData
import java.util.Locale
import net.ktnx.mobileledger.BuildConfig
import net.ktnx.mobileledger.R
import net.ktnx.mobileledger.db.Profile
import net.ktnx.mobileledger.ui.HueRing
import timber.log.Timber

object Colors {
    const val DEFAULT_HUE_DEG = 261

    @JvmField
    val themeWatch = MutableLiveData(0)

    private val EMPTY_STATES = arrayOf(intArrayOf())
    private const val SWIPE_COLOR_COUNT = 6

    private val themeIDs = intArrayOf(
        R.style.AppTheme_default, R.style.AppTheme_000, R.style.AppTheme_005,
        R.style.AppTheme_010, R.style.AppTheme_015, R.style.AppTheme_020, R.style.AppTheme_025,
        R.style.AppTheme_030, R.style.AppTheme_035, R.style.AppTheme_040, R.style.AppTheme_045,
        R.style.AppTheme_050, R.style.AppTheme_055, R.style.AppTheme_060, R.style.AppTheme_065,
        R.style.AppTheme_070, R.style.AppTheme_075, R.style.AppTheme_080, R.style.AppTheme_085,
        R.style.AppTheme_090, R.style.AppTheme_095, R.style.AppTheme_100, R.style.AppTheme_105,
        R.style.AppTheme_110, R.style.AppTheme_115, R.style.AppTheme_120, R.style.AppTheme_125,
        R.style.AppTheme_130, R.style.AppTheme_135, R.style.AppTheme_140, R.style.AppTheme_145,
        R.style.AppTheme_150, R.style.AppTheme_155, R.style.AppTheme_160, R.style.AppTheme_165,
        R.style.AppTheme_170, R.style.AppTheme_175, R.style.AppTheme_180, R.style.AppTheme_185,
        R.style.AppTheme_190, R.style.AppTheme_195, R.style.AppTheme_200, R.style.AppTheme_205,
        R.style.AppTheme_210, R.style.AppTheme_215, R.style.AppTheme_220, R.style.AppTheme_225,
        R.style.AppTheme_230, R.style.AppTheme_235, R.style.AppTheme_240, R.style.AppTheme_245,
        R.style.AppTheme_250, R.style.AppTheme_255, R.style.AppTheme_260, R.style.AppTheme_265,
        R.style.AppTheme_270, R.style.AppTheme_275, R.style.AppTheme_280, R.style.AppTheme_285,
        R.style.AppTheme_290, R.style.AppTheme_295, R.style.AppTheme_300, R.style.AppTheme_305,
        R.style.AppTheme_310, R.style.AppTheme_315, R.style.AppTheme_320, R.style.AppTheme_325,
        R.style.AppTheme_330, R.style.AppTheme_335, R.style.AppTheme_340, R.style.AppTheme_345,
        R.style.AppTheme_350, R.style.AppTheme_355
    )

    private val themePrimaryColor = HashMap<Int, Int>()

    @JvmField
    @ColorInt
    var primary: Int = 0

    @JvmField
    @ColorInt
    var tableRowDarkBG: Int = 0

    @JvmField
    var profileThemeId: Int = DEFAULT_HUE_DEG

    @JvmStatic
    fun refreshColors(theme: Resources.Theme) {
        val tv = TypedValue()
        theme.resolveAttribute(R.attr.table_row_dark_bg, tv, true)
        tableRowDarkBG = tv.data
        theme.resolveAttribute(androidx.appcompat.R.attr.colorPrimary, tv, true)
        primary = tv.data

        if (themePrimaryColor.isEmpty()) {
            for (themeId in themeIDs) {
                val tmpTheme = theme.resources.newTheme()
                tmpTheme.applyStyle(themeId, true)
                tmpTheme.resolveAttribute(androidx.appcompat.R.attr.colorPrimary, tv, false)
                themePrimaryColor[themeId] = tv.data
            }
        }

        // trigger theme observers
        themeWatch.postValue((themeWatch.value ?: 0) + 1)
    }

    @JvmStatic
    @ColorInt
    fun getPrimaryColorForHue(hueDegrees: Int): Int {
        if (hueDegrees == DEFAULT_HUE_DEG) {
            return themePrimaryColor.getValue(R.style.AppTheme_default)
        }
        val mod = hueDegrees % HueRing.hueStepDegrees
        return if (mod == 0) {
            val themeId = getThemeIdForHue(hueDegrees)
            val result = themePrimaryColor.getValue(themeId)
            Timber.d("getPrimaryColorForHue(%d) = %x", hueDegrees, result)
            result
        } else {
            val x0 = hueDegrees - mod
            val x1 = (x0 + HueRing.hueStepDegrees) % 360
            val y0 = themePrimaryColor.getValue(getThemeIdForHue(x0)).toFloat()
            val y1 = themePrimaryColor.getValue(getThemeIdForHue(x1)).toFloat()
            kotlin.math.round(y0 + hueDegrees * (y1 - y0) / (x1 - x0)).toInt()
        }
    }

    @JvmStatic
    fun getThemeIdForHue(themeHue: Int): Int {
        var adjustedHue = themeHue
        var themeIndex = -1
        if (adjustedHue == 360) adjustedHue = 0
        if (adjustedHue in 0 until 360 && adjustedHue != DEFAULT_HUE_DEG) {
            if (adjustedHue % HueRing.hueStepDegrees != 0) {
                Timber.w("Adjusting unexpected hue %d", adjustedHue)
                themeIndex = kotlin.math.round(1f * adjustedHue / HueRing.hueStepDegrees).toInt()
            } else {
                themeIndex = adjustedHue / HueRing.hueStepDegrees
            }
        }

        return themeIDs[themeIndex + 1] // 0 is the default theme
    }

    @JvmStatic
    fun setupTheme(activity: Activity, themeHue: Int) {
        val themeId = getThemeIdForHue(themeHue)
        activity.setTheme(themeId)

        refreshColors(activity.theme)
    }

    @JvmStatic
    fun getColorStateList(): ColorStateList = getColorStateList(profileThemeId)

    @JvmStatic
    fun getColorStateList(hue: Int): ColorStateList = ColorStateList(EMPTY_STATES, getSwipeCircleColors(hue))

    @JvmStatic
    fun getSwipeCircleColors(): IntArray = getSwipeCircleColors(profileThemeId)

    @JvmStatic
    fun getSwipeCircleColors(hue: Int): IntArray {
        val colors = IntArray(SWIPE_COLOR_COUNT)
        var currentHue = hue
        for (i in 0 until SWIPE_COLOR_COUNT) {
            colors[i] = getPrimaryColorForHue(currentHue)
            currentHue = (currentHue + 360 / SWIPE_COLOR_COUNT) % 360
        }
        return colors
    }

    @JvmStatic
    fun getNewProfileThemeHue(profiles: List<Profile>?): Int {
        if (profiles.isNullOrEmpty()) return DEFAULT_HUE_DEG

        val chosenHue: Int

        if (profiles.size == 1) {
            var opposite = profiles[0].theme + 180
            opposite %= 360
            chosenHue = opposite
        } else {
            val hues = ArrayList<Int>()
            for (p in profiles) {
                var hue = p.theme
                if (hue == -1) hue = DEFAULT_HUE_DEG
                hues.add(hue)
            }
            hues.sort()
            if (BuildConfig.DEBUG) {
                val huesSB = StringBuilder()
                for (h in hues) {
                    if (huesSB.isNotEmpty()) huesSB.append(", ")
                    huesSB.append(h)
                }
                Timber.d(String.format("used hues: %s", huesSB))
            }
            hues.add(hues[0])

            var lastHue = -1
            var largestInterval = 0
            val largestIntervalStarts = ArrayList<Int>()

            for (h in hues) {
                if (lastHue == -1) {
                    lastHue = h
                    continue
                }

                val interval = if (h > lastHue) {
                    h - lastHue // 10 -> 20 is a step of 10
                } else {
                    h + (360 - lastHue) // 350 -> 20 is a step of 30
                }

                if (interval > largestInterval) {
                    largestInterval = interval
                    largestIntervalStarts.clear()
                    largestIntervalStarts.add(lastHue)
                } else if (interval == largestInterval) {
                    largestIntervalStarts.add(lastHue)
                }

                lastHue = h
            }

            val chosenIndex = (Math.random() * largestIntervalStarts.size).toInt()
            val chosenIntervalStart = largestIntervalStarts[chosenIndex]

            Timber.d(
                "Choosing the middle colour between %d and %d",
                chosenIntervalStart,
                chosenIntervalStart + largestInterval
            )

            var adjustedInterval = largestInterval
            if (adjustedInterval % 2 != 0) {
                adjustedInterval++ // round up the middle point
            }

            chosenHue = (chosenIntervalStart + (adjustedInterval / 2)) % 360
        }

        var finalHue = chosenHue
        val mod = finalHue % HueRing.hueStepDegrees
        if (mod != 0) {
            if (mod > HueRing.hueStepDegrees / 2) {
                finalHue += (HueRing.hueStepDegrees - mod) // 13 += (5-3) = 15
            } else {
                finalHue -= mod // 12 -= 2 = 10
            }
        }

        Timber.d(String.format(Locale.US, "New profile hue: %d", finalHue))

        return finalHue
    }
}
