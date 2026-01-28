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

package net.ktnx.mobileledger.domain.usecase

import net.ktnx.mobileledger.core.domain.model.Account
import net.ktnx.mobileledger.core.domain.model.AccountAmount
import net.ktnx.mobileledger.feature.account.usecase.AccountHierarchyResolver
import net.ktnx.mobileledger.feature.account.usecase.AccountHierarchyResolverImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [AccountHierarchyResolverImpl].
 */
class AccountHierarchyResolverImplTest {

    private lateinit var resolver: AccountHierarchyResolverImpl

    @Before
    fun setup() {
        resolver = AccountHierarchyResolverImpl()
    }

    private fun account(id: Long = 1L, name: String, level: Int = 0, amounts: List<AccountAmount> = emptyList()) =
        Account(
            id = id,
            name = name,
            level = level,
            amounts = amounts
        )

    private fun amount(amount: Float, currency: String = "USD") = AccountAmount(currency = currency, amount = amount)

    // ========== resolve tests ==========

    @Test
    fun `resolve returns empty list for empty input`() {
        val result = resolver.resolve(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `resolve single account has no sub-accounts`() {
        val accounts = listOf(account(name = "Assets"))
        val result = resolver.resolve(accounts)

        assertEquals(1, result.size)
        assertEquals("Assets", result[0].account.name)
        assertFalse(result[0].hasSubAccounts)
    }

    @Test
    fun `resolve parent account has sub-accounts`() {
        val accounts = listOf(
            account(id = 1, name = "Assets", level = 0),
            account(id = 2, name = "Assets:Bank", level = 1),
            account(id = 3, name = "Assets:Cash", level = 1)
        )

        val result = resolver.resolve(accounts)

        assertEquals(3, result.size)

        // Assets should have sub-accounts
        val assets = result.find { it.account.name == "Assets" }
        assertTrue(assets!!.hasSubAccounts)

        // Assets:Bank should NOT have sub-accounts
        val bank = result.find { it.account.name == "Assets:Bank" }
        assertFalse(bank!!.hasSubAccounts)

        // Assets:Cash should NOT have sub-accounts
        val cash = result.find { it.account.name == "Assets:Cash" }
        assertFalse(cash!!.hasSubAccounts)
    }

    @Test
    fun `resolve nested hierarchy correctly identifies parents`() {
        val accounts = listOf(
            account(id = 1, name = "Assets", level = 0),
            account(id = 2, name = "Assets:Bank", level = 1),
            account(id = 3, name = "Assets:Bank:Checking", level = 2),
            account(id = 4, name = "Assets:Bank:Savings", level = 2)
        )

        val result = resolver.resolve(accounts)

        // Assets has sub-accounts
        assertTrue(result.find { it.account.name == "Assets" }!!.hasSubAccounts)

        // Assets:Bank has sub-accounts
        assertTrue(result.find { it.account.name == "Assets:Bank" }!!.hasSubAccounts)

        // Leaf accounts don't have sub-accounts
        assertFalse(result.find { it.account.name == "Assets:Bank:Checking" }!!.hasSubAccounts)
        assertFalse(result.find { it.account.name == "Assets:Bank:Savings" }!!.hasSubAccounts)
    }

    @Test
    fun `resolve multiple top-level accounts without children`() {
        val accounts = listOf(
            account(id = 1, name = "Assets"),
            account(id = 2, name = "Expenses"),
            account(id = 3, name = "Income")
        )

        val result = resolver.resolve(accounts)

        assertEquals(3, result.size)
        result.forEach { assertFalse(it.hasSubAccounts) }
    }

    // ========== filterZeroBalance tests ==========

    @Test
    fun `filterZeroBalance returns all when showZeroBalance is true`() {
        val accounts = listOf(
            account(name = "Assets", amounts = listOf(amount(0f))),
            account(name = "Expenses", amounts = listOf(amount(100f)))
        )
        val resolved = resolver.resolve(accounts)

        val result = resolver.filterZeroBalance(resolved, showZeroBalance = true)

        assertEquals(2, result.size)
    }

    @Test
    fun `filterZeroBalance removes zero-balance accounts`() {
        val accounts = listOf(
            account(id = 1, name = "Assets", amounts = listOf(amount(100f))),
            account(id = 2, name = "Expenses", amounts = listOf(amount(0f))),
            account(id = 3, name = "Income", amounts = listOf(amount(500f)))
        )
        val resolved = resolver.resolve(accounts)

        val result = resolver.filterZeroBalance(resolved, showZeroBalance = false)

        assertEquals(2, result.size)
        assertTrue(result.any { it.account.name == "Assets" })
        assertTrue(result.any { it.account.name == "Income" })
        assertFalse(result.any { it.account.name == "Expenses" })
    }

    @Test
    fun `filterZeroBalance keeps zero-balance parent of non-zero child`() {
        val accounts = listOf(
            account(id = 1, name = "Assets", level = 0, amounts = listOf(amount(0f))),
            account(id = 2, name = "Assets:Bank", level = 1, amounts = listOf(amount(100f)))
        )
        val resolved = resolver.resolve(accounts)

        val result = resolver.filterZeroBalance(resolved, showZeroBalance = false)

        assertEquals(2, result.size)
        assertTrue(result.any { it.account.name == "Assets" })
        assertTrue(result.any { it.account.name == "Assets:Bank" })
    }

    @Test
    fun `filterZeroBalance removes zero-balance parent with zero children`() {
        val accounts = listOf(
            account(id = 1, name = "Assets", level = 0, amounts = listOf(amount(0f))),
            account(id = 2, name = "Assets:Bank", level = 1, amounts = listOf(amount(0f)))
        )
        val resolved = resolver.resolve(accounts)

        val result = resolver.filterZeroBalance(resolved, showZeroBalance = false)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `filterZeroBalance handles empty amounts as zero`() {
        val accounts = listOf(
            account(id = 1, name = "Assets", amounts = emptyList()),
            account(id = 2, name = "Expenses", amounts = listOf(amount(100f)))
        )
        val resolved = resolver.resolve(accounts)

        val result = resolver.filterZeroBalance(resolved, showZeroBalance = false)

        assertEquals(1, result.size)
        assertEquals("Expenses", result[0].account.name)
    }

    @Test
    fun `filterZeroBalance handles multiple currencies`() {
        val accounts = listOf(
            account(
                id = 1,
                name = "Assets",
                amounts = listOf(amount(0f, "USD"), amount(100f, "EUR"))
            ),
            account(
                id = 2,
                name = "Expenses",
                amounts = listOf(amount(0f, "USD"), amount(0f, "EUR"))
            )
        )
        val resolved = resolver.resolve(accounts)

        val result = resolver.filterZeroBalance(resolved, showZeroBalance = false)

        // Assets has non-zero EUR amount, should be kept
        assertEquals(1, result.size)
        assertEquals("Assets", result[0].account.name)
    }

    @Test
    fun `filterZeroBalance complex hierarchy preserves parents correctly`() {
        // Scenario:
        // Assets (0) <- should be kept as parent
        //   Assets:Bank (0) <- should be kept as parent
        //     Assets:Bank:Checking (100) <- non-zero, kept
        //     Assets:Bank:Savings (0) <- zero, removed
        //   Assets:Cash (0) <- zero, no children with non-zero, removed
        // Expenses (50) <- non-zero, kept

        val accounts = listOf(
            account(id = 1, name = "Assets", level = 0, amounts = listOf(amount(0f))),
            account(id = 2, name = "Assets:Bank", level = 1, amounts = listOf(amount(0f))),
            account(id = 3, name = "Assets:Bank:Checking", level = 2, amounts = listOf(amount(100f))),
            account(id = 4, name = "Assets:Bank:Savings", level = 2, amounts = listOf(amount(0f))),
            account(id = 5, name = "Assets:Cash", level = 1, amounts = listOf(amount(0f))),
            account(id = 6, name = "Expenses", level = 0, amounts = listOf(amount(50f)))
        )
        val resolved = resolver.resolve(accounts)

        val result = resolver.filterZeroBalance(resolved, showZeroBalance = false)

        val names = result.map { it.account.name }
        assertTrue("Assets should be kept", names.contains("Assets"))
        assertTrue("Assets:Bank should be kept", names.contains("Assets:Bank"))
        assertTrue("Assets:Bank:Checking should be kept", names.contains("Assets:Bank:Checking"))
        assertFalse("Assets:Bank:Savings should be removed", names.contains("Assets:Bank:Savings"))
        assertFalse("Assets:Cash should be removed", names.contains("Assets:Cash"))
        assertTrue("Expenses should be kept", names.contains("Expenses"))
    }

    @Test
    fun `filterZeroBalance returns empty for empty input`() {
        val result = resolver.filterZeroBalance(emptyList(), showZeroBalance = false)
        assertTrue(result.isEmpty())
    }
}
