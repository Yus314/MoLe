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

import net.ktnx.mobileledger.json.config.ApiVersionConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [UnifiedParsedPosting].
 *
 * Tests verify:
 * - Default values
 * - Transaction ID parsing from different formats
 * - Domain model conversion
 * - Comment trimming
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
    // pcomment setter tests
    // ========================================

    @Test
    fun `pcomment setter trims leading whitespace`() {
        val posting = UnifiedParsedPosting()
        posting.pcomment = "  trimmed"
        assertEquals("trimmed", posting.pcomment)
    }

    @Test
    fun `pcomment setter trims trailing whitespace`() {
        val posting = UnifiedParsedPosting()
        posting.pcomment = "trimmed  "
        assertEquals("trimmed", posting.pcomment)
    }

    @Test
    fun `pcomment setter trims both sides`() {
        val posting = UnifiedParsedPosting()
        posting.pcomment = "  trimmed  "
        assertEquals("trimmed", posting.pcomment)
    }

    @Test
    fun `pcomment setter preserves internal whitespace`() {
        val posting = UnifiedParsedPosting()
        posting.pcomment = "hello world"
        assertEquals("hello world", posting.pcomment)
    }

    // ========================================
    // setPtransactionFromJson tests
    // ========================================

    @Test
    fun `setPtransactionFromJson handles Int input`() {
        val posting = UnifiedParsedPosting()
        posting.setPtransactionFromJson(42)
        assertEquals("42", posting.ptransaction_)
    }

    @Test
    fun `setPtransactionFromJson handles Long input`() {
        val posting = UnifiedParsedPosting()
        posting.setPtransactionFromJson(100L)
        assertEquals("100", posting.ptransaction_)
    }

    @Test
    fun `setPtransactionFromJson handles String input`() {
        val posting = UnifiedParsedPosting()
        posting.setPtransactionFromJson("123")
        assertEquals("123", posting.ptransaction_)
    }

    @Test
    fun `setPtransactionFromJson handles null input`() {
        val posting = UnifiedParsedPosting()
        posting.setPtransactionFromJson(null)
        assertEquals("0", posting.ptransaction_)
    }

    @Test
    fun `setPtransactionFromJson handles unknown type`() {
        val posting = UnifiedParsedPosting()
        posting.setPtransactionFromJson(listOf(1, 2, 3))
        assertEquals("0", posting.ptransaction_)
    }

    // ========================================
    // setTransactionIdAsInt tests
    // ========================================

    @Test
    fun `setTransactionIdAsInt converts to string`() {
        val posting = UnifiedParsedPosting()
        posting.setTransactionIdAsInt(99)
        assertEquals("99", posting.ptransaction_)
    }

    @Test
    fun `setTransactionIdAsInt handles zero`() {
        val posting = UnifiedParsedPosting()
        posting.setTransactionIdAsInt(0)
        assertEquals("0", posting.ptransaction_)
    }

    // ========================================
    // setTransactionIdAsString tests
    // ========================================

    @Test
    fun `setTransactionIdAsString stores string directly`() {
        val posting = UnifiedParsedPosting()
        posting.setTransactionIdAsString("abc-123")
        assertEquals("abc-123", posting.ptransaction_)
    }

    // ========================================
    // toDomain tests
    // ========================================

    @Test
    fun `toDomain converts basic posting`() {
        val posting = UnifiedParsedPosting().apply {
            paccount = "Assets:Bank"
        }

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
        val posting = UnifiedParsedPosting().apply {
            paccount = "Assets:Bank"
            pamount = mutableListOf(
                UnifiedParsedAmount().apply {
                    acommodity = "USD"
                    aquantity = UnifiedParsedQuantity().apply {
                        decimalMantissa = 10050L
                        decimalPlaces = 2
                    }
                }
            )
        }

        val domain = posting.toDomain()

        assertEquals(100.50f, domain.amount!!, 0.01f)
        assertEquals("USD", domain.currency)
    }

    @Test
    fun `toDomain handles empty pamount`() {
        val posting = UnifiedParsedPosting().apply {
            paccount = "Assets:Bank"
            pamount = mutableListOf()
        }

        val domain = posting.toDomain()

        assertNull(domain.amount)
    }

    @Test
    fun `toDomain converts comment`() {
        val posting = UnifiedParsedPosting().apply {
            paccount = "Assets:Bank"
            pcomment = "Test comment"
        }

        val domain = posting.toDomain()

        assertEquals("Test comment", domain.comment)
    }

    @Test
    fun `toDomain returns null comment for empty pcomment`() {
        val posting = UnifiedParsedPosting().apply {
            paccount = "Assets:Bank"
            pcomment = ""
        }

        val domain = posting.toDomain()

        assertNull(domain.comment)
    }

    // ========================================
    // getTransactionIdForSerialization tests
    // ========================================

    @Test
    fun `getTransactionIdForSerialization returns Int for IntType config`() {
        val posting = UnifiedParsedPosting().apply {
            setTransactionIdAsString("42")
        }
        // V1_14_15 uses IntType
        val config = ApiVersionConfig.V1_14_15

        val result = posting.getTransactionIdForSerialization(config)

        assertEquals(42, result)
    }

    @Test
    fun `getTransactionIdForSerialization returns String for StringType config`() {
        val posting = UnifiedParsedPosting().apply {
            setTransactionIdAsString("abc-123")
        }
        // V1_32_40 uses StringType
        val config = ApiVersionConfig.V1_32_40

        val result = posting.getTransactionIdForSerialization(config)

        assertEquals("abc-123", result)
    }

    @Test
    fun `getTransactionIdForSerialization returns 0 for invalid Int`() {
        val posting = UnifiedParsedPosting().apply {
            setTransactionIdAsString("not-a-number")
        }
        // V1_23 uses IntType
        val config = ApiVersionConfig.V1_23

        val result = posting.getTransactionIdForSerialization(config)

        assertEquals(0, result)
    }

    // ========================================
    // Property setter tests
    // ========================================

    @Test
    fun `paccount can be set`() {
        val posting = UnifiedParsedPosting()
        posting.paccount = "Expenses:Food"
        assertEquals("Expenses:Food", posting.paccount)
    }

    @Test
    fun `pstatus can be set`() {
        val posting = UnifiedParsedPosting()
        posting.pstatus = "Cleared"
        assertEquals("Cleared", posting.pstatus)
    }

    @Test
    fun `ptype can be set`() {
        val posting = UnifiedParsedPosting()
        posting.ptype = "VirtualPosting"
        assertEquals("VirtualPosting", posting.ptype)
    }

    @Test
    fun `pdate can be set`() {
        val posting = UnifiedParsedPosting()
        posting.pdate = "2024-06-15"
        assertEquals("2024-06-15", posting.pdate)
    }

    @Test
    fun `ptags can be modified`() {
        val posting = UnifiedParsedPosting()
        posting.ptags.add(listOf("tag", "value"))
        assertEquals(1, posting.ptags.size)
    }

    // ========================================
    // pamount tests
    // ========================================

    @Test
    fun `pamount can hold multiple amounts`() {
        val posting = UnifiedParsedPosting()
        posting.pamount = mutableListOf(
            UnifiedParsedAmount().apply { acommodity = "USD" },
            UnifiedParsedAmount().apply { acommodity = "EUR" }
        )

        assertEquals(2, posting.pamount!!.size)
    }
}
