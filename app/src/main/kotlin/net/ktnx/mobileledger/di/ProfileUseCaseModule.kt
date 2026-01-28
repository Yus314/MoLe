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
import net.ktnx.mobileledger.domain.usecase.ProfilePersistence
import net.ktnx.mobileledger.domain.usecase.ProfilePersistenceImpl

/**
 * Hilt module for profile-related use cases that depend on app services.
 *
 * Note: Pure CRUD use cases (Observe, Get, SetCurrent, UpdateOrder) are in
 * feature:profile module (ProfileModule).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ProfileUseCaseModule {

    @Binds
    @Singleton
    abstract fun bindProfilePersistence(impl: ProfilePersistenceImpl): ProfilePersistence
}
