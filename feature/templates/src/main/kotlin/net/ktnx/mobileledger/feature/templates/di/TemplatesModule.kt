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

package net.ktnx.mobileledger.feature.templates.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import net.ktnx.mobileledger.feature.templates.usecase.DeleteTemplateUseCase
import net.ktnx.mobileledger.feature.templates.usecase.DeleteTemplateUseCaseImpl
import net.ktnx.mobileledger.feature.templates.usecase.DuplicateTemplateUseCase
import net.ktnx.mobileledger.feature.templates.usecase.DuplicateTemplateUseCaseImpl
import net.ktnx.mobileledger.feature.templates.usecase.GetAllTemplatesUseCase
import net.ktnx.mobileledger.feature.templates.usecase.GetAllTemplatesUseCaseImpl
import net.ktnx.mobileledger.feature.templates.usecase.GetTemplateUseCase
import net.ktnx.mobileledger.feature.templates.usecase.GetTemplateUseCaseImpl
import net.ktnx.mobileledger.feature.templates.usecase.ObserveTemplatesUseCase
import net.ktnx.mobileledger.feature.templates.usecase.ObserveTemplatesUseCaseImpl
import net.ktnx.mobileledger.feature.templates.usecase.SaveTemplateUseCase
import net.ktnx.mobileledger.feature.templates.usecase.SaveTemplateUseCaseImpl
import net.ktnx.mobileledger.feature.templates.usecase.TemplateMatcher
import net.ktnx.mobileledger.feature.templates.usecase.TemplateMatcherImpl
import net.ktnx.mobileledger.feature.templates.usecase.TemplatePatternValidator
import net.ktnx.mobileledger.feature.templates.usecase.TemplatePatternValidatorImpl

/**
 * Hilt module for providing template feature dependencies.
 *
 * Contains:
 * - CRUD use cases: Observe, Get, GetAll, Save, Delete, Duplicate
 * - Utility use cases: TemplateMatcher, TemplatePatternValidator
 *
 * Note: TemplateAccountRowManager and TemplateDataMapper remain in app
 * due to UI type dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class TemplatesModule {

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

    @Binds
    @Singleton
    abstract fun bindTemplateMatcher(impl: TemplateMatcherImpl): TemplateMatcher

    @Binds
    @Singleton
    abstract fun bindTemplatePatternValidator(impl: TemplatePatternValidatorImpl): TemplatePatternValidator
}
