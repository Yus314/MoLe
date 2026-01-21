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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [Account] Room entity.
 */
class AccountEntityTest {

    @Test
    fun `default values are correct`() {
        val account = Account()

        assertEquals(0L, account.id)
        assertEquals(0L, account.profileId)
        assertEquals(0, account.level)
        assertEquals("", account.name)
        assertEquals("", account.nameUpper)
        assertNull(account.parentName)
        assertTrue(account.expanded)
        assertFalse(account.amountsExpanded)
        assertEquals(0L, account.generation)
    }

    @Test
    fun `all fields can be set and read`() {
        val account = Account()

        account.id = 10L
        account.profileId = 5L
        account.level = 2
        account.name = "Assets:Bank:Checking"
        account.nameUpper = "ASSETS:BANK:CHECKING"
        account.parentName = "Assets:Bank"
        account.expanded = false
        account.amountsExpanded = true
        account.generation = 15L

        assertEquals(10L, account.id)
        assertEquals(5L, account.profileId)
        assertEquals(2, account.level)
        assertEquals("Assets:Bank:Checking", account.name)
        assertEquals("ASSETS:BANK:CHECKING", account.nameUpper)
        assertEquals("Assets:Bank", account.parentName)
        assertFalse(account.expanded)
        assertTrue(account.amountsExpanded)
        assertEquals(15L, account.generation)
    }

    @Test
    fun `isExpanded returns expanded value`() {
        val account = Account()

        assertTrue(account.isExpanded())

        account.expanded = false
        assertFalse(account.isExpanded())
    }

    @Test
    fun `isAmountsExpanded returns amountsExpanded value`() {
        val account = Account()

        assertFalse(account.isAmountsExpanded())

        account.amountsExpanded = true
        assertTrue(account.isAmountsExpanded())
    }

    @Test
    fun `toString returns name`() {
        val account = Account()
        account.name = "Assets:Cash"

        assertEquals("Assets:Cash", account.toString())
    }

    @Test
    fun `toString returns empty string when name is empty`() {
        val account = Account()

        assertEquals("", account.toString())
    }

    @Test
    fun `level can be various values`() {
        val account = Account()

        account.level = 0
        assertEquals(0, account.level)

        account.level = 1
        assertEquals(1, account.level)

        account.level = 5
        assertEquals(5, account.level)
    }

    @Test
    fun `parentName can be null`() {
        val account = Account()
        account.parentName = "Parent"

        account.parentName = null

        assertNull(account.parentName)
    }

    @Test
    fun `name can contain hierarchical path`() {
        val account = Account()

        account.name = "Expenses:Food:Groceries:Supermarket"

        assertEquals("Expenses:Food:Groceries:Supermarket", account.name)
    }

    @Test
    fun `nameUpper stores uppercase version`() {
        val account = Account()

        account.nameUpper = "EXPENSES:FOOD"

        assertEquals("EXPENSES:FOOD", account.nameUpper)
    }
}
