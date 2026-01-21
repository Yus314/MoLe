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

package net.ktnx.mobileledger.backup

import android.util.JsonReader
import java.io.StringReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for backup parsers.
 *
 * Tests verify:
 * - CurrencyBackupParser: parsing currencies with various fields
 * - ProfileBackupParser: parsing profiles with auth and optional fields
 * - TemplateBackupParser: parsing templates with nested accounts
 */
@RunWith(RobolectricTestRunner::class)
class BackupParsersTest {

    // ========================================
    // CurrencyBackupParser tests
    // ========================================

    @Test
    fun `CurrencyBackupParser parses single currency`() {
        // Given
        val json = """[{"name":"USD","position":"before","hasGap":true}]"""
        val parser = CurrencyBackupParser()

        // When
        val currencies = parser.parse(createReader(json))

        // Then
        assertEquals(1, currencies.size)
        assertEquals("USD", currencies[0].name)
        assertEquals("before", currencies[0].position)
        assertTrue(currencies[0].hasGap)
    }

    @Test
    fun `CurrencyBackupParser parses multiple currencies`() {
        // Given
        val json = """[
            {"name":"USD","position":"before","hasGap":true},
            {"name":"EUR","position":"after","hasGap":false}
        ]"""
        val parser = CurrencyBackupParser()

        // When
        val currencies = parser.parse(createReader(json))

        // Then
        assertEquals(2, currencies.size)
        assertEquals("USD", currencies[0].name)
        assertEquals("EUR", currencies[1].name)
    }

    @Test
    fun `CurrencyBackupParser handles null position gracefully`() {
        // Given
        val json = """[{"name":"USD","position":null,"hasGap":true}]"""
        val parser = CurrencyBackupParser()

        // When
        val currencies = parser.parse(createReader(json))

        // Then
        assertEquals(1, currencies.size)
        assertEquals("USD", currencies[0].name)
        // null position leaves the default value ("after")
        assertEquals("after", currencies[0].position)
    }

    @Test
    fun `CurrencyBackupParser handles null hasGap gracefully`() {
        // Given
        val json = """[{"name":"USD","position":"before","hasGap":null}]"""
        val parser = CurrencyBackupParser()

        // When
        val currencies = parser.parse(createReader(json))

        // Then
        assertEquals(1, currencies.size)
        // hasGap should remain at default (true)
        assertTrue(currencies[0].hasGap)
    }

    @Test(expected = RuntimeException::class)
    fun `CurrencyBackupParser throws on missing name`() {
        // Given - name is empty after parsing
        val json = """[{"position":"before","hasGap":true}]"""
        val parser = CurrencyBackupParser()

        // When
        parser.parse(createReader(json))

        // Then - RuntimeException expected
    }

    @Test(expected = RuntimeException::class)
    fun `CurrencyBackupParser throws on unknown field`() {
        // Given
        val json = """[{"name":"USD","unknownField":"value"}]"""
        val parser = CurrencyBackupParser()

        // When
        parser.parse(createReader(json))

        // Then - RuntimeException expected
    }

    @Test
    fun `CurrencyBackupParser parses empty array`() {
        // Given
        val json = """[]"""
        val parser = CurrencyBackupParser()

        // When
        val currencies = parser.parse(createReader(json))

        // Then
        assertTrue(currencies.isEmpty())
    }

    // ========================================
    // ProfileBackupParser tests
    // ========================================

    @Test
    fun `ProfileBackupParser parses single profile`() {
        // Given - use correct BackupKeys values
        val json = """[{
            "uuid":"test-uuid-123",
            "name":"Test Profile",
            "url":"https://example.com/ledger",
            "useAuth":false,
            "permitPosting":true
        }]"""
        val parser = ProfileBackupParser()

        // When
        val profiles = parser.parse(createReader(json))

        // Then
        assertEquals(1, profiles.size)
        assertEquals("test-uuid-123", profiles[0].uuid)
        assertEquals("Test Profile", profiles[0].name)
        assertEquals("https://example.com/ledger", profiles[0].url)
        assertFalse(profiles[0].useAuthentication)
        assertTrue(profiles[0].permitPosting)
    }

