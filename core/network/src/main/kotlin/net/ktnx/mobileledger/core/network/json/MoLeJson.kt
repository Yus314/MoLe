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

package net.ktnx.mobileledger.core.network.json

import kotlinx.serialization.json.Json

/**
 * Global Json instance configured for MoLe's JSON serialization needs.
 *
 * Configuration:
 * - ignoreUnknownKeys: Allows forward compatibility with newer API versions
 * - isLenient: Tolerates minor JSON format variations
 * - coerceInputValues: Handles null values for non-nullable types with defaults
 * - encodeDefaults: Only encodes non-default values for smaller output
 */
val MoLeJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    encodeDefaults = false
}
