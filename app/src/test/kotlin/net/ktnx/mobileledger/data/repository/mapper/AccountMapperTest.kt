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

import net.ktnx.mobileledger.db.Account as DbAccount
import net.ktnx.mobileledger.db.AccountValue
import net.ktnx.mobileledger.db.AccountWithAmounts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountMapperTest {

    @Test
    fun `toDomain maps basic account correctly`() {
        val dbAccount = DbAccount().apply {
            id = 1L
            profileId = 100L
            name = "Assets"
            nameUpper = "ASSETS"
            parentName = null
            level = 0
            expanded = true
            amountsExpanded = false
        }
        val dbAccountWithAmounts = AccountWithAmounts().apply {
            account = dbAccount
            amounts = emptyList()
        }

        val result = with(AccountMapper) { dbAccountWithAmounts.toDomain() }

        assertEquals(1L, result.id)
        assertEquals("Assets", result.name)
        assertEquals(0, result.level)
        assertTrue(result.isExpanded)
        assertTrue(result.amounts.isEmpty())
    }

    @Test
    fun `toDomain maps account with amounts correctly`() {
        val dbAccount = DbAccount().apply {
            id = 1L
            profileId = 100L
            name = "Assets:Bank"
            nameUpper = "ASSETS:BANK"
            parentName = "Assets"
            level = 1
            expanded = false
        }
        val amount1 = AccountValue().apply {
            id = 10L
            accountId = 1L
            currency = "USD"
            value = 100.50f
        }
        val amount2 = AccountValue().apply {
            id = 11L
            accountId = 1L
            currency = "EUR"
            value = 50.25f
        }
        val dbAccountWithAmounts = AccountWithAmounts().apply {
            account = dbAccount
            amounts = listOf(amount1, amount2)
        }

        val result = with(AccountMapper) { dbAccountWithAmounts.toDomain() }

        assertEquals(1L, result.id)
        assertEquals("Assets:Bank", result.name)
        assertEquals(1, result.level)
        assertFalse(result.isExpanded)
        assertEquals(2, result.amounts.size)
        assertEquals("USD", result.amounts[0].currency)
        assertEquals(100.50f, result.amounts[0].amount)
        assertEquals("EUR", result.amounts[1].currency)
        assertEquals(50.25f, result.amounts[1].amount)
    }

    @Test
    fun `toDomain maps hierarchy level correctly`() {
        val dbAccount = DbAccount().apply {
            id = 1L
            profileId = 100L
            name = "Assets:Bank:Checking"
            nameUpper = "ASSETS:BANK:CHECKING"
            parentName = "Assets:Bank"
            level = 2
            expanded = true
        }
        val dbAccountWithAmounts = AccountWithAmounts().apply {
            account = dbAccount
            amounts = emptyList()
        }

        val result = with(AccountMapper) { dbAccountWithAmounts.toDomain() }

        assertEquals(2, result.level)
        assertEquals("Assets:Bank", result.parentName)
        assertEquals("Checking", result.shortName)
    }

    @Test
    fun `toDomain maps expanded state correctly`() {
        val expandedAccount = DbAccount().apply {
            id = 1L
            profileId = 100L
            name = "Assets"
            nameUpper = "ASSETS"
            level = 0
            expanded = true
            amountsExpanded = true
        }
        val collapsedAccount = DbAccount().apply {
            id = 2L
            profileId = 100L
            name = "Liabilities"
            nameUpper = "LIABILITIES"
            level = 0
            expanded = false
            amountsExpanded = false
        }
        val expandedWithAmounts = AccountWithAmounts().apply {
            account = expandedAccount
            amounts = emptyList()
        }
        val collapsedWithAmounts = AccountWithAmounts().apply {
            account = collapsedAccount
            amounts = emptyList()
        }

        val expandedResult = with(AccountMapper) { expandedWithAmounts.toDomain() }
        val collapsedResult = with(AccountMapper) { collapsedWithAmounts.toDomain() }

        assertTrue(expandedResult.isExpanded)
        assertFalse(collapsedResult.isExpanded)
    }

    @Test
    fun `toDomain handles empty currency as empty string`() {
        val dbAccount = DbAccount().apply {
            id = 1L
            profileId = 100L
            name = "Assets"
            nameUpper = "ASSETS"
            level = 0
            expanded = true
        }
        val amount = AccountValue().apply {
            id = 10L
            accountId = 1L
            currency = ""
            value = 100f
        }
        val dbAccountWithAmounts = AccountWithAmounts().apply {
            account = dbAccount
            amounts = listOf(amount)
        }

        val result = with(AccountMapper) { dbAccountWithAmounts.toDomain() }

        assertEquals(1, result.amounts.size)
        assertEquals("", result.amounts[0].currency)
    }

    @Test
    fun `toEntity maps domain account correctly`() {
        val domainAccount = net.ktnx.mobileledger.domain.model.Account(
            id = 1L,
            name = "Assets:Bank",
            level = 1,
            isExpanded = true,
            isVisible = true,
            amounts = listOf(
                net.ktnx.mobileledger.domain.model.AccountAmount(
                    currency = "USD",
                    amount = 100f
                )
            )
        )
        val profileId = 100L

        val result = with(AccountMapper) { domainAccount.toEntity(profileId) }

        assertEquals(1L, result.account.id)
        assertEquals("Assets:Bank", result.account.name)
        assertEquals("ASSETS:BANK", result.account.nameUpper)
        assertEquals("Assets", result.account.parentName)
        assertEquals(1, result.account.level)
        assertEquals(100L, result.account.profileId)
        assertTrue(result.account.expanded)
        assertEquals(1, result.amounts.size)
        assertEquals("USD", result.amounts[0].currency)
        assertEquals(100f, result.amounts[0].value)
    }

    @Test
    fun `toEntity handles new account with null id`() {
        val domainAccount = net.ktnx.mobileledger.domain.model.Account(
            id = null,
            name = "Assets",
            level = 0
        )
        val profileId = 100L

        val result = with(AccountMapper) { domainAccount.toEntity(profileId) }

        assertEquals(0L, result.account.id)
        assertEquals("Assets", result.account.name)
    }

    @Test
    fun `roundTrip preserves data`() {
        val original = net.ktnx.mobileledger.domain.model.Account(
            id = 1L,
            name = "Assets:Bank:Checking",
            level = 2,
            isExpanded = true,
            isVisible = true,
            amounts = listOf(
                net.ktnx.mobileledger.domain.model.AccountAmount(
                    currency = "USD",
                    amount = 100f
                ),
                net.ktnx.mobileledger.domain.model.AccountAmount(
                    currency = "EUR",
                    amount = 50f
                )
            )
        )
        val profileId = 100L

        val entity = with(AccountMapper) { original.toEntity(profileId) }
        val result = with(AccountMapper) { entity.toDomain() }

        assertEquals(original.id, result.id)
        assertEquals(original.name, result.name)
        assertEquals(original.level, result.level)
        assertEquals(original.isExpanded, result.isExpanded)
        assertEquals(original.amounts.size, result.amounts.size)
        assertEquals(original.amounts[0].currency, result.amounts[0].currency)
        assertEquals(original.amounts[0].amount, result.amounts[0].amount)
        assertEquals(original.amounts[1].currency, result.amounts[1].currency)
        assertEquals(original.amounts[1].amount, result.amounts[1].amount)
    }
}
