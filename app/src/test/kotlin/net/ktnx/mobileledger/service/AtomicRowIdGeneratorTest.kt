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

package net.ktnx.mobileledger.service

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [AtomicRowIdGenerator].
 *
 * Tests verify:
 * - Sequential ID generation starts at 1
 * - Reset functionality
 * - Thread safety
 */
class AtomicRowIdGeneratorTest {

    private lateinit var generator: AtomicRowIdGenerator

    @Before
    fun setup() {
        generator = AtomicRowIdGenerator()
    }

    @Test
    fun `nextId returns 1 on first call`() {
        // When
        val id = generator.nextId()

        // Then
        assertEquals(1, id)
    }

    @Test
    fun `nextId increments sequentially`() {
        // When
        val id1 = generator.nextId()
        val id2 = generator.nextId()
        val id3 = generator.nextId()

        // Then
        assertEquals(1, id1)
        assertEquals(2, id2)
        assertEquals(3, id3)
    }

    @Test
    fun `reset sets counter to 0`() {
        // Given
        generator.nextId()
        generator.nextId()
        generator.nextId()

        // When
        generator.reset()
        val idAfterReset = generator.nextId()

        // Then
        assertEquals(1, idAfterReset)
    }

    @Test
    fun `reset can be called multiple times`() {
        // Given/When
        generator.nextId()
        generator.reset()
        generator.nextId()
        generator.nextId()
        generator.reset()
        val id = generator.nextId()

        // Then
        assertEquals(1, id)
    }

    @Test
    fun `generates unique IDs under concurrent access`() {
        // Given
        val threadCount = 10
        val idsPerThread = 100
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val allIds = mutableListOf<Int>()
        val lock = Any()

        // When
        repeat(threadCount) {
            executor.submit {
                try {
                    repeat(idsPerThread) {
                        val id = generator.nextId()
                        synchronized(lock) {
                            allIds.add(id)
                        }
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await(5, TimeUnit.SECONDS)
        executor.shutdown()

        // Then
        assertEquals(threadCount * idsPerThread, allIds.size)
        val uniqueIds = allIds.toSet()
        assertEquals(allIds.size, uniqueIds.size) // All IDs are unique
    }

    @Test
    fun `IDs are always positive`() {
        // When
        repeat(100) {
            val id = generator.nextId()

            // Then
            assertTrue(id > 0)
        }
    }

    @Test
    fun `separate instances have independent counters`() {
        // Given
        val generator1 = AtomicRowIdGenerator()
        val generator2 = AtomicRowIdGenerator()

        // When
        generator1.nextId() // 1
        generator1.nextId() // 2
        generator1.nextId() // 3
        val id2_1 = generator2.nextId()

        // Then
        assertEquals(1, id2_1) // Generator2 starts at 1
    }
}
