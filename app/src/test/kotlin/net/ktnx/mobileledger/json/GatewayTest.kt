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

package net.ktnx.mobileledger.json

import net.ktnx.mobileledger.core.common.utils.SimpleDate
import net.ktnx.mobileledger.core.domain.model.CurrencyPosition
import net.ktnx.mobileledger.core.domain.model.CurrencySettings
import net.ktnx.mobileledger.core.domain.model.Transaction
import net.ktnx.mobileledger.core.domain.model.TransactionLine
import net.ktnx.mobileledger.json.unified.UnifiedParsedLedgerTransaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [Gateway] factory and serialization.
 *
 * Tests verify:
 * - Gateway factory returns correct implementation for each API version
 * - Transaction serialization produces valid JSON
 * - CurrencySettings are correctly applied to serialized output
 */
class GatewayTest {

    // ========================================
    // Gateway factory tests
    // ========================================

    @Test
    fun `forApiVersion returns v1_32 Gateway`() {
        val gateway = Gateway.forApiVersion(API.v1_32)
        assertTrue(gateway is net.ktnx.mobileledger.json.v1_32.Gateway)
    }

    @Test
    fun `forApiVersion returns v1_40 Gateway`() {
        val gateway = Gateway.forApiVersion(API.v1_40)
        assertTrue(gateway is net.ktnx.mobileledger.json.v1_40.Gateway)
    }

    @Test
    fun `forApiVersion returns v1_50 Gateway`() {
        val gateway = Gateway.forApiVersion(API.v1_50)
        assertTrue(gateway is net.ktnx.mobileledger.json.v1_50.Gateway)
    }

    @Test(expected = RuntimeException::class)
    fun `forApiVersion throws for API auto`() {
        Gateway.forApiVersion(API.auto)
    }

    // ========================================
    // Transaction serialization tests
    // ========================================

    @Test
    fun `v1_32 Gateway serializes transaction to valid JSON`() {
        val gateway = Gateway.forApiVersion(API.v1_32)
        val transaction = createTestTransaction()

        val json = gateway.transactionSaveRequest(transaction)

        assertNotNull(json)
        assertTrue(json.contains("\"tdate\""))
        assertTrue(json.contains("\"tdescription\""))
        assertTrue(json.contains("\"tpostings\""))
    }

    @Test
    fun `v1_40 Gateway serializes transaction to valid JSON`() {
        val gateway = Gateway.forApiVersion(API.v1_40)
        val transaction = createTestTransaction()

        val json = gateway.transactionSaveRequest(transaction)

        assertNotNull(json)
        assertTrue(json.contains("\"tdate\""))
        assertTrue(json.contains("\"tdescription\""))
    }

    @Test
    fun `v1_50 Gateway serializes transaction to valid JSON`() {
        val gateway = Gateway.forApiVersion(API.v1_50)
        val transaction = createTestTransaction()

        val json = gateway.transactionSaveRequest(transaction)

        assertNotNull(json)
        assertTrue(json.contains("\"tdate\""))
        assertTrue(json.contains("\"tdescription\""))
    }

    @Test
    fun `serialized JSON contains correct date format`() {
        val gateway = Gateway.forApiVersion(API.v1_32)
        val transaction = Transaction(
            id = null,
            ledgerId = 1,
            date = SimpleDate(2024, 12, 25),
            description = "Christmas",
            comment = null,
            lines = emptyList()
        )

        val json = gateway.transactionSaveRequest(transaction)

        assertTrue(json.contains("\"2024-12-25\""))
    }

    @Test
    fun `serialized JSON contains description`() {
        val gateway = Gateway.forApiVersion(API.v1_32)
        val transaction = Transaction(
            id = null,
            ledgerId = 1,
            date = SimpleDate(2024, 6, 15),
            description = "Test purchase",
            comment = null,
            lines = emptyList()
        )

        val json = gateway.transactionSaveRequest(transaction)

        assertTrue(json.contains("Test purchase"))
    }

    @Test
    fun `serialized JSON contains posting account names`() {
        val gateway = Gateway.forApiVersion(API.v1_32)
        val transaction = createTestTransaction()

        val json = gateway.transactionSaveRequest(transaction)

        assertTrue(json.contains("Assets:Bank"))
        assertTrue(json.contains("Expenses:Food"))
    }

    @Test
    fun `serialized JSON contains posting amounts`() {
        val gateway = Gateway.forApiVersion(API.v1_32)
        val transaction = createTestTransaction()

        val json = gateway.transactionSaveRequest(transaction)

        assertTrue(json.contains("USD"))
    }

