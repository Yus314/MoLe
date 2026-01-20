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

package net.ktnx.mobileledger.fake

import net.ktnx.mobileledger.domain.usecase.ProfileValidator

/**
 * Fake implementation of [ProfileValidator] for testing.
 *
 * Provides controllable behavior for tests:
 * - Configure validation result via [validationResult]
 * - Configure URL validation result via [urlValidationError]
 * - Configure auth validation result via [authValidationErrors]
 * - Track method calls via [validateCallCount], [lastData]
 * - Reset state between tests via [reset]
 */
class FakeProfileValidator : ProfileValidator {

    /**
     * Custom validation result to return. If null, returns Success.
     */
    var validationResult: ProfileValidator.ValidationResult? = null

    /**
     * URL validation error to return. If null, URL is valid.
     */
    var urlValidationError: String? = null

    /**
     * Authentication validation errors to return.
     */
    var authValidationErrors: Map<ProfileValidator.Field, String> = emptyMap()

    /**
     * Number of times [validate] was called.
     */
    var validateCallCount = 0
        private set

    /**
     * Number of times [validateUrl] was called.
     */
    var validateUrlCallCount = 0
        private set

    /**
     * Number of times [validateAuthentication] was called.
     */
    var validateAuthCallCount = 0
        private set

    /**
     * The data passed to the last [validate] call.
     */
    var lastData: ProfileValidator.ProfileData? = null
        private set

    /**
     * The URL passed to the last [validateUrl] call.
     */
    var lastUrl: String? = null
        private set

    override fun validate(data: ProfileValidator.ProfileData): ProfileValidator.ValidationResult {
        validateCallCount++
        lastData = data
        return validationResult ?: ProfileValidator.ValidationResult.Success
    }

    override fun validateUrl(url: String): String? {
        validateUrlCallCount++
        lastUrl = url
        return urlValidationError
    }

    override fun validateAuthentication(
        user: String,
        password: String,
        authRequired: Boolean
    ): Map<ProfileValidator.Field, String> {
        validateAuthCallCount++
        return authValidationErrors
    }

    /**
     * Reset all state to initial values.
     */
    fun reset() {
        validationResult = null
        urlValidationError = null
        authValidationErrors = emptyMap()
        validateCallCount = 0
        validateUrlCallCount = 0
        validateAuthCallCount = 0
        lastData = null
        lastUrl = null
    }

    /**
     * Configure a failure validation result with the given errors.
     */
    fun setValidationErrors(vararg errors: Pair<ProfileValidator.Field, String>) {
        validationResult = ProfileValidator.ValidationResult.Failure(errors.toMap())
    }
}
