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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerVersionTest {

    // ========================================
    // displayString tests
    // ========================================

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
    fun `displayString includes patch when present`() {
        val version = ServerVersion(major = 1, minor = 19, patch = 1)

        assertEquals("1.19.1", version.displayString)
    }

    @Test
    fun `displayString shows pre-legacy for isPre_1_20_1`() {
        val version = ServerVersion(isPre_1_20_1 = true)

        assertEquals("(before 1.20)", version.displayString)
    }

    // ========================================
    // isPre_1_20_1 tests
    // ========================================

    @Test
    fun `isPre_1_20_1 defaults to false`() {
        val version = ServerVersion(major = 1, minor = 19)

        assertFalse(version.isPre_1_20_1)
    }

    @Test
    fun `isPre_1_20_1 can be set to true`() {
        val version = ServerVersion(major = 1, minor = 18, isPre_1_20_1 = true)

        assertTrue(version.isPre_1_20_1)
    }

    // ========================================
    // atLeast tests
    // ========================================

    @Test
    fun `atLeast returns true for exact match`() {
        val version = ServerVersion(major = 1, minor = 19)

        assertTrue(version.atLeast(1, 19))
    }

    @Test
    fun `atLeast returns true for higher minor version`() {
        val version = ServerVersion(major = 1, minor = 25)

        assertTrue(version.atLeast(1, 19))
    }

    @Test
    fun `atLeast returns true for higher major version`() {
        val version = ServerVersion(major = 2, minor = 0)

        assertTrue(version.atLeast(1, 50))
    }

    @Test
    fun `atLeast returns false for lower minor version`() {
        val version = ServerVersion(major = 1, minor = 18)

        assertFalse(version.atLeast(1, 19))
    }

    @Test
    fun `atLeast returns false for lower major version`() {
        val version = ServerVersion(major = 0, minor = 99)

        assertFalse(version.atLeast(1, 14))
    }

    @Test
    fun `atLeast handles edge case major version 0`() {
        val version = ServerVersion(major = 0, minor = 5)

        assertTrue(version.atLeast(0, 5))
        assertFalse(version.atLeast(0, 6))
    }

    // ========================================
    // getSuitableApiVersion tests
    // Note: API enum requires Android context, so we test null cases only
    // ========================================

    @Test
    fun `getSuitableApiVersion returns null for pre-legacy`() {
        val version = ServerVersion(isPre_1_20_1 = true)

        assertNull(version.getSuitableApiVersion())
    }

    // Note: getSuitableApiVersion for supported versions (>=1.14) cannot be tested
    // in unit tests because the API enum requires Android context for initialization.
    // These tests would need to be in instrumentation tests.

    // ========================================
    // parse tests
    // ========================================

    @Test
    fun `parse returns ServerVersion for major_minor format`() {
        val result = ServerVersion.parse("1.19")

        assertNotNull(result)
        assertEquals(1, result!!.major)
        assertEquals(19, result.minor)
        assertNull(result.patch)
        assertFalse(result.isPre_1_20_1)
    }

    @Test
    fun `parse returns ServerVersion for major_minor_patch format`() {
        val result = ServerVersion.parse("1.19.1")

        assertNotNull(result)
        assertEquals(1, result!!.major)
        assertEquals(19, result.minor)
        assertEquals(1, result.patch)
        assertFalse(result.isPre_1_20_1)
    }

    @Test
    fun `parse returns preLegacy for pre-1_19 string`() {
        val result = ServerVersion.parse("pre-1.19")

        assertNotNull(result)
        assertTrue(result!!.isPre_1_20_1)
    }

    @Test
    fun `parse returns null for invalid format`() {
        assertNull(ServerVersion.parse("1"))
        assertNull(ServerVersion.parse("invalid"))
        assertNull(ServerVersion.parse(""))
        assertNull(ServerVersion.parse("a.b"))
        assertNull(ServerVersion.parse("1.x"))
    }

    @Test
    fun `parse handles zero versions`() {
        val result = ServerVersion.parse("0.0")

        assertNotNull(result)
        assertEquals(0, result!!.major)
        assertEquals(0, result.minor)
    }

    @Test
    fun `parse handles large version numbers`() {
        val result = ServerVersion.parse("10.100.50")

        assertNotNull(result)
        assertEquals(10, result!!.major)
        assertEquals(100, result.minor)
        assertEquals(50, result.patch)
    }

    @Test
    fun `parse handles extra version parts`() {
        // Versions like "1.2.3.4" should parse first 3 parts
        val result = ServerVersion.parse("1.2.3.4")

        assertNotNull(result)
        assertEquals(1, result!!.major)
        assertEquals(2, result.minor)
        assertEquals(3, result.patch)
    }

    // ========================================
    // preLegacy tests
    // ========================================

    @Test
    fun `preLegacy creates version with isPre_1_20_1 true`() {
        val result = ServerVersion.preLegacy()

        assertTrue(result.isPre_1_20_1)
        assertEquals(0, result.major)
        assertEquals(0, result.minor)
        assertNull(result.patch)
    }

    @Test
    fun `preLegacy displayString shows before 1_20`() {
        val result = ServerVersion.preLegacy()

        assertEquals("(before 1.20)", result.displayString)
    }

    // ========================================
    // equality and copy tests
    // ========================================

    @Test
    fun `serverVersion with same values are equal`() {
        val version1 = ServerVersion(major = 1, minor = 19, isPre_1_20_1 = false)
        val version2 = ServerVersion(major = 1, minor = 19, isPre_1_20_1 = false)

        assertEquals(version1, version2)
    }

    @Test
    fun `serverVersion copy preserves values`() {
        val original = ServerVersion(major = 1, minor = 19, isPre_1_20_1 = true)

        val copied = original.copy(minor = 20)

        assertEquals(1, copied.major)
        assertEquals(20, copied.minor)
        assertTrue(copied.isPre_1_20_1)
    }

    @Test
    fun `serverVersion with patch equality`() {
        val version1 = ServerVersion(major = 1, minor = 19, patch = 1)
        val version2 = ServerVersion(major = 1, minor = 19, patch = 1)
        val version3 = ServerVersion(major = 1, minor = 19, patch = 2)

        assertEquals(version1, version2)
        assertTrue(version1 != version3)
    }

    @Test
    fun `serverVersion hashCode consistent with equality`() {
        val version1 = ServerVersion(major = 1, minor = 19, patch = 1)
        val version2 = ServerVersion(major = 1, minor = 19, patch = 1)

        assertEquals(version1.hashCode(), version2.hashCode())
    }

    @Test
    fun `serverVersion toString contains values`() {
        val version = ServerVersion(major = 1, minor = 19, patch = 1)
        val str = version.toString()

        assertTrue(str.contains("1"))
        assertTrue(str.contains("19"))
    }
}
