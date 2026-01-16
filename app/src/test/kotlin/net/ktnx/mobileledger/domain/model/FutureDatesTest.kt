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

import org.junit.Assert.assertEquals
import org.junit.Test

class FutureDatesTest {

    @Test
    fun `enum has expected values`() {
        val values = FutureDates.entries

        assertEquals(5, values.size)
        assertEquals(FutureDates.None, values[0])
        assertEquals(FutureDates.OneWeek, values[1])
        assertEquals(FutureDates.TwoWeeks, values[2])
        assertEquals(FutureDates.OneMonth, values[3])
        assertEquals(FutureDates.All, values[4])
    }

    @Test
    fun `fromInt returns correct enum for valid index`() {
        assertEquals(FutureDates.None, FutureDates.fromInt(0))
        assertEquals(FutureDates.OneWeek, FutureDates.fromInt(1))
        assertEquals(FutureDates.TwoWeeks, FutureDates.fromInt(2))
        assertEquals(FutureDates.OneMonth, FutureDates.fromInt(3))
        assertEquals(FutureDates.All, FutureDates.fromInt(4))
    }

    @Test
    fun `fromInt returns None for invalid negative index`() {
        assertEquals(FutureDates.None, FutureDates.fromInt(-1))
    }

    @Test
    fun `fromInt returns None for invalid large index`() {
        assertEquals(FutureDates.None, FutureDates.fromInt(999))
    }

    @Test
    fun `toInt returns correct ordinal`() {
        assertEquals(0, FutureDates.None.toInt())
        assertEquals(1, FutureDates.OneWeek.toInt())
        assertEquals(2, FutureDates.TwoWeeks.toInt())
        assertEquals(3, FutureDates.OneMonth.toInt())
        assertEquals(4, FutureDates.All.toInt())
    }

    @Test
    fun `roundtrip fromInt and toInt preserves value`() {
        for (original in FutureDates.entries) {
            val intValue = original.toInt()
            val restored = FutureDates.fromInt(intValue)
            assertEquals(original, restored)
        }
    }
}
