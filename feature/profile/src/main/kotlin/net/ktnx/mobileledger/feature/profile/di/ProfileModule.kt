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

package net.ktnx.mobileledger.feature.profile.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import net.ktnx.mobileledger.feature.profile.usecase.GetAllProfilesUseCase
import net.ktnx.mobileledger.feature.profile.usecase.GetAllProfilesUseCaseImpl
import net.ktnx.mobileledger.feature.profile.usecase.GetProfileByIdUseCase
import net.ktnx.mobileledger.feature.profile.usecase.GetProfileByIdUseCaseImpl
import net.ktnx.mobileledger.feature.profile.usecase.ObserveCurrentProfileUseCase
import net.ktnx.mobileledger.feature.profile.usecase.ObserveCurrentProfileUseCaseImpl
import net.ktnx.mobileledger.feature.profile.usecase.ObserveProfilesUseCase
import net.ktnx.mobileledger.feature.profile.usecase.ObserveProfilesUseCaseImpl
import net.ktnx.mobileledger.feature.profile.usecase.ProfileValidator
import net.ktnx.mobileledger.feature.profile.usecase.ProfileValidatorImpl
import net.ktnx.mobileledger.feature.profile.usecase.SetCurrentProfileUseCase
import net.ktnx.mobileledger.feature.profile.usecase.SetCurrentProfileUseCaseImpl
import net.ktnx.mobileledger.feature.profile.usecase.UpdateProfileOrderUseCase
import net.ktnx.mobileledger.feature.profile.usecase.UpdateProfileOrderUseCaseImpl

/**
 * Hilt module for providing profile feature dependencies.
 *
 * Contains:
 * - CRUD use cases: Observe, Get, GetAll, SetCurrent, UpdateOrder
 * - Utility use cases: ProfileValidator
 *
 * Note: ProfilePersistence remains in app due to AuthDataProvider dependency.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ProfileModule {

    @Binds
    @Singleton
    abstract fun bindObserveCurrentProfileUseCase(impl: ObserveCurrentProfileUseCaseImpl): ObserveCurrentProfileUseCase

    @Binds
    @Singleton
    abstract fun bindObserveProfilesUseCase(impl: ObserveProfilesUseCaseImpl): ObserveProfilesUseCase

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
    abstract fun bindProfileValidator(impl: ProfileValidatorImpl): ProfileValidator
}
