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

import android.util.Log
import net.ktnx.mobileledger.BuildConfig

object Logger {
    @JvmStatic
    fun debug(tag: String, msg: String) {
        if (BuildConfig.DEBUG) Log.d(tag, msg)
    }

    @JvmStatic
    fun debug(tag: String, msg: String, e: Throwable) {
        if (BuildConfig.DEBUG) Log.d(tag, msg, e)
    }

    @JvmStatic
    fun warn(tag: String, msg: String) {
        Log.w(tag, msg)
    }

    @JvmStatic
    fun warn(tag: String, msg: String, e: Throwable) {
        Log.w(tag, msg, e)
    }
}
