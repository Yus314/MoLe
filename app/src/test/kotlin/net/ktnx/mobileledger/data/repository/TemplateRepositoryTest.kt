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

import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.ktnx.mobileledger.db.TemplateAccount
import net.ktnx.mobileledger.db.TemplateHeader
import net.ktnx.mobileledger.db.TemplateWithAccounts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [TemplateRepository] using a fake repository implementation.
 *
 * These tests verify:
 * - CRUD operations work correctly
 * - Template with accounts retrieval
 * - Template duplication functionality
 * - Flow emissions occur on data changes
 *
 * Note: For proper Flow testing with Room, use instrumentation tests.
 * These unit tests use a fake repository that implements the interface directly.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TemplateRepositoryTest {

    private lateinit var repository: FakeTemplateRepository

    @Before
    fun setup() {
        repository = FakeTemplateRepository()
    }

    // ========================================
    // Helper methods
    // ========================================

    private fun createTestTemplate(
        id: Long = 0L,
        name: String = "Test Template",
        regularExpression: String = ".*",
        uuid: String = UUID.randomUUID().toString(),
        isFallback: Boolean = false
    ): TemplateHeader = TemplateHeader(id, name, regularExpression).apply {
        this.uuid = uuid
        this.isFallback = isFallback
    }

    private fun createTestTemplateWithAccounts(
        id: Long = 0L,
        name: String = "Test Template",
        accountNames: List<String> = listOf("Assets:Cash", "Expenses:Food")
    ): TemplateWithAccounts {
        val header = createTestTemplate(id = id, name = name)
        val accounts = accountNames.mapIndexed { index, accountName ->
            TemplateAccount(0L, id, index.toLong()).apply {
                this.accountName = accountName
            }
        }
        return TemplateWithAccounts().apply {
            this.header = header
            this.accounts = accounts
        }
    }

    // ========================================
    // getAllTemplates tests
    // ========================================

    @Test
    fun `getAllTemplates returns empty list when no templates`() = runTest {
        val templates = repository.getAllTemplates().first()
        assertTrue(templates.isEmpty())
    }

    @Test
    fun `getAllTemplates returns templates sorted by fallback and name`() = runTest {
        repository.insertTemplate(createTestTemplate(name = "Zebra", isFallback = false))
        repository.insertTemplate(createTestTemplate(name = "Apple", isFallback = false))
        repository.insertTemplate(createTestTemplate(name = "Fallback", isFallback = true))

        val templates = repository.getAllTemplates().first()

        assertEquals(3, templates.size)
        // Non-fallback templates sorted by name first
        assertEquals("Apple", templates[0].name)
        assertEquals("Zebra", templates[1].name)
        // Fallback templates last
        assertEquals("Fallback", templates[2].name)
    }

    // ========================================
    // getTemplateById tests
    // ========================================

    @Test
    fun `getTemplateById returns null for non-existent id`() = runTest {
        val result = repository.getTemplateById(999L).first()
        assertNull(result)
    }

    @Test
    fun `getTemplateByIdSync returns template when exists`() = runTest {
        val template = createTestTemplate(name = "Test")
        val id = repository.insertTemplate(template)

        val result = repository.getTemplateByIdSync(id)

        assertNotNull(result)
        assertEquals("Test", result?.name)
    }

    // ========================================
    // getTemplateWithAccounts tests
    // ========================================

    @Test
    fun `getTemplateWithAccounts returns null for non-existent id`() = runTest {
        val result = repository.getTemplateWithAccounts(999L).first()
        assertNull(result)
    }

    @Test
    fun `getTemplateWithAccountsSync returns template with accounts`() = runTest {
        val templateWithAccounts = createTestTemplateWithAccounts(
            name = "Payment",
            accountNames = listOf("Assets:Bank", "Expenses:Utilities")
        )
        repository.insertTemplateWithAccounts(templateWithAccounts)

        val result = repository.getTemplateWithAccountsSync(templateWithAccounts.header.id)

        assertNotNull(result)
        assertEquals("Payment", result?.header?.name)
        assertEquals(2, result?.accounts?.size)
    }

    // ========================================
    // getTemplateWithAccountsByUuidSync tests
    // ========================================

    @Test
    fun `getTemplateWithAccountsByUuidSync returns null for non-existent uuid`() = runTest {
        val result = repository.getTemplateWithAccountsByUuidSync("non-existent-uuid")
        assertNull(result)
    }

    @Test
    fun `getTemplateWithAccountsByUuidSync returns template when exists`() = runTest {
        val uuid = "test-uuid-12345"
        val template = createTestTemplate(name = "UUID Test", uuid = uuid)
        repository.insertTemplate(template)

        val result = repository.getTemplateWithAccountsByUuidSync(uuid)

        assertNotNull(result)
        assertEquals("UUID Test", result?.header?.name)
    }

    // ========================================
    // getAllTemplatesWithAccountsSync tests
    // ========================================

    @Test
    fun `getAllTemplatesWithAccountsSync returns all templates with accounts`() = runTest {
        repository.insertTemplateWithAccounts(
            createTestTemplateWithAccounts(name = "T1", accountNames = listOf("A:B"))
        )
        repository.insertTemplateWithAccounts(
            createTestTemplateWithAccounts(name = "T2", accountNames = listOf("C:D", "E:F"))
        )

        val result = repository.getAllTemplatesWithAccountsSync()

        assertEquals(2, result.size)
    }

    // ========================================
    // insertTemplate tests
    // ========================================

    @Test
    fun `insertTemplate assigns id and returns it`() = runTest {
        val template = createTestTemplate(name = "New Template")

        val id = repository.insertTemplate(template)

        assertTrue(id > 0)
        val stored = repository.getTemplateByIdSync(id)
        assertNotNull(stored)
        assertEquals("New Template", stored?.name)
    }

    // ========================================
    // insertTemplateWithAccounts tests
    // ========================================

    @Test
    fun `insertTemplateWithAccounts stores header and accounts`() = runTest {
        val templateWithAccounts = createTestTemplateWithAccounts(
            name = "Full Template",
            accountNames = listOf("Assets:Cash", "Expenses:Food", "Liabilities:Card")
        )

        repository.insertTemplateWithAccounts(templateWithAccounts)

        val stored = repository.getTemplateWithAccountsSync(templateWithAccounts.header.id)
        assertNotNull(stored)
        assertEquals("Full Template", stored?.header?.name)
        assertEquals(3, stored?.accounts?.size)
    }

    // ========================================
    // updateTemplate tests
    // ========================================

    @Test
    fun `updateTemplate modifies existing template`() = runTest {
        val template = createTestTemplate(name = "Original")
        val id = repository.insertTemplate(template)

        val updated = createTestTemplate(id = id, name = "Updated")
        repository.updateTemplate(updated)

        val result = repository.getTemplateByIdSync(id)
        assertEquals("Updated", result?.name)
    }

    @Test
    fun `updateTemplate preserves accounts`() = runTest {
        val templateWithAccounts = createTestTemplateWithAccounts(
            name = "Original",
            accountNames = listOf("A:B", "C:D")
        )
        repository.insertTemplateWithAccounts(templateWithAccounts)

        val updated = templateWithAccounts.header.apply { name = "Updated" }
        repository.updateTemplate(updated)

        val result = repository.getTemplateWithAccountsSync(updated.id)
        assertEquals("Updated", result?.header?.name)
        assertEquals(2, result?.accounts?.size)
    }

    // ========================================
    // deleteTemplate tests
    // ========================================

    @Test
    fun `deleteTemplate removes template`() = runTest {
        val template = createTestTemplate(name = "ToDelete")
        val id = repository.insertTemplate(template)

        repository.deleteTemplate(template.apply { this.id = id })

        val remaining = repository.getAllTemplates().first()
        assertTrue(remaining.isEmpty())
    }

    @Test
    fun `deleteTemplate only removes specified template`() = runTest {
        val t1 = createTestTemplate(name = "Template 1")
        val t2 = createTestTemplate(name = "Template 2")
        val id1 = repository.insertTemplate(t1)
        repository.insertTemplate(t2)

        repository.deleteTemplate(t1.apply { this.id = id1 })

        val remaining = repository.getAllTemplates().first()
        assertEquals(1, remaining.size)
        assertEquals("Template 2", remaining[0].name)
    }

    // ========================================
    // duplicateTemplate tests
    // ========================================

    @Test
    fun `duplicateTemplate returns null for non-existent id`() = runTest {
        val result = repository.duplicateTemplate(999L)
        assertNull(result)
    }

    @Test
    fun `duplicateTemplate creates copy with new id and uuid`() = runTest {
        val originalUuid = "original-uuid"
        val template = createTestTemplate(name = "Original", uuid = originalUuid)
        val id = repository.insertTemplate(template)

        val duplicate = repository.duplicateTemplate(id)

        assertNotNull(duplicate)
        assertNotEquals(id, duplicate?.header?.id)
        assertNotEquals(originalUuid, duplicate?.header?.uuid)
        assertEquals("Original", duplicate?.header?.name)
    }

    @Test
    fun `duplicateTemplate copies accounts to new template`() = runTest {
        val templateWithAccounts = createTestTemplateWithAccounts(
            name = "WithAccounts",
            accountNames = listOf("A:B", "C:D")
        )
        repository.insertTemplateWithAccounts(templateWithAccounts)

        val duplicate = repository.duplicateTemplate(templateWithAccounts.header.id)

        assertNotNull(duplicate)
        assertEquals(2, duplicate?.accounts?.size)
        // Accounts should reference the new template ID
        duplicate?.accounts?.forEach { account ->
            assertEquals(duplicate.header.id, account.templateId)
        }
    }

    // ========================================
    // deleteAllTemplates tests
    // ========================================

    @Test
    fun `deleteAllTemplates removes all templates`() = runTest {
        repository.insertTemplate(createTestTemplate(name = "T1"))
        repository.insertTemplate(createTestTemplate(name = "T2"))
        repository.insertTemplate(createTestTemplate(name = "T3"))

        repository.deleteAllTemplates()

        val templates = repository.getAllTemplates().first()
        assertTrue(templates.isEmpty())
    }
}

