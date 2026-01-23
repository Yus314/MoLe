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

package net.ktnx.mobileledger.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import java.util.concurrent.TimeUnit
import logcat.logcat
import net.ktnx.mobileledger.BuildConfig
import net.ktnx.mobileledger.json.MoLeJson

/**
 * Factory for creating configured Ktor HttpClient instances.
 *
 * This factory provides pre-configured clients for both production use
 * (with OkHttp engine) and testing (with MockEngine).
 */
object KtorClientFactory {

    private const val TIMEOUT_SECONDS = 30L
    private const val TIMEOUT_MS = TIMEOUT_SECONDS * 1000

    /**
     * Create a production HttpClient with OkHttp engine.
     *
     * @param enableLogging Whether to enable request/response logging (defaults to DEBUG builds)
     * @return Configured HttpClient instance
     */
    fun create(enableLogging: Boolean = BuildConfig.DEBUG): HttpClient = HttpClient(OkHttp) {
        engine {
            config {
                connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                followRedirects(false)
            }
        }

        install(ContentNegotiation) {
            json(MoLeJson)
        }

        if (enableLogging) {
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        logcat("KtorHttp") { message }
                    }
                }
                level = LogLevel.HEADERS
            }
        }

        install(HttpTimeout) {
            requestTimeoutMillis = TIMEOUT_MS
            connectTimeoutMillis = TIMEOUT_MS
            socketTimeoutMillis = TIMEOUT_MS
        }

        expectSuccess = false
    }

    /**
     * Create an HttpClient with a custom engine.
     *
     * Primarily used for testing with MockEngine.
     *
     * @param engine The HttpClientEngine to use
     * @param enableLogging Whether to enable request/response logging
     * @return Configured HttpClient instance
     */
    fun createWithEngine(engine: HttpClientEngine, enableLogging: Boolean = false): HttpClient = HttpClient(engine) {
        install(ContentNegotiation) {
            json(MoLeJson)
        }

        if (enableLogging) {
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        logcat("KtorHttp") { message }
                    }
                }
                level = LogLevel.HEADERS
            }
        }

        install(HttpTimeout) {
            requestTimeoutMillis = TIMEOUT_MS
            connectTimeoutMillis = TIMEOUT_MS
            socketTimeoutMillis = TIMEOUT_MS
        }

        expectSuccess = false
    }
}