    // ========================================
    // CurrencySettings tests
    // ========================================

    @Test
    fun `serialization with default CurrencySettings uses left position`() {
        val gateway = Gateway.forApiVersion(API.v1_32)
        val transaction = createTestTransaction()

        val json = gateway.transactionSaveRequest(transaction, CurrencySettings.DEFAULT)

        assertTrue(json.contains("\"ascommodityside\":\"L\""))
    }

    @Test
    fun `serialization with AFTER position uses right side`() {
        val gateway = Gateway.forApiVersion(API.v1_32)
        val transaction = createTestTransaction()
        val settings = object : CurrencySettings {
            override val symbolPosition = CurrencyPosition.AFTER
            override val hasGap = false
        }

        val json = gateway.transactionSaveRequest(transaction, settings)

        assertTrue(json.contains("\"ascommodityside\":\"R\""))
    }

    @Test
    fun `serialization with gap enabled sets commodityspaced true`() {
        val gateway = Gateway.forApiVersion(API.v1_32)
        val transaction = createTestTransaction()
        val settings = object : CurrencySettings {
            override val symbolPosition = CurrencyPosition.BEFORE
            override val hasGap = true
        }

        val json = gateway.transactionSaveRequest(transaction, settings)

        assertTrue(json.contains("\"ascommodityspaced\":true"))
    }

    @Test
    fun `serialization with gap disabled omits commodityspaced (default false)`() {
        val gateway = Gateway.forApiVersion(API.v1_32)
        val transaction = createTestTransaction()
        val settings = object : CurrencySettings {
            override val symbolPosition = CurrencyPosition.BEFORE
            override val hasGap = false
        }

        val json = gateway.transactionSaveRequest(transaction, settings)

        // MoLeJson has encodeDefaults=false, so false values are omitted
        // When gap is disabled, ascommodityspaced should not be true
        assertTrue(!json.contains("\"ascommodityspaced\":true"))
    }

    @Test
    fun `all API versions respect CurrencySettings`() {
        val transaction = createTestTransaction()
        val settings = object : CurrencySettings {
            override val symbolPosition = CurrencyPosition.AFTER
            override val hasGap = true
        }

        listOf(API.v1_32, API.v1_40, API.v1_50).forEach { api ->
            val gateway = Gateway.forApiVersion(api)
            val json = gateway.transactionSaveRequest(transaction, settings)

            assertTrue("$api should have R side", json.contains("\"ascommodityside\":\"R\""))
            assertTrue("$api should have spaced true", json.contains("\"ascommodityspaced\":true"))
        }
    }

    // ========================================
    // Round-trip tests
    // ========================================

    @Test
    fun `serialized transaction can be deserialized`() {
        val gateway = Gateway.forApiVersion(API.v1_32)
        val transaction = createTestTransaction()

        val json = gateway.transactionSaveRequest(transaction)
        val parsed = MoLeJson.decodeFromString<UnifiedParsedLedgerTransaction>(json)

        assertEquals("2024-06-15", parsed.tdate)
        assertEquals("Test transaction", parsed.tdescription)
        assertEquals(2, parsed.tpostings?.size)
    }

    @Test
    fun `round-trip preserves transaction data`() {
        val gateway = Gateway.forApiVersion(API.v1_32)
        val original = createTestTransaction()

        val json = gateway.transactionSaveRequest(original)
        val parsed = MoLeJson.decodeFromString<UnifiedParsedLedgerTransaction>(json)
        val restored = parsed.toDomain()

        assertEquals(original.date.year, restored.date.year)
        assertEquals(original.date.month, restored.date.month)
        assertEquals(original.date.day, restored.date.day)
        assertEquals(original.description, restored.description)
        assertEquals(original.lines.size, restored.lines.size)
    }

    @Test
    fun `round-trip preserves posting accounts`() {
        val gateway = Gateway.forApiVersion(API.v1_32)
        val original = createTestTransaction()

        val json = gateway.transactionSaveRequest(original)
        val parsed = MoLeJson.decodeFromString<UnifiedParsedLedgerTransaction>(json)
        val restored = parsed.toDomain()

        assertEquals("Assets:Bank", restored.lines[0].accountName)
        assertEquals("Expenses:Food", restored.lines[1].accountName)
    }

