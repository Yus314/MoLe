/*
 * Copyright © 2019 Damyan Ivanov.
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

import android.content.Context
import android.util.TypedValue
import kotlin.math.roundToInt

/**
 * Convert dp (density-independent pixels) to px (actual pixels).
 */
fun Context.dp2px(dp: Float): Int = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    dp,
    resources.displayMetrics
).roundToInt()

/**
 * Convert sp (scale-independent pixels) to px (actual pixels).
 */
fun Context.sp2px(sp: Float): Int = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_SP,
    sp,
    resources.displayMetrics
).roundToInt()

// Legacy object for backward compatibility - will be removed in future
@Deprecated("Use Context.dp2px() and Context.sp2px() extension functions instead")
object DimensionUtils {
    @JvmStatic
    @Deprecated("Use Context.dp2px() instead", ReplaceWith("context.dp2px(dp)"))
    fun dp2px(context: Context, dp: Float): Int = context.dp2px(dp)

    @JvmStatic
    @Deprecated("Use Context.sp2px() instead", ReplaceWith("context.sp2px(sp)"))
    fun sp2px(context: Context, sp: Float): Int = context.sp2px(sp)
}
