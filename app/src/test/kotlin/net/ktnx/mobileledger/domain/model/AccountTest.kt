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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountTest {

    @Test
    fun `parentName returns null for top level account`() {
        val account = Account(
            id = 1L,
            name = "Assets"
        )

        assertNull(account.parentName)
    }

    @Test
    fun `parentName returns parent for nested account`() {
        val account = Account(
            id = 1L,
            name = "Assets:Bank"
        )

        assertEquals("Assets", account.parentName)
    }

    @Test
    fun `parentName returns full parent path for deeply nested account`() {
        val account = Account(
            id = 1L,
            name = "Assets:Bank:Checking"
        )

        assertEquals("Assets:Bank", account.parentName)
    }

    @Test
    fun `shortName returns full name for top level account`() {
        val account = Account(
            id = 1L,
            name = "Assets"
        )

        assertEquals("Assets", account.shortName)
    }

    @Test
    fun `shortName returns last segment for nested account`() {
        val account = Account(
            id = 1L,
            name = "Assets:Bank:Checking"
        )

        assertEquals("Checking", account.shortName)
    }

    @Test
    fun `hasAmounts returns false for empty amounts list`() {
        val account = Account(
            id = 1L,
            name = "Assets",
            amounts = emptyList()
        )

        assertFalse(account.hasAmounts)
    }

    @Test
    fun `hasAmounts returns true when amounts exist`() {
        val account = Account(
            id = 1L,
            name = "Assets",
            amounts = listOf(
                AccountAmount(currency = "USD", amount = 100f)
            )
        )

        assertTrue(account.hasAmounts)
    }

    @Test
    fun `hasAmounts returns true with multiple amounts`() {
        val account = Account(
            id = 1L,
            name = "Assets",
            amounts = listOf(
                AccountAmount(currency = "USD", amount = 100f),
                AccountAmount(currency = "EUR", amount = 50f)
            )
        )

        assertTrue(account.hasAmounts)
    }

    @Test
    fun `level defaults to 0`() {
        val account = Account(
            id = 1L,
            name = "Assets"
        )

        assertEquals(0, account.level)
    }

    @Test
    fun `isExpanded defaults to false`() {
        val account = Account(
            id = 1L,
            name = "Assets"
        )

        assertFalse(account.isExpanded)
    }

    @Test
    fun `isVisible defaults to true`() {
        val account = Account(
            id = 1L,
            name = "Assets"
        )

        assertTrue(account.isVisible)
    }

    @Test
    fun `account with null id is considered new`() {
        val account = Account(
            id = null,
            name = "Assets"
        )

        assertNull(account.id)
    }

    @Test
    fun `copy preserves all properties`() {
        val original = Account(
            id = 1L,
            name = "Assets:Bank",
            level = 1,
            isExpanded = true,
            isVisible = true,
            amounts = listOf(AccountAmount(currency = "USD", amount = 100f))
        )

        val copy = original.copy(isExpanded = false)

        assertEquals(original.id, copy.id)
        assertEquals(original.name, copy.name)
        assertEquals(original.level, copy.level)
        assertFalse(copy.isExpanded)
        assertEquals(original.isVisible, copy.isVisible)
        assertEquals(original.amounts, copy.amounts)
    }
}
