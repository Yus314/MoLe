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

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import net.ktnx.mobileledger.model.Data

/**
 * Hilt module providing the global [Data] singleton.
 *
 * This module provides the existing [Data] Kotlin object to enable dependency
 * injection while maintaining backward compatibility with existing code.
 *
 * ## Provided Dependencies
 *
 * - [Data] - The global application state holder (singleton)
 *   - Contains the currently selected profile
 *   - Holds observable state for background tasks
 *   - Provides access to transaction counts and update status
 *
 * ## Usage
 *
 * ViewModels can request [Data] via constructor injection:
 *
 * ```kotlin
 * @HiltViewModel
 * class MyViewModel @Inject constructor(
 *     private val data: Data
 * ) : ViewModel() {
 *     fun getProfile() = data.getProfile()
 * }
 * ```
 *
 * ## Note
 *
 * The [Data] object has static initialization that accesses the database.
 * For unit tests that don't require the full Android environment, consider
 * using instrumentation tests where [TestDatabaseModule] provides an
 * in-memory database.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideData(): Data = Data
}
