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

import java.net.MalformedURLException
import java.net.URL
import javax.inject.Inject

/**
 * Implementation of [ProfileValidator].
 *
 * Provides validation logic for profile data including:
 * - Name: must not be blank
 * - URL: must be a valid HTTP/HTTPS URL
 * - Authentication: user/password required when auth is enabled
 */
class ProfileValidatorImpl @Inject constructor() : ProfileValidator {

    companion object {
        // Error messages (Japanese)
        const val ERROR_NAME_REQUIRED = "プロファイル名は必須です"
        const val ERROR_URL_REQUIRED = "URLは必須です"
        const val ERROR_URL_INVALID = "無効なURLです"
        const val ERROR_AUTH_USER_REQUIRED = "ユーザー名は必須です"
        const val ERROR_AUTH_PASSWORD_REQUIRED = "パスワードは必須です"
    }

    override fun validate(data: ProfileValidator.ProfileData): ProfileValidator.ValidationResult {
        val errors = mutableMapOf<ProfileValidator.Field, String>()

        // Validate name
        if (data.name.isBlank()) {
            errors[ProfileValidator.Field.NAME] = ERROR_NAME_REQUIRED
        }

        // Validate URL
        validateUrl(data.url)?.let { urlError ->
            errors[ProfileValidator.Field.URL] = urlError
        }

        // Validate authentication
        errors.putAll(validateAuthentication(data.authUser, data.authPassword, data.useAuthentication))

        return if (errors.isEmpty()) {
            ProfileValidator.ValidationResult.Success
        } else {
            ProfileValidator.ValidationResult.Failure(errors)
        }
    }

    override fun validateUrl(url: String): String? {
        if (url.isBlank()) {
            return ERROR_URL_REQUIRED
        }

        return try {
            val parsedUrl = URL(url)
            val host = parsedUrl.host
            if (host.isNullOrEmpty()) {
                return ERROR_URL_INVALID
            }

            val protocol = parsedUrl.protocol.uppercase()
            if (protocol != "HTTP" && protocol != "HTTPS") {
                return ERROR_URL_INVALID
            }

            null // Valid URL
        } catch (e: MalformedURLException) {
            ERROR_URL_INVALID
        }
    }

    override fun validateAuthentication(
        user: String,
        password: String,
        authRequired: Boolean
    ): Map<ProfileValidator.Field, String> {
        if (!authRequired) {
            return emptyMap()
        }

        val errors = mutableMapOf<ProfileValidator.Field, String>()

        if (user.isBlank()) {
            errors[ProfileValidator.Field.AUTH_USER] = ERROR_AUTH_USER_REQUIRED
        }

        if (password.isBlank()) {
            errors[ProfileValidator.Field.AUTH_PASSWORD] = ERROR_AUTH_PASSWORD_REQUIRED
        }

        return errors
    }
}
