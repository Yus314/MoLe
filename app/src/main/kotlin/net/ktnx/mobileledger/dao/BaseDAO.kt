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

package net.ktnx.mobileledger.dao

import java.util.concurrent.Executors
import net.ktnx.mobileledger.utils.Misc

abstract class BaseDAO<T> {
    abstract fun insertSync(item: T): Long

    fun insert(item: T) {
        asyncRunner.execute { insertSync(item) }
    }

    fun insert(item: T, receiver: OnInsertedReceiver) {
        asyncRunner.execute {
            val id = insertSync(item)
            Misc.onMainThread { receiver.onInsert(id) }
        }
    }

    abstract fun updateSync(item: T)

    fun update(item: T) {
        asyncRunner.execute { updateSync(item) }
    }

    fun update(item: T, onDone: Runnable) {
        asyncRunner.execute {
            updateSync(item)
            Misc.onMainThread(onDone)
        }
    }

    abstract fun deleteSync(item: T)

    fun delete(item: T) {
        asyncRunner.execute { deleteSync(item) }
    }

    fun delete(item: T, onDone: Runnable) {
        asyncRunner.execute {
            deleteSync(item)
            Misc.onMainThread(onDone)
        }
    }

    fun interface OnInsertedReceiver {
        fun onInsert(id: Long)
    }

    companion object {
        @JvmStatic
        private val asyncRunner = Executors.newSingleThreadExecutor()

        @JvmStatic
        fun runAsync(runnable: Runnable) {
            asyncRunner.execute(runnable)
        }
    }
}
