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
 * Unit tests for [Transaction] Room entity.
 */
class TransactionEntityTest {

    @Test
    fun `default values are correct`() {
        val transaction = Transaction()

        assertEquals(0L, transaction.id)
        assertEquals(0L, transaction.ledgerId)
        assertEquals(0L, transaction.profileId)
        assertEquals("", transaction.dataHash)
        assertEquals(0, transaction.year)
        assertEquals(0, transaction.month)
        assertEquals(0, transaction.day)
        assertEquals("", transaction.description)
        assertEquals("", transaction.descriptionUpper)
        assertNull(transaction.comment)
        assertEquals(0L, transaction.generation)
    }

    @Test
    fun `description setter updates descriptionUpper`() {
        val transaction = Transaction()

        transaction.description = "Test Description"

        assertEquals("Test Description", transaction.description)
        assertEquals("TEST DESCRIPTION", transaction.descriptionUpper)
    }

    @Test
    fun `description setter with lowercase`() {
        val transaction = Transaction()

        transaction.description = "expense at grocery store"

        assertEquals("expense at grocery store", transaction.description)
        assertEquals("EXPENSE AT GROCERY STORE", transaction.descriptionUpper)
    }

    @Test
    fun `description setter with mixed case`() {
        val transaction = Transaction()

        transaction.description = "PayPal Payment"

        assertEquals("PayPal Payment", transaction.description)
        assertEquals("PAYPAL PAYMENT", transaction.descriptionUpper)
    }

    @Test
    fun `description setter with empty string`() {
        val transaction = Transaction()
        transaction.description = "Initial"

        transaction.description = ""

        assertEquals("", transaction.description)
        assertEquals("", transaction.descriptionUpper)
    }

    @Test
    fun `description setter with unicode`() {
        val transaction = Transaction()

        transaction.description = "日本語テスト"

        assertEquals("日本語テスト", transaction.description)
        assertEquals("日本語テスト", transaction.descriptionUpper)
    }

    @Test
    fun `copyDataFrom copies all fields except id`() {
        val source = Transaction().apply {
            id = 999L
            ledgerId = 42L
            profileId = 5L
            dataHash = "abc123"
            year = 2026
            month = 1
            day = 21
            description = "Original Description"
            comment = "Test comment"
            generation = 10L
        }

        val target = Transaction()
        target.id = 1L

        target.copyDataFrom(source)

        // id should NOT be copied
        assertEquals(1L, target.id)

        // all other fields should be copied
        assertEquals(42L, target.ledgerId)
        assertEquals(5L, target.profileId)
        assertEquals("abc123", target.dataHash)
        assertEquals(2026, target.year)
        assertEquals(1, target.month)
        assertEquals(21, target.day)
        assertEquals("Original Description", target.description)
        assertEquals("ORIGINAL DESCRIPTION", target.descriptionUpper)
        assertEquals("Test comment", target.comment)
        assertEquals(10L, target.generation)
    }

    @Test
    fun `copyDataFrom updates descriptionUpper correctly`() {
        val source = Transaction().apply {
            description = "lowercase description"
        }

        val target = Transaction()
        target.copyDataFrom(source)

        assertEquals("lowercase description", target.description)
        assertEquals("LOWERCASE DESCRIPTION", target.descriptionUpper)
    }

    @Test
    fun `copyDataFrom handles null comment`() {
        val source = Transaction().apply {
            comment = null
        }

        val target = Transaction()
        target.comment = "existing comment"
        target.copyDataFrom(source)

        assertNull(target.comment)
    }

    @Test
    fun `all fields can be set and read`() {
        val transaction = Transaction()

        transaction.id = 100L
        transaction.ledgerId = 200L
        transaction.profileId = 300L
        transaction.dataHash = "hash123"
        transaction.year = 2025
        transaction.month = 12
        transaction.day = 31
        transaction.description = "Year End Transaction"
        transaction.comment = "Final transaction"
        transaction.generation = 50L

        assertEquals(100L, transaction.id)
        assertEquals(200L, transaction.ledgerId)
        assertEquals(300L, transaction.profileId)
        assertEquals("hash123", transaction.dataHash)
        assertEquals(2025, transaction.year)
        assertEquals(12, transaction.month)
        assertEquals(31, transaction.day)
        assertEquals("Year End Transaction", transaction.description)
        assertEquals("YEAR END TRANSACTION", transaction.descriptionUpper)
        assertEquals("Final transaction", transaction.comment)
        assertEquals(50L, transaction.generation)
    }

    @Test
    fun `descriptionUpper can be set directly`() {
        val transaction = Transaction()

        transaction.descriptionUpper = "CUSTOM UPPER"

        assertEquals("CUSTOM UPPER", transaction.descriptionUpper)
    }

    @Test
    fun `description setter overwrites previous descriptionUpper`() {
        val transaction = Transaction()
        transaction.descriptionUpper = "OLD UPPER"

        transaction.description = "new description"

        assertEquals("new description", transaction.description)
        assertEquals("NEW DESCRIPTION", transaction.descriptionUpper)
    }
}
