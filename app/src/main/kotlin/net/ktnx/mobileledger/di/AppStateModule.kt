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
import net.ktnx.mobileledger.model.AppStateManager

/**
 * Hilt module providing the global [AppStateManager] singleton.
 *
 * This module provides the [AppStateManager] Kotlin object to enable dependency
 * injection while maintaining backward compatibility with existing code.
 *
 * ## Provided Dependencies
 *
 * - [AppStateManager] - The global application UI/state holder (singleton)
 *   - Holds observable state for background tasks
 *   - Contains drawer open/closed state
 *   - Contains currency formatting preferences
 *   - Contains locale settings
 *   - Provides access to update status information
 *
 * ## Migration Note (008-data-layer-repository)
 *
 * Profile-related state has been moved to ProfileRepository:
 * - `AppStateManager.profiles` -> `ProfileRepository.getAllProfiles()`
 * - `AppStateManager.getProfile()` -> `ProfileRepository.currentProfile`
 *
 * ViewModels should inject ProfileRepository for profile data access.
 *
 * ## Usage
 *
 * ViewModels can request [AppStateManager] via constructor injection:
 *
 * ```kotlin
 * @HiltViewModel
 * class MyViewModel @Inject constructor(
 *     private val appStateManager: AppStateManager
 * ) : ViewModel() {
 *     fun isBackgroundTaskRunning() = appStateManager.backgroundTasksRunning.value
 * }
 * ```
 */
@Module
@InstallIn(SingletonComponent::class)
object AppStateModule {

    @Provides
    @Singleton
    fun provideAppStateManager(): AppStateManager = AppStateManager
}
