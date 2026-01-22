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

package net.ktnx.mobileledger.json.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Unit tests for [ApiVersionConfig].
 *
 * Tests verify:
 * - Config properties for each version (v1_32+ only)
 * - Object identity (singleton pattern)
 * - Enum values and valueOf
 *
 * Note: forApi tests are skipped because API enum depends on Android.
 */
class ApiVersionConfigTest {

    // ========================================
    // Object identity tests (singleton pattern)
    // ========================================

    @Test
    fun `V1_32_40 is same instance on repeated access`() {
        val first = ApiVersionConfig.V1_32_40
        val second = ApiVersionConfig.V1_32_40
        assertSame(first, second)
    }

    @Test
    fun `V1_50 is same instance on repeated access`() {
        val first = ApiVersionConfig.V1_50
        val second = ApiVersionConfig.V1_50
        assertSame(first, second)
    }

    @Test
    fun `different versions are different instances`() {
        assertNotSame(ApiVersionConfig.V1_32_40, ApiVersionConfig.V1_50)
    }

    // ========================================
    // V1_32_40 properties tests
    // ========================================

    @Test
    fun `V1_32_40 has DirectBalance accountBalanceExtractor`() {
        assertEquals(AccountBalanceExtractor.DirectBalance, ApiVersionConfig.V1_32_40.accountBalanceExtractor)
    }

    // ========================================
    // V1_50 properties tests
    // ========================================

    @Test
    fun `V1_50 has PeriodBasedBalance accountBalanceExtractor`() {
        assertEquals(AccountBalanceExtractor.PeriodBasedBalance, ApiVersionConfig.V1_50.accountBalanceExtractor)
    }

    // ========================================
    // AccountBalanceExtractor enum tests
    // ========================================

    @Test
    fun `AccountBalanceExtractor has two values`() {
        val values = AccountBalanceExtractor.values()
        assertEquals(2, values.size)
    }

    @Test
    fun `AccountBalanceExtractor valueOf DirectBalance`() {
        assertEquals(AccountBalanceExtractor.DirectBalance, AccountBalanceExtractor.valueOf("DirectBalance"))
    }

    @Test
    fun `AccountBalanceExtractor valueOf PeriodBasedBalance`() {
        assertEquals(AccountBalanceExtractor.PeriodBasedBalance, AccountBalanceExtractor.valueOf("PeriodBasedBalance"))
    }
}