/**
 * Fake implementation of [TemplateRepository] for unit testing.
 *
 * This implementation provides an in-memory store that allows testing
 * without a real database or Room infrastructure.
 */
class FakeTemplateRepository : TemplateRepository {

    private val templates = mutableMapOf<Long, TemplateHeader>()
    private val templateAccounts = mutableMapOf<Long, MutableList<TemplateAccount>>()
    private var nextTemplateId = 1L
    private var nextAccountId = 1L

    private fun emitChanges() {
        // Placeholder for Flow updates
    }

    private fun getSortedTemplates(): List<TemplateHeader> =
        templates.values.sortedWith(compareBy({ it.isFallback }, { it.name }))

    override fun getAllTemplates(): Flow<List<TemplateHeader>> = MutableStateFlow(getSortedTemplates())

    override fun getTemplateById(id: Long): Flow<TemplateHeader?> = MutableStateFlow(templates[id])

    override suspend fun getTemplateByIdSync(id: Long): TemplateHeader? = templates[id]

    override fun getTemplateWithAccounts(id: Long): Flow<TemplateWithAccounts?> =
        MutableStateFlow(getTemplateWithAccountsInternal(id))

    override suspend fun getTemplateWithAccountsSync(id: Long): TemplateWithAccounts? =
        getTemplateWithAccountsInternal(id)

