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

import net.ktnx.mobileledger.core.domain.model.CurrencyPosition
import net.ktnx.mobileledger.core.domain.model.CurrencySettings
import net.ktnx.mobileledger.core.domain.model.TransactionLine
import net.ktnx.mobileledger.json.MoLeJson
import net.ktnx.mobileledger.json.config.ApiVersionConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [UnifiedParsedPosting].
 *
 * Tests verify:
 * - Default values
 * - Domain model conversion
 * - JSON deserialization (including ptransaction_ Int/String handling)
 */
class UnifiedParsedPostingTest {

    // ========================================
    // Default values tests
    // ========================================

    @Test
    fun `default pstatus is Unmarked`() {
        val posting = UnifiedParsedPosting()
        assertEquals("Unmarked", posting.pstatus)
    }

    @Test
    fun `default paccount is null`() {
        val posting = UnifiedParsedPosting()
        assertNull(posting.paccount)
    }

    @Test
    fun `default pamount is null`() {
        val posting = UnifiedParsedPosting()
        assertNull(posting.pamount)
    }

    @Test
    fun `default ptype is RegularPosting`() {
        val posting = UnifiedParsedPosting()
        assertEquals("RegularPosting", posting.ptype)
    }

    @Test
    fun `default ptags is empty list`() {
        val posting = UnifiedParsedPosting()
        assertTrue(posting.ptags.isEmpty())
    }

    @Test
    fun `default pcomment is empty string`() {
        val posting = UnifiedParsedPosting()
        assertEquals("", posting.pcomment)
    }

    @Test
    fun `default ptransaction_ is 0`() {
        val posting = UnifiedParsedPosting()
        assertEquals("0", posting.ptransaction_)
    }

    // ========================================
    // Constructor initialization tests
    // ========================================

    @Test
    fun `can create posting with paccount`() {
        val posting = UnifiedParsedPosting(paccount = "Assets:Bank")
        assertEquals("Assets:Bank", posting.paccount)
    }

    @Test
    fun `can create posting with pcomment`() {
        val posting = UnifiedParsedPosting(pcomment = "Test comment")
        assertEquals("Test comment", posting.pcomment)
    }

    @Test
    fun `deserialized pcomment trims leading whitespace`() {
        val json = """{"pcomment": "  trimmed"}"""
        val posting = MoLeJson.decodeFromString<UnifiedParsedPosting>(json)
        assertEquals("trimmed", posting.pcomment)
    }

    @Test
    fun `deserialized pcomment trims trailing whitespace`() {
        val json = """{"pcomment": "trimmed  "}"""
        val posting = MoLeJson.decodeFromString<UnifiedParsedPosting>(json)
        assertEquals("trimmed", posting.pcomment)
    }

    @Test
    fun `deserialized pcomment trims both sides`() {
        val json = """{"pcomment": "  trimmed  "}"""
        val posting = MoLeJson.decodeFromString<UnifiedParsedPosting>(json)
        assertEquals("trimmed", posting.pcomment)
    }

    @Test
    fun `deserialized pcomment preserves internal whitespace`() {
        val json = """{"pcomment": "hello world"}"""
        val posting = MoLeJson.decodeFromString<UnifiedParsedPosting>(json)
        assertEquals("hello world", posting.pcomment)
    }

    @Test
    fun `can create posting with ptransaction_`() {
        val posting = UnifiedParsedPosting(ptransaction_ = "42")
        assertEquals("42", posting.ptransaction_)
    }

    // ========================================
    // toDomain tests
    // ========================================

    @Test
    fun `toDomain converts basic posting`() {
        val posting = UnifiedParsedPosting(paccount = "Assets:Bank")

        val domain = posting.toDomain()

        assertEquals("Assets:Bank", domain.accountName)
    }

    @Test
    fun `toDomain handles null paccount`() {
        val posting = UnifiedParsedPosting()

        val domain = posting.toDomain()

        assertEquals("", domain.accountName)
    }

