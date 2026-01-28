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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.asLog
import logcat.logcat
import net.ktnx.mobileledger.core.domain.model.Profile
import net.ktnx.mobileledger.feature.account.usecase.AccountHierarchyResolver
import net.ktnx.mobileledger.feature.account.usecase.GetShowZeroBalanceUseCase
import net.ktnx.mobileledger.feature.account.usecase.ObserveAccountsWithAmountsUseCase
import net.ktnx.mobileledger.feature.account.usecase.SetShowZeroBalanceUseCase
import net.ktnx.mobileledger.feature.profile.usecase.ObserveCurrentProfileUseCase

/**
 * ViewModel for the Account Summary tab.
 *
 * Handles:
 * - Loading accounts for the current profile (reactively via Flow)
 * - Zero balance filter toggle
 * - Account expansion state
 * - Amounts expansion state
 *
 * Observes ProfileRepository.currentProfile and AccountRepository.observeAllWithAmounts
 * to automatically reload accounts when profile changes or data is synced.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AccountSummaryViewModel @Inject constructor(
    private val observeCurrentProfileUseCase: ObserveCurrentProfileUseCase,
    private val observeAccountsWithAmountsUseCase: ObserveAccountsWithAmountsUseCase,
    private val getShowZeroBalanceUseCase: GetShowZeroBalanceUseCase,
    private val setShowZeroBalanceUseCase: SetShowZeroBalanceUseCase,
    private val accountHierarchyResolver: AccountHierarchyResolver
) : ViewModel() {

    val currentProfile: StateFlow<Profile?> = observeCurrentProfileUseCase()

    private val _showZeroBalances = MutableStateFlow(getShowZeroBalanceUseCase())
    private val _headerText = MutableStateFlow("----")

    // Track expansion state overrides: Map<accountId, isExpanded>
    // If an account is in this map, its expansion state is overridden
    private val _expandedAccountOverrides = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    private val _expandedAmountsOverrides = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    private val _error = MutableStateFlow<String?>(null)

    // Reload trigger - incrementing this forces a re-subscription to the accounts flow
    private val _reloadTrigger = MutableStateFlow(0)

    private val _effects = Channel<AccountSummaryEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    /**
     * Flow that observes accounts reactively.
     * Automatically updates when:
     * - Current profile changes
     * - showZeroBalances setting changes
     * - Underlying account data changes (e.g., after sync)
     */
    private val accountsFlow = combine(
        currentProfile,
        _showZeroBalances,
        _reloadTrigger
    ) { profile, showZeroBalances, _ ->
        Pair(profile, showZeroBalances)
    }
        .flatMapLatest { (profile, showZeroBalances) ->
            _error.value = null
            val profileId = profile?.id
            if (profileId == null) {
                flowOf(emptyList<AccountHierarchyResolver.ResolvedAccount>())
            } else {
                observeAccountsWithAmountsUseCase(profileId, showZeroBalances)
                    .map { accounts ->
                        _error.value = null
                        processAccounts(accounts, showZeroBalances)
                    }
                    .catch { e ->
                        logcat { "Error loading accounts: ${e.asLog()}" }
                        _error.value = e.message ?: "Unknown error loading accounts"
                        emit(emptyList())
                    }
            }
        }

    /**
     * UI State that combines accounts with header text and expansion states.
     */
    val uiState: StateFlow<AccountSummaryUiState> = combine(
        accountsFlow,
        _showZeroBalances,
        _headerText,
        _expandedAccountOverrides,
        _expandedAmountsOverrides,
        _error
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val resolvedAccounts = values[0] as List<AccountHierarchyResolver.ResolvedAccount>
        val showZeroBalances = values[1] as Boolean
        val headerText = values[2] as String

        @Suppress("UNCHECKED_CAST")
        val expandedAccountOverrides = values[3] as Map<Long, Boolean>

        @Suppress("UNCHECKED_CAST")
        val expandedAmountsOverrides = values[4] as Map<Long, Boolean>
        val error = values[5] as String?

        val displayItems =
            buildDisplayList(resolvedAccounts, headerText, expandedAccountOverrides, expandedAmountsOverrides)
        AccountSummaryUiState(
            accounts = displayItems,
            isLoading = false,
            error = error,
            showZeroBalanceAccounts = showZeroBalances,
            headerText = headerText
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        AccountSummaryUiState(showZeroBalanceAccounts = _showZeroBalances.value)
    )

    fun onEvent(event: AccountSummaryEvent) {
        when (event) {
            is AccountSummaryEvent.ToggleZeroBalanceAccounts -> toggleZeroBalanceAccounts()
            is AccountSummaryEvent.ToggleAccountExpanded -> toggleAccountExpanded(event.accountId)
            is AccountSummaryEvent.ToggleAmountsExpanded -> toggleAmountsExpanded(event.accountId)
            is AccountSummaryEvent.ShowAccountTransactions -> showAccountTransactions(event.accountName)
        }
    }

    /**
     * Update header text from external source (e.g., sync info).
     */
    fun updateHeaderText(text: String) {
        _headerText.value = text
    }

    /**
     * Reload accounts for the current profile.
     * Note: With reactive Flow, this is generally not needed as data updates automatically.
     * Kept for backward compatibility with external callers.
     */
    fun reloadAccounts() {
        // Increment the reload trigger to force a re-subscription to the accounts flow
        _reloadTrigger.value++
    }

    private fun processAccounts(
        accounts: List<net.ktnx.mobileledger.core.domain.model.Account>,
        showZeroBalances: Boolean
    ): List<AccountHierarchyResolver.ResolvedAccount> {
        val resolvedAccounts = accountHierarchyResolver.resolve(accounts)
        return accountHierarchyResolver.filterZeroBalance(resolvedAccounts, showZeroBalances)
    }

    private fun buildDisplayList(
        resolvedAccounts: List<AccountHierarchyResolver.ResolvedAccount>,
        headerText: String,
        expandedAccountOverrides: Map<Long, Boolean>,
        expandedAmountsOverrides: Map<Long, Boolean>
    ): List<AccountSummaryListItem> {
        val adapterList = mutableListOf<AccountSummaryListItem>()
        adapterList.add(AccountSummaryListItem.Header(headerText))

        for (resolved in resolvedAccounts) {
            val account = resolved.account
            val accountId = account.id ?: 0L
            // Use override if present, otherwise use domain model's value
            val isExpanded = expandedAccountOverrides[accountId] ?: account.isExpanded
            val amountsExpanded = expandedAmountsOverrides[accountId] ?: false
            adapterList.add(
                AccountSummaryListItem.Account(
                    id = accountId,
                    name = account.name,
                    shortName = account.shortName,
                    level = account.level,
                    amounts = account.amounts.map { amount ->
                        AccountAmount(
                            amount = amount.amount,
                            currency = amount.currency,
                            formattedAmount = formatAmount(amount.amount, amount.currency)
                        )
                    },
                    parentName = account.parentName,
                    hasSubAccounts = resolved.hasSubAccounts,
                    isExpanded = isExpanded,
                    amountsExpanded = amountsExpanded
                )
            )
        }

        return adapterList
    }

    private fun toggleZeroBalanceAccounts() {
        val newValue = !_showZeroBalances.value
        _showZeroBalances.value = newValue
        setShowZeroBalanceUseCase(newValue)
    }

    private fun toggleAccountExpanded(accountId: Long) {
        _expandedAccountOverrides.update { current ->
            val currentState = current[accountId]
            if (currentState != null) {
                // Already overridden, toggle the override
                current + (accountId to !currentState)
            } else {
                // Not overridden yet, get the current display state and toggle it
                val displayState = uiState.value.accounts
                    .filterIsInstance<AccountSummaryListItem.Account>()
                    .find { it.id == accountId }?.isExpanded ?: true
                current + (accountId to !displayState)
            }
        }
    }

    private fun toggleAmountsExpanded(accountId: Long) {
        _expandedAmountsOverrides.update { current ->
            val currentState = current[accountId]
            if (currentState != null) {
                // Already overridden, toggle the override
                current + (accountId to !currentState)
            } else {
                // Not overridden yet, get the current display state and toggle it
                val displayState = uiState.value.accounts
                    .filterIsInstance<AccountSummaryListItem.Account>()
                    .find { it.id == accountId }?.amountsExpanded ?: false
                current + (accountId to !displayState)
            }
        }
    }

    private fun showAccountTransactions(accountName: String) {
        viewModelScope.launch {
            _effects.send(AccountSummaryEffect.ShowAccountTransactions(accountName))
        }
    }

    /**
     * Format amount with currency for display.
     * Simple formatting that doesn't depend on App singleton.
     */
    private fun formatAmount(amount: Float, currency: String?): String {
        val formattedValue = String.format(java.util.Locale.US, "%,.2f", amount)
        return if (currency.isNullOrEmpty()) {
            formattedValue
        } else {
            "$formattedValue $currency"
        }
    }
}
