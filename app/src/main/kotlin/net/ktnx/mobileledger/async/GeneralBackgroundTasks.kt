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

package net.ktnx.mobileledger.async

import net.ktnx.mobileledger.utils.Misc
import java.util.concurrent.Executors

/**
 * Suitable for short tasks, not involving network communication
 */
object GeneralBackgroundTasks {
    private val runner = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())

    @JvmStatic
    fun run(runnable: Runnable) {
        runner.execute(runnable)
    }

    @JvmStatic
    fun run(runnable: Runnable, onSuccess: Runnable) {
        runner.execute {
            runnable.run()
            Misc.onMainThread(onSuccess)
        }
    }

    @JvmStatic
    fun run(
        runnable: Runnable,
        onSuccess: Runnable?,
        onError: ErrorCallback?,
        onDone: Runnable?
    ) {
        runner.execute {
            try {
                runnable.run()
                onSuccess?.let { Misc.onMainThread(it) }
            } catch (e: Exception) {
                if (onError != null) {
                    Misc.onMainThread { onError.error(e) }
                } else {
                    throw e
                }
            } finally {
                onDone?.let { Misc.onMainThread(it) }
            }
        }
    }

    abstract class ErrorCallback {
        abstract fun error(e: Exception)
    }
}
