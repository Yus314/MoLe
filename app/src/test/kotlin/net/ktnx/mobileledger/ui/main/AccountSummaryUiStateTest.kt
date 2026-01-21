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

package net.ktnx.mobileledger.ui.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for AccountSummaryUiState and related data classes.
 */
class AccountSummaryUiStateTest {

    // ========================================
    // AccountSummaryUiState tests
    // ========================================

    @Test
    fun `AccountSummaryUiState default values are correct`() {
        val state = AccountSummaryUiState()
        assertTrue(state.accounts.isEmpty())
        assertTrue(state.showZeroBalanceAccounts)
        assertFalse(state.isLoading)
        assertEquals("----", state.headerText)
        assertNull(state.error)
    }

    @Test
    fun `AccountSummaryUiState can be customized`() {
        val accounts = listOf(createAccount(1, "Assets", 100f))
        val state = AccountSummaryUiState(
            accounts = accounts,
            showZeroBalanceAccounts = false,
            isLoading = true,
            headerText = "Last update: today",
            error = "Network error"
        )
        assertEquals(accounts, state.accounts)
        assertFalse(state.showZeroBalanceAccounts)
        assertTrue(state.isLoading)
        assertEquals("Last update: today", state.headerText)
        assertEquals("Network error", state.error)
    }

    // ========================================
    // AccountSummaryListItem.Account.allAmountsAreZero tests
    // ========================================

    @Test
    fun `allAmountsAreZero returns true when all amounts are zero`() {
        val account = createAccount(1, "Assets", 0f)
        assertTrue(account.allAmountsAreZero())
    }

    @Test
    fun `allAmountsAreZero returns true when multiple amounts are zero`() {
        val account = AccountSummaryListItem.Account(
            id = 1,
            name = "Assets",
            shortName = "Assets",
            level = 0,
            amounts = listOf(
                AccountAmount(0f, "USD", "0.00"),
                AccountAmount(0f, "EUR", "0.00")
            )
        )
        assertTrue(account.allAmountsAreZero())
    }

    @Test
    fun `allAmountsAreZero returns false when any amount is non-zero`() {
        val account = createAccount(1, "Assets", 100f)
        assertFalse(account.allAmountsAreZero())
    }

    @Test
    fun `allAmountsAreZero returns false with mixed amounts`() {
        val account = AccountSummaryListItem.Account(
            id = 1,
            name = "Assets",
            shortName = "Assets",
            level = 0,
            amounts = listOf(
                AccountAmount(0f, "USD", "0.00"),
                AccountAmount(50f, "EUR", "50.00")
            )
        )
        assertFalse(account.allAmountsAreZero())
    }

    @Test
    fun `allAmountsAreZero returns true for empty amounts list`() {
        val account = AccountSummaryListItem.Account(
            id = 1,
            name = "Assets",
            shortName = "Assets",
            level = 0,
            amounts = emptyList()
        )
        assertTrue(account.allAmountsAreZero())
    }

    @Test
    fun `allAmountsAreZero handles negative amounts`() {
        val account = createAccount(1, "Assets", -100f)
        assertFalse(account.allAmountsAreZero())
    }

    // ========================================
    // AccountSummaryListItem.removeZeroAccounts tests
    // ========================================

