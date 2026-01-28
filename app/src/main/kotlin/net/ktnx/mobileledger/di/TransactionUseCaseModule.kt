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

package net.ktnx.mobileledger.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import net.ktnx.mobileledger.domain.usecase.AccountSuggestionLookup
import net.ktnx.mobileledger.domain.usecase.AccountSuggestionLookupImpl
import net.ktnx.mobileledger.domain.usecase.GetFirstTransactionByDescriptionUseCase
import net.ktnx.mobileledger.domain.usecase.GetFirstTransactionByDescriptionUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.GetLastSyncTimestampUseCase
import net.ktnx.mobileledger.domain.usecase.GetLastSyncTimestampUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.GetTransactionByIdUseCase
import net.ktnx.mobileledger.domain.usecase.GetTransactionByIdUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.GetTransactionsUseCase
import net.ktnx.mobileledger.domain.usecase.GetTransactionsUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.ObserveTransactionsUseCase
import net.ktnx.mobileledger.domain.usecase.ObserveTransactionsUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.SearchAccountNamesUseCase
import net.ktnx.mobileledger.domain.usecase.SearchAccountNamesUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.SearchTransactionDescriptionsUseCase
import net.ktnx.mobileledger.domain.usecase.SearchTransactionDescriptionsUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.SetLastSyncTimestampUseCase
import net.ktnx.mobileledger.domain.usecase.SetLastSyncTimestampUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.StoreTransactionUseCase
import net.ktnx.mobileledger.domain.usecase.StoreTransactionUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.TransactionAccountRowManager
import net.ktnx.mobileledger.domain.usecase.TransactionAccountRowManagerImpl
import net.ktnx.mobileledger.domain.usecase.TransactionSender
import net.ktnx.mobileledger.domain.usecase.TransactionSenderImpl

/**
 * Hilt module for transaction-related use cases.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class TransactionUseCaseModule {

    @Binds
    @Singleton
    abstract fun bindObserveTransactionsUseCase(impl: ObserveTransactionsUseCaseImpl): ObserveTransactionsUseCase

    @Binds
    @Singleton
    abstract fun bindGetTransactionsUseCase(impl: GetTransactionsUseCaseImpl): GetTransactionsUseCase

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

    @Binds
    @Singleton
    abstract fun bindSearchTransactionDescriptionsUseCase(
        impl: SearchTransactionDescriptionsUseCaseImpl
    ): SearchTransactionDescriptionsUseCase

    @Binds
    @Singleton
    abstract fun bindSearchAccountNamesUseCase(impl: SearchAccountNamesUseCaseImpl): SearchAccountNamesUseCase

    @Binds
    @Singleton
    abstract fun bindTransactionSender(impl: TransactionSenderImpl): TransactionSender

    @Binds
    @Singleton
    abstract fun bindTransactionAccountRowManager(impl: TransactionAccountRowManagerImpl): TransactionAccountRowManager

    @Binds
    @Singleton
    abstract fun bindAccountSuggestionLookup(impl: AccountSuggestionLookupImpl): AccountSuggestionLookup

    @Binds
    @Singleton
    abstract fun bindGetLastSyncTimestampUseCase(impl: GetLastSyncTimestampUseCaseImpl): GetLastSyncTimestampUseCase

    @Binds
    @Singleton
    abstract fun bindSetLastSyncTimestampUseCase(impl: SetLastSyncTimestampUseCaseImpl): SetLastSyncTimestampUseCase
}
