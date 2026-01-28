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

    @Binds
    @Singleton
    abstract fun bindTransactionBalanceCalculator(impl: TransactionBalanceCalculatorImpl): TransactionBalanceCalculator

    @Binds
    @Singleton
    abstract fun bindTransactionListConverter(impl: TransactionListConverterImpl): TransactionListConverter

    @Binds
    @Singleton
    abstract fun bindTransactionDateNavigator(impl: TransactionDateNavigatorImpl): TransactionDateNavigator
}