    @Test
    fun `toDomain converts amount`() {
        val posting = UnifiedParsedPosting(
            paccount = "Assets:Bank",
            pamount = mutableListOf(
                UnifiedParsedAmount(
                    acommodity = "USD",
                    aquantity = UnifiedParsedQuantity(
                        decimalMantissa = 10050L,
                        decimalPlaces = 2
                    )
                )
            )
        )

        val domain = posting.toDomain()

        assertEquals(100.50f, domain.amount!!, 0.01f)
        assertEquals("USD", domain.currency)
    }

    @Test
    fun `toDomain handles empty pamount`() {
        val posting = UnifiedParsedPosting(
            paccount = "Assets:Bank",
            pamount = mutableListOf()
        )

        val domain = posting.toDomain()

        assertNull(domain.amount)
    }

    @Test
    fun `toDomain converts comment`() {
        val posting = UnifiedParsedPosting(
            paccount = "Assets:Bank",
            pcomment = "Test comment"
        )

        val domain = posting.toDomain()

        assertEquals("Test comment", domain.comment)
    }

    @Test
    fun `toDomain returns null comment for empty pcomment`() {
        val posting = UnifiedParsedPosting(
            paccount = "Assets:Bank",
            pcomment = ""
        )

        val domain = posting.toDomain()

        assertNull(domain.comment)
    }

    // ========================================
    // getTransactionIdForSerialization tests
    // ========================================

    @Test
    fun `getTransactionIdForSerialization returns String`() {
        val posting = UnifiedParsedPosting(ptransaction_ = "42")

        val result = posting.getTransactionIdForSerialization()

        assertEquals("42", result)
    }

    @Test
    fun `getTransactionIdForSerialization preserves string value`() {
        val posting = UnifiedParsedPosting(ptransaction_ = "abc-123")

        val result = posting.getTransactionIdForSerialization()

        assertEquals("abc-123", result)
    }

    @Test
    fun `getTransactionIdForSerialization returns default for new posting`() {
        val posting = UnifiedParsedPosting()

        val result = posting.getTransactionIdForSerialization()

        assertEquals("0", result)
    }

    // ========================================
    // JSON deserialization tests
    // ========================================

    @Test
    fun `deserialize posting with ptransaction_ as Int`() {
        val json = """
            {
                "paccount": "Assets:Bank",
                "ptransaction_": 42,
                "pstatus": "Unmarked",
                "ptype": "RegularPosting"
            }
        """.trimIndent()

        val posting = MoLeJson.decodeFromString<UnifiedParsedPosting>(json)

        assertEquals("Assets:Bank", posting.paccount)
        assertEquals("42", posting.ptransaction_)
    }

    @Test
    fun `deserialize posting with ptransaction_ as String`() {
        val json = """
            {
                "paccount": "Assets:Bank",
                "ptransaction_": "123",
                "pstatus": "Unmarked",
                "ptype": "RegularPosting"
            }
        """.trimIndent()

        val posting = MoLeJson.decodeFromString<UnifiedParsedPosting>(json)

        assertEquals("123", posting.ptransaction_)
    }

    @Test
    fun `deserialize posting with pamount`() {
        val json = """
            {
                "paccount": "Expenses:Food",
                "pamount": [{
                    "acommodity": "USD",
                    "aquantity": {
                        "decimalMantissa": 1050,
                        "decimalPlaces": 2
                    }
                }],
                "pstatus": "Unmarked",
                "ptype": "RegularPosting"
            }
        """.trimIndent()

        val posting = MoLeJson.decodeFromString<UnifiedParsedPosting>(json)

        assertEquals("Expenses:Food", posting.paccount)
        assertEquals(1, posting.pamount?.size)
        assertEquals("USD", posting.pamount?.get(0)?.acommodity)
    }