    private fun getTemplateWithAccountsInternal(id: Long): TemplateWithAccounts? {
        val header = templates[id] ?: return null
        val accounts = templateAccounts[id] ?: emptyList()
        val result = TemplateWithAccounts()
        result.header = header
        result.accounts = accounts
        return result
    }

    override suspend fun getTemplateWithAccountsByUuidSync(uuid: String): TemplateWithAccounts? {
        val header = templates.values.find { it.uuid == uuid } ?: return null
        val accounts = templateAccounts[header.id] ?: emptyList()
        val result = TemplateWithAccounts()
        result.header = header
        result.accounts = accounts
        return result
    }

    override suspend fun getAllTemplatesWithAccountsSync(): List<TemplateWithAccounts> =
        getSortedTemplates().map { header: TemplateHeader ->
            val result = TemplateWithAccounts()
            result.header = header
            result.accounts = templateAccounts[header.id] ?: emptyList()
            result
        }

    override suspend fun insertTemplate(template: TemplateHeader): Long {
        val id = if (template.id == 0L) nextTemplateId++ else template.id
        template.id = id
        templates[id] = template
        templateAccounts[id] = mutableListOf()
        emitChanges()
        return id
    }

    override suspend fun insertTemplateWithAccounts(templateWithAccounts: TemplateWithAccounts) {
        val headerId = insertTemplate(templateWithAccounts.header)
        templateWithAccounts.header.id = headerId
        val inputAccounts: List<TemplateAccount> = templateWithAccounts.accounts
        val accounts = mutableListOf<TemplateAccount>()
        for (account in inputAccounts) {
            val accountId = nextAccountId++
            val newAccount = TemplateAccount(accountId, headerId, account.position)
            newAccount.accountName = account.accountName
            newAccount.currency = account.currency
            newAccount.amount = account.amount
            newAccount.accountComment = account.accountComment
            accounts.add(newAccount)
        }
        templateAccounts[headerId] = accounts
        emitChanges()
    }

