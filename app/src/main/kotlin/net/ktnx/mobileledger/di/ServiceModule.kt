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
import net.ktnx.mobileledger.service.AppStateService
import net.ktnx.mobileledger.service.AppStateServiceImpl
import net.ktnx.mobileledger.service.AuthDataProvider
import net.ktnx.mobileledger.service.AuthDataProviderImpl
import net.ktnx.mobileledger.service.BackgroundTaskManager
import net.ktnx.mobileledger.service.BackgroundTaskManagerImpl
import net.ktnx.mobileledger.service.CurrencyFormatter
import net.ktnx.mobileledger.service.CurrencyFormatterImpl

/**
 * Hilt module providing application-level services.
 *
 * This module provides singleton instances of:
 * - BackgroundTaskManager: Manages background task state and progress
 * - CurrencyFormatter: Handles locale-dependent currency/number formatting
 * - AppStateService: Manages UI-level application state
 *
 * ## Design Notes (009-eliminate-data-singleton)
 *
 * These services replace the AppStateManager singleton with properly
 * scoped, injectable services that can be tested independently.
 *
 * - All services are @Singleton scoped
 * - Uses @Binds for interface-to-implementation mapping
 * - Services are independent and stateless where possible
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {

    @Binds
    @Singleton
    abstract fun bindBackgroundTaskManager(impl: BackgroundTaskManagerImpl): BackgroundTaskManager

    @Binds
    @Singleton
    abstract fun bindCurrencyFormatter(impl: CurrencyFormatterImpl): CurrencyFormatter

    @Binds
    @Singleton
    abstract fun bindAppStateService(impl: AppStateServiceImpl): AppStateService

    @Binds
    @Singleton
    abstract fun bindAuthDataProvider(impl: AuthDataProviderImpl): AuthDataProvider
}
