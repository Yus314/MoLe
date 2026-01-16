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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerVersionTest {

    @Test
    fun `displayString formats major and minor version`() {
        val version = ServerVersion(major = 1, minor = 19)

        assertEquals("1.19", version.displayString)
    }

    @Test
    fun `displayString formats zero versions`() {
        val version = ServerVersion(major = 0, minor = 0)

        assertEquals("0.0", version.displayString)
    }

    @Test
    fun `displayString formats double digit versions`() {
        val version = ServerVersion(major = 10, minor = 25)

        assertEquals("10.25", version.displayString)
    }

    @Test
    fun `isPre_1_19 defaults to false`() {
        val version = ServerVersion(major = 1, minor = 19)

        assertFalse(version.isPre_1_19)
    }

    @Test
    fun `isPre_1_19 can be set to true`() {
        val version = ServerVersion(major = 1, minor = 18, isPre_1_19 = true)

        assertTrue(version.isPre_1_19)
    }

    @Test
    fun `serverVersion with same values are equal`() {
        val version1 = ServerVersion(major = 1, minor = 19, isPre_1_19 = false)
        val version2 = ServerVersion(major = 1, minor = 19, isPre_1_19 = false)

        assertEquals(version1, version2)
    }

    @Test
    fun `serverVersion copy preserves values`() {
        val original = ServerVersion(major = 1, minor = 19, isPre_1_19 = true)

        val copied = original.copy(minor = 20)

        assertEquals(1, copied.major)
        assertEquals(20, copied.minor)
        assertTrue(copied.isPre_1_19)
    }
}
