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

package net.ktnx.mobileledger.core.network.json

import net.ktnx.mobileledger.core.domain.model.API
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [API] enum.
 *
 * Tests verify:
 * - Integer value conversion
 * - Description strings
 * - allVersions array
 * - Backward compatibility for legacy values
 */
class APITest {

    // ========================================
    // toInt tests
    // ========================================

    @Test
    fun `auto toInt returns 0`() {
        assertEquals(0, API.auto.toInt())
    }

    @Test
    fun `v1_32 toInt returns -6`() {
        assertEquals(-6, API.v1_32.toInt())
    }

    @Test
    fun `v1_40 toInt returns -7`() {
        assertEquals(-7, API.v1_40.toInt())
    }

    @Test
    fun `v1_50 toInt returns -8`() {
        assertEquals(-8, API.v1_50.toInt())
    }

    // ========================================
    // valueOf tests
    // ========================================

    @Test
    fun `valueOf 0 returns auto`() {
        assertEquals(API.auto, API.valueOf(0))
    }

    @Test
    fun `valueOf -6 returns v1_32`() {
        assertEquals(API.v1_32, API.valueOf(-6))
    }

    @Test
    fun `valueOf -7 returns v1_40`() {
        assertEquals(API.v1_40, API.valueOf(-7))
    }

    @Test
    fun `valueOf -8 returns v1_50`() {
        assertEquals(API.v1_50, API.valueOf(-8))
    }

    // ========================================
    // Legacy value backward compatibility
    // ========================================

    @Test
    fun `valueOf unknown value returns auto`() {
        assertEquals(API.auto, API.valueOf(999))
    }

    @Test
    fun `valueOf negative unknown returns auto`() {
        assertEquals(API.auto, API.valueOf(-999))
    }

    @Test
    fun `valueOf legacy html value 1 returns auto`() {
        assertEquals(API.auto, API.valueOf(1))
    }

    @Test
    fun `valueOf legacy v1_14 value -1 returns auto`() {
        assertEquals(API.auto, API.valueOf(-1))
    }

    @Test
    fun `valueOf legacy v1_15 value -2 returns auto`() {
        assertEquals(API.auto, API.valueOf(-2))
    }

    @Test
    fun `valueOf legacy v1_19_1 value -3 returns auto`() {
        assertEquals(API.auto, API.valueOf(-3))
    }

    @Test
    fun `valueOf legacy v1_23 value -4 returns auto`() {
        assertEquals(API.auto, API.valueOf(-4))
    }

    // ========================================
    // description tests
    // ========================================

    @Test
    fun `auto description is automatic`() {
        assertEquals("(automatic)", API.auto.description)
    }

    @Test
    fun `v1_32 description is 1_32`() {
        assertEquals("1.32", API.v1_32.description)
    }

    @Test
    fun `v1_40 description is 1_40`() {
        assertEquals("1.40", API.v1_40.description)
    }

    @Test
    fun `v1_50 description is 1_50`() {
        assertEquals("1.50", API.v1_50.description)
    }

    // ========================================
    // allVersions tests
    // ========================================

    @Test
    fun `allVersions contains 3 versions`() {
        assertEquals(3, API.allVersions.size)
    }

    @Test
    fun `allVersions is ordered newest to oldest`() {
        assertArrayEquals(
            arrayOf(API.v1_50, API.v1_40, API.v1_32),
            API.allVersions
        )
    }

    @Test
    fun `allVersions does not include auto`() {
        API.allVersions.forEach { version ->
            assert(version != API.auto) { "allVersions should not contain auto" }
        }
    }

    // ========================================
    // Round-trip tests
    // ========================================

    @Test
    fun `toInt and valueOf round-trip for auto`() {
        val original = API.auto
        val restored = API.valueOf(original.toInt())
        assertEquals(original, restored)
    }

    @Test
    fun `toInt and valueOf round-trip for v1_32`() {
        val original = API.v1_32
        val restored = API.valueOf(original.toInt())
        assertEquals(original, restored)
    }

    @Test
    fun `toInt and valueOf round-trip for v1_40`() {
        val original = API.v1_40
        val restored = API.valueOf(original.toInt())
        assertEquals(original, restored)
    }

    @Test
    fun `toInt and valueOf round-trip for v1_50`() {
        val original = API.v1_50
        val restored = API.valueOf(original.toInt())
        assertEquals(original, restored)
    }

    @Test
    fun `all versions round-trip correctly`() {
        API.entries.forEach { api ->
            val restored = API.valueOf(api.toInt())
            assertEquals("$api should round-trip", api, restored)
        }
    }

    // ========================================
    // Enum entries tests
    // ========================================

    @Test
    fun `API has 4 entries`() {
        assertEquals(4, API.entries.size)
    }

    @Test
    fun `API entries include auto and all versions`() {
        val entries = API.entries.toSet()
        assert(API.auto in entries)
        assert(API.v1_32 in entries)
        assert(API.v1_40 in entries)
        assert(API.v1_50 in entries)
    }
}
