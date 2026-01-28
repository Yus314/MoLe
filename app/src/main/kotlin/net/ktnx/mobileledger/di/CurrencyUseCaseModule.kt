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
import net.ktnx.mobileledger.domain.usecase.DeleteCurrencyUseCase
import net.ktnx.mobileledger.domain.usecase.DeleteCurrencyUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.GetAllCurrenciesUseCase
import net.ktnx.mobileledger.domain.usecase.GetAllCurrenciesUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.SaveCurrencyUseCase
import net.ktnx.mobileledger.domain.usecase.SaveCurrencyUseCaseImpl

/**
 * Hilt module for currency-related use cases.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CurrencyUseCaseModule {

    @Binds
    @Singleton
    abstract fun bindGetAllCurrenciesUseCase(impl: GetAllCurrenciesUseCaseImpl): GetAllCurrenciesUseCase

    @Binds
    @Singleton
    abstract fun bindSaveCurrencyUseCase(impl: SaveCurrencyUseCaseImpl): SaveCurrencyUseCase

    @Binds
    @Singleton
    abstract fun bindDeleteCurrencyUseCase(impl: DeleteCurrencyUseCaseImpl): DeleteCurrencyUseCase
}
