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

package net.ktnx.mobileledger.core.sync.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import net.ktnx.mobileledger.core.sync.AccountListFetcher
import net.ktnx.mobileledger.core.sync.AccountListFetcherImpl
import net.ktnx.mobileledger.core.sync.SyncPersistence
import net.ktnx.mobileledger.core.sync.SyncPersistenceImpl
import net.ktnx.mobileledger.core.sync.TransactionListFetcher
import net.ktnx.mobileledger.core.sync.TransactionListFetcherImpl
import net.ktnx.mobileledger.core.sync.TransactionSyncer
import net.ktnx.mobileledger.core.sync.TransactionSyncerImpl

/**
 * Hilt module for providing sync-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {

    @Binds
    @Singleton
    abstract fun bindTransactionSyncer(impl: TransactionSyncerImpl): TransactionSyncer

    @Binds
    @Singleton
    abstract fun bindAccountListFetcher(impl: AccountListFetcherImpl): AccountListFetcher

    @Binds
    @Singleton
    abstract fun bindTransactionListFetcher(impl: TransactionListFetcherImpl): TransactionListFetcher

    @Binds
    @Singleton
    abstract fun bindSyncPersistence(impl: SyncPersistenceImpl): SyncPersistence
}
