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
import net.ktnx.mobileledger.domain.usecase.AccountHierarchyResolver
import net.ktnx.mobileledger.domain.usecase.AccountHierarchyResolverImpl
import net.ktnx.mobileledger.domain.usecase.ConfigBackup
import net.ktnx.mobileledger.domain.usecase.ConfigBackupImpl
import net.ktnx.mobileledger.domain.usecase.DatabaseInitializer
import net.ktnx.mobileledger.domain.usecase.DatabaseInitializerImpl
import net.ktnx.mobileledger.domain.usecase.ProfilePersistence
import net.ktnx.mobileledger.domain.usecase.ProfilePersistenceImpl
import net.ktnx.mobileledger.domain.usecase.ProfileValidator
import net.ktnx.mobileledger.domain.usecase.ProfileValidatorImpl
import net.ktnx.mobileledger.domain.usecase.TemplateAccountRowManager
import net.ktnx.mobileledger.domain.usecase.TemplateAccountRowManagerImpl
import net.ktnx.mobileledger.domain.usecase.TemplateDataMapper
import net.ktnx.mobileledger.domain.usecase.TemplateDataMapperImpl
import net.ktnx.mobileledger.domain.usecase.TemplateMatcher
import net.ktnx.mobileledger.domain.usecase.TemplateMatcherImpl
import net.ktnx.mobileledger.domain.usecase.TemplatePatternValidator
import net.ktnx.mobileledger.domain.usecase.TemplatePatternValidatorImpl
import net.ktnx.mobileledger.domain.usecase.TransactionBalanceCalculator
import net.ktnx.mobileledger.domain.usecase.TransactionBalanceCalculatorImpl
import net.ktnx.mobileledger.domain.usecase.TransactionDateNavigator
import net.ktnx.mobileledger.domain.usecase.TransactionDateNavigatorImpl
import net.ktnx.mobileledger.domain.usecase.TransactionListConverter
import net.ktnx.mobileledger.domain.usecase.TransactionListConverterImpl
import net.ktnx.mobileledger.domain.usecase.TransactionSender
import net.ktnx.mobileledger.domain.usecase.TransactionSenderImpl
import net.ktnx.mobileledger.domain.usecase.TransactionSyncer
import net.ktnx.mobileledger.domain.usecase.TransactionSyncerImpl
import net.ktnx.mobileledger.domain.usecase.VersionDetector
import net.ktnx.mobileledger.domain.usecase.VersionDetectorImpl

/**
 * Hilt module for providing use case dependencies.
 *
 * This module binds interface implementations for use cases used in ViewModels.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class UseCaseModule {

    @Binds
    @Singleton
    abstract fun bindTransactionSender(impl: TransactionSenderImpl): TransactionSender

    @Binds
    @Singleton
    abstract fun bindTransactionSyncer(impl: TransactionSyncerImpl): TransactionSyncer

    @Binds
    @Singleton
    abstract fun bindConfigBackup(impl: ConfigBackupImpl): ConfigBackup

    @Binds
    @Singleton
    abstract fun bindDatabaseInitializer(impl: DatabaseInitializerImpl): DatabaseInitializer

    @Binds
    @Singleton
    abstract fun bindVersionDetector(impl: VersionDetectorImpl): VersionDetector

    @Binds
    @Singleton
    abstract fun bindTransactionBalanceCalculator(impl: TransactionBalanceCalculatorImpl): TransactionBalanceCalculator

    @Binds
    @Singleton
    abstract fun bindProfileValidator(impl: ProfileValidatorImpl): ProfileValidator

    @Binds
    @Singleton
    abstract fun bindProfilePersistence(impl: ProfilePersistenceImpl): ProfilePersistence

    @Binds
    @Singleton
    abstract fun bindTemplateMatcher(impl: TemplateMatcherImpl): TemplateMatcher

    @Binds
    @Singleton
    abstract fun bindTemplatePatternValidator(impl: TemplatePatternValidatorImpl): TemplatePatternValidator

    @Binds
    @Singleton
    abstract fun bindTransactionListConverter(impl: TransactionListConverterImpl): TransactionListConverter

    @Binds
    @Singleton
    abstract fun bindAccountHierarchyResolver(impl: AccountHierarchyResolverImpl): AccountHierarchyResolver

    @Binds
    @Singleton
    abstract fun bindTransactionDateNavigator(impl: TransactionDateNavigatorImpl): TransactionDateNavigator

    @Binds
    @Singleton
    abstract fun bindTemplateAccountRowManager(impl: TemplateAccountRowManagerImpl): TemplateAccountRowManager

    @Binds
    @Singleton
    abstract fun bindTemplateDataMapper(impl: TemplateDataMapperImpl): TemplateDataMapper
}
