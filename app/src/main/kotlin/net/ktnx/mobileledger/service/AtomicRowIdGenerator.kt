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

import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

/**
 * Thread-safe implementation of RowIdGenerator using AtomicInteger.
 * Each ViewModel gets its own instance (no @Singleton scope) to ensure
 * independent ID sequences.
 */
class AtomicRowIdGenerator @Inject constructor() : RowIdGenerator {
    private val counter = AtomicInteger(0)

    override fun nextId(): Int = counter.incrementAndGet()

    override fun reset() {
        counter.set(0)
    }
}
