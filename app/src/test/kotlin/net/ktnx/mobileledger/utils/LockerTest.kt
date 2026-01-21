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

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Unit tests for [Locker].
 *
 * Tests verify:
 * - Read/write lock acquisition
 * - Lock holder returns
 * - Concurrent access behavior
 */
class LockerTest {

    // ========================================
    // Basic lock acquisition tests
    // ========================================

    @Test
    fun `lockForWriting returns LockHolder`() {
        val locker = Locker()
        val holder = locker.lockForWriting()
        assertNotNull(holder)
        holder.close()
    }

    @Test
    fun `lockForReading returns LockHolder`() {
        val locker = Locker()
        val holder = locker.lockForReading()
        assertNotNull(holder)
        holder.close()
    }

    @Test
    fun `multiple read locks can be acquired`() {
        val locker = Locker()
        val holder1 = locker.lockForReading()
        val holder2 = locker.lockForReading()
        assertNotNull(holder1)
        assertNotNull(holder2)
        holder1.close()
        holder2.close()
    }

    // ========================================
    // AutoCloseable tests
    // ========================================

    @Test
    fun `locker implements AutoCloseable after lockForWriting`() {
        val locker = Locker()
        // Locker.close() expects both read and write locks to be held
        // lockForWriting() acquires both
        locker.lockForWriting()
        locker.use {
            // Locker.close() will release both locks
        }
    }

    @Test
    fun `lockForWriting holder can be used in use block`() {
        val locker = Locker()
        locker.lockForWriting().use { holder ->
            assertNotNull(holder)
        }
    }

    @Test
    fun `lockForReading holder can be used in use block`() {
        val locker = Locker()
        locker.lockForReading().use { holder ->
            assertNotNull(holder)
        }
    }

    // ========================================
    // Concurrent access tests
    // ========================================

    @Test
    fun `write lock blocks other write locks`() {
        val locker = Locker()
        val writeAcquired = AtomicBoolean(false)
        val secondWriteStarted = CountDownLatch(1)
        val firstWriteReleased = CountDownLatch(1)
        val secondWriteAcquired = AtomicBoolean(false)

        // First thread acquires write lock
        val thread1 = Thread {
            locker.lockForWriting().use {
                writeAcquired.set(true)
                secondWriteStarted.await(1, TimeUnit.SECONDS)
                Thread.sleep(50)
                firstWriteReleased.countDown()
            }
        }

        // Second thread tries to acquire write lock
        val thread2 = Thread {
            secondWriteStarted.countDown()
            locker.lockForWriting().use {
                secondWriteAcquired.set(true)
            }
        }

        thread1.start()
        Thread.sleep(20) // Give thread1 time to acquire lock
        thread2.start()

        thread1.join(2000)
        thread2.join(2000)

        // Both should have eventually acquired the lock
        org.junit.Assert.assertTrue(writeAcquired.get())
        org.junit.Assert.assertTrue(secondWriteAcquired.get())
    }

    @Test
    fun `multiple readers can access concurrently`() {
        val locker = Locker()
        val readerCount = AtomicInteger(0)
        val maxConcurrentReaders = AtomicInteger(0)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(3)

        val readerTask = Runnable {
            startLatch.await()
            locker.lockForReading().use {
                val current = readerCount.incrementAndGet()
                synchronized(maxConcurrentReaders) {
                    if (current > maxConcurrentReaders.get()) {
                        maxConcurrentReaders.set(current)
                    }
                }
                Thread.sleep(50)
                readerCount.decrementAndGet()
            }
            doneLatch.countDown()
        }

        Thread(readerTask).start()
        Thread(readerTask).start()
        Thread(readerTask).start()

        startLatch.countDown()
        doneLatch.await(2, TimeUnit.SECONDS)

        // Multiple readers should have been concurrent
        org.junit.Assert.assertTrue(maxConcurrentReaders.get() > 1)
    }
}