    @Test
    fun `ProfileBackupParser parses profile with authentication`() {
        // Given
        val json = """[{
            "uuid":"auth-uuid",
            "name":"Auth Profile",
            "url":"https://example.com",
            "useAuth":true,
            "authUser":"testuser",
            "authPass":"testpass"
        }]"""
        val parser = ProfileBackupParser()

        // When
        val profiles = parser.parse(createReader(json))

        // Then
        assertEquals(1, profiles.size)
        assertTrue(profiles[0].useAuthentication)
        assertEquals("testuser", profiles[0].authUser)
        assertEquals("testpass", profiles[0].authPassword)
    }

    @Test
    fun `ProfileBackupParser parses all optional fields`() {
        // Given - use correct BackupKeys values
        val json = """[{
            "uuid":"full-uuid",
            "name":"Full Profile",
            "url":"https://example.com",
            "useAuth":false,
            "apiVersion":3,
            "permitPosting":true,
            "defaultCommodity":"EUR",
            "showCommodityByDefault":true,
            "showCommentsByDefault":false,
            "futureDates":2,
            "preferredAccountsFilter":"Assets",
            "colour":5
        }]"""
        val parser = ProfileBackupParser()

        // When
        val profiles = parser.parse(createReader(json))

        // Then
        assertEquals(1, profiles.size)
        val p = profiles[0]
        assertEquals(3, p.apiVersion)
        assertTrue(p.permitPosting)
        assertEquals("EUR", p.defaultCommodity)
        assertTrue(p.showCommodityByDefault)
        assertFalse(p.showCommentsByDefault)
        assertEquals(2, p.futureDates)
        assertEquals("Assets", p.preferredAccountsFilter)
        assertEquals(5, p.theme)
    }

    @Test
    fun `ProfileBackupParser handles null fields gracefully`() {
        // Given
        val json = """[{
            "uuid":"null-test",
            "name":"Null Test",
            "url":"https://example.com",
            "defaultCommodity":null,
            "preferredAccountsFilter":null
        }]"""
        val parser = ProfileBackupParser()

        // When
        val profiles = parser.parse(createReader(json))

        // Then
        assertEquals(1, profiles.size)
        // null values should leave defaults
        assertNull(profiles[0].defaultCommodity)
        assertNull(profiles[0].preferredAccountsFilter)
    }

    @Test(expected = IllegalStateException::class)
    fun `ProfileBackupParser throws on unknown field`() {
        // Given
        val json = """[{"uuid":"test","name":"Test","unknownField":"value"}]"""
        val parser = ProfileBackupParser()

        // When
        parser.parse(createReader(json))

        // Then - IllegalStateException expected
    }

    @Test
    fun `ProfileBackupParser parses multiple profiles`() {
        // Given
        val json = """[
            {"uuid":"uuid1","name":"Profile 1","url":"https://example1.com"},
            {"uuid":"uuid2","name":"Profile 2","url":"https://example2.com"}
        ]"""
        val parser = ProfileBackupParser()

        // When
        val profiles = parser.parse(createReader(json))

        // Then
        assertEquals(2, profiles.size)
        assertEquals("Profile 1", profiles[0].name)
        assertEquals("Profile 2", profiles[1].name)
    }

    @Test
    fun `ProfileBackupParser parses empty array`() {
        // Given
        val json = """[]"""
        val parser = ProfileBackupParser()

        // When
        val profiles = parser.parse(createReader(json))

        // Then
        assertTrue(profiles.isEmpty())
    }

    // ========================================
    // TemplateBackupParser tests
    // ========================================

