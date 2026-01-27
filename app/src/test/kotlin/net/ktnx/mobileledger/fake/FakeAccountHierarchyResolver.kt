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

package net.ktnx.mobileledger.fake

import net.ktnx.mobileledger.core.domain.model.Account
import net.ktnx.mobileledger.domain.usecase.AccountHierarchyResolver
import net.ktnx.mobileledger.domain.usecase.AccountHierarchyResolverImpl

/**
 * Fake implementation of [AccountHierarchyResolver] for testing.
 *
 * Provides controllable behavior for tests:
 * - Configure custom results via [customResolveResult] and [customFilterResult]
 * - Track method calls via [resolveCallCount], [filterCallCount]
 * - Reset state between tests via [reset]
 */
class FakeAccountHierarchyResolver : AccountHierarchyResolver {

    /**
     * Custom result to return from [resolve]. If null, delegates to real implementation.
     */
    var customResolveResult: List<AccountHierarchyResolver.ResolvedAccount>? = null

    /**
     * Custom result to return from [filterZeroBalance]. If null, delegates to real implementation.
     */
    var customFilterResult: List<AccountHierarchyResolver.ResolvedAccount>? = null

    /**
     * Number of times [resolve] was called.
     */
    var resolveCallCount = 0
        private set

    /**
     * Number of times [filterZeroBalance] was called.
     */
    var filterCallCount = 0
        private set

    /**
     * The accounts passed to the last [resolve] call.
     */
    var lastResolveAccounts: List<Account>? = null
        private set

    /**
     * The accounts passed to the last [filterZeroBalance] call.
     */
    var lastFilterAccounts: List<AccountHierarchyResolver.ResolvedAccount>? = null
        private set

    /**
     * Real implementation for delegation when custom results are null.
     */
    private val realImpl = AccountHierarchyResolverImpl()

    override fun resolve(accounts: List<Account>): List<AccountHierarchyResolver.ResolvedAccount> {
        resolveCallCount++
        lastResolveAccounts = accounts
        return customResolveResult ?: realImpl.resolve(accounts)
    }

    override fun filterZeroBalance(
        accounts: List<AccountHierarchyResolver.ResolvedAccount>,
        showZeroBalance: Boolean
    ): List<AccountHierarchyResolver.ResolvedAccount> {
        filterCallCount++
        lastFilterAccounts = accounts
        return customFilterResult ?: realImpl.filterZeroBalance(accounts, showZeroBalance)
    }

    /**
     * Reset all state to initial values.
     */
    fun reset() {
        customResolveResult = null
        customFilterResult = null
        resolveCallCount = 0
        filterCallCount = 0
        lastResolveAccounts = null
        lastFilterAccounts = null
    }
}