    @Test
    fun `removeZeroAccounts returns empty list for empty input`() {
        val result = AccountSummaryListItem.removeZeroAccounts(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `removeZeroAccounts keeps header items`() {
        val list = listOf<AccountSummaryListItem>(
            AccountSummaryListItem.Header("Last update: today")
        )
        val result = AccountSummaryListItem.removeZeroAccounts(list)
        assertEquals(1, result.size)
        assertTrue(result[0] is AccountSummaryListItem.Header)
    }

    @Test
    fun `removeZeroAccounts keeps non-zero accounts`() {
        val list = listOf<AccountSummaryListItem>(
            createAccount(1, "Assets", 100f)
        )
        val result = AccountSummaryListItem.removeZeroAccounts(list)
        assertEquals(1, result.size)
    }

    @Test
    fun `removeZeroAccounts removes zero balance accounts`() {
        val list = listOf<AccountSummaryListItem>(
            createAccount(1, "Assets", 100f),
            createAccount(2, "Expenses", 0f)
        )
        val result = AccountSummaryListItem.removeZeroAccounts(list)
        assertEquals(1, result.size)
        assertEquals("Assets", (result[0] as AccountSummaryListItem.Account).name)
    }

    @Test
    fun `removeZeroAccounts keeps parent with non-zero child`() {
        val list = listOf<AccountSummaryListItem>(
            createAccount(1, "Assets", 0f),
            createAccount(2, "Assets:Bank", 100f)
        )
        val result = AccountSummaryListItem.removeZeroAccounts(list)
        assertEquals(2, result.size)
        assertEquals("Assets", (result[0] as AccountSummaryListItem.Account).name)
        assertEquals("Assets:Bank", (result[1] as AccountSummaryListItem.Account).name)
    }

    @Test
    fun `removeZeroAccounts removes parent and child when both zero`() {
        val list = listOf<AccountSummaryListItem>(
            createAccount(1, "Assets", 0f),
            createAccount(2, "Assets:Bank", 0f)
        )
        val result = AccountSummaryListItem.removeZeroAccounts(list)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `removeZeroAccounts handles deep hierarchy`() {
        val list = listOf<AccountSummaryListItem>(
            createAccount(1, "Assets", 0f),
            createAccount(2, "Assets:Bank", 0f),
            createAccount(3, "Assets:Bank:Checking", 100f)
        )
        val result = AccountSummaryListItem.removeZeroAccounts(list)
        assertEquals(3, result.size)
    }

    @Test
    fun `removeZeroAccounts handles header followed by zero account`() {
        val list = listOf<AccountSummaryListItem>(
            AccountSummaryListItem.Header("Test"),
            createAccount(1, "Assets", 0f)
        )
        val result = AccountSummaryListItem.removeZeroAccounts(list)
        assertEquals(1, result.size)
        assertTrue(result[0] is AccountSummaryListItem.Header)
    }

    @Test
    fun `removeZeroAccounts handles header followed by non-zero account`() {
        val list = listOf<AccountSummaryListItem>(
            AccountSummaryListItem.Header("Test"),
            createAccount(1, "Assets", 100f)
        )
        val result = AccountSummaryListItem.removeZeroAccounts(list)
        assertEquals(2, result.size)
    }

    @Test
    fun `removeZeroAccounts handles mixed accounts`() {
        val list = listOf<AccountSummaryListItem>(
            AccountSummaryListItem.Header("Accounts"),
            createAccount(1, "Assets", 100f),
            createAccount(2, "Liabilities", 0f),
            createAccount(3, "Expenses", 50f),
            createAccount(4, "Income", 0f)
        )
        val result = AccountSummaryListItem.removeZeroAccounts(list)
        assertEquals(3, result.size) // Header, Assets, Expenses
    }

    @Test
    fun `removeZeroAccounts preserves order`() {
        val list = listOf<AccountSummaryListItem>(
            createAccount(1, "Assets", 100f),
            createAccount(2, "Expenses", 200f),
            createAccount(3, "Income", 300f)
        )
        val result = AccountSummaryListItem.removeZeroAccounts(list)
        assertEquals(3, result.size)
        assertEquals("Assets", (result[0] as AccountSummaryListItem.Account).name)
        assertEquals("Expenses", (result[1] as AccountSummaryListItem.Account).name)
        assertEquals("Income", (result[2] as AccountSummaryListItem.Account).name)
    }

    // ========================================
    // AccountAmount tests
    // ========================================

    @Test
    fun `AccountAmount constructor sets all fields`() {
        val amount = AccountAmount(100.50f, "USD", "$100.50")
        assertEquals(100.50f, amount.amount, 0.001f)
        assertEquals("USD", amount.currency)
        assertEquals("$100.50", amount.formattedAmount)
    }

    @Test
    fun `AccountAmount handles negative amounts`() {
        val amount = AccountAmount(-50.25f, "EUR", "-€50.25")
        assertEquals(-50.25f, amount.amount, 0.001f)
    }

    @Test
    fun `AccountAmount handles zero`() {
        val amount = AccountAmount(0f, "JPY", "¥0")
        assertEquals(0f, amount.amount, 0.001f)
    }

    // ========================================
    // AccountSummaryListItem.Header tests
    // ========================================

    @Test
    fun `Header contains text`() {
        val header = AccountSummaryListItem.Header("Last update: 2026-01-21")
        assertEquals("Last update: 2026-01-21", header.text)
    }

    // ========================================
    // AccountSummaryListItem.Account tests
    // ========================================

    @Test
    fun `Account default values are correct`() {
        val account = AccountSummaryListItem.Account(
            id = 1,
            name = "Assets",
            shortName = "Assets",
            level = 0,
            amounts = emptyList()
        )
        assertNull(account.parentName)
        assertFalse(account.hasSubAccounts)
        assertTrue(account.isExpanded)
        assertFalse(account.amountsExpanded)
    }

    @Test
    fun `Account can be customized`() {
        val account = AccountSummaryListItem.Account(
            id = 1,
            name = "Assets:Bank",
            shortName = "Bank",
            level = 1,
            amounts = listOf(AccountAmount(100f, "USD", "$100")),
            parentName = "Assets",
            hasSubAccounts = true,
            isExpanded = false,
            amountsExpanded = true
        )
        assertEquals(1L, account.id)
        assertEquals("Assets:Bank", account.name)
        assertEquals("Bank", account.shortName)
        assertEquals(1, account.level)
        assertEquals("Assets", account.parentName)
        assertTrue(account.hasSubAccounts)
        assertFalse(account.isExpanded)
        assertTrue(account.amountsExpanded)
    }

    // ========================================
    // AccountSummaryEvent tests
    // ========================================

    @Test
    fun `ToggleAccountExpanded contains accountId`() {
        val event = AccountSummaryEvent.ToggleAccountExpanded(5L)
        assertEquals(5L, event.accountId)
    }

    @Test
    fun `ToggleAmountsExpanded contains accountId`() {
        val event = AccountSummaryEvent.ToggleAmountsExpanded(3L)
        assertEquals(3L, event.accountId)
    }

    @Test
    fun `ShowAccountTransactions contains accountName`() {
        val event = AccountSummaryEvent.ShowAccountTransactions("Assets:Bank")
        assertEquals("Assets:Bank", event.accountName)
    }

    // ========================================
    // AccountSummaryEffect tests
    // ========================================

    @Test
    fun `ShowAccountTransactions effect contains accountName`() {
        val effect = AccountSummaryEffect.ShowAccountTransactions("Expenses:Food")
        assertEquals("Expenses:Food", effect.accountName)
    }

    // ========================================
    // Helper functions
    // ========================================

    private fun createAccount(id: Long, name: String, amount: Float): AccountSummaryListItem.Account =
        AccountSummaryListItem.Account(
            id = id,
            name = name,
            shortName = name.substringAfterLast(':'),
            level = name.count { it == ':' },
            amounts = listOf(AccountAmount(amount, "USD", "$$amount"))
        )
}
