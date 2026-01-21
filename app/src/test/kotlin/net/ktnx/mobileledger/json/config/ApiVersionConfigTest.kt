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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [ApiVersionConfig].
 *
 * Tests verify:
 * - Config properties for each version
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
    fun `V1_14_15 is same instance on repeated access`() {
        val first = ApiVersionConfig.V1_14_15
        val second = ApiVersionConfig.V1_14_15
        assertSame(first, second)
    }

    @Test
    fun `V1_19_1 is same instance on repeated access`() {
        val first = ApiVersionConfig.V1_19_1
        val second = ApiVersionConfig.V1_19_1
        assertSame(first, second)
    }

    @Test
    fun `V1_23 is same instance on repeated access`() {
        val first = ApiVersionConfig.V1_23
        val second = ApiVersionConfig.V1_23
        assertSame(first, second)
    }

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
        assertNotSame(ApiVersionConfig.V1_14_15, ApiVersionConfig.V1_19_1)
        assertNotSame(ApiVersionConfig.V1_14_15, ApiVersionConfig.V1_23)
        assertNotSame(ApiVersionConfig.V1_14_15, ApiVersionConfig.V1_32_40)
        assertNotSame(ApiVersionConfig.V1_14_15, ApiVersionConfig.V1_50)
        assertNotSame(ApiVersionConfig.V1_32_40, ApiVersionConfig.V1_50)
    }

    // ========================================
    // V1_14_15 properties tests
    // ========================================

    @Test
    fun `V1_14_15 has IntType transactionIdType`() {
        assertEquals(TransactionIdType.IntType, ApiVersionConfig.V1_14_15.transactionIdType)
    }

    @Test
    fun `V1_14_15 has DecimalPointChar styleConfig`() {
        assertEquals(StyleFieldConfig.DecimalPointChar, ApiVersionConfig.V1_14_15.styleConfig)
    }

    @Test
    fun `V1_14_15 has DirectBalance accountBalanceExtractor`() {
        assertEquals(AccountBalanceExtractor.DirectBalance, ApiVersionConfig.V1_14_15.accountBalanceExtractor)
    }

    // ========================================
    // V1_19_1 properties tests
    // ========================================

    @Test
    fun `V1_19_1 has IntType transactionIdType`() {
        assertEquals(TransactionIdType.IntType, ApiVersionConfig.V1_19_1.transactionIdType)
    }

    @Test
    fun `V1_19_1 has DecimalPointCharWithParsedPrecision styleConfig`() {
        assertEquals(StyleFieldConfig.DecimalPointCharWithParsedPrecision, ApiVersionConfig.V1_19_1.styleConfig)
    }

    @Test
    fun `V1_19_1 has DirectBalance accountBalanceExtractor`() {
        assertEquals(AccountBalanceExtractor.DirectBalance, ApiVersionConfig.V1_19_1.accountBalanceExtractor)
    }

    // ========================================
    // V1_23 properties tests
    // ========================================

    @Test
    fun `V1_23 has IntType transactionIdType`() {
        assertEquals(TransactionIdType.IntType, ApiVersionConfig.V1_23.transactionIdType)
    }

    @Test
    fun `V1_23 has DecimalPointCharIntPrecision styleConfig`() {
        assertEquals(StyleFieldConfig.DecimalPointCharIntPrecision, ApiVersionConfig.V1_23.styleConfig)
    }

    @Test
    fun `V1_23 has DirectBalance accountBalanceExtractor`() {
        assertEquals(AccountBalanceExtractor.DirectBalance, ApiVersionConfig.V1_23.accountBalanceExtractor)
    }

    // ========================================
    // V1_32_40 properties tests
    // ========================================

    @Test
    fun `V1_32_40 has StringType transactionIdType`() {
        assertEquals(TransactionIdType.StringType, ApiVersionConfig.V1_32_40.transactionIdType)
    }

    @Test
    fun `V1_32_40 has DecimalMarkString styleConfig`() {
        assertEquals(StyleFieldConfig.DecimalMarkString, ApiVersionConfig.V1_32_40.styleConfig)
    }

    @Test
    fun `V1_32_40 has DirectBalance accountBalanceExtractor`() {
        assertEquals(AccountBalanceExtractor.DirectBalance, ApiVersionConfig.V1_32_40.accountBalanceExtractor)
    }

    // ========================================
    // V1_50 properties tests
    // ========================================

    @Test
    fun `V1_50 has StringType transactionIdType`() {
        assertEquals(TransactionIdType.StringType, ApiVersionConfig.V1_50.transactionIdType)
    }

    @Test
    fun `V1_50 has DecimalMarkString styleConfig`() {
        assertEquals(StyleFieldConfig.DecimalMarkString, ApiVersionConfig.V1_50.styleConfig)
    }

    @Test
    fun `V1_50 has PeriodBasedBalance accountBalanceExtractor`() {
        assertEquals(AccountBalanceExtractor.PeriodBasedBalance, ApiVersionConfig.V1_50.accountBalanceExtractor)
    }

    // ========================================
    // TransactionIdType enum tests
    // ========================================

    @Test
    fun `TransactionIdType has two values`() {
        val values = TransactionIdType.values()
        assertEquals(2, values.size)
    }

    @Test
    fun `TransactionIdType valueOf IntType`() {
        assertEquals(TransactionIdType.IntType, TransactionIdType.valueOf("IntType"))
    }

    @Test
    fun `TransactionIdType valueOf StringType`() {
        assertEquals(TransactionIdType.StringType, TransactionIdType.valueOf("StringType"))
    }

    // ========================================
    // StyleFieldConfig enum tests
    // ========================================

    @Test
    fun `StyleFieldConfig has four values`() {
        val values = StyleFieldConfig.values()
        assertEquals(4, values.size)
    }

    @Test
    fun `StyleFieldConfig valueOf DecimalPointChar`() {
        assertEquals(StyleFieldConfig.DecimalPointChar, StyleFieldConfig.valueOf("DecimalPointChar"))
    }

    @Test
    fun `StyleFieldConfig valueOf DecimalMarkString`() {
        assertEquals(StyleFieldConfig.DecimalMarkString, StyleFieldConfig.valueOf("DecimalMarkString"))
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
