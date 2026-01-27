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
import org.junit.Test

class FutureDatesTest {

    @Test
    fun `enum has expected values`() {
        val values = FutureDates.entries

        assertEquals(9, values.size)
        assertEquals(FutureDates.None, values[0])
        assertEquals(FutureDates.OneWeek, values[1])
        assertEquals(FutureDates.TwoWeeks, values[2])
        assertEquals(FutureDates.OneMonth, values[3])
        assertEquals(FutureDates.TwoMonths, values[4])
        assertEquals(FutureDates.ThreeMonths, values[5])
        assertEquals(FutureDates.SixMonths, values[6])
        assertEquals(FutureDates.OneYear, values[7])
        assertEquals(FutureDates.All, values[8])
    }

    @Test
    fun `fromInt returns correct enum for valid value`() {
        assertEquals(FutureDates.None, FutureDates.fromInt(0))
        assertEquals(FutureDates.OneWeek, FutureDates.fromInt(7))
        assertEquals(FutureDates.TwoWeeks, FutureDates.fromInt(14))
        assertEquals(FutureDates.OneMonth, FutureDates.fromInt(30))
        assertEquals(FutureDates.TwoMonths, FutureDates.fromInt(60))
        assertEquals(FutureDates.ThreeMonths, FutureDates.fromInt(90))
        assertEquals(FutureDates.SixMonths, FutureDates.fromInt(180))
        assertEquals(FutureDates.OneYear, FutureDates.fromInt(365))
        assertEquals(FutureDates.All, FutureDates.fromInt(-1))
    }

    @Test
    fun `fromInt returns None for invalid value`() {
        assertEquals(FutureDates.None, FutureDates.fromInt(-2))
        assertEquals(FutureDates.None, FutureDates.fromInt(999))
        assertEquals(FutureDates.None, FutureDates.fromInt(1))
        assertEquals(FutureDates.None, FutureDates.fromInt(100))
    }

    @Test
    fun `toInt returns correct value`() {
        assertEquals(0, FutureDates.None.toInt())
        assertEquals(7, FutureDates.OneWeek.toInt())
        assertEquals(14, FutureDates.TwoWeeks.toInt())
        assertEquals(30, FutureDates.OneMonth.toInt())
        assertEquals(60, FutureDates.TwoMonths.toInt())
        assertEquals(90, FutureDates.ThreeMonths.toInt())
        assertEquals(180, FutureDates.SixMonths.toInt())
        assertEquals(365, FutureDates.OneYear.toInt())
        assertEquals(-1, FutureDates.All.toInt())
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
