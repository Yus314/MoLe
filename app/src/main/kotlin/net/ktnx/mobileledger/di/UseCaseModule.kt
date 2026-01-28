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
import net.ktnx.mobileledger.domain.usecase.ConfigBackup
import net.ktnx.mobileledger.domain.usecase.ConfigBackupImpl
import net.ktnx.mobileledger.domain.usecase.DatabaseInitializer
import net.ktnx.mobileledger.domain.usecase.DatabaseInitializerImpl
import net.ktnx.mobileledger.domain.usecase.VersionDetector
import net.ktnx.mobileledger.domain.usecase.VersionDetectorImpl

/**
 * Hilt module for app-level infrastructure use cases.
 *
 * Use case modules are split by domain:
 * - feature:profile - Profile-related use cases
 * - feature:templates - Template-related use cases
 * - feature:transaction - Transaction-related use cases
 * - feature:account - Account-related use cases
 * - CurrencyUseCaseModule: Currency-related use cases
 * - PreferencesUseCaseModule: Preferences-related use cases
 * - TransactionUseCaseModule: Transaction sender & row manager (app-level dependencies)
 * - UseCaseModule (this): Infrastructure use cases
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class UseCaseModule {

    @Binds
    @Singleton
    abstract fun bindConfigBackup(impl: ConfigBackupImpl): ConfigBackup

    @Binds
    @Singleton
    abstract fun bindDatabaseInitializer(impl: DatabaseInitializerImpl): DatabaseInitializer

    @Binds
    @Singleton
    abstract fun bindVersionDetector(impl: VersionDetectorImpl): VersionDetector
}
