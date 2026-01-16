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
import net.ktnx.mobileledger.service.CurrencyFormatter

/**
 * Hilt EntryPoint for accessing CurrencyFormatter from non-Hilt classes.
 *
 * This is used by JSON parsing classes that use companion object methods
 * and cannot receive dependencies via constructor injection.
 *
 * ## Usage
 *
 * Initialize once in App.onCreate():
 * ```kotlin
 * CurrencyFormatterEntryPoint.initialize(this)
 * ```
 *
 * Then access from static contexts:
 * ```kotlin
 * val formatter = CurrencyFormatterEntryPoint.get()
 * val position = formatter.currencySymbolPosition.value
 * ```
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface CurrencyFormatterEntryPoint {
    fun currencyFormatter(): CurrencyFormatter

    companion object {
        @Volatile
        private var cachedFormatter: CurrencyFormatter? = null

        /**
         * Initialize the EntryPoint with application context.
         * Must be called once during app startup (e.g., in App.onCreate()).
         */
        fun initialize(context: Context) {
            if (cachedFormatter == null) {
                synchronized(this) {
                    if (cachedFormatter == null) {
                        cachedFormatter = EntryPointAccessors.fromApplication(
                            context.applicationContext,
                            CurrencyFormatterEntryPoint::class.java
                        ).currencyFormatter()
                    }
                }
            }
        }

        /**
         * Get the CurrencyFormatter instance.
         * @throws IllegalStateException if not initialized
         */
        fun get(): CurrencyFormatter = cachedFormatter
            ?: throw IllegalStateException(
                "CurrencyFormatterEntryPoint not initialized. Call initialize() in App.onCreate()"
            )

        /**
         * Get the CurrencyFormatter instance, or null if not initialized.
         * Useful for fallback behavior in tests.
         */
        fun getOrNull(): CurrencyFormatter? = cachedFormatter
    }
}
