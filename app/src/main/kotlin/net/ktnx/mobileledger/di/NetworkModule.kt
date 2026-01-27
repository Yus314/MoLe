/*
 * Copyright © 2025 Damyan Ivanov.
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
import io.ktor.client.HttpClient
import javax.inject.Singleton
import net.ktnx.mobileledger.core.network.HledgerClient
import net.ktnx.mobileledger.core.network.HledgerClientImpl
import net.ktnx.mobileledger.core.network.KtorClientFactory

/**
 * Hilt module for providing network-related dependencies.
 *
 * This module provides the Ktor HttpClient and HledgerClient for
 * communicating with hledger-web servers.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient = KtorClientFactory.create()

    @Provides
    @Singleton
    fun provideHledgerClient(httpClient: HttpClient): HledgerClient = HledgerClientImpl(httpClient)
}