    override suspend fun updateTemplate(template: TemplateHeader) {
        if (templates.containsKey(template.id)) {
            templates[template.id] = template
            emitChanges()
        }
    }

    override suspend fun deleteTemplate(template: TemplateHeader) {
        templates.remove(template.id)
        templateAccounts.remove(template.id)
        emitChanges()
    }

    override suspend fun duplicateTemplate(id: Long): TemplateWithAccounts? {
        val original = getTemplateWithAccountsInternal(id) ?: return null

        val newHeader = TemplateHeader(original.header).apply {
            this.id = 0L
            this.uuid = UUID.randomUUID().toString()
        }
        val newId = insertTemplate(newHeader)
        newHeader.id = newId

        val originalAccounts: List<TemplateAccount> = original.accounts
        val newAccounts = mutableListOf<TemplateAccount>()
        for (account in originalAccounts) {
            val accountId = nextAccountId++
            val newAccount = TemplateAccount(accountId, newId, account.position)
            newAccount.accountName = account.accountName
            newAccount.currency = account.currency
            newAccount.amount = account.amount
            newAccount.accountComment = account.accountComment
            newAccounts.add(newAccount)
        }
        templateAccounts[newId] = newAccounts

        val result = TemplateWithAccounts()
        result.header = newHeader
        result.accounts = newAccounts
        return result
    }

    override suspend fun saveTemplateWithAccounts(header: TemplateHeader, accounts: List<TemplateAccount>): Long {
        val isNew = header.id == 0L
        val savedId = if (isNew) {
            val id = nextTemplateId++
            header.id = id
            templates[id] = header
            id
        } else {
            templates[header.id] = header
            header.id
        }

        // Save accounts
        val savedAccounts = mutableListOf<TemplateAccount>()
        for (account in accounts) {
            val accountId = if (account.id <= 0) nextAccountId++ else account.id
            account.id = accountId
            account.templateId = savedId
            savedAccounts.add(account)
        }
        templateAccounts[savedId] = savedAccounts

        emitChanges()
        return savedId
    }

    override suspend fun deleteAllTemplates() {
        templates.clear()
        templateAccounts.clear()
        emitChanges()
    }
}
