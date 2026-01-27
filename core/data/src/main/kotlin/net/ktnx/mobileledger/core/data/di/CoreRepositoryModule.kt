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

package net.ktnx.mobileledger.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import net.ktnx.mobileledger.core.data.repository.impl.AccountRepositoryImpl
import net.ktnx.mobileledger.core.data.repository.impl.CurrencyRepositoryImpl
import net.ktnx.mobileledger.core.data.repository.impl.OptionRepositoryImpl
import net.ktnx.mobileledger.core.data.repository.impl.PreferencesRepositoryImpl
import net.ktnx.mobileledger.core.data.repository.impl.ProfileRepositoryImpl
import net.ktnx.mobileledger.core.data.repository.impl.TemplateRepositoryImpl
import net.ktnx.mobileledger.core.data.repository.impl.TransactionRepositoryImpl
import net.ktnx.mobileledger.core.domain.repository.AccountRepository
import net.ktnx.mobileledger.core.domain.repository.CurrencyRepository
import net.ktnx.mobileledger.core.domain.repository.OptionRepository
import net.ktnx.mobileledger.core.domain.repository.PreferencesRepository
import net.ktnx.mobileledger.core.domain.repository.ProfileRepository
import net.ktnx.mobileledger.core.domain.repository.TemplateRepository
import net.ktnx.mobileledger.core.domain.repository.TransactionRepository

/**
 * Hilt module providing core Repository implementations.
 *
 * This module binds Repository interfaces to their implementations in core:data.
 * These repositories handle database operations for domain models.
 *
 * ## Provided Repositories
 *
 * - ProfileRepository: Profile management
 * - AccountRepository: Account data access
 * - CurrencyRepository: Currency settings
 * - OptionRepository: Profile-specific options
 * - PreferencesRepository: App-wide preferences
 * - TransactionRepository: Transaction data access
 * - TemplateRepository: Transaction template management
 *
 * ## Design Notes
 *
 * - All repositories are @Singleton scoped
 * - Uses @Binds for interface-to-implementation mapping
 * - Repositories use CoreExceptionMapper for error handling
 * - Repositories wrap DAOs and provide Flow-based APIs
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CoreRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds
    @Singleton
    abstract fun bindCurrencyRepository(impl: CurrencyRepositoryImpl): CurrencyRepository

    @Binds
    @Singleton
    abstract fun bindOptionRepository(impl: OptionRepositoryImpl): OptionRepository

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(impl: PreferencesRepositoryImpl): PreferencesRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindTemplateRepository(impl: TemplateRepositoryImpl): TemplateRepository
}
