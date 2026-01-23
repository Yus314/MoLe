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
import android.text.Editable
import android.view.WindowManager
import net.ktnx.mobileledger.domain.model.BalanceConstants

object Misc {
    const val ZERO_WIDTH_SPACE: Char = '\u200B'

    @JvmStatic
    @Deprecated(
        "Use Float.isEffectivelyZero() extension function instead",
        ReplaceWith("f.isEffectivelyZero()", "net.ktnx.mobileledger.utils.isEffectivelyZero")
    )
    fun isZero(f: Float): Boolean = f < BalanceConstants.BALANCE_EPSILON && f > -BalanceConstants.BALANCE_EPSILON

    @JvmStatic
    @Deprecated(
        "Use Float.equalsWithTolerance() extension function instead",
        ReplaceWith("a.equalsWithTolerance(b)", "net.ktnx.mobileledger.utils.equalsWithTolerance")
    )
    fun equalFloats(a: Float, b: Float): Boolean = isZero(a - b)

    @JvmStatic
    @Deprecated(
        "Use Activity.showSoftKeyboard() extension function instead",
        ReplaceWith("activity.showSoftKeyboard()", "net.ktnx.mobileledger.utils.showSoftKeyboard")
    )
    fun showSoftKeyboard(activity: Activity) {
        val cf = activity.resources.configuration
        if (cf.keyboard == Configuration.KEYBOARD_NOKEYS ||
            cf.keyboardHidden == Configuration.KEYBOARDHIDDEN_YES
        ) {
            activity.window.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
            )
        }
    }

    @JvmStatic
    @Deprecated(
        "Use Activity.hideSoftKeyboard() extension function instead",
        ReplaceWith("activity.hideSoftKeyboard()", "net.ktnx.mobileledger.utils.hideSoftKeyboard")
    )
    fun hideSoftKeyboard(activity: Activity) {
        val cf = activity.resources.configuration
        if (cf.keyboard == Configuration.KEYBOARD_NOKEYS ||
            cf.keyboardHidden == Configuration.KEYBOARDHIDDEN_NO
        ) {
            activity.window.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
            )
        }
    }

    @JvmStatic
    @Deprecated(
        "Use String?.emptyToNull() extension function instead",
        ReplaceWith("str.emptyToNull()", "net.ktnx.mobileledger.utils.emptyToNull")
    )
    fun emptyIsNull(str: String?): String? = if (str != null && str.isEmpty()) null else str

    @JvmStatic
    @Deprecated("Use Kotlin stdlib `str ?: \"\"` instead")
    fun nullIsEmpty(str: String?): String = str ?: ""

    @JvmStatic
    @Deprecated(
        "Use Editable?.toStringOrEmpty() extension function instead",
        ReplaceWith("e.toStringOrEmpty()", "net.ktnx.mobileledger.utils.toStringOrEmpty")
    )
    fun nullIsEmpty(e: Editable?): String = e?.toString() ?: ""

    @JvmStatic
    @Deprecated(
        "Use Kotlin's null coalescing: (a ?: \"\") == (b ?: \"\")",
        ReplaceWith("(a ?: \"\") == (b ?: \"\")")
    )
    fun equalStrings(a: String?, b: String?): Boolean = (a ?: "") == (b ?: "")

    @JvmStatic
    @Deprecated(
        "Use String?.equalsCharSequence() extension function instead",
        ReplaceWith("u.equalsCharSequence(text)", "net.ktnx.mobileledger.utils.equalsCharSequence")
    )
    fun stringEqualToCharSequence(u: String?, text: CharSequence): Boolean = (u ?: "") == text.toString()

    @JvmStatic
    @Deprecated("Use Kotlin stdlib String?.trim() instead")
    fun trim(string: String?): String? = string?.trim()

    @JvmStatic
    @Deprecated(
        "Use Kotlin's == operator which handles nulls correctly: a == b",
        ReplaceWith("a == b")
    )
    fun equalIntegers(a: Int?, b: Int?): Boolean = a == b

    @JvmStatic
    @Deprecated(
        "Use Kotlin's == operator which handles nulls correctly: a == b",
        ReplaceWith("a == b")
    )
    fun equalLongs(a: Long?, b: Long?): Boolean = a == b

    @JvmStatic
    @Deprecated(
        "Use String?.withWrapHints() extension function instead",
        ReplaceWith("input.withWrapHints()", "net.ktnx.mobileledger.utils.withWrapHints")
    )
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
