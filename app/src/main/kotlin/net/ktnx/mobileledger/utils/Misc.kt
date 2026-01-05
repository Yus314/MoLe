/*
 * Copyright © 2021 Damyan Ivanov.
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
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.view.WindowManager
import androidx.fragment.app.Fragment

object Misc {
    const val ZERO_WIDTH_SPACE: Char = '\u200B'

    @JvmStatic
    fun isZero(f: Float): Boolean = (f < 0.005f) && (f > -0.005f)

    @JvmStatic
    fun equalFloats(a: Float, b: Float): Boolean = isZero(a - b)

    @JvmStatic
    fun showSoftKeyboard(activity: Activity) {
        val cf = activity.resources.configuration
        if (cf.keyboard == Configuration.KEYBOARD_NOKEYS ||
            cf.keyboardHidden == Configuration.KEYBOARDHIDDEN_YES
        ) {
            activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }
    }

    @JvmStatic
    fun showSoftKeyboard(fragment: Fragment) {
        fragment.activity?.let { showSoftKeyboard(it) }
    }

    @JvmStatic
    fun hideSoftKeyboard(fragment: Fragment) {
        fragment.activity?.let { hideSoftKeyboard(it) }
    }

    @JvmStatic
    fun hideSoftKeyboard(activity: Activity) {
        val cf = activity.resources.configuration
        if (cf.keyboard == Configuration.KEYBOARD_NOKEYS ||
            cf.keyboardHidden == Configuration.KEYBOARDHIDDEN_NO
        ) {
            activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        }
    }

    @JvmStatic
    fun emptyIsNull(str: String?): String? = if (str != null && str.isEmpty()) null else str

    @JvmStatic
    fun nullIsEmpty(str: String?): String = str ?: ""

    @JvmStatic
    fun nullIsEmpty(e: Editable?): String = e?.toString() ?: ""

    @JvmStatic
    fun equalStrings(a: String?, b: String?): Boolean = nullIsEmpty(a) == nullIsEmpty(b)

    @JvmStatic
    fun stringEqualToCharSequence(u: String?, text: CharSequence): Boolean = nullIsEmpty(u) == text.toString()

    @JvmStatic
    fun trim(string: String?): String? = string?.trim()

    @JvmStatic
    fun equalIntegers(a: Int?, b: Int?): Boolean {
        if (a == null && b == null) return true
        if (a == null || b == null) return false
        return a == b
    }

    @JvmStatic
    fun equalLongs(a: Long?, b: Long?): Boolean {
        if (a == null && b == null) return true
        if (a == null || b == null) return false
        return a == b
    }

    @JvmStatic
    fun onMainThread(r: Runnable) {
        Handler(Looper.getMainLooper()).post(r)
    }

    @JvmStatic
    fun addWrapHints(input: String?): String? {
        if (input == null) return null

        val result = StringBuilder()
        var lastPos = 0
        var pos = input.indexOf(':')

        while (pos >= 0) {
            result.append(input.substring(lastPos, pos + 1))
                .append(ZERO_WIDTH_SPACE)
            lastPos = pos + 1
            pos = input.indexOf(':', lastPos + 1)
        }
        if (lastPos > 0) {
            result.append(input.substring(lastPos))
        }

        return result.toString()
    }
}
