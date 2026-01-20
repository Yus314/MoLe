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

import android.view.View
import android.widget.TextView
import net.ktnx.mobileledger.domain.model.BalanceConstants

/**
 * Extension functions for common patterns used throughout the codebase.
 * These provide more idiomatic Kotlin alternatives to utility methods.
 */

// String Extensions

/**
 * Returns null if the string is empty, otherwise returns the string.
 * Equivalent to Misc.emptyIsNull()
 */
fun String?.emptyToNull(): String? = if (this.isNullOrEmpty()) null else this

/**
 * Adds zero-width space hints after colons to improve text wrapping.
 * Equivalent to Misc.addWrapHints()
 */
fun String?.withWrapHints(): String? {
    if (this == null) return null

    val result = StringBuilder()
    var lastPos = 0
    var pos = indexOf(':')

    while (pos >= 0) {
        result.append(substring(lastPos, pos + 1))
            .append(Misc.ZERO_WIDTH_SPACE)
        lastPos = pos + 1
        pos = indexOf(':', lastPos + 1)
    }
    if (lastPos > 0) {
        result.append(substring(lastPos))
    }

    return result.toString()
}

// View Extensions

/**
 * Sets view visibility to VISIBLE.
 */
fun View.show() {
    visibility = View.VISIBLE
}

/**
 * Sets view visibility to GONE.
 */
fun View.hide() {
    visibility = View.GONE
}

/**
 * Sets view visibility to INVISIBLE.
 */
fun View.invisible() {
    visibility = View.INVISIBLE
}

/**
 * Sets view visibility based on condition.
 * @param visible If true, sets VISIBLE; if false, sets GONE
 */
fun View.visibleIf(visible: Boolean) {
    visibility = if (visible) View.VISIBLE else View.GONE
}

/**
 * Sets view visibility based on condition.
 * @param visible If true, sets VISIBLE; if false, sets INVISIBLE
 */
fun View.visibleOrInvisible(visible: Boolean) {
    visibility = if (visible) View.VISIBLE else View.INVISIBLE
}

/**
 * Returns true if the view is currently visible.
 */
val View.isVisible: Boolean
    get() = visibility == View.VISIBLE

/**
 * Returns true if the view is currently gone.
 */
val View.isGone: Boolean
    get() = visibility == View.GONE

// TextView Extensions

/**
 * Gets the text content as a String, never null.
 */
val TextView.textString: String
    get() = text?.toString() ?: ""

// Float Extensions

/**
 * Returns true if this float is effectively zero (within BALANCE_EPSILON tolerance).
 * Equivalent to Misc.isZero()
 */
fun Float.isEffectivelyZero(): Boolean =
    this < BalanceConstants.BALANCE_EPSILON && this > -BalanceConstants.BALANCE_EPSILON

/**
 * Returns true if this float equals another within tolerance.
 * Equivalent to Misc.equalFloats()
 */
fun Float.equalsWithTolerance(other: Float): Boolean = (this - other).isEffectivelyZero()
