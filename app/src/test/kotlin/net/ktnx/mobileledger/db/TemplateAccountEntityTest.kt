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

import net.ktnx.mobileledger.core.database.entity.TemplateAccount
import net.ktnx.mobileledger.core.database.entity.TemplateHeader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [TemplateAccount] Room entity.
 */
class TemplateAccountEntityTest {

    @Test
    fun `primary constructor sets id, templateId, and position`() {
        val account = TemplateAccount(1L, 10L, 5L)

        assertEquals(1L, account.id)
        assertEquals(10L, account.templateId)
        assertEquals(5L, account.position)
    }

    @Test
    fun `primary constructor leaves optional fields as null`() {
        val account = TemplateAccount(1L, 10L, 0L)

        assertNull(account.accountName)
        assertNull(account.accountNameMatchGroup)
        assertNull(account.currency)
        assertNull(account.currencyMatchGroup)
        assertNull(account.amount)
        assertNull(account.amountMatchGroup)
        assertNull(account.accountComment)
        assertNull(account.accountCommentMatchGroup)
        assertNull(account.negateAmount)
    }

    @Test
    fun `copy constructor copies all fields`() {
        val original = TemplateAccount(1L, 10L, 5L)
        original.accountName = "Expenses:Food"
        original.accountNameMatchGroup = 1
        original.currency = 42L
        original.currencyMatchGroup = 2
        original.amount = 100.50f
        original.amountMatchGroup = 3
        original.accountComment = "comment"
        original.accountCommentMatchGroup = 4
        original.negateAmount = true

        val copy = TemplateAccount(original)

        assertEquals(original.id, copy.id)
        assertEquals(original.templateId, copy.templateId)
        assertEquals(original.position, copy.position)
        assertEquals(original.accountName, copy.accountName)
        assertEquals(original.accountNameMatchGroup, copy.accountNameMatchGroup)
        assertEquals(original.currency, copy.currency)
        assertEquals(original.currencyMatchGroup, copy.currencyMatchGroup)
        assertEquals(original.amount, copy.amount)
        assertEquals(original.amountMatchGroup, copy.amountMatchGroup)
        assertEquals(original.accountComment, copy.accountComment)
        assertEquals(original.accountCommentMatchGroup, copy.accountCommentMatchGroup)
        assertEquals(original.negateAmount, copy.negateAmount)
    }

    @Test
    fun `setPosition converts int to long`() {
        val account = TemplateAccount(1L, 10L, 0L)

        account.setPosition(42)

        assertEquals(42L, account.position)
    }

    @Test
    fun `setPosition with zero`() {
        val account = TemplateAccount(1L, 10L, 5L)

        account.setPosition(0)

        assertEquals(0L, account.position)
    }

    @Test
    fun `setPosition with negative value`() {
        val account = TemplateAccount(1L, 10L, 0L)

        account.setPosition(-1)

        assertEquals(-1L, account.position)
    }

    @Test
    fun `createDuplicate creates copy with id 0 and new templateId`() {
        val original = TemplateAccount(42L, 10L, 5L)
        original.accountName = "Assets:Cash"
        original.amount = 100.0f

        val header = TemplateHeader(99L, "New Template", "pattern")

        val duplicate = original.createDuplicate(header)

        assertEquals(0L, duplicate.id)
        assertEquals(99L, duplicate.templateId)
        assertEquals(original.position, duplicate.position)
        assertEquals(original.accountName, duplicate.accountName)
        assertEquals(original.amount, duplicate.amount)
    }

    @Test
    fun `createDuplicate preserves all fields except id and templateId`() {
        val original = TemplateAccount(100L, 50L, 3L)
        original.accountName = "Expenses:Groceries"
        original.accountNameMatchGroup = 1
        original.currency = 1L
        original.currencyMatchGroup = 2
        original.amount = 50.25f
        original.amountMatchGroup = 3
        original.accountComment = "weekly shopping"
        original.accountCommentMatchGroup = 4
        original.negateAmount = true

        val header = TemplateHeader(200L, "Duplicate Template", ".*")

        val duplicate = original.createDuplicate(header)

        assertEquals(0L, duplicate.id)
        assertEquals(200L, duplicate.templateId)
        assertEquals(3L, duplicate.position)
        assertEquals("Expenses:Groceries", duplicate.accountName)
        assertEquals(1, duplicate.accountNameMatchGroup)
        assertEquals(1L, duplicate.currency)
        assertEquals(2, duplicate.currencyMatchGroup)
        assertEquals(50.25f, duplicate.amount)
        assertEquals(3, duplicate.amountMatchGroup)
        assertEquals("weekly shopping", duplicate.accountComment)
        assertEquals(4, duplicate.accountCommentMatchGroup)
        assertTrue(duplicate.negateAmount!!)
    }

    @Test
    fun `all optional fields can be set and read`() {
        val account = TemplateAccount(1L, 10L, 0L)

        account.accountName = "Assets:Bank"
        account.accountNameMatchGroup = 1
        account.currency = 5L
        account.currencyMatchGroup = 2
        account.amount = 999.99f
        account.amountMatchGroup = 3
        account.accountComment = "test comment"
        account.accountCommentMatchGroup = 4
        account.negateAmount = false

        assertEquals("Assets:Bank", account.accountName)
        assertEquals(1, account.accountNameMatchGroup)
        assertEquals(5L, account.currency)
        assertEquals(2, account.currencyMatchGroup)
        assertEquals(999.99f, account.amount)
        assertEquals(3, account.amountMatchGroup)
        assertEquals("test comment", account.accountComment)
        assertEquals(4, account.accountCommentMatchGroup)
        assertFalse(account.negateAmount!!)
    }

    @Test
    fun `negateAmount can be null, true, or false`() {
        val account = TemplateAccount(1L, 10L, 0L)

        assertNull(account.negateAmount)

        account.negateAmount = true
        assertTrue(account.negateAmount!!)

        account.negateAmount = false
        assertFalse(account.negateAmount!!)

        account.negateAmount = null
        assertNull(account.negateAmount)
    }
}
