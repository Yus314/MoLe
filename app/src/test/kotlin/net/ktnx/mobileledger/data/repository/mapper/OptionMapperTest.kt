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

import net.ktnx.mobileledger.core.database.entity.Option as DbOption
import net.ktnx.mobileledger.core.domain.model.AppOption
import net.ktnx.mobileledger.data.repository.mapper.OptionMapper.toDomain
import net.ktnx.mobileledger.data.repository.mapper.OptionMapper.toEntity
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [OptionMapper].
 *
 * Tests verify:
 * - Conversion from database entity to domain model
 * - Conversion from domain model to database entity
 * - Round-trip preservation of values
 */
class OptionMapperTest {

    // ========================================
    // toDomain tests
    // ========================================

    @Test
    fun `toDomain converts profileId`() {
        val dbOption = DbOption(profileId = 1L, name = "test", value = "value")
        val domain = dbOption.toDomain()
        assertEquals(1L, domain.profileId)
    }

    @Test
    fun `toDomain converts name`() {
        val dbOption = DbOption(profileId = 1L, name = "optionName", value = "value")
        val domain = dbOption.toDomain()
        assertEquals("optionName", domain.name)
    }

    @Test
    fun `toDomain converts value`() {
        val dbOption = DbOption(profileId = 1L, name = "test", value = "optionValue")
        val domain = dbOption.toDomain()
        assertEquals("optionValue", domain.value)
    }

    @Test
    fun `toDomain handles empty value`() {
        val dbOption = DbOption(profileId = 1L, name = "test", value = "")
        val domain = dbOption.toDomain()
        assertEquals("", domain.value)
    }

    @Test
    fun `toDomain handles special characters in value`() {
        val dbOption = DbOption(profileId = 1L, name = "test", value = "value with\nnewline")
        val domain = dbOption.toDomain()
        assertEquals("value with\nnewline", domain.value)
    }

    // ========================================
    // toEntity tests
    // ========================================

    @Test
    fun `toEntity converts profileId`() {
        val appOption = AppOption(profileId = 2L, name = "test", value = "value")
        val entity = appOption.toEntity()
        assertEquals(2L, entity.profileId)
    }

    @Test
    fun `toEntity converts name`() {
        val appOption = AppOption(profileId = 1L, name = "optionName", value = "value")
        val entity = appOption.toEntity()
        assertEquals("optionName", entity.name)
    }

    @Test
    fun `toEntity converts value`() {
        val appOption = AppOption(profileId = 1L, name = "test", value = "optionValue")
        val entity = appOption.toEntity()
        assertEquals("optionValue", entity.value)
    }

    @Test
    fun `toEntity handles empty value`() {
        val appOption = AppOption(profileId = 1L, name = "test", value = "")
        val entity = appOption.toEntity()
        assertEquals("", entity.value)
    }

    // ========================================
    // Round-trip tests
    // ========================================

    @Test
    fun `round-trip from entity to domain and back preserves values`() {
        val original = DbOption(profileId = 5L, name = "setting", value = "123")
        val roundTripped = original.toDomain().toEntity()
        assertEquals(original.profileId, roundTripped.profileId)
        assertEquals(original.name, roundTripped.name)
        assertEquals(original.value, roundTripped.value)
    }

    @Test
    fun `round-trip from domain to entity and back preserves values`() {
        val original = AppOption(profileId = 10L, name = "preference", value = "data")
        val roundTripped = original.toEntity().toDomain()
        assertEquals(original.profileId, roundTripped.profileId)
        assertEquals(original.name, roundTripped.name)
        assertEquals(original.value, roundTripped.value)
    }

    // ========================================
    // Known option name tests
    // ========================================

    @Test
    fun `handles OPT_LAST_SCRAPE option`() {
        val dbOption = DbOption(
            profileId = 1L,
            name = AppOption.OPT_LAST_SCRAPE,
            value = "1234567890"
        )
        val domain = dbOption.toDomain()
        assertEquals(AppOption.OPT_LAST_SCRAPE, domain.name)
        assertEquals("1234567890", domain.value)
    }
}
