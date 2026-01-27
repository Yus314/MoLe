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

package net.ktnx.mobileledger.data.repository

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.ktnx.mobileledger.core.database.dao.TemplateAccountDAO
import net.ktnx.mobileledger.core.database.dao.TemplateHeaderDAO
import net.ktnx.mobileledger.core.database.entity.TemplateAccount
import net.ktnx.mobileledger.core.database.entity.TemplateHeader
import net.ktnx.mobileledger.core.database.entity.TemplateWithAccounts
import net.ktnx.mobileledger.core.domain.model.AppException
import net.ktnx.mobileledger.core.domain.model.Template
import net.ktnx.mobileledger.core.domain.model.TemplateLine
import net.ktnx.mobileledger.core.domain.repository.CurrencyRepository
import net.ktnx.mobileledger.domain.usecase.AppExceptionMapper
import net.ktnx.mobileledger.domain.usecase.sync.SyncExceptionMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [TemplateRepositoryImpl].
 *
 * Tests verify:
 * - Query operations (observe and get)
 * - Mutation operations (save, delete, duplicate)
 * - Error handling with Result<T>
 * - Domain model mapping
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TemplateRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockTemplateHeaderDAO: TemplateHeaderDAO
    private lateinit var mockTemplateAccountDAO: TemplateAccountDAO
    private lateinit var mockCurrencyRepository: CurrencyRepository
    private lateinit var appExceptionMapper: AppExceptionMapper
    private lateinit var repository: TemplateRepositoryImpl

    @Before
    fun setup() {
        mockTemplateHeaderDAO = mockk(relaxed = true)
        mockTemplateAccountDAO = mockk(relaxed = true)
        mockCurrencyRepository = mockk(relaxed = true)
        appExceptionMapper = AppExceptionMapper(SyncExceptionMapper())

        repository = TemplateRepositoryImpl(
            templateHeaderDAO = mockTemplateHeaderDAO,
            templateAccountDAO = mockTemplateAccountDAO,
            currencyRepository = mockCurrencyRepository,
            appExceptionMapper = appExceptionMapper,
            ioDispatcher = testDispatcher
        )
    }

    // ========================================
    // Helper methods
    // ========================================

    private fun createDbTemplateHeader(
        id: Long = 1L,
        name: String = "Test Template",
        pattern: String = ".*",
        uuid: String = UUID.randomUUID().toString()
    ): TemplateHeader = TemplateHeader(id, name, pattern).apply {
        this.uuid = uuid
        this.isFallback = false
    }

    private fun createDbTemplateAccount(
        id: Long = 1L,
        templateId: Long = 1L,
        accountName: String = "Expenses:Food",
        position: Long = 0L
    ): TemplateAccount = TemplateAccount(id, templateId, position).apply {
        this.accountName = accountName
    }

    private fun createDbTemplateWithAccounts(
        header: TemplateHeader = createDbTemplateHeader(),
        accounts: List<TemplateAccount> = listOf(
            createDbTemplateAccount(position = 0L),
            createDbTemplateAccount(id = 2L, accountName = "Assets:Bank", position = 1L)
        )
    ): TemplateWithAccounts = TemplateWithAccounts().apply {
        this.header = header
        this.accounts = accounts
    }

    private fun createDomainTemplate(
        id: Long? = null,
        name: String = "Test Template",
        pattern: String = ".*"
    ): Template = Template(
        id = id,
        name = name,
        pattern = pattern,
        testText = null,
        isFallback = false,
        transactionDescription = null,
        transactionDescriptionMatchGroup = null,
        transactionComment = null,
        transactionCommentMatchGroup = null,
        dateYear = null,
        dateYearMatchGroup = null,
        dateMonth = null,
        dateMonthMatchGroup = null,
        dateDay = null,
        dateDayMatchGroup = null,
        lines = listOf(
            TemplateLine(
                id = null,
                accountName = "Expenses:Food",
                accountNameGroup = null,
                amount = 100.0f,
                amountGroup = null,
                currencyId = null,
                currencyName = null,
                currencyGroup = null,
                comment = null,
                commentGroup = null,
                negateAmount = false
            )
        )
    )

    // ========================================
    // Query Operations - Flow tests
    // ========================================

    @Test
    fun `observeAllTemplatesAsDomain returns mapped domain models`() = runTest(testDispatcher) {
        // Given
        val dbEntities = listOf(createDbTemplateWithAccounts())
        every { mockTemplateHeaderDAO.getTemplatesWithAccounts() } returns flowOf(dbEntities)

        // When
        val result = repository.observeAllTemplatesAsDomain().first()

        // Then
        assertEquals(1, result.size)
        assertEquals("Test Template", result[0].name)
        verify { mockTemplateHeaderDAO.getTemplatesWithAccounts() }
    }

    @Test
    fun `observeAllTemplatesAsDomain returns empty list when no templates`() = runTest(testDispatcher) {
        // Given
        every { mockTemplateHeaderDAO.getTemplatesWithAccounts() } returns flowOf(emptyList())

        // When
        val result = repository.observeAllTemplatesAsDomain().first()

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `observeTemplateAsDomain returns mapped domain model`() = runTest(testDispatcher) {
        // Given
        val dbEntity = createDbTemplateWithAccounts()
        every { mockTemplateHeaderDAO.getTemplateWithAccounts(1L) } returns flowOf(dbEntity)

        // When
        val result = repository.observeTemplateAsDomain(1L).first()

        // Then
        assertNotNull(result)
        assertEquals("Test Template", result?.name)
    }

    // Note: Flow-based "not found" test removed because DAO returns Flow<TemplateWithAccounts>
    // (non-nullable). The sync version getTemplateWithAccountsSync returns nullable and is tested below.

    // ========================================
    // Query Operations - Suspend tests
    // ========================================

    @Test
    fun `getTemplateAsDomain returns Result success with mapped domain model`() = runTest(testDispatcher) {
        // Given
        val dbEntity = createDbTemplateWithAccounts()
        coEvery { mockTemplateHeaderDAO.getTemplateWithAccountsSync(1L) } returns dbEntity

        // When
        val result = repository.getTemplateAsDomain(1L)

        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
        assertEquals("Test Template", result.getOrNull()?.name)
    }

    @Test
    fun `getTemplateAsDomain returns null when not found`() = runTest(testDispatcher) {
        // Given
        coEvery { mockTemplateHeaderDAO.getTemplateWithAccountsSync(999L) } returns null

        // When
        val result = repository.getTemplateAsDomain(999L)

        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `getTemplateAsDomain returns Result failure on exception`() = runTest(testDispatcher) {
        // Given
        coEvery { mockTemplateHeaderDAO.getTemplateWithAccountsSync(1L) } throws RuntimeException("DB error")

        // When
        val result = repository.getTemplateAsDomain(1L)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppException)
    }

    @Test
    fun `getAllTemplatesAsDomain returns all templates`() = runTest(testDispatcher) {
        // Given
        val dbEntities = listOf(
            createDbTemplateWithAccounts(),
            createDbTemplateWithAccounts(header = createDbTemplateHeader(id = 2L, name = "Template 2"))
        )
        coEvery { mockTemplateHeaderDAO.getAllTemplatesWithAccountsSync() } returns dbEntities

        // When
        val result = repository.getAllTemplatesAsDomain()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)
    }

    // ========================================
    // Mutation Operations tests
    // ========================================

    @Test
    fun `saveTemplate inserts new template`() = runTest(testDispatcher) {
        // Given
        val domainTemplate = createDomainTemplate(id = null)
        coEvery { mockTemplateHeaderDAO.insertSync(any()) } returns 1L
        coEvery { mockTemplateAccountDAO.prepareForSave(any()) } just Runs
        coEvery { mockTemplateAccountDAO.insertSync(any()) } returns 1L
        coEvery { mockTemplateAccountDAO.finishSave(any()) } just Runs

        // When
        val result = repository.saveTemplate(domainTemplate)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1L, result.getOrNull())
        coVerify { mockTemplateHeaderDAO.insertSync(any()) }
    }

    @Test
    fun `saveTemplate updates existing template`() = runTest(testDispatcher) {
        // Given
        val domainTemplate = createDomainTemplate(id = 1L)
        coEvery { mockTemplateHeaderDAO.updateSync(any()) } just Runs
        coEvery { mockTemplateAccountDAO.prepareForSave(any()) } just Runs
        coEvery { mockTemplateAccountDAO.updateSync(any()) } just Runs
        coEvery { mockTemplateAccountDAO.finishSave(any()) } just Runs

        // When
        val result = repository.saveTemplate(domainTemplate)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1L, result.getOrNull())
        coVerify { mockTemplateHeaderDAO.updateSync(any()) }
    }

    @Test
    fun `deleteTemplateById deletes existing template`() = runTest(testDispatcher) {
        // Given
        val dbHeader = createDbTemplateHeader()
        coEvery { mockTemplateHeaderDAO.getTemplateSync(1L) } returns dbHeader
        coEvery { mockTemplateHeaderDAO.deleteSync(any()) } just Runs

        // When
        val result = repository.deleteTemplateById(1L)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
        coVerify { mockTemplateHeaderDAO.deleteSync(dbHeader) }
    }

    @Test
    fun `deleteTemplateById returns false when template not found`() = runTest(testDispatcher) {
        // Given
        coEvery { mockTemplateHeaderDAO.getTemplateSync(999L) } returns null

        // When
        val result = repository.deleteTemplateById(999L)

        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() == true)
    }

    @Test
    fun `duplicateTemplate creates copy of existing template`() = runTest(testDispatcher) {
        // Given
        val originalEntity = createDbTemplateWithAccounts()
        coEvery { mockTemplateHeaderDAO.getTemplateWithAccountsSync(1L) } returns originalEntity
        coEvery { mockTemplateHeaderDAO.insertSync(any()) } returns 2L
        coEvery { mockTemplateAccountDAO.insertSync(any()) } returns 1L

        // When
        val result = repository.duplicateTemplate(1L)

        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
        coVerify { mockTemplateHeaderDAO.insertSync(any()) }
    }

    @Test
    fun `duplicateTemplate returns null when template not found`() = runTest(testDispatcher) {
        // Given
        coEvery { mockTemplateHeaderDAO.getTemplateWithAccountsSync(999L) } returns null

        // When
        val result = repository.duplicateTemplate(999L)

        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `deleteAllTemplates clears all templates`() = runTest(testDispatcher) {
        // Given
        coEvery { mockTemplateHeaderDAO.deleteAllSync() } just Runs

        // When
        val result = repository.deleteAllTemplates()

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockTemplateHeaderDAO.deleteAllSync() }
    }
}
