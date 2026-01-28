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
import net.ktnx.mobileledger.domain.usecase.GetStartupProfileIdUseCase
import net.ktnx.mobileledger.domain.usecase.GetStartupProfileIdUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.GetStartupThemeUseCase
import net.ktnx.mobileledger.domain.usecase.GetStartupThemeUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.SetStartupProfileIdUseCase
import net.ktnx.mobileledger.domain.usecase.SetStartupProfileIdUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.SetStartupThemeUseCase
import net.ktnx.mobileledger.domain.usecase.SetStartupThemeUseCaseImpl

/**
 * Hilt module for preferences-related use cases.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PreferencesUseCaseModule {

    @Binds
    @Singleton
    abstract fun bindGetStartupThemeUseCase(impl: GetStartupThemeUseCaseImpl): GetStartupThemeUseCase

    @Binds
    @Singleton
    abstract fun bindSetStartupThemeUseCase(impl: SetStartupThemeUseCaseImpl): SetStartupThemeUseCase

    @Binds
    @Singleton
    abstract fun bindGetStartupProfileIdUseCase(impl: GetStartupProfileIdUseCaseImpl): GetStartupProfileIdUseCase

    @Binds
    @Singleton
    abstract fun bindSetStartupProfileIdUseCase(impl: SetStartupProfileIdUseCaseImpl): SetStartupProfileIdUseCase
}
