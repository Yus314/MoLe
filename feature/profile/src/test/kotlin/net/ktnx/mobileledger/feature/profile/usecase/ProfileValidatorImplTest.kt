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

package net.ktnx.mobileledger.feature.profile.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [ProfileValidatorImpl].
 */
class ProfileValidatorImplTest {

    private lateinit var validator: ProfileValidatorImpl

    @Before
    fun setup() {
        validator = ProfileValidatorImpl()
    }

    private fun data(
        name: String = "Test Profile",
        url: String = "https://example.com",
        useAuthentication: Boolean = false,
        authUser: String = "",
        authPassword: String = ""
    ) = ProfileValidator.ProfileData(
        name = name,
        url = url,
        useAuthentication = useAuthentication,
        authUser = authUser,
        authPassword = authPassword
    )

    // ========== validate() tests ==========

    @Test
    fun `validate returns success for valid data without auth`() {
        val result = validator.validate(data())

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate returns success for valid data with auth`() {
        val result = validator.validate(
            data(
                useAuthentication = true,
                authUser = "user",
                authPassword = "pass"
            )
        )

        assertTrue(result.isValid)
    }

    @Test
    fun `validate returns failure for blank name`() {
        val result = validator.validate(data(name = ""))

        assertFalse(result.isValid)
        assertTrue(result.errors.containsKey(ProfileValidator.Field.NAME))
        assertEquals(ProfileValidatorImpl.ERROR_NAME_REQUIRED, result.errors[ProfileValidator.Field.NAME])
    }

    @Test
    fun `validate returns failure for whitespace-only name`() {
        val result = validator.validate(data(name = "   "))

        assertFalse(result.isValid)
        assertTrue(result.errors.containsKey(ProfileValidator.Field.NAME))
    }

    @Test
    fun `validate returns failure for blank url`() {
        val result = validator.validate(data(url = ""))

        assertFalse(result.isValid)
        assertTrue(result.errors.containsKey(ProfileValidator.Field.URL))
        assertEquals(ProfileValidatorImpl.ERROR_URL_REQUIRED, result.errors[ProfileValidator.Field.URL])
    }

    @Test
    fun `validate returns failure for invalid url`() {
        val result = validator.validate(data(url = "not-a-url"))

        assertFalse(result.isValid)
        assertTrue(result.errors.containsKey(ProfileValidator.Field.URL))
        assertEquals(ProfileValidatorImpl.ERROR_URL_INVALID, result.errors[ProfileValidator.Field.URL])
    }

    @Test
    fun `validate returns failure for missing auth user when auth enabled`() {
        val result = validator.validate(
            data(
                useAuthentication = true,
                authUser = "",
                authPassword = "pass"
            )
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.containsKey(ProfileValidator.Field.AUTH_USER))
        assertEquals(ProfileValidatorImpl.ERROR_AUTH_USER_REQUIRED, result.errors[ProfileValidator.Field.AUTH_USER])
    }

    @Test
    fun `validate returns failure for missing auth password when auth enabled`() {
        val result = validator.validate(
            data(
                useAuthentication = true,
                authUser = "user",
                authPassword = ""
            )
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.containsKey(ProfileValidator.Field.AUTH_PASSWORD))
        assertEquals(
            ProfileValidatorImpl.ERROR_AUTH_PASSWORD_REQUIRED,
            result.errors[ProfileValidator.Field.AUTH_PASSWORD]
        )
    }

    @Test
    fun `validate ignores auth fields when auth disabled`() {
        val result = validator.validate(
            data(
                useAuthentication = false,
                authUser = "",
                authPassword = ""
            )
        )

        assertTrue(result.isValid)
    }

    @Test
    fun `validate returns multiple errors for multiple invalid fields`() {
        val result = validator.validate(
            data(
                name = "",
                url = "",
                useAuthentication = true,
                authUser = "",
                authPassword = ""
            )
        )

        assertFalse(result.isValid)
        assertEquals(4, result.errors.size)
        assertTrue(result.errors.containsKey(ProfileValidator.Field.NAME))
        assertTrue(result.errors.containsKey(ProfileValidator.Field.URL))
        assertTrue(result.errors.containsKey(ProfileValidator.Field.AUTH_USER))
        assertTrue(result.errors.containsKey(ProfileValidator.Field.AUTH_PASSWORD))
    }

    // ========== validateUrl() tests ==========

    @Test
    fun `validateUrl returns null for valid https url`() {
        val result = validator.validateUrl("https://example.com")
        assertNull(result)
    }

    @Test
    fun `validateUrl returns null for valid http url`() {
        val result = validator.validateUrl("http://example.com")
        assertNull(result)
    }

    @Test
    fun `validateUrl returns null for url with path`() {
        val result = validator.validateUrl("https://example.com/api/v1")
        assertNull(result)
    }

    @Test
    fun `validateUrl returns null for url with port`() {
        val result = validator.validateUrl("https://example.com:8080")
        assertNull(result)
    }

    @Test
    fun `validateUrl returns error for blank url`() {
        val result = validator.validateUrl("")
        assertEquals(ProfileValidatorImpl.ERROR_URL_REQUIRED, result)
    }

    @Test
    fun `validateUrl returns error for malformed url`() {
        val result = validator.validateUrl("not a valid url")
        assertEquals(ProfileValidatorImpl.ERROR_URL_INVALID, result)
    }

    @Test
    fun `validateUrl returns error for ftp protocol`() {
        val result = validator.validateUrl("ftp://example.com")
        assertEquals(ProfileValidatorImpl.ERROR_URL_INVALID, result)
    }

    @Test
    fun `validateUrl returns error for file protocol`() {
        val result = validator.validateUrl("file:///etc/passwd")
        assertEquals(ProfileValidatorImpl.ERROR_URL_INVALID, result)
    }

    @Test
    fun `validateUrl returns error for url without host`() {
        val result = validator.validateUrl("https://")
        assertEquals(ProfileValidatorImpl.ERROR_URL_INVALID, result)
    }

    // ========== validateAuthentication() tests ==========

    @Test
    fun `validateAuthentication returns empty map when auth not required`() {
        val result = validator.validateAuthentication(
            user = "",
            password = "",
            authRequired = false
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `validateAuthentication returns empty map for valid credentials`() {
        val result = validator.validateAuthentication(
            user = "testuser",
            password = "testpass",
            authRequired = true
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `validateAuthentication returns error for blank user`() {
        val result = validator.validateAuthentication(
            user = "",
            password = "testpass",
            authRequired = true
        )

        assertEquals(1, result.size)
        assertTrue(result.containsKey(ProfileValidator.Field.AUTH_USER))
    }

    @Test
    fun `validateAuthentication returns error for blank password`() {
        val result = validator.validateAuthentication(
            user = "testuser",
            password = "",
            authRequired = true
        )

        assertEquals(1, result.size)
        assertTrue(result.containsKey(ProfileValidator.Field.AUTH_PASSWORD))
    }

    @Test
    fun `validateAuthentication returns errors for both blank user and password`() {
        val result = validator.validateAuthentication(
            user = "",
            password = "",
            authRequired = true
        )

        assertEquals(2, result.size)
        assertTrue(result.containsKey(ProfileValidator.Field.AUTH_USER))
        assertTrue(result.containsKey(ProfileValidator.Field.AUTH_PASSWORD))
    }

    @Test
    fun `validateAuthentication handles whitespace-only user`() {
        val result = validator.validateAuthentication(
            user = "   ",
            password = "testpass",
            authRequired = true
        )

        assertEquals(1, result.size)
        assertTrue(result.containsKey(ProfileValidator.Field.AUTH_USER))
    }
}
