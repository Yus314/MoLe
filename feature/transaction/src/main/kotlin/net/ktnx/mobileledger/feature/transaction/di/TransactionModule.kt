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

package net.ktnx.mobileledger.feature.transaction.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import net.ktnx.mobileledger.feature.transaction.usecase.AccountSuggestionLookup
import net.ktnx.mobileledger.feature.transaction.usecase.AccountSuggestionLookupImpl
import net.ktnx.mobileledger.feature.transaction.usecase.GetFirstTransactionByDescriptionUseCase
import net.ktnx.mobileledger.feature.transaction.usecase.GetFirstTransactionByDescriptionUseCaseImpl
import net.ktnx.mobileledger.feature.transaction.usecase.GetLastSyncTimestampUseCase
import net.ktnx.mobileledger.feature.transaction.usecase.GetLastSyncTimestampUseCaseImpl
import net.ktnx.mobileledger.feature.transaction.usecase.GetTransactionByIdUseCase
import net.ktnx.mobileledger.feature.transaction.usecase.GetTransactionByIdUseCaseImpl
import net.ktnx.mobileledger.feature.transaction.usecase.GetTransactionsUseCase
import net.ktnx.mobileledger.feature.transaction.usecase.GetTransactionsUseCaseImpl
import net.ktnx.mobileledger.feature.transaction.usecase.ObserveTransactionsUseCase
import net.ktnx.mobileledger.feature.transaction.usecase.ObserveTransactionsUseCaseImpl
import net.ktnx.mobileledger.feature.transaction.usecase.SearchAccountNamesUseCase
import net.ktnx.mobileledger.feature.transaction.usecase.SearchAccountNamesUseCaseImpl
import net.ktnx.mobileledger.feature.transaction.usecase.SearchTransactionDescriptionsUseCase
import net.ktnx.mobileledger.feature.transaction.usecase.SearchTransactionDescriptionsUseCaseImpl
import net.ktnx.mobileledger.feature.transaction.usecase.SetLastSyncTimestampUseCase
import net.ktnx.mobileledger.feature.transaction.usecase.SetLastSyncTimestampUseCaseImpl
import net.ktnx.mobileledger.feature.transaction.usecase.StoreTransactionUseCase
import net.ktnx.mobileledger.feature.transaction.usecase.StoreTransactionUseCaseImpl
import net.ktnx.mobileledger.feature.transaction.usecase.TransactionBalanceCalculator
import net.ktnx.mobileledger.feature.transaction.usecase.TransactionBalanceCalculatorImpl
import net.ktnx.mobileledger.feature.transaction.usecase.TransactionDateNavigator
import net.ktnx.mobileledger.feature.transaction.usecase.TransactionDateNavigatorImpl
import net.ktnx.mobileledger.feature.transaction.usecase.TransactionListConverter
import net.ktnx.mobileledger.feature.transaction.usecase.TransactionListConverterImpl

/**
 * Hilt module for providing transaction feature dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class TransactionModule {

    // ============================================
    // Existing bindings
    // ============================================

    @Binds
    @Singleton
    abstract fun bindTransactionBalanceCalculator(impl: TransactionBalanceCalculatorImpl): TransactionBalanceCalculator

    @Binds
    @Singleton
    abstract fun bindTransactionListConverter(impl: TransactionListConverterImpl): TransactionListConverter

    @Binds
    @Singleton
    abstract fun bindTransactionDateNavigator(impl: TransactionDateNavigatorImpl): TransactionDateNavigator

    // ============================================
    // Transaction List UseCases
    // ============================================

    @Binds
    @Singleton
    abstract fun bindObserveTransactionsUseCase(impl: ObserveTransactionsUseCaseImpl): ObserveTransactionsUseCase

    @Binds
    @Singleton
    abstract fun bindGetTransactionsUseCase(impl: GetTransactionsUseCaseImpl): GetTransactionsUseCase

    @Binds
    @Singleton
    abstract fun bindSearchAccountNamesUseCase(impl: SearchAccountNamesUseCaseImpl): SearchAccountNamesUseCase

    // ============================================
    // Transaction Entry UseCases
    // ============================================

    @Binds
    @Singleton
    abstract fun bindSearchTransactionDescriptionsUseCase(
        impl: SearchTransactionDescriptionsUseCaseImpl
    ): SearchTransactionDescriptionsUseCase

    @Binds
    @Singleton
    abstract fun bindStoreTransactionUseCase(impl: StoreTransactionUseCaseImpl): StoreTransactionUseCase

    @Binds
    @Singleton
    abstract fun bindGetTransactionByIdUseCase(impl: GetTransactionByIdUseCaseImpl): GetTransactionByIdUseCase

    @Binds
    @Singleton
    abstract fun bindGetFirstTransactionByDescriptionUseCase(
        impl: GetFirstTransactionByDescriptionUseCaseImpl
    ): GetFirstTransactionByDescriptionUseCase

    // ============================================
    // Account Suggestion Lookup
    // ============================================

    @Binds
    @Singleton
    abstract fun bindAccountSuggestionLookup(impl: AccountSuggestionLookupImpl): AccountSuggestionLookup

    // ============================================
    // Sync Timestamp UseCases
    // ============================================

    @Binds
    @Singleton
    abstract fun bindGetLastSyncTimestampUseCase(impl: GetLastSyncTimestampUseCaseImpl): GetLastSyncTimestampUseCase

    @Binds
    @Singleton
    abstract fun bindSetLastSyncTimestampUseCase(impl: SetLastSyncTimestampUseCaseImpl): SetLastSyncTimestampUseCase
}