    @Test
    fun `TemplateBackupParser parses single template`() {
        // Given
        val json = """[{
            "uuid":"template-uuid",
            "name":"Test Template",
            "regex":".*test.*"
        }]"""
        val parser = TemplateBackupParser()

        // When
        val templates = parser.parse(createReader(json))

        // Then
        assertEquals(1, templates.size)
        assertEquals("template-uuid", templates[0].header.uuid)
        assertEquals("Test Template", templates[0].header.name)
        assertEquals(".*test.*", templates[0].header.regularExpression)
    }

    @Test
    fun `TemplateBackupParser parses template with accounts`() {
        // Given
        val json = """[{
            "uuid":"template-with-accounts",
            "name":"Template With Accounts",
            "regex":".*",
            "accounts":[
                {"name":"Expenses:Food","amount":100.50},
                {"name":"Assets:Bank","amount":-100.50,"negateAmount":true}
            ]
        }]"""
        val parser = TemplateBackupParser()

        // When
        val templates = parser.parse(createReader(json))

        // Then
        assertEquals(1, templates.size)
        assertEquals(2, templates[0].accounts.size)

        val firstAccount = templates[0].accounts[0]
        assertEquals("Expenses:Food", firstAccount.accountName)
        assertEquals(100.50f, firstAccount.amount!!, 0.01f)

        val secondAccount = templates[0].accounts[1]
        assertEquals("Assets:Bank", secondAccount.accountName)
        assertTrue(secondAccount.negateAmount!!)
    }

    @Test
    fun `TemplateBackupParser converts Double to Float for amount`() {
        // Given - JSON numbers are parsed as Double, should be converted to Float
        val json = """[{
            "uuid":"amount-test",
            "name":"Amount Test",
            "regex":".*",
            "accounts":[{"name":"Account","amount":123.456}]
        }]"""
        val parser = TemplateBackupParser()

        // When
        val templates = parser.parse(createReader(json))

        // Then
        val amount = templates[0].accounts[0].amount
        assertEquals(123.456f, amount!!, 0.001f)
    }

    @Test
    fun `TemplateBackupParser parses all header fields`() {
        // Given - use correct BackupKeys values
        val json = """[{
            "uuid":"full-template",
            "name":"Full Template",
            "regex":"(\\d{4})-(\\d{2})-(\\d{2})",
            "testText":"2025-01-15 Sample text",
            "dateYear":2025,
            "dateYearMatchGroup":1,
            "dateMonth":1,
            "dateMonthMatchGroup":2,
            "dateDay":15,
            "dateDayMatchGroup":3,
            "description":"Sample Transaction",
            "descriptionMatchGroup":0,
            "comment":"Transaction comment",
            "commentMatchGroup":0,
            "isFallback":true
        }]"""
        val parser = TemplateBackupParser()

        // When
        val templates = parser.parse(createReader(json))

        // Then
        assertEquals(1, templates.size)
        val header = templates[0].header
        assertEquals("full-template", header.uuid)
        assertEquals("Full Template", header.name)
        assertEquals("(\\d{4})-(\\d{2})-(\\d{2})", header.regularExpression)
        assertEquals("2025-01-15 Sample text", header.testText)
        assertEquals(2025, header.dateYear)
        assertEquals(1, header.dateYearMatchGroup)
        assertEquals(1, header.dateMonth)
        assertEquals(2, header.dateMonthMatchGroup)
        assertEquals(15, header.dateDay)
        assertEquals(3, header.dateDayMatchGroup)
        assertEquals("Sample Transaction", header.transactionDescription)
        assertEquals(0, header.transactionDescriptionMatchGroup)
        assertEquals("Transaction comment", header.transactionComment)
        assertEquals(0, header.transactionCommentMatchGroup)
        assertTrue(header.isFallback)
    }

