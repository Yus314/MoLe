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

package net.ktnx.mobileledger.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [Option] Room entity.
 */
class OptionEntityTest {

    @Test
    fun `constructor sets all fields`() {
        val option = Option(1L, "test_name", "test_value")

        assertEquals(1L, option.profileId)
        assertEquals("test_name", option.name)
        assertEquals("test_value", option.value)
    }

    @Test
    fun `value can be null`() {
        val option = Option(1L, "test_name", null)

        assertNull(option.value)
    }

    @Test
    fun `fields can be modified`() {
        val option = Option(1L, "name1", "value1")

        option.profileId = 2L
        option.name = "name2"
        option.value = "value2"

        assertEquals(2L, option.profileId)
        assertEquals("name2", option.name)
        assertEquals("value2", option.value)
    }

    @Test
    fun `toString returns name`() {
        val option = Option(1L, "my_option", "some_value")

        assertEquals("my_option", option.toString())
    }

    @Test
    fun `OPT_LAST_SCRAPE constant is correct`() {
        assertEquals("last_scrape", Option.OPT_LAST_SCRAPE)
    }

    @Test
    fun `can create option with OPT_LAST_SCRAPE`() {
        val option = Option(1L, Option.OPT_LAST_SCRAPE, "2026-01-21")

        assertEquals("last_scrape", option.name)
        assertEquals("2026-01-21", option.value)
    }

    @Test
    fun `value can be empty string`() {
        val option = Option(1L, "name", "")

        assertEquals("", option.value)
    }

    @Test
    fun `value can be set to null after creation`() {
        val option = Option(1L, "name", "initial")

        option.value = null

        assertNull(option.value)
    }
}
