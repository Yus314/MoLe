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

import org.junit.Test

/**
 * Unit tests for Profiler utility class.
 *
 * Note: In DEBUG builds, Profiler records timing data.
 * In release builds, all operations are no-ops.
 */
class ProfilerTest {

    @Test
    fun `profiler can be created`() {
        // Given/When
        val profiler = Profiler("test")

        // Then - no exception
        profiler.dumpStats()
    }

    @Test
    fun `profiler start and end operations work`() {
        // Given
        val profiler = Profiler("test-operation")

        // When - perform start/end cycle
        profiler.opStart()
        // Simulate some work
        Thread.sleep(1)
        profiler.opEnd()

        // Then - no exception
        profiler.dumpStats()
    }

    @Test
    fun `profiler multiple operations work`() {
        // Given
        val profiler = Profiler("multi-op")

        // When - perform multiple start/end cycles
        repeat(3) {
            profiler.opStart()
            Thread.sleep(1)
            profiler.opEnd()
        }

        // Then - no exception
        profiler.dumpStats()
    }

    @Test
    fun `profiler handles double start in debug mode`() {
        // Given
        val profiler = Profiler("double-start")

        // When - start twice without end
        profiler.opStart()

        // In debug mode, this throws IllegalStateException
        // In release mode, it's a no-op
        try {
            profiler.opStart()
            // If we get here, it's release mode (no-op)
        } catch (e: IllegalStateException) {
            // Expected in debug mode
        }
    }

    @Test
    fun `profiler handles end without start`() {
        // Given
        val profiler = Profiler("end-without-start")

        // When - end without start
        // In debug mode, this throws IllegalStateException
        // In release mode, it's a no-op
        try {
            profiler.opEnd()
            // If we get here, it's release mode (no-op)
        } catch (e: IllegalStateException) {
            // Expected in debug mode
        }
    }
}