    @Test
    fun `TemplateBackupParser parses all account fields`() {
        // Given - use correct BackupKeys values
        val json = """[{
            "uuid":"account-fields",
            "name":"Account Fields Test",
            "regex":".*",
            "accounts":[{
                "name":"Expenses:Food",
                "nameMatchGroup":1,
                "comment":"Account comment",
                "commentMatchGroup":2,
                "amount":50.00,
                "amountGroup":3,
                "negateAmount":true,
                "commodity":100,
                "commodityGroup":4
            }]
        }]"""
        val parser = TemplateBackupParser()

        // When
        val templates = parser.parse(createReader(json))

        // Then
        assertEquals(1, templates[0].accounts.size)
        val account = templates[0].accounts[0]
        assertEquals("Expenses:Food", account.accountName)
        assertEquals(1, account.accountNameMatchGroup)
        assertEquals("Account comment", account.accountComment)
        assertEquals(2, account.accountCommentMatchGroup)
        assertEquals(50.00f, account.amount!!, 0.01f)
        assertEquals(3, account.amountMatchGroup)
        assertTrue(account.negateAmount!!)
        assertEquals(100L, account.currency)
        assertEquals(4, account.currencyMatchGroup)
    }

    @Test
    fun `TemplateBackupParser handles null fields gracefully`() {
        // Given
        val json = """[{
            "uuid":"null-test",
            "name":"Null Test",
            "regex":".*",
            "testText":null,
            "description":null,
            "accounts":[{"name":null,"amount":null}]
        }]"""
        val parser = TemplateBackupParser()

        // When
        val templates = parser.parse(createReader(json))

        // Then
        assertEquals(1, templates.size)
        // null values should leave defaults
        assertNull(templates[0].header.testText)
        assertNull(templates[0].header.transactionDescription)
        assertNull(templates[0].accounts[0].accountName)
        assertNull(templates[0].accounts[0].amount)
    }

    @Test(expected = RuntimeException::class)
    fun `TemplateBackupParser throws on unknown header field`() {
        // Given
        val json = """[{"uuid":"test","name":"Test","regex":".*","unknownField":"value"}]"""
        val parser = TemplateBackupParser()

        // When
        parser.parse(createReader(json))

        // Then - RuntimeException expected
    }

    @Test(expected = IllegalStateException::class)
    fun `TemplateBackupParser throws on unknown account field`() {
        // Given
        val json = """[{
            "uuid":"test",
            "name":"Test",
            "regex":".*",
            "accounts":[{"name":"Account","unknownField":"value"}]
        }]"""
        val parser = TemplateBackupParser()

        // When
        parser.parse(createReader(json))

        // Then - IllegalStateException expected
    }

    @Test
    fun `TemplateBackupParser parses empty accounts array`() {
        // Given
        val json = """[{
            "uuid":"no-accounts",
            "name":"No Accounts",
            "regex":".*",
            "accounts":[]
        }]"""
        val parser = TemplateBackupParser()

        // When
        val templates = parser.parse(createReader(json))

        // Then
        assertEquals(1, templates.size)
        assertTrue(templates[0].accounts.isEmpty())
    }

    @Test
    fun `TemplateBackupParser parses multiple templates`() {
        // Given
        val json = """[
            {"uuid":"uuid1","name":"Template 1","regex":".*1.*"},
            {"uuid":"uuid2","name":"Template 2","regex":".*2.*"}
        ]"""
        val parser = TemplateBackupParser()

        // When
        val templates = parser.parse(createReader(json))

        // Then
        assertEquals(2, templates.size)
        assertEquals("Template 1", templates[0].header.name)
        assertEquals("Template 2", templates[1].header.name)
    }

    @Test
    fun `TemplateBackupParser parses empty array`() {
        // Given
        val json = """[]"""
        val parser = TemplateBackupParser()

        // When
        val templates = parser.parse(createReader(json))

        // Then
        assertTrue(templates.isEmpty())
    }

    // ========================================
    // Helper methods
    // ========================================

    private fun createReader(json: String): JsonReader = JsonReader(StringReader(json))
}
