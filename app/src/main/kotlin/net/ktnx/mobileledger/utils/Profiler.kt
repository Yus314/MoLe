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

import logcat.logcat
import net.ktnx.mobileledger.BuildConfig

class Profiler(private val name: String) {
    private var opStart: Long = 0
    private var opCount: Long = 0
    private var opMills: Long = 0

    fun opStart() {
        if (!BuildConfig.DEBUG) return

        check(opStart == 0L) { "opStart() already called with no opEnd()" }
        this.opStart = System.currentTimeMillis()
        opCount++
    }

    fun opEnd() {
        if (!BuildConfig.DEBUG) return

        check(opStart != 0L) { "opStart() not called" }
        opMills += System.currentTimeMillis() - opStart
        opStart = 0
    }

    fun dumpStats() {
        if (!BuildConfig.DEBUG) return

        logcat {
            "Operation '$name' executed $opCount times for $opMills ms. Average time ${"%.2f".format(
                1.0 * opMills / opCount
            )}ms"
        }
    }
}
