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
import net.ktnx.mobileledger.data.repository.AccountRepository
import net.ktnx.mobileledger.data.repository.AccountRepositoryImpl
import net.ktnx.mobileledger.data.repository.CurrencyRepository
import net.ktnx.mobileledger.data.repository.CurrencyRepositoryImpl
import net.ktnx.mobileledger.data.repository.OptionRepository
import net.ktnx.mobileledger.data.repository.OptionRepositoryImpl
import net.ktnx.mobileledger.data.repository.PreferencesRepository
import net.ktnx.mobileledger.data.repository.PreferencesRepositoryImpl
import net.ktnx.mobileledger.data.repository.ProfileRepository
import net.ktnx.mobileledger.data.repository.ProfileRepositoryImpl
import net.ktnx.mobileledger.data.repository.TemplateRepository
import net.ktnx.mobileledger.data.repository.TemplateRepositoryImpl
import net.ktnx.mobileledger.data.repository.TransactionRepository
import net.ktnx.mobileledger.data.repository.TransactionRepositoryImpl

/**
 * Hilt module providing Repository implementations.
 *
 * This module binds Repository interfaces to their implementations,
 * enabling dependency injection of repositories into ViewModels.
 *
 * ## Provided Repositories
 *
 * Repositories will be added incrementally as they are implemented:
 * - TransactionRepository (Phase 3, US1)
 * - ProfileRepository (Phase 4, US2)
 * - AccountRepository (Phase 5, US2)
 * - TemplateRepository (Phase 5, US2)
 * - CurrencyRepository (Phase 5, US2)
 *
 * ## Usage
 *
 * ViewModels can request repositories via constructor injection:
 *
 * ```kotlin
 * @HiltViewModel
 * class MyViewModel @Inject constructor(
 *     private val transactionRepository: TransactionRepository,
 *     private val profileRepository: ProfileRepository
 * ) : ViewModel() {
 *     // Use repositories for data access
 * }
 * ```
 *
 * ## Design Notes (008-data-layer-repository)
 *
 * - All repositories are @Singleton scoped
 * - Uses @Binds for interface-to-implementation mapping
 * - Repositories wrap existing DAOs and provide Flow-based APIs
 * - Profile selection state is managed by ProfileRepository
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds
    @Singleton
    abstract fun bindTemplateRepository(impl: TemplateRepositoryImpl): TemplateRepository

    @Binds
    @Singleton
    abstract fun bindCurrencyRepository(impl: CurrencyRepositoryImpl): CurrencyRepository

    @Binds
    @Singleton
    abstract fun bindOptionRepository(impl: OptionRepositoryImpl): OptionRepository

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(impl: PreferencesRepositoryImpl): PreferencesRepository
}
