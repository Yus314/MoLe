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
import net.ktnx.mobileledger.domain.usecase.TemplateAccountRowManager
import net.ktnx.mobileledger.domain.usecase.TemplateAccountRowManagerImpl
import net.ktnx.mobileledger.domain.usecase.TemplateDataMapper
import net.ktnx.mobileledger.domain.usecase.TemplateDataMapperImpl

/**
 * Hilt module for template-related use cases that depend on app UI types.
 *
 * Note: Pure CRUD use cases (Observe, Get, Save, Delete, Duplicate) are in
 * feature:templates module (TemplatesModule).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class TemplateUseCaseModule {

    @Binds
    @Singleton
    abstract fun bindTemplateAccountRowManager(impl: TemplateAccountRowManagerImpl): TemplateAccountRowManager

    @Binds
    @Singleton
    abstract fun bindTemplateDataMapper(impl: TemplateDataMapperImpl): TemplateDataMapper
}
