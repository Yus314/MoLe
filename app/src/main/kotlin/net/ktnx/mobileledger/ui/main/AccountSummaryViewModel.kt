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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.asLog
import logcat.logcat
import net.ktnx.mobileledger.domain.usecase.AccountHierarchyResolver
import net.ktnx.mobileledger.domain.usecase.GetAccountsWithAmountsUseCase
import net.ktnx.mobileledger.domain.usecase.GetShowZeroBalanceUseCase
import net.ktnx.mobileledger.domain.usecase.ObserveCurrentProfileUseCase
import net.ktnx.mobileledger.domain.usecase.SetShowZeroBalanceUseCase
import net.ktnx.mobileledger.domain.model.Profile

/**
 * ViewModel for the Account Summary tab.
 *
 * Handles:
 * - Loading accounts for the current profile
 * - Zero balance filter toggle
 * - Account expansion state
 * - Amounts expansion state
 *
 * Observes ProfileRepository.currentProfile to reload accounts when profile changes.
 */
@HiltViewModel
class AccountSummaryViewModel @Inject constructor(
    private val observeCurrentProfileUseCase: ObserveCurrentProfileUseCase,
    private val getAccountsWithAmountsUseCase: GetAccountsWithAmountsUseCase,
    private val getShowZeroBalanceUseCase: GetShowZeroBalanceUseCase,
    private val setShowZeroBalanceUseCase: SetShowZeroBalanceUseCase,
    private val accountHierarchyResolver: AccountHierarchyResolver
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountSummaryUiState())
    val uiState: StateFlow<AccountSummaryUiState> = _uiState.asStateFlow()

    val currentProfile: StateFlow<Profile?> = observeCurrentProfileUseCase()

    private val _effects = Channel<AccountSummaryEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        loadInitialPreferences()
        observeProfileChanges()
    }

    private fun loadInitialPreferences() {
        _uiState.update { it.copy(showZeroBalanceAccounts = getShowZeroBalanceUseCase()) }
    }

    private fun observeProfileChanges() {
        viewModelScope.launch {
            currentProfile.collect { profile ->
                val profileId = profile?.id
                if (profileId != null) {
                    loadAccounts(profileId)
                } else {
                    _uiState.update { it.copy(accounts = emptyList(), isLoading = false) }
                }
            }
        }
    }

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
        _uiState.update { state ->
            val updatedAccounts = state.accounts.map { item ->
                when (item) {
                    is AccountSummaryListItem.Header -> AccountSummaryListItem.Header(text)
                    else -> item
                }
            }
            state.copy(accounts = updatedAccounts, headerText = text)
        }
    }

    /**
     * Reload accounts for the current profile.
     * Can be called externally when data changes are detected.
     */
    fun reloadAccounts() {
        val profileId = currentProfile.value?.id ?: return
        loadAccounts(profileId)
    }

    private fun loadAccounts(profileId: Long) {
        val showZeroBalances = _uiState.value.showZeroBalanceAccounts

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val result = getAccountsWithAmountsUseCase(profileId, showZeroBalances)
                if (result.isFailure) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.exceptionOrNull()?.message ?: "Unknown error loading accounts"
                        )
                    }
                    return@launch
                }
                val domainAccounts = result.getOrElse { emptyList() }

                // Delegate hierarchy resolution to UseCase
                val resolvedAccounts = accountHierarchyResolver.resolve(domainAccounts)
                val filteredAccounts = accountHierarchyResolver.filterZeroBalance(
                    resolvedAccounts,
                    showZeroBalances
                )

                // Build the display list
                val adapterList = mutableListOf<AccountSummaryListItem>()
                val headerText = _uiState.value.headerText.ifEmpty { "----" }
                adapterList.add(AccountSummaryListItem.Header(headerText))

                for (resolved in filteredAccounts) {
                    val account = resolved.account
                    adapterList.add(
                        AccountSummaryListItem.Account(
                            id = account.id ?: 0L,
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
                            isExpanded = account.isExpanded,
                            amountsExpanded = false
                        )
                    )
                }

                _uiState.update {
                    it.copy(
                        accounts = adapterList,
                        isLoading = false,
                        headerText = headerText,
                        error = null
                    )
                }
            } catch (e: Exception) {
                logcat { "Error loading accounts: ${e.asLog()}" }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error"
                    )
                }
            }
        }
    }

    private fun toggleZeroBalanceAccounts() {
        val newValue = !_uiState.value.showZeroBalanceAccounts
        _uiState.update { it.copy(showZeroBalanceAccounts = newValue) }
        setShowZeroBalanceUseCase(newValue)
        reloadAccounts()
    }

    private fun toggleAccountExpanded(accountId: Long) {
        _uiState.update { state ->
            val updatedAccounts = state.accounts.map { item ->
                when (item) {
                    is AccountSummaryListItem.Account -> {
                        if (item.id == accountId) {
                            item.copy(isExpanded = !item.isExpanded)
                        } else {
                            item
                        }
                    }

                    else -> item
                }
            }
            state.copy(accounts = updatedAccounts)
        }
    }

    private fun toggleAmountsExpanded(accountId: Long) {
        _uiState.update { state ->
            val updatedAccounts = state.accounts.map { item ->
                when (item) {
                    is AccountSummaryListItem.Account -> {
                        if (item.id == accountId) {
                            item.copy(amountsExpanded = !item.amountsExpanded)
                        } else {
                            item
                        }
                    }

                    else -> item
                }
            }
            state.copy(accounts = updatedAccounts)
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