    @Test
    fun `deserialize posting ignores unknown properties`() {
        val json = """
            {
                "paccount": "Assets:Cash",
                "unknownField": "value"
            }
        """.trimIndent()

        val posting = MoLeJson.decodeFromString<UnifiedParsedPosting>(json)

        assertEquals("Assets:Cash", posting.paccount)
    }

    // ========================================
    // Full object tests
    // ========================================

    @Test
    fun `can create complete posting object`() {
        val posting = UnifiedParsedPosting(
            paccount = "Expenses:Food",
            pstatus = "Cleared",
            ptype = "RegularPosting",
            pcomment = "Groceries",
            ptransaction_ = "99",
            pamount = mutableListOf(
                UnifiedParsedAmount(acommodity = "USD")
            )
        )

        assertEquals("Expenses:Food", posting.paccount)
        assertEquals("Cleared", posting.pstatus)
        assertEquals("Groceries", posting.pcomment)
        assertEquals("99", posting.ptransaction_)
        assertEquals(1, posting.pamount?.size)
    }

    // ========================================
    // fromDomain tests
    // ========================================

    @Test
    fun `fromDomain creates posting with account name`() {
        val line = TransactionLine(
            id = null,
            accountName = "Assets:Bank",
            amount = 100f,
            currency = "USD",
            comment = null
        )

        val posting = UnifiedParsedPosting.fromDomain(line, ApiVersionConfig.V1_32_40)

        assertEquals("Assets:Bank", posting.paccount)
    }

    @Test
    fun `fromDomain creates posting with amount`() {
        val line = TransactionLine(
            id = null,
            accountName = "Assets:Bank",
            amount = 100.50f,
            currency = "USD",
            comment = null
        )

        val posting = UnifiedParsedPosting.fromDomain(line, ApiVersionConfig.V1_32_40)

        assertEquals(1, posting.pamount?.size)
        assertEquals("USD", posting.pamount?.first()?.acommodity)
        assertEquals(10050L, posting.pamount?.first()?.aquantity?.decimalMantissa)
        assertEquals(2, posting.pamount?.first()?.aquantity?.decimalPlaces)
    }

    @Test
    fun `fromDomain creates posting with comment`() {
        val line = TransactionLine(
            id = null,
            accountName = "Assets:Bank",
            amount = 100f,
            currency = "USD",
            comment = "Test comment"
        )

        val posting = UnifiedParsedPosting.fromDomain(line, ApiVersionConfig.V1_32_40)

        assertEquals("Test comment", posting.pcomment)
    }

    @Test
    fun `fromDomain handles null comment`() {
        val line = TransactionLine(
            id = null,
            accountName = "Assets:Bank",
            amount = 100f,
            currency = "USD",
            comment = null
        )

        val posting = UnifiedParsedPosting.fromDomain(line, ApiVersionConfig.V1_32_40)

        assertEquals("", posting.pcomment)
    }

    @Test
    fun `fromDomain handles null amount as zero`() {
        val line = TransactionLine(
            id = null,
            accountName = "Assets:Bank",
            amount = null,
            currency = "USD",
            comment = null
        )

        val posting = UnifiedParsedPosting.fromDomain(line, ApiVersionConfig.V1_32_40)

        assertEquals(0L, posting.pamount?.first()?.aquantity?.decimalMantissa)
    }

    // ========================================
    // CurrencySettings tests
    // ========================================

    @Test
    fun `fromDomain with default settings uses left commodity side`() {
        val line = createTestLine()

        val posting = UnifiedParsedPosting.fromDomain(
            line,
            ApiVersionConfig.V1_32_40,
            CurrencySettings.DEFAULT
        )

        assertEquals('L', posting.pamount?.first()?.astyle?.ascommodityside)
    }

    @Test
    fun `fromDomain with AFTER position uses right commodity side`() {
        val line = createTestLine()
        val settings = object : CurrencySettings {
            override val symbolPosition = CurrencyPosition.AFTER
            override val hasGap = false
        }

        val posting = UnifiedParsedPosting.fromDomain(line, ApiVersionConfig.V1_32_40, settings)

        assertEquals('R', posting.pamount?.first()?.astyle?.ascommodityside)
    }

