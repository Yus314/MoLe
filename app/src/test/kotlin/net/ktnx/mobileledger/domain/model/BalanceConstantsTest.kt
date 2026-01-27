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

package net.ktnx.mobileledger.domain.model

import net.ktnx.mobileledger.core.common.utils.SimpleDate
import net.ktnx.mobileledger.core.domain.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [BalanceConstants].
 */
class BalanceConstantsTest {

    @Test
    fun `BALANCE_EPSILON has expected value`() {
        assertEquals(0.005f, BalanceConstants.BALANCE_EPSILON)
    }

    @Test
    fun `BALANCE_EPSILON is positive`() {
        assertTrue(BalanceConstants.BALANCE_EPSILON > 0)
    }

    @Test
    fun `BALANCE_EPSILON is small enough for currency precision`() {
        assertTrue(BalanceConstants.BALANCE_EPSILON < 0.01f)
    }
}
