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
import net.ktnx.mobileledger.domain.usecase.AccountSuggestionLookup
import net.ktnx.mobileledger.domain.usecase.AccountSuggestionLookupImpl
import net.ktnx.mobileledger.domain.usecase.ConfigBackup
import net.ktnx.mobileledger.domain.usecase.ConfigBackupImpl
import net.ktnx.mobileledger.domain.usecase.DatabaseInitializer
import net.ktnx.mobileledger.domain.usecase.DatabaseInitializerImpl
import net.ktnx.mobileledger.domain.usecase.DeleteCurrencyUseCase
import net.ktnx.mobileledger.domain.usecase.DeleteCurrencyUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.DeleteTemplateUseCase
import net.ktnx.mobileledger.domain.usecase.DeleteTemplateUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.DuplicateTemplateUseCase
import net.ktnx.mobileledger.domain.usecase.DuplicateTemplateUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.GetAccountsWithAmountsUseCase
import net.ktnx.mobileledger.domain.usecase.GetAccountsWithAmountsUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.GetAllCurrenciesUseCase
import net.ktnx.mobileledger.domain.usecase.GetAllCurrenciesUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.GetAllProfilesUseCase
import net.ktnx.mobileledger.domain.usecase.GetAllProfilesUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.GetAllTemplatesUseCase
import net.ktnx.mobileledger.domain.usecase.GetAllTemplatesUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.GetFirstTransactionByDescriptionUseCase
import net.ktnx.mobileledger.domain.usecase.GetFirstTransactionByDescriptionUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.GetLastSyncTimestampUseCase
import net.ktnx.mobileledger.domain.usecase.GetLastSyncTimestampUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.GetProfileByIdUseCase
import net.ktnx.mobileledger.domain.usecase.GetProfileByIdUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.GetShowZeroBalanceUseCase
import net.ktnx.mobileledger.domain.usecase.GetShowZeroBalanceUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.GetStartupProfileIdUseCase
import net.ktnx.mobileledger.domain.usecase.GetStartupProfileIdUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.GetStartupThemeUseCase
import net.ktnx.mobileledger.domain.usecase.GetStartupThemeUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.GetTemplateUseCase
import net.ktnx.mobileledger.domain.usecase.GetTemplateUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.GetTransactionByIdUseCase
import net.ktnx.mobileledger.domain.usecase.GetTransactionByIdUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.GetTransactionsUseCase
import net.ktnx.mobileledger.domain.usecase.GetTransactionsUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.ObserveAccountsWithAmountsUseCase
import net.ktnx.mobileledger.domain.usecase.ObserveAccountsWithAmountsUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.ObserveCurrentProfileUseCase
import net.ktnx.mobileledger.domain.usecase.ObserveCurrentProfileUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.ObserveProfilesUseCase
import net.ktnx.mobileledger.domain.usecase.ObserveProfilesUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.ObserveTemplatesUseCase
import net.ktnx.mobileledger.domain.usecase.ObserveTemplatesUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.ObserveTransactionsUseCase
import net.ktnx.mobileledger.domain.usecase.ObserveTransactionsUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.ProfilePersistence
import net.ktnx.mobileledger.domain.usecase.ProfilePersistenceImpl
import net.ktnx.mobileledger.domain.usecase.ProfileValidator
import net.ktnx.mobileledger.domain.usecase.ProfileValidatorImpl
import net.ktnx.mobileledger.domain.usecase.SaveCurrencyUseCase
import net.ktnx.mobileledger.domain.usecase.SaveCurrencyUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.SaveTemplateUseCase
import net.ktnx.mobileledger.domain.usecase.SaveTemplateUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.SearchAccountNamesUseCase
import net.ktnx.mobileledger.domain.usecase.SearchAccountNamesUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.SearchTransactionDescriptionsUseCase
import net.ktnx.mobileledger.domain.usecase.SearchTransactionDescriptionsUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.SetCurrentProfileUseCase
import net.ktnx.mobileledger.domain.usecase.SetCurrentProfileUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.SetLastSyncTimestampUseCase
import net.ktnx.mobileledger.domain.usecase.SetLastSyncTimestampUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.SetShowZeroBalanceUseCase
import net.ktnx.mobileledger.domain.usecase.SetShowZeroBalanceUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.SetStartupProfileIdUseCase
import net.ktnx.mobileledger.domain.usecase.SetStartupProfileIdUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.SetStartupThemeUseCase
import net.ktnx.mobileledger.domain.usecase.SetStartupThemeUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.StoreTransactionUseCase
import net.ktnx.mobileledger.domain.usecase.StoreTransactionUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.TemplateAccountRowManager
import net.ktnx.mobileledger.domain.usecase.TemplateAccountRowManagerImpl
import net.ktnx.mobileledger.domain.usecase.TemplateDataMapper
import net.ktnx.mobileledger.domain.usecase.TemplateDataMapperImpl
import net.ktnx.mobileledger.domain.usecase.TemplateMatcher
import net.ktnx.mobileledger.domain.usecase.TemplateMatcherImpl
import net.ktnx.mobileledger.domain.usecase.TemplatePatternValidator
import net.ktnx.mobileledger.domain.usecase.TemplatePatternValidatorImpl
import net.ktnx.mobileledger.domain.usecase.TransactionAccountRowManager
import net.ktnx.mobileledger.domain.usecase.TransactionAccountRowManagerImpl
import net.ktnx.mobileledger.domain.usecase.TransactionBalanceCalculator
import net.ktnx.mobileledger.domain.usecase.TransactionBalanceCalculatorImpl
import net.ktnx.mobileledger.domain.usecase.TransactionDateNavigator
import net.ktnx.mobileledger.domain.usecase.TransactionDateNavigatorImpl
import net.ktnx.mobileledger.domain.usecase.TransactionListConverter
import net.ktnx.mobileledger.domain.usecase.TransactionListConverterImpl
import net.ktnx.mobileledger.domain.usecase.TransactionSender
import net.ktnx.mobileledger.domain.usecase.TransactionSenderImpl
import net.ktnx.mobileledger.domain.usecase.UpdateProfileOrderUseCase
import net.ktnx.mobileledger.domain.usecase.UpdateProfileOrderUseCaseImpl
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

    @Binds
    @Singleton
    abstract fun bindTransactionAccountRowManager(impl: TransactionAccountRowManagerImpl): TransactionAccountRowManager

    @Binds
    @Singleton
    abstract fun bindAccountSuggestionLookup(impl: AccountSuggestionLookupImpl): AccountSuggestionLookup

    @Binds
    @Singleton
    abstract fun bindObserveProfilesUseCase(impl: ObserveProfilesUseCaseImpl): ObserveProfilesUseCase

    @Binds
    @Singleton
    abstract fun bindObserveCurrentProfileUseCase(impl: ObserveCurrentProfileUseCaseImpl): ObserveCurrentProfileUseCase

    @Binds
    @Singleton
    abstract fun bindGetProfileByIdUseCase(impl: GetProfileByIdUseCaseImpl): GetProfileByIdUseCase

    @Binds
    @Singleton
    abstract fun bindGetAllProfilesUseCase(impl: GetAllProfilesUseCaseImpl): GetAllProfilesUseCase

    @Binds
    @Singleton
    abstract fun bindSetCurrentProfileUseCase(impl: SetCurrentProfileUseCaseImpl): SetCurrentProfileUseCase

    @Binds
    @Singleton
    abstract fun bindUpdateProfileOrderUseCase(impl: UpdateProfileOrderUseCaseImpl): UpdateProfileOrderUseCase

    @Binds
    @Singleton
    abstract fun bindGetStartupThemeUseCase(impl: GetStartupThemeUseCaseImpl): GetStartupThemeUseCase

    @Binds
    @Singleton
    abstract fun bindGetStartupProfileIdUseCase(impl: GetStartupProfileIdUseCaseImpl): GetStartupProfileIdUseCase

    @Binds
    @Singleton
    abstract fun bindSetStartupThemeUseCase(impl: SetStartupThemeUseCaseImpl): SetStartupThemeUseCase

    @Binds
    @Singleton
    abstract fun bindSetStartupProfileIdUseCase(impl: SetStartupProfileIdUseCaseImpl): SetStartupProfileIdUseCase

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
    abstract fun bindGetAllCurrenciesUseCase(impl: GetAllCurrenciesUseCaseImpl): GetAllCurrenciesUseCase

    @Binds
    @Singleton
    abstract fun bindSaveCurrencyUseCase(impl: SaveCurrencyUseCaseImpl): SaveCurrencyUseCase

    @Binds
    @Singleton
    abstract fun bindDeleteCurrencyUseCase(impl: DeleteCurrencyUseCaseImpl): DeleteCurrencyUseCase

    @Binds
    @Singleton
    abstract fun bindObserveTransactionsUseCase(impl: ObserveTransactionsUseCaseImpl): ObserveTransactionsUseCase

    @Binds
    @Singleton
    abstract fun bindGetTransactionsUseCase(impl: GetTransactionsUseCaseImpl): GetTransactionsUseCase

    @Binds
    @Singleton
    abstract fun bindSearchTransactionDescriptionsUseCase(
        impl: SearchTransactionDescriptionsUseCaseImpl
    ): SearchTransactionDescriptionsUseCase

    @Binds
    @Singleton
    abstract fun bindStoreTransactionUseCase(impl: StoreTransactionUseCaseImpl): StoreTransactionUseCase

    @Binds
    @Singleton
    abstract fun bindGetTransactionByIdUseCase(impl: GetTransactionByIdUseCaseImpl): GetTransactionByIdUseCase

    @Binds
    @Singleton
    abstract fun bindGetFirstTransactionByDescriptionUseCase(
        impl: GetFirstTransactionByDescriptionUseCaseImpl
    ): GetFirstTransactionByDescriptionUseCase

    @Binds
    @Singleton
    abstract fun bindSearchAccountNamesUseCase(impl: SearchAccountNamesUseCaseImpl): SearchAccountNamesUseCase

    @Binds
    @Singleton
    abstract fun bindGetLastSyncTimestampUseCase(impl: GetLastSyncTimestampUseCaseImpl): GetLastSyncTimestampUseCase

    @Binds
    @Singleton
    abstract fun bindSetLastSyncTimestampUseCase(impl: SetLastSyncTimestampUseCaseImpl): SetLastSyncTimestampUseCase

    @Binds
    @Singleton
    abstract fun bindObserveTemplatesUseCase(impl: ObserveTemplatesUseCaseImpl): ObserveTemplatesUseCase

    @Binds
    @Singleton
    abstract fun bindGetTemplateUseCase(impl: GetTemplateUseCaseImpl): GetTemplateUseCase

    @Binds
    @Singleton
    abstract fun bindGetAllTemplatesUseCase(impl: GetAllTemplatesUseCaseImpl): GetAllTemplatesUseCase

    @Binds
    @Singleton
    abstract fun bindSaveTemplateUseCase(impl: SaveTemplateUseCaseImpl): SaveTemplateUseCase

    @Binds
    @Singleton
    abstract fun bindDeleteTemplateUseCase(impl: DeleteTemplateUseCaseImpl): DeleteTemplateUseCase

    @Binds
    @Singleton
    abstract fun bindDuplicateTemplateUseCase(impl: DuplicateTemplateUseCaseImpl): DuplicateTemplateUseCase
}