    @Test
    fun `fromDomain with BEFORE position uses left commodity side`() {
        val line = createTestLine()
        val settings = object : CurrencySettings {
            override val symbolPosition = CurrencyPosition.BEFORE
            override val hasGap = false
        }

        val posting = UnifiedParsedPosting.fromDomain(line, ApiVersionConfig.V1_32_40, settings)

        assertEquals('L', posting.pamount?.first()?.astyle?.ascommodityside)
    }

    @Test
    fun `fromDomain with gap enabled sets commodityspaced true`() {
        val line = createTestLine()
        val settings = object : CurrencySettings {
            override val symbolPosition = CurrencyPosition.BEFORE
            override val hasGap = true
        }

        val posting = UnifiedParsedPosting.fromDomain(line, ApiVersionConfig.V1_32_40, settings)

        assertEquals(true, posting.pamount?.first()?.astyle?.isAscommodityspaced)
    }

    @Test
    fun `fromDomain with gap disabled sets commodityspaced false`() {
        val line = createTestLine()
        val settings = object : CurrencySettings {
            override val symbolPosition = CurrencyPosition.BEFORE
            override val hasGap = false
        }

        val posting = UnifiedParsedPosting.fromDomain(line, ApiVersionConfig.V1_32_40, settings)

        assertEquals(false, posting.pamount?.first()?.astyle?.isAscommodityspaced)
    }

    @Test
    fun `fromDomain with AFTER and gap uses R side with spacing`() {
        val line = createTestLine()
        val settings = object : CurrencySettings {
            override val symbolPosition = CurrencyPosition.AFTER
            override val hasGap = true
        }

        val posting = UnifiedParsedPosting.fromDomain(line, ApiVersionConfig.V1_32_40, settings)

        assertEquals('R', posting.pamount?.first()?.astyle?.ascommodityside)
        assertEquals(true, posting.pamount?.first()?.astyle?.isAscommodityspaced)
    }

    // ========================================
    // Round-trip tests
    // ========================================

    @Test
    fun `fromDomain and toDomain round-trip preserves account`() {
        val original = createTestLine()

        val posting = UnifiedParsedPosting.fromDomain(original, ApiVersionConfig.V1_32_40)
        val restored = posting.toDomain()

        assertEquals(original.accountName, restored.accountName)
    }

    @Test
    fun `fromDomain and toDomain round-trip preserves currency`() {
        val original = createTestLine()

        val posting = UnifiedParsedPosting.fromDomain(original, ApiVersionConfig.V1_32_40)
        val restored = posting.toDomain()

        assertEquals(original.currency, restored.currency)
    }

    @Test
    fun `fromDomain and toDomain round-trip preserves amount`() {
        val original = TransactionLine(
            id = null,
            accountName = "Assets:Bank",
            amount = 123.45f,
            currency = "EUR",
            comment = null
        )

        val posting = UnifiedParsedPosting.fromDomain(original, ApiVersionConfig.V1_32_40)
        val restored = posting.toDomain()

        assertEquals(original.amount!!, restored.amount!!, 0.01f)
    }

    @Test
    fun `fromDomain and toDomain round-trip preserves comment`() {
        val original = TransactionLine(
            id = null,
            accountName = "Assets:Bank",
            amount = 100f,
            currency = "USD",
            comment = "Test comment"
        )

        val posting = UnifiedParsedPosting.fromDomain(original, ApiVersionConfig.V1_32_40)
        val restored = posting.toDomain()

        assertEquals(original.comment, restored.comment)
    }

    // ========================================
    // Helper methods
    // ========================================

    private fun createTestLine() = TransactionLine(
        id = null,
        accountName = "Assets:Bank",
        amount = 100f,
        currency = "USD",
        comment = null
    )
}
