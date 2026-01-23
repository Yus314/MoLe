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
import net.ktnx.mobileledger.service.ThemeService

/**
 * Hilt EntryPoint for accessing ThemeService from non-Hilt classes.
 *
 * This is used by View classes and utility objects that cannot receive
 * dependencies via constructor injection.
 *
 * ## Usage
 *
 * Initialize once in App.onCreate():
 * ```kotlin
 * ThemeServiceEntryPoint.initialize(this)
 * ```
 *
 * Then access from static contexts:
 * ```kotlin
 * val themeService = ThemeServiceEntryPoint.get()
 * val color = themeService.getPrimaryColorForHue(hue)
 * ```
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ThemeServiceEntryPoint {
    fun themeService(): ThemeService

    companion object {
        @Volatile
        private var cachedService: ThemeService? = null

        /**
         * Initialize the EntryPoint with application context.
         * Must be called once during app startup (e.g., in App.onCreate()).
         */
        fun initialize(context: Context) {
            if (cachedService == null) {
                synchronized(this) {
                    if (cachedService == null) {
                        cachedService = EntryPointAccessors.fromApplication(
                            context.applicationContext,
                            ThemeServiceEntryPoint::class.java
                        ).themeService()
                    }
                }
            }
        }

        /**
         * Get the ThemeService instance.
         * @throws IllegalStateException if not initialized
         */
        fun get(): ThemeService = cachedService
            ?: throw IllegalStateException(
                "ThemeServiceEntryPoint not initialized. Call initialize() in App.onCreate()"
            )

        /**
         * Get the ThemeService instance, or null if not initialized.
         * Useful for fallback behavior in tests.
         */
        fun getOrNull(): ThemeService? = cachedService
    }
}
