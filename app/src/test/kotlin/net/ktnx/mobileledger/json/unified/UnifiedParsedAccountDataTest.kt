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

package net.ktnx.mobileledger.json.unified

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [UnifiedParsedAccountData] and [UnifiedParsedBalanceData].
 *
 * Tests verify:
 * - Default values
 * - JSON deserialization with custom deserializer
 * - Period entry parsing
 */
class UnifiedParsedAccountDataTest {

    // ========================================
    // Default values tests
    // ========================================

    @Test
    fun `default pdperiods is null`() {
        val accountData = UnifiedParsedAccountData()
        assertNull(accountData.pdperiods)
    }

    @Test
    fun `default pdpre is null`() {
        val accountData = UnifiedParsedAccountData()
        assertNull(accountData.pdpre)
    }

    @Test
    fun `getFirstPeriodBalance returns null for empty periods`() {
        val accountData = UnifiedParsedAccountData()
        assertNull(accountData.getFirstPeriodBalance())
    }

    // ========================================
    // UnifiedParsedBalanceData default values tests
    // ========================================

    @Test
    fun `UnifiedParsedBalanceData default bdincludingsubs is null`() {
        val balanceData = UnifiedParsedBalanceData()
        assertNull(balanceData.bdincludingsubs)
    }

    @Test
    fun `UnifiedParsedBalanceData default bdexcludingsubs is null`() {
        val balanceData = UnifiedParsedBalanceData()
        assertNull(balanceData.bdexcludingsubs)
    }

    @Test
    fun `UnifiedParsedBalanceData default bdnumpostings is 0`() {
        val balanceData = UnifiedParsedBalanceData()
        assertEquals(0, balanceData.bdnumpostings)
    }

    // ========================================
    // PeriodEntry tests
    // ========================================

    @Test
    fun `PeriodEntry can be created with defaults`() {
        val entry = UnifiedParsedAccountData.PeriodEntry()
        assertNull(entry.date)
        assertNull(entry.balanceData)
    }

    @Test
    fun `PeriodEntry can store date and balance`() {
        val balanceData = UnifiedParsedBalanceData()
        val entry = UnifiedParsedAccountData.PeriodEntry("2024-01-01", balanceData)
        assertEquals("2024-01-01", entry.date)
        assertNotNull(entry.balanceData)
    }

    // ========================================
    // JSON deserialization tests - simple cases
    // ========================================

    @Test
    fun `deserialize empty account data`() {
        val mapper = ObjectMapper()
        val json = """{}"""

        val accountData = mapper.readValue(json, UnifiedParsedAccountData::class.java)

        assertNull(accountData.pdperiods)
        assertNull(accountData.pdpre)
    }

    @Test
    fun `deserialize with empty pdperiods array`() {
        val mapper = ObjectMapper()
        val json = """
            {
                "pdperiods": []
            }
        """.trimIndent()

        val accountData = mapper.readValue(json, UnifiedParsedAccountData::class.java)

        assertNotNull(accountData.pdperiods)
        assertEquals(0, accountData.pdperiods!!.size)
    }

    @Test
    fun `deserialize with pdpre`() {
        val mapper = ObjectMapper()
        val json = """
            {
                "pdpre": {
                    "bdincludingsubs": [],
                    "bdexcludingsubs": [],
                    "bdnumpostings": 5
                }
            }
        """.trimIndent()

        val accountData = mapper.readValue(json, UnifiedParsedAccountData::class.java)

        assertNotNull(accountData.pdpre)
        assertEquals(5, accountData.pdpre!!.bdnumpostings)
    }

    // ========================================
    // JSON deserialization tests - pdperiods with heterogeneous array
    // ========================================

    @Test
    fun `deserialize pdperiods with single entry`() {
        val mapper = ObjectMapper()
        val json = """
            {
                "pdperiods": [
                    ["0000-01-01", {"bdincludingsubs": [], "bdexcludingsubs": [], "bdnumpostings": 1}]
                ]
            }
        """.trimIndent()

        val accountData = mapper.readValue(json, UnifiedParsedAccountData::class.java)

        assertNotNull(accountData.pdperiods)
        assertEquals(1, accountData.pdperiods!!.size)
        assertEquals("0000-01-01", accountData.pdperiods!![0].date)
        assertEquals(1, accountData.pdperiods!![0].balanceData!!.bdnumpostings)
    }

    @Test
    fun `deserialize pdperiods with multiple entries`() {
        val mapper = ObjectMapper()
        val json = """
            {
                "pdperiods": [
                    ["2024-01-01", {"bdnumpostings": 5}],
                    ["2024-02-01", {"bdnumpostings": 10}]
                ]
            }
        """.trimIndent()

        val accountData = mapper.readValue(json, UnifiedParsedAccountData::class.java)

        assertNotNull(accountData.pdperiods)
        assertEquals(2, accountData.pdperiods!!.size)
        assertEquals("2024-01-01", accountData.pdperiods!![0].date)
        assertEquals(5, accountData.pdperiods!![0].balanceData!!.bdnumpostings)
        assertEquals("2024-02-01", accountData.pdperiods!![1].date)
        assertEquals(10, accountData.pdperiods!![1].balanceData!!.bdnumpostings)
    }

    @Test
    fun `getFirstPeriodBalance returns first entry balance`() {
        val mapper = ObjectMapper()
        val json = """
            {
                "pdperiods": [
                    ["0000-01-01", {"bdnumpostings": 42}]
                ]
            }
        """.trimIndent()

        val accountData = mapper.readValue(json, UnifiedParsedAccountData::class.java)

        val balance = accountData.getFirstPeriodBalance()
        assertNotNull(balance)
        assertEquals(42, balance!!.bdnumpostings)
    }

    // ========================================
    // JSON deserialization tests - balance data with amounts
    // ========================================

    @Test
    fun `deserialize balance data with amounts`() {
        val mapper = ObjectMapper()
        val json = """
            {
                "pdpre": {
                    "bdincludingsubs": [
                        {"acommodity": "USD", "aquantity": {"decimalMantissa": 10000, "decimalPlaces": 2}}
                    ],
                    "bdexcludingsubs": [],
                    "bdnumpostings": 3
                }
            }
        """.trimIndent()

        val accountData = mapper.readValue(json, UnifiedParsedAccountData::class.java)

        assertNotNull(accountData.pdpre)
        assertNotNull(accountData.pdpre!!.bdincludingsubs)
        assertEquals(1, accountData.pdpre!!.bdincludingsubs!!.size)
        assertEquals("USD", accountData.pdpre!!.bdincludingsubs!![0].acommodity)
    }

    @Test
    fun `deserialize ignores unknown properties`() {
        val mapper = ObjectMapper()
        val json = """
            {
                "pdpre": {
                    "bdnumpostings": 1,
                    "unknownField": "value"
                },
                "anotherUnknown": 123
            }
        """.trimIndent()

        // Should not throw exception
        val accountData = mapper.readValue(json, UnifiedParsedAccountData::class.java)
        assertEquals(1, accountData.pdpre!!.bdnumpostings)
    }
}
