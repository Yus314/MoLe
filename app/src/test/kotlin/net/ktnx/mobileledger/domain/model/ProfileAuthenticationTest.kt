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

package net.ktnx.mobileledger.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Unit tests for [ProfileAuthentication].
 */
class ProfileAuthenticationTest {

    @Test
    fun `properties stored correctly`() {
        val auth = ProfileAuthentication(user = "testuser", password = "testpass")

        assertEquals("testuser", auth.user)
        assertEquals("testpass", auth.password)
    }

    @Test
    fun `empty user and password allowed`() {
        val auth = ProfileAuthentication(user = "", password = "")

        assertEquals("", auth.user)
        assertEquals("", auth.password)
    }

    @Test
    fun `equals compares all fields`() {
        val auth1 = ProfileAuthentication("user", "pass")
        val auth2 = ProfileAuthentication("user", "pass")

        assertEquals(auth1, auth2)
    }

    @Test
    fun `different user not equal`() {
        val auth1 = ProfileAuthentication("user1", "pass")
        val auth2 = ProfileAuthentication("user2", "pass")

        assertNotEquals(auth1, auth2)
    }

    @Test
    fun `different password not equal`() {
        val auth1 = ProfileAuthentication("user", "pass1")
        val auth2 = ProfileAuthentication("user", "pass2")

        assertNotEquals(auth1, auth2)
    }

    @Test
    fun `copy preserves unchanged fields`() {
        val original = ProfileAuthentication("user", "pass")
        val modified = original.copy(password = "newpass")

        assertEquals("user", modified.user)
        assertEquals("newpass", modified.password)
    }

    @Test
    fun `hashCode is consistent`() {
        val auth1 = ProfileAuthentication("user", "pass")
        val auth2 = ProfileAuthentication("user", "pass")

        assertEquals(auth1.hashCode(), auth2.hashCode())
    }
}
