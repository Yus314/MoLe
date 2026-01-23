/*
 * Copyright © 2019 Damyan Ivanov.
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

package net.ktnx.mobileledger.json

import net.ktnx.mobileledger.json.unified.UnifiedParsedQuantity
import org.junit.Assert.assertEquals
import org.junit.Test

class ParsedQuantityTest {
    @Test
    fun fromString() {
        var pq = UnifiedParsedQuantity.fromString("-22")
        assertEquals(0, pq.decimalPlaces)
        assertEquals(-22L, pq.decimalMantissa)

        pq = UnifiedParsedQuantity.fromString("-123.45")
        assertEquals(2, pq.decimalPlaces)
        assertEquals(-12345L, pq.decimalMantissa)
    }
}
