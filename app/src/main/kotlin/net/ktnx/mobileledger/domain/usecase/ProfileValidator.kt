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

package net.ktnx.mobileledger.domain.usecase

/**
 * UseCase for validating profile data.
 *
 * Provides centralized validation logic for profile forms, including:
 * - Name validation
 * - URL format validation
 * - Authentication credentials validation
 */
interface ProfileValidator {

    /**
     * Input data for profile validation.
     */
    data class ProfileData(
        val name: String,
        val url: String,
        val useAuthentication: Boolean,
        val authUser: String,
        val authPassword: String
    )

    /**
     * Fields that can have validation errors.
     */
    enum class Field {
        NAME,
        URL,
        AUTH_USER,
        AUTH_PASSWORD
    }

    /**
     * Result of profile validation.
     */
    sealed class ValidationResult {
        data object Success : ValidationResult()
        data class Failure(val fieldErrors: Map<Field, String>) : ValidationResult()

        val isValid: Boolean
            get() = this is Success

        val errors: Map<Field, String>
            get() = when (this) {
                is Success -> emptyMap()
                is Failure -> this.fieldErrors
            }
    }

    /**
     * Validate all profile fields.
     *
     * @param data The profile data to validate
     * @return [ValidationResult.Success] if valid, [ValidationResult.Failure] with error map otherwise
     */
    fun validate(data: ProfileData): ValidationResult

    /**
     * Validate URL format only.
     *
     * @param url The URL to validate
     * @return Error message if invalid, null if valid
     */
    fun validateUrl(url: String): String?

    /**
     * Validate authentication credentials.
     *
     * @param user The username
     * @param password The password
     * @param authRequired Whether authentication is required
     * @return Map of field to error message for any invalid fields
     */
    fun validateAuthentication(user: String, password: String, authRequired: Boolean): Map<Field, String>
}
