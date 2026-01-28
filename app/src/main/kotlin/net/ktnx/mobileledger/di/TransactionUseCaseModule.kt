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
import net.ktnx.mobileledger.domain.usecase.TransactionAccountRowManager
import net.ktnx.mobileledger.domain.usecase.TransactionAccountRowManagerImpl
import net.ktnx.mobileledger.domain.usecase.TransactionSender
import net.ktnx.mobileledger.domain.usecase.TransactionSenderImpl

/**
 * Hilt module for transaction-related use cases that depend on app-level components.
 *
 * Note: Most transaction UseCases have been moved to feature:transaction module.
 * Only UseCases with app-level dependencies remain here:
 * - TransactionSender: Depends on CurrencyFormatter (app service)
 * - TransactionAccountRowManager: Depends on TransactionAccountRow (app/ui)
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class TransactionUseCaseModule {

    @Binds
    @Singleton
    abstract fun bindTransactionSender(impl: TransactionSenderImpl): TransactionSender

    @Binds
    @Singleton
    abstract fun bindTransactionAccountRowManager(impl: TransactionAccountRowManagerImpl): TransactionAccountRowManager
}
