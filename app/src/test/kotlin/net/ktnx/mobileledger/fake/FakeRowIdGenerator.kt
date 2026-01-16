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

package net.ktnx.mobileledger.fake

import net.ktnx.mobileledger.service.RowIdGenerator

/**
 * Fake implementation for testing.
 * Provides deterministic, controllable ID generation.
 */
class FakeRowIdGenerator : RowIdGenerator {
    private var counter = 0

    /** Track all generated IDs for assertions */
    val generatedIds = mutableListOf<Int>()

    /** Count of nextId() calls */
    val nextIdCallCount: Int get() = generatedIds.size

    /** Count of reset() calls */
    var resetCallCount = 0
        private set

    override fun nextId(): Int {
        val id = ++counter
        generatedIds.add(id)
        return id
    }

    override fun reset() {
        counter = 0
        resetCallCount++
    }

    /** Reset fake state for test isolation */
    fun resetFakeState() {
        counter = 0
        generatedIds.clear()
        resetCallCount = 0
    }
}
