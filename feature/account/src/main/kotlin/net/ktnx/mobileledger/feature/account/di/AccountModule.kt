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

package net.ktnx.mobileledger.feature.account.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import net.ktnx.mobileledger.feature.account.usecase.AccountHierarchyResolver
import net.ktnx.mobileledger.feature.account.usecase.AccountHierarchyResolverImpl
import net.ktnx.mobileledger.feature.account.usecase.GetAccountsWithAmountsUseCase
import net.ktnx.mobileledger.feature.account.usecase.GetAccountsWithAmountsUseCaseImpl
import net.ktnx.mobileledger.feature.account.usecase.GetShowZeroBalanceUseCase
import net.ktnx.mobileledger.feature.account.usecase.GetShowZeroBalanceUseCaseImpl
import net.ktnx.mobileledger.feature.account.usecase.ObserveAccountsWithAmountsUseCase
import net.ktnx.mobileledger.feature.account.usecase.ObserveAccountsWithAmountsUseCaseImpl
import net.ktnx.mobileledger.feature.account.usecase.SetShowZeroBalanceUseCase
import net.ktnx.mobileledger.feature.account.usecase.SetShowZeroBalanceUseCaseImpl

/**
 * Hilt module for providing account feature dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AccountModule {

    @Binds
    @Singleton
    abstract fun bindGetAccountsWithAmountsUseCase(
        impl: GetAccountsWithAmountsUseCaseImpl
    ): GetAccountsWithAmountsUseCase

    @Binds
    @Singleton
    abstract fun bindObserveAccountsWithAmountsUseCase(
        impl: ObserveAccountsWithAmountsUseCaseImpl
    ): ObserveAccountsWithAmountsUseCase

    @Binds
    @Singleton
    abstract fun bindGetShowZeroBalanceUseCase(impl: GetShowZeroBalanceUseCaseImpl): GetShowZeroBalanceUseCase

    @Binds
    @Singleton
    abstract fun bindSetShowZeroBalanceUseCase(impl: SetShowZeroBalanceUseCaseImpl): SetShowZeroBalanceUseCase

    @Binds
    @Singleton
    abstract fun bindAccountHierarchyResolver(impl: AccountHierarchyResolverImpl): AccountHierarchyResolver
}
