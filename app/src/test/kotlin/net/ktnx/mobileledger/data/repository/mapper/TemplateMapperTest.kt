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

package net.ktnx.mobileledger.data.repository.mapper

import net.ktnx.mobileledger.data.repository.mapper.TemplateMapper.toDomain
import net.ktnx.mobileledger.data.repository.mapper.TemplateMapper.toEntity
import net.ktnx.mobileledger.db.TemplateAccount
import net.ktnx.mobileledger.db.TemplateHeader
import net.ktnx.mobileledger.db.TemplateWithAccounts
import net.ktnx.mobileledger.domain.model.Template
import net.ktnx.mobileledger.domain.model.TemplateLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateMapperTest {

    // ========================================
    // toDomain tests
    // ========================================

    @Test
    fun `toDomain maps basic template correctly`() {
        val header = createTemplateHeader(
            id = 1L,
            name = "Test Template",
            regularExpression = ".*test.*"
        )
        val accounts = listOf(
            createTemplateAccount(id = 1L, templateId = 1L, position = 0, accountName = "Assets:Cash"),
            createTemplateAccount(id = 2L, templateId = 1L, position = 1, accountName = "Expenses:Food")
        )
        val entity = createTemplateWithAccounts(header, accounts)

        val domain = entity.toDomain()

        assertEquals(1L, domain.id)
        assertEquals("Test Template", domain.name)
        assertEquals(".*test.*", domain.pattern)
        assertEquals(2, domain.lines.size)
    }

    @Test
    fun `toDomain maps template with all fields`() {
        val header = createTemplateHeader(
            id = 1L,
            name = "Full Template",
            regularExpression = "(\\d+)-(\\d+)-(\\d+)"
        ).apply {
            testText = "2026-01-16"
            transactionDescription = "Test Description"
            transactionDescriptionMatchGroup = 1
            transactionComment = "Test Comment"
            transactionCommentMatchGroup = 2
            dateYear = 2026
            dateYearMatchGroup = 3
            dateMonth = 1
            dateMonthMatchGroup = 4
            dateDay = 16
            dateDayMatchGroup = 5
            isFallback = true
        }
        val entity = createTemplateWithAccounts(header, emptyList())

        val domain = entity.toDomain()

        assertEquals("2026-01-16", domain.testText)
        assertEquals("Test Description", domain.transactionDescription)
        assertEquals(1, domain.transactionDescriptionMatchGroup)
        assertEquals("Test Comment", domain.transactionComment)
        assertEquals(2, domain.transactionCommentMatchGroup)
        assertEquals(2026, domain.dateYear)
        assertEquals(3, domain.dateYearMatchGroup)
        assertEquals(1, domain.dateMonth)
        assertEquals(4, domain.dateMonthMatchGroup)
        assertEquals(16, domain.dateDay)
        assertEquals(5, domain.dateDayMatchGroup)
        assertTrue(domain.isFallback)
    }

    @Test
    fun `toDomain with empty accounts`() {
        val header = createTemplateHeader(id = 1L, name = "Empty", regularExpression = ".*")
        val entity = createTemplateWithAccounts(header, emptyList())

        val domain = entity.toDomain()

        assertTrue(domain.lines.isEmpty())
    }

    @Test
    fun `toDomain with null optional fields`() {
        val header = createTemplateHeader(id = 1L, name = "Minimal", regularExpression = ".*")
        val entity = createTemplateWithAccounts(header, emptyList())

        val domain = entity.toDomain()

        assertNull(domain.testText)
        assertNull(domain.transactionDescription)
        assertNull(domain.transactionDescriptionMatchGroup)
        assertNull(domain.transactionComment)
        assertNull(domain.transactionCommentMatchGroup)
        assertNull(domain.dateYear)
        assertNull(domain.dateYearMatchGroup)
        assertNull(domain.dateMonth)
        assertNull(domain.dateMonthMatchGroup)
        assertNull(domain.dateDay)
        assertNull(domain.dateDayMatchGroup)
        assertFalse(domain.isFallback)
    }

    @Test
    fun `toDomain sorts accounts by position`() {
        val header = createTemplateHeader(id = 1L, name = "Test", regularExpression = ".*")
        val accounts = listOf(
            createTemplateAccount(id = 2L, templateId = 1L, position = 2, accountName = "Third"),
            createTemplateAccount(id = 1L, templateId = 1L, position = 0, accountName = "First"),
            createTemplateAccount(id = 3L, templateId = 1L, position = 1, accountName = "Second")
        )
        val entity = createTemplateWithAccounts(header, accounts)

        val domain = entity.toDomain()

        assertEquals("First", domain.lines[0].accountName)
        assertEquals("Second", domain.lines[1].accountName)
        assertEquals("Third", domain.lines[2].accountName)
    }

    @Test
    fun `toDomain maps template account with all fields`() {
        val header = createTemplateHeader(id = 1L, name = "Test", regularExpression = ".*")
        val account = createTemplateAccount(
            id = 1L,
            templateId = 1L,
            position = 0,
            accountName = "Assets:Cash"
        ).apply {
            accountNameMatchGroup = 1
            amount = 100f
            amountMatchGroup = 2
            currency = 5L
            currencyMatchGroup = 3
            accountComment = "Comment"
            accountCommentMatchGroup = 4
            negateAmount = true
        }
        val entity = createTemplateWithAccounts(header, listOf(account))

        val domain = entity.toDomain()
        val line = domain.lines[0]

        assertEquals(1L, line.id)
        assertEquals("Assets:Cash", line.accountName)
        assertEquals(1, line.accountNameGroup)
        assertEquals(100f, line.amount)
        assertEquals(2, line.amountGroup)
        assertEquals(5L, line.currencyId)
        assertEquals(3, line.currencyGroup)
        assertEquals("Comment", line.comment)
        assertEquals(4, line.commentGroup)
        assertTrue(line.negateAmount)
    }

    // ========================================
    // toEntity tests
    // ========================================

    @Test
    fun `toEntity maps new template correctly`() {
        val domain = Template(
            name = "New Template",
            pattern = ".*test.*",
            lines = listOf(
                TemplateLine(accountName = "Assets:Cash"),
                TemplateLine(accountName = "Expenses:Food")
            )
        )

        val entity = domain.toEntity()

        assertEquals(0L, entity.header.id)
        assertEquals("New Template", entity.header.name)
        assertEquals(".*test.*", entity.header.regularExpression)
        assertEquals(2, entity.accounts.size)
    }

    @Test
    fun `toEntity maps existing template correctly`() {
        val domain = Template(
            id = 123L,
            name = "Existing Template",
            pattern = ".*"
        )

        val entity = domain.toEntity()

        assertEquals(123L, entity.header.id)
    }

    @Test
    fun `toEntity maps all header fields`() {
        val domain = Template(
            id = 1L,
            name = "Full Template",
            pattern = "(\\d+)-(\\d+)-(\\d+)",
            testText = "2026-01-16",
            transactionDescription = "Test Description",
            transactionDescriptionMatchGroup = 1,
            transactionComment = "Test Comment",
            transactionCommentMatchGroup = 2,
            dateYear = 2026,
            dateYearMatchGroup = 3,
            dateMonth = 1,
            dateMonthMatchGroup = 4,
            dateDay = 16,
            dateDayMatchGroup = 5,
            isFallback = true
        )

        val entity = domain.toEntity()
        val header = entity.header

        assertEquals("2026-01-16", header.testText)
        assertEquals("Test Description", header.transactionDescription)
        assertEquals(1, header.transactionDescriptionMatchGroup)
        assertEquals("Test Comment", header.transactionComment)
        assertEquals(2, header.transactionCommentMatchGroup)
        assertEquals(2026, header.dateYear)
        assertEquals(3, header.dateYearMatchGroup)
        assertEquals(1, header.dateMonth)
        assertEquals(4, header.dateMonthMatchGroup)
        assertEquals(16, header.dateDay)
        assertEquals(5, header.dateDayMatchGroup)
        assertTrue(header.isFallback)
    }

    @Test
    fun `toEntity maps template line with position`() {
        val domain = Template(
            name = "Test",
            pattern = ".*",
            lines = listOf(
                TemplateLine(accountName = "First"),
                TemplateLine(accountName = "Second"),
                TemplateLine(accountName = "Third")
            )
        )

        val entity = domain.toEntity()

        assertEquals(0L, entity.accounts[0].position)
        assertEquals(1L, entity.accounts[1].position)
        assertEquals(2L, entity.accounts[2].position)
    }

    @Test
    fun `toEntity maps template line all fields`() {
        val line = TemplateLine(
            id = 1L,
            accountName = "Assets:Cash",
            accountNameGroup = 1,
            amount = 100f,
            amountGroup = 2,
            currencyId = 5L,
            currencyGroup = 3,
            comment = "Comment",
            commentGroup = 4,
            negateAmount = true
        )
        val domain = Template(
            id = 10L,
            name = "Test",
            pattern = ".*",
            lines = listOf(line)
        )

        val entity = domain.toEntity()
        val account = entity.accounts[0]

        assertEquals(1L, account.id)
        assertEquals(10L, account.templateId)
        assertEquals("Assets:Cash", account.accountName)
        assertEquals(1, account.accountNameMatchGroup)
        assertEquals(100f, account.amount)
        assertEquals(2, account.amountMatchGroup)
        assertEquals(5L, account.currency)
        assertEquals(3, account.currencyMatchGroup)
        assertEquals("Comment", account.accountComment)
        assertEquals(4, account.accountCommentMatchGroup)
        assertTrue(account.negateAmount == true)
    }

    @Test
    fun `toEntity sets new line id to 0`() {
        val line = TemplateLine(accountName = "Assets:Cash")
        val domain = Template(
            name = "Test",
            pattern = ".*",
            lines = listOf(line)
        )

        val entity = domain.toEntity()

        assertEquals(0L, entity.accounts[0].id)
    }

    // ========================================
    // Round-trip tests
    // ========================================

    @Test
    fun `roundTrip preserves basic data`() {
        val original = Template(
            id = 1L,
            name = "Test Template",
            pattern = ".*test.*",
            lines = listOf(
                TemplateLine(id = 1L, accountName = "Assets:Cash", amount = 100f),
                TemplateLine(id = 2L, accountName = "Expenses:Food", amount = -100f)
            )
        )

        val roundTripped = original.toEntity().toDomain()

        assertEquals(original.id, roundTripped.id)
        assertEquals(original.name, roundTripped.name)
        assertEquals(original.pattern, roundTripped.pattern)
        assertEquals(original.lines.size, roundTripped.lines.size)
        assertEquals(original.lines[0].accountName, roundTripped.lines[0].accountName)
        assertEquals(original.lines[0].amount, roundTripped.lines[0].amount)
    }

    @Test
    fun `roundTrip preserves all fields`() {
        val original = Template(
            id = 1L,
            name = "Full Template",
            pattern = "(\\d+)",
            testText = "123",
            transactionDescription = "Desc",
            transactionDescriptionMatchGroup = 1,
            transactionComment = "Comment",
            transactionCommentMatchGroup = 2,
            dateYear = 2026,
            dateYearMatchGroup = 3,
            dateMonth = 1,
            dateMonthMatchGroup = 4,
            dateDay = 16,
            dateDayMatchGroup = 5,
            isFallback = true,
            lines = listOf(
                TemplateLine(
                    id = 1L,
                    accountName = "Account",
                    accountNameGroup = 6,
                    amount = 100f,
                    amountGroup = 7,
                    currencyId = 5L,
                    currencyGroup = 8,
                    comment = "LineComment",
                    commentGroup = 9,
                    negateAmount = true
                )
            )
        )

        val roundTripped = original.toEntity().toDomain()

        assertEquals(original.testText, roundTripped.testText)
        assertEquals(original.transactionDescription, roundTripped.transactionDescription)
        assertEquals(original.transactionDescriptionMatchGroup, roundTripped.transactionDescriptionMatchGroup)
        assertEquals(original.transactionComment, roundTripped.transactionComment)
        assertEquals(original.transactionCommentMatchGroup, roundTripped.transactionCommentMatchGroup)
        assertEquals(original.dateYear, roundTripped.dateYear)
        assertEquals(original.dateYearMatchGroup, roundTripped.dateYearMatchGroup)
        assertEquals(original.dateMonth, roundTripped.dateMonth)
        assertEquals(original.dateMonthMatchGroup, roundTripped.dateMonthMatchGroup)
        assertEquals(original.dateDay, roundTripped.dateDay)
        assertEquals(original.dateDayMatchGroup, roundTripped.dateDayMatchGroup)
        assertEquals(original.isFallback, roundTripped.isFallback)

        val originalLine = original.lines[0]
        val roundTrippedLine = roundTripped.lines[0]
        assertEquals(originalLine.accountName, roundTrippedLine.accountName)
        assertEquals(originalLine.accountNameGroup, roundTrippedLine.accountNameGroup)
        assertEquals(originalLine.amount, roundTrippedLine.amount)
        assertEquals(originalLine.amountGroup, roundTrippedLine.amountGroup)
        assertEquals(originalLine.currencyId, roundTrippedLine.currencyId)
        assertEquals(originalLine.currencyGroup, roundTrippedLine.currencyGroup)
        assertEquals(originalLine.comment, roundTrippedLine.comment)
        assertEquals(originalLine.commentGroup, roundTrippedLine.commentGroup)
        assertEquals(originalLine.negateAmount, roundTrippedLine.negateAmount)
    }

    // ========================================
    // Helper functions
    // ========================================

    private fun createTemplateHeader(id: Long, name: String, regularExpression: String): TemplateHeader =
        TemplateHeader(id, name, regularExpression)

    private fun createTemplateAccount(
        id: Long,
        templateId: Long,
        position: Long,
        accountName: String? = null
    ): TemplateAccount = TemplateAccount(id, templateId, position).apply {
        this.accountName = accountName
    }

    private fun createTemplateWithAccounts(
        header: TemplateHeader,
        accounts: List<TemplateAccount>
    ): TemplateWithAccounts = TemplateWithAccounts().apply {
        this.header = header
        this.accounts = accounts
    }
}
