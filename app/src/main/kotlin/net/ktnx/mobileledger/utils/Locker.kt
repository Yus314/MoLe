/*
 * Copyright © 2020 Damyan Ivanov.
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

import java.util.concurrent.locks.ReentrantReadWriteLock

class Locker : AutoCloseable {
    private val lock = ReentrantReadWriteLock()

    fun lockForWriting(): LockHolder {
        val wLock = lock.writeLock()
        wLock.lock()

        val rLock = lock.readLock()
        rLock.lock()

        return LockHolder(rLock, wLock)
    }

    fun lockForReading(): LockHolder {
        val rLock = lock.readLock()
        rLock.lock()
        return LockHolder(rLock)
    }

    override fun close() {
        lock.readLock().unlock()
        lock.writeLock().unlock()
    }
}
