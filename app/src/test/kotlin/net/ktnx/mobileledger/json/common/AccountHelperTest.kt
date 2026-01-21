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

package net.ktnx.mobileledger.json.common

import net.ktnx.mobileledger.domain.model.AmountStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [AccountHelper].
 */
class AccountHelperTest {

    @Test
    fun `createSimpleBalance creates balance with all fields`() {
        // Given
        val commodity = "USD"
        val amount = 100.50f
        val amountStyle = AmountStyle(
            commodityPosition = AmountStyle.Position.BEFORE,
            isCommoditySpaced = true,
            precision = 2,
            decimalMark = "."
        )

        // When
        val balance = AccountHelper.createSimpleBalance(commodity, amount, amountStyle)

        // Then
        assertNotNull(balance)
        assertEquals("USD", balance.commodity)
        assertEquals(100.50f, balance.amount, 0.001f)
        assertEquals(amountStyle, balance.amountStyle)
    }

    @Test
    fun `createSimpleBalance creates balance with null amountStyle`() {
        // Given
        val commodity = "EUR"
        val amount = 250.75f

        // When
        val balance = AccountHelper.createSimpleBalance(commodity, amount, null)

        // Then
        assertNotNull(balance)
        assertEquals("EUR", balance.commodity)
        assertEquals(250.75f, balance.amount, 0.001f)
        assertNull(balance.amountStyle)
    }

    @Test
    fun `createSimpleBalance handles negative amount`() {
        // Given
        val commodity = "JPY"
        val amount = -500f

        // When
        val balance = AccountHelper.createSimpleBalance(commodity, amount, null)

        // Then
        assertEquals(-500f, balance.amount, 0.001f)
    }

    @Test
    fun `createSimpleBalance handles zero amount`() {
        // Given
        val commodity = "GBP"
        val amount = 0f

        // When
        val balance = AccountHelper.createSimpleBalance(commodity, amount, null)

        // Then
        assertEquals(0f, balance.amount, 0.001f)
    }

    @Test
    fun `createSimpleBalance handles empty commodity`() {
        // Given
        val commodity = ""
        val amount = 100f

        // When
        val balance = AccountHelper.createSimpleBalance(commodity, amount, null)

        // Then
        assertEquals("", balance.commodity)
    }

    @Test
    fun `createSimpleBalance with AFTER position style`() {
        // Given
        val commodity = "JPY"
        val amount = 1000f
        val amountStyle = AmountStyle(
            commodityPosition = AmountStyle.Position.AFTER,
            isCommoditySpaced = false,
            precision = 0,
            decimalMark = "."
        )

        // When
        val balance = AccountHelper.createSimpleBalance(commodity, amount, amountStyle)

        // Then
        assertEquals(AmountStyle.Position.AFTER, balance.amountStyle?.commodityPosition)
        assertEquals(0, balance.amountStyle?.precision)
    }
}
