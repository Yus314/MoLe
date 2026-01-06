/*
 * Copyright © 2024 Damyan Ivanov.
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

package net.ktnx.mobileledger.json.common

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule

/**
 * Singleton provider for Jackson ObjectMapper configured with KotlinModule.
 *
 * This centralizes ObjectMapper configuration and ensures consistent
 * serialization/deserialization behavior across all JSON parser versions.
 */
object ObjectMapperProvider {
    /**
     * Pre-configured ObjectMapper instance with Kotlin support.
     * This instance is thread-safe and can be reused across the application.
     */
    val objectMapper: ObjectMapper by lazy {
        ObjectMapper().apply {
            registerModule(KotlinModule.Builder().build())
        }
    }
}
