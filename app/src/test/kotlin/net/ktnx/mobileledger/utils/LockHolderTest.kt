/*
 * Copyright © 2026 Damyan Ivanov.
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

import net.ktnx.mobileledger.core.common.utils.LockHolder
import net.ktnx.mobileledger.core.common.utils.Locker
import org.junit.Test

/**
 * Unit tests for [LockHolder].
 *
 * Tests verify:
 * - Lock acquisition and release through Locker
 * - Downgrade functionality
 * - AutoCloseable behavior
 */
class LockHolderTest {

    // ========================================
    // Basic close tests
    // ========================================

    @Test
    fun `close releases read lock`() {
        val locker = Locker()
        val holder = locker.lockForReading()
        holder.close()
        // If close didn't release the lock, a second write lock would block
        val holder2 = locker.lockForWriting()
        holder2.close()
    }

    @Test
    fun `close releases write lock`() {
        val locker = Locker()
        val holder = locker.lockForWriting()
        holder.close()
        // If close didn't release the lock, a second write lock would block
        val holder2 = locker.lockForWriting()
        holder2.close()
    }

    @Test
    fun `close can be called multiple times`() {
        val locker = Locker()
        val holder = locker.lockForReading()
        holder.close()
        // Second close should not throw
        holder.close()
    }

    // ========================================
    // AutoCloseable tests
    // ========================================

    @Test
    fun `implements AutoCloseable`() {
        val locker = Locker()
        val holder = locker.lockForReading()
        // Verify it can be used with use{} block (AutoCloseable)
        holder.use {
            // Lock is held inside use block
        }
        // After use block, lock should be released
        val holder2 = locker.lockForWriting()
        holder2.close()
    }

    @Test
    fun `use block releases lock on normal exit`() {
        val locker = Locker()
        locker.lockForReading().use {
            // Do something with lock
        }
        // Lock should be released, so we can acquire write lock
        val holder = locker.lockForWriting()
        holder.close()
    }

    @Test
    fun `use block releases lock on exception`() {
        val locker = Locker()
        try {
            locker.lockForWriting().use {
                throw RuntimeException("Test exception")
            }
        } catch (_: RuntimeException) {
            // Expected
        }
        // Lock should be released, so we can acquire write lock
        val holder = locker.lockForWriting()
        holder.close()
    }

    // ========================================
    // Downgrade tests
    // ========================================

    @Test
    fun `downgrade releases write lock but keeps read lock`() {
        val locker = Locker()
        val holder = locker.lockForWriting()
        holder.downgrade()
        // After downgrade, another thread should be able to read
        val holder2 = locker.lockForReading()
        holder2.close()
        holder.close()
    }

    @Test
    fun `downgrade on read-only lock does nothing`() {
        val locker = Locker()
        val holder = locker.lockForReading()
        // Should not throw
        holder.downgrade()
        holder.close()
    }

    @Test
    fun `downgrade can be called multiple times`() {
        val locker = Locker()
        val holder = locker.lockForWriting()
        holder.downgrade()
        // Second downgrade should not throw
        holder.downgrade()
        holder.close()
    }

    @Test(expected = IllegalStateException::class)
    fun `downgrade after close throws IllegalStateException`() {
        val locker = Locker()
        val holder = locker.lockForReading()
        holder.close()
        holder.downgrade()
    }

    // ========================================
    // Complex scenarios
    // ========================================

    @Test
    fun `write then downgrade then close sequence`() {
        val locker = Locker()
        val holder = locker.lockForWriting()
        // Perform write operations
        holder.downgrade()
        // Now in read-only mode
        holder.close()
    }

    @Test
    fun `nested lock holders work correctly`() {
        val locker = Locker()
        locker.lockForReading().use { outer ->
            locker.lockForReading().use {
                // Both read locks held
            }
            // Inner lock released
        }
        // Both locks released
        val writeHolder = locker.lockForWriting()
        writeHolder.close()
    }
}
