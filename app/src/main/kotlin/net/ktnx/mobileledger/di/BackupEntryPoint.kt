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

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import net.ktnx.mobileledger.data.repository.CurrencyRepository
import net.ktnx.mobileledger.data.repository.PreferencesRepository
import net.ktnx.mobileledger.data.repository.ProfileRepository
import net.ktnx.mobileledger.data.repository.TemplateRepository
import net.ktnx.mobileledger.db.DB
import net.ktnx.mobileledger.service.ThemeService

/**
 * Hilt EntryPoint for accessing repositories from non-Hilt classes.
 *
 * This is used by backup/restore classes that cannot use constructor injection:
 * - MobileLedgerBackupAgent (Android BackupAgent)
 * - RawConfigWriter/RawConfigReader (utility classes)
 *
 * ## Usage
 *
 * ```kotlin
 * val entryPoint = BackupEntryPoint.get(context)
 * val profiles = entryPoint.profileRepository().getAllProfilesSync()
 * ```
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface BackupEntryPoint {
    fun profileRepository(): ProfileRepository
    fun templateRepository(): TemplateRepository
    fun currencyRepository(): CurrencyRepository
    fun preferencesRepository(): PreferencesRepository
    fun db(): DB
    fun themeService(): ThemeService

    @IoDispatcher
    fun ioDispatcher(): CoroutineDispatcher

    companion object {
        /**
         * Get the BackupEntryPoint from an Android Context.
         *
         * @param context Any Android context (Activity, Service, Application, etc.)
         * @return The BackupEntryPoint instance
         */
        fun get(context: Context): BackupEntryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            BackupEntryPoint::class.java
        )
    }
}