    @Test
    fun `round-trip preserves posting amounts`() {
        val gateway = Gateway.forApiVersion(API.v1_32)
        val original = createTestTransaction()

        val json = gateway.transactionSaveRequest(original)
        val parsed = MoLeJson.decodeFromString<UnifiedParsedLedgerTransaction>(json)
        val restored = parsed.toDomain()

        assertEquals(100.0f, restored.lines[0].amount!!, 0.01f)
        assertEquals(-100.0f, restored.lines[1].amount!!, 0.01f)
    }

    // ========================================
    // API version specific tests
    // ========================================

    @Test
    fun `v1_32_40 produces single tsourcepos object`() {
        val gateway = Gateway.forApiVersion(API.v1_32)
        val transaction = createTestTransaction()

        val json = gateway.transactionSaveRequest(transaction)
        val parsed = MoLeJson.decodeFromString<UnifiedParsedLedgerTransaction>(json)

        assertEquals(1, parsed.tsourcepos.size)
    }

    @Test
    fun `v1_50 produces two tsourcepos objects`() {
        val gateway = Gateway.forApiVersion(API.v1_50)
        val transaction = createTestTransaction()

        val json = gateway.transactionSaveRequest(transaction)
        val parsed = MoLeJson.decodeFromString<UnifiedParsedLedgerTransaction>(json)

        assertEquals(2, parsed.tsourcepos.size)
    }

    // ========================================
    // Edge case tests
    // ========================================

    @Test
    fun `serialization handles empty lines list`() {
        val gateway = Gateway.forApiVersion(API.v1_32)
        val transaction = Transaction(
            id = null,
            ledgerId = 1,
            date = SimpleDate(2024, 6, 15),
            description = "Empty transaction",
            comment = null,
            lines = emptyList()
        )

        val json = gateway.transactionSaveRequest(transaction)

        assertNotNull(json)
        assertTrue(json.contains("\"tpostings\":[]"))
    }

    @Test
    fun `serialization handles transaction with comment`() {
        val gateway = Gateway.forApiVersion(API.v1_32)
        val transaction = Transaction(
            id = null,
            ledgerId = 1,
            date = SimpleDate(2024, 6, 15),
            description = "Test",
            comment = "A comment",
            lines = emptyList()
        )

        val json = gateway.transactionSaveRequest(transaction)

        assertTrue(json.contains("A comment"))
    }

    @Test
    fun `serialization filters empty account names`() {
        val gateway = Gateway.forApiVersion(API.v1_32)
        val transaction = Transaction(
            id = null,
            ledgerId = 1,
            date = SimpleDate(2024, 6, 15),
            description = "Test",
            comment = null,
            lines = listOf(
                TransactionLine(null, "Assets:Bank", 100f, "USD", null),
                TransactionLine(null, "", -100f, "USD", null)
            )
        )

        val json = gateway.transactionSaveRequest(transaction)
        val parsed = MoLeJson.decodeFromString<UnifiedParsedLedgerTransaction>(json)

        assertEquals(1, parsed.tpostings?.size)
    }

    @Test
    fun `serialization handles special characters in description`() {
        val gateway = Gateway.forApiVersion(API.v1_32)
        val transaction = Transaction(
            id = null,
            ledgerId = 1,
            date = SimpleDate(2024, 6, 15),
            description = "Test \"quoted\" & <special>",
            comment = null,
            lines = emptyList()
        )

        val json = gateway.transactionSaveRequest(transaction)

        assertNotNull(json)
        // JSON should be valid (kotlinx.serialization handles escaping)
        val parsed = MoLeJson.decodeFromString<UnifiedParsedLedgerTransaction>(json)
        assertEquals("Test \"quoted\" & <special>", parsed.tdescription)
    }

    @Test
    fun `serialization handles unicode in description`() {
        val gateway = Gateway.forApiVersion(API.v1_32)
        val transaction = Transaction(
            id = null,
            ledgerId = 1,
            date = SimpleDate(2024, 6, 15),
            description = "日本語テスト",
            comment = null,
            lines = emptyList()
        )

        val json = gateway.transactionSaveRequest(transaction)
        val parsed = MoLeJson.decodeFromString<UnifiedParsedLedgerTransaction>(json)

        assertEquals("日本語テスト", parsed.tdescription)
    }

    // ========================================
    // Helper methods
    // ========================================

    private fun createTestTransaction() = Transaction(
        id = null,
        ledgerId = 1,
        date = SimpleDate(2024, 6, 15),
        description = "Test transaction",
        comment = null,
        lines = listOf(
            TransactionLine(null, "Assets:Bank", 100f, "USD", null),
            TransactionLine(null, "Expenses:Food", -100f, "USD", null)
        )
    )
}
