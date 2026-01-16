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

package net.ktnx.mobileledger.data.repository.mapper

import net.ktnx.mobileledger.data.repository.mapper.CurrencyMapper.toDomain
import net.ktnx.mobileledger.data.repository.mapper.CurrencyMapper.toEntity
import net.ktnx.mobileledger.db.Currency as DbCurrency
import net.ktnx.mobileledger.domain.model.Currency as DomainCurrency
import net.ktnx.mobileledger.domain.model.CurrencyPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyMapperTest {

    // ========================================
    // toDomain tests
    // ========================================

    @Test
    fun `toDomain maps basic currency correctly`() {
        val entity = DbCurrency(id = 1L, name = "USD", position = "after", hasGap = true)

        val domain = entity.toDomain()

        assertEquals(1L, domain.id)
        assertEquals("USD", domain.name)
        assertEquals(CurrencyPosition.AFTER, domain.position)
        assertTrue(domain.hasGap)
    }

    @Test
    fun `toDomain maps position before`() {
        val entity = DbCurrency(id = 1L, name = "$", position = "before", hasGap = false)

        val domain = entity.toDomain()

        assertEquals(CurrencyPosition.BEFORE, domain.position)
        assertFalse(domain.hasGap)
    }

    @Test
    fun `toDomain with unknown position defaults to after`() {
        val entity = DbCurrency(id = 1L, name = "EUR", position = "unknown", hasGap = true)

        val domain = entity.toDomain()

        assertEquals(CurrencyPosition.AFTER, domain.position)
    }

    @Test
    fun `toDomain handles uppercase position`() {
        val entity = DbCurrency(id = 1L, name = "JPY", position = "BEFORE", hasGap = false)

        val domain = entity.toDomain()

        assertEquals(CurrencyPosition.BEFORE, domain.position)
    }

    // ========================================
    // toEntity tests
    // ========================================

    @Test
    fun `toEntity maps new currency correctly`() {
        val domain = DomainCurrency(
            name = "USD",
            position = CurrencyPosition.AFTER,
            hasGap = true
        )

        val entity = domain.toEntity()

        assertEquals(0L, entity.id)
        assertEquals("USD", entity.name)
        assertEquals("after", entity.position)
        assertTrue(entity.hasGap)
    }

    @Test
    fun `toEntity maps existing currency correctly`() {
        val domain = DomainCurrency(
            id = 123L,
            name = "EUR",
            position = CurrencyPosition.BEFORE,
            hasGap = false
        )

        val entity = domain.toEntity()

        assertEquals(123L, entity.id)
        assertEquals("EUR", entity.name)
        assertEquals("before", entity.position)
        assertFalse(entity.hasGap)
    }

    @Test
    fun `toEntity maps position before`() {
        val domain = DomainCurrency(
            name = "$",
            position = CurrencyPosition.BEFORE,
            hasGap = false
        )

        val entity = domain.toEntity()

        assertEquals("before", entity.position)
    }

    @Test
    fun `toEntity maps position after`() {
        val domain = DomainCurrency(
            name = "円",
            position = CurrencyPosition.AFTER,
            hasGap = true
        )

        val entity = domain.toEntity()

        assertEquals("after", entity.position)
    }

    // ========================================
    // Round-trip tests
    // ========================================

    @Test
    fun `roundTrip preserves data with position after`() {
        val original = DomainCurrency(
            id = 1L,
            name = "USD",
            position = CurrencyPosition.AFTER,
            hasGap = true
        )

        val roundTripped = original.toEntity().toDomain()

        assertEquals(original.id, roundTripped.id)
        assertEquals(original.name, roundTripped.name)
        assertEquals(original.position, roundTripped.position)
        assertEquals(original.hasGap, roundTripped.hasGap)
    }

    @Test
    fun `roundTrip preserves data with position before`() {
        val original = DomainCurrency(
            id = 2L,
            name = "$",
            position = CurrencyPosition.BEFORE,
            hasGap = false
        )

        val roundTripped = original.toEntity().toDomain()

        assertEquals(original.id, roundTripped.id)
        assertEquals(original.name, roundTripped.name)
        assertEquals(original.position, roundTripped.position)
        assertEquals(original.hasGap, roundTripped.hasGap)
    }

    @Test
    fun `roundTrip preserves new currency`() {
        val original = DomainCurrency(
            name = "BTC",
            position = CurrencyPosition.BEFORE,
            hasGap = true
        )

        val roundTripped = original.toEntity().toDomain()

        // New currency gets id=0 after toEntity, which maps to id=0 in domain
        assertEquals(0L, roundTripped.id)
        assertEquals(original.name, roundTripped.name)
        assertEquals(original.position, roundTripped.position)
        assertEquals(original.hasGap, roundTripped.hasGap)
    }
}
