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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.ktnx.mobileledger.core.domain.model.Template
import net.ktnx.mobileledger.core.domain.model.TemplateLine
import net.ktnx.mobileledger.core.testing.fake.FakeTemplateRepository
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
 * - Template with lines retrieval
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
        id: Long? = null,
        name: String = "Test Template",
        pattern: String = ".*",
        isFallback: Boolean = false,
        lines: List<TemplateLine> = emptyList()
    ): Template = Template(
        id = id,
        name = name,
        pattern = pattern,
        isFallback = isFallback,
        lines = lines
    )

    private fun createTestLine(
        id: Long? = null,
        accountName: String = "Assets:Cash",
        amount: Float? = null,
        currencyId: Long? = null
    ): TemplateLine = TemplateLine(
        id = id,
        accountName = accountName,
        amount = amount,
        currencyId = currencyId
    )

    // ========================================
    // observeAllTemplatesAsDomain tests
    // ========================================

    @Test
    fun `observeAllTemplatesAsDomain returns empty list when no templates`() = runTest {
        val templates = repository.observeAllTemplatesAsDomain().first()
        assertTrue(templates.isEmpty())
    }

    @Test
    fun `observeAllTemplatesAsDomain returns templates sorted by fallback and name`() = runTest {
        repository.saveTemplate(createTestTemplate(name = "Zebra", isFallback = false)).getOrThrow()
        repository.saveTemplate(createTestTemplate(name = "Apple", isFallback = false)).getOrThrow()
        repository.saveTemplate(createTestTemplate(name = "Fallback", isFallback = true)).getOrThrow()

        val templates = repository.observeAllTemplatesAsDomain().first()

        assertEquals(3, templates.size)
        // Non-fallback templates sorted by name first
        assertEquals("Apple", templates[0].name)
        assertEquals("Zebra", templates[1].name)
        // Fallback templates last
        assertEquals("Fallback", templates[2].name)
    }

    // ========================================
    // observeTemplateAsDomain tests
    // ========================================

    @Test
    fun `observeTemplateAsDomain returns null for non-existent id`() = runTest {
        val result = repository.observeTemplateAsDomain(999L).first()
        assertNull(result)
    }

    @Test
    fun `getTemplateAsDomain returns template when exists`() = runTest {
        val template = createTestTemplate(name = "Test")
        val id = repository.saveTemplate(template).getOrThrow()

        val result = repository.getTemplateAsDomain(id).getOrNull()

        assertNotNull(result)
        assertEquals("Test", result?.name)
    }

    // ========================================
    // getTemplateAsDomain with lines tests
    // ========================================

    @Test
    fun `getTemplateAsDomain returns null for non-existent id`() = runTest {
        val result = repository.getTemplateAsDomain(999L).getOrNull()
        assertNull(result)
    }

    @Test
    fun `getTemplateAsDomain returns template with lines`() = runTest {
        val template = createTestTemplate(
            name = "Payment",
            lines = listOf(
                createTestLine(accountName = "Assets:Bank"),
                createTestLine(accountName = "Expenses:Utilities")
            )
        )
        val id = repository.saveTemplate(template).getOrThrow()

        val result = repository.getTemplateAsDomain(id).getOrNull()

        assertNotNull(result)
        assertEquals("Payment", result?.name)
        assertEquals(2, result?.lines?.size)
    }

    // ========================================
    // getAllTemplatesAsDomain tests
    // ========================================

    @Test
    fun `getAllTemplatesAsDomain returns all templates with lines`() = runTest {
        repository.saveTemplate(
            createTestTemplate(name = "T1", lines = listOf(createTestLine(accountName = "A:B")))
        ).getOrThrow()
        repository.saveTemplate(
            createTestTemplate(
                name = "T2",
                lines = listOf(
                    createTestLine(accountName = "C:D"),
                    createTestLine(accountName = "E:F")
                )
            )
        ).getOrThrow()

        val result = repository.getAllTemplatesAsDomain().getOrThrow()

        assertEquals(2, result.size)
    }

    // ========================================
    // saveTemplate tests (insert)
    // ========================================

    @Test
    fun `saveTemplate assigns id for new template`() = runTest {
        val template = createTestTemplate(name = "New Template")

        val id = repository.saveTemplate(template).getOrThrow()

        assertTrue(id > 0)
        val stored = repository.getTemplateAsDomain(id).getOrNull()
        assertNotNull(stored)
        assertEquals("New Template", stored?.name)
    }

    @Test
    fun `saveTemplate stores header and lines`() = runTest {
        val template = createTestTemplate(
            name = "Full Template",
            lines = listOf(
                createTestLine(accountName = "Assets:Cash"),
                createTestLine(accountName = "Expenses:Food"),
                createTestLine(accountName = "Liabilities:Card")
            )
        )

        val id = repository.saveTemplate(template).getOrThrow()

        val stored = repository.getTemplateAsDomain(id).getOrNull()
        assertNotNull(stored)
        assertEquals("Full Template", stored?.name)
        assertEquals(3, stored?.lines?.size)
    }

    // ========================================
    // saveTemplate tests (update)
    // ========================================

    @Test
    fun `saveTemplate modifies existing template`() = runTest {
        val template = createTestTemplate(name = "Original")
        val id = repository.saveTemplate(template).getOrThrow()

        val updatedTemplate = createTestTemplate(id = id, name = "Updated")
        repository.saveTemplate(updatedTemplate).getOrThrow()

        val result = repository.getTemplateAsDomain(id).getOrNull()
        assertEquals("Updated", result?.name)
    }

    @Test
    fun `saveTemplate updates lines when updating`() = runTest {
        val template = createTestTemplate(
            name = "Original",
            lines = listOf(
                createTestLine(accountName = "A:B"),
                createTestLine(accountName = "C:D")
            )
        )
        val id = repository.saveTemplate(template).getOrThrow()

        val updatedTemplate = createTestTemplate(
            id = id,
            name = "Updated",
            lines = listOf(
                createTestLine(accountName = "X:Y"),
                createTestLine(accountName = "Z:W")
            )
        )
        repository.saveTemplate(updatedTemplate).getOrThrow()

        val result = repository.getTemplateAsDomain(id).getOrNull()
        assertEquals("Updated", result?.name)
        assertEquals(2, result?.lines?.size)
        assertTrue(result?.lines?.any { it.accountName == "X:Y" } ?: false)
    }

    // ========================================
    // deleteTemplateById tests
    // ========================================

    @Test
    fun `deleteTemplateById removes template`() = runTest {
        val template = createTestTemplate(name = "ToDelete")
        val id = repository.saveTemplate(template).getOrThrow()

        repository.deleteTemplateById(id).getOrThrow()

        val remaining = repository.observeAllTemplatesAsDomain().first()
        assertTrue(remaining.isEmpty())
    }

    @Test
    fun `deleteTemplateById only removes specified template`() = runTest {
        val id1 = repository.saveTemplate(createTestTemplate(name = "Template 1")).getOrThrow()
        repository.saveTemplate(createTestTemplate(name = "Template 2")).getOrThrow()

        repository.deleteTemplateById(id1).getOrThrow()

        val remaining = repository.observeAllTemplatesAsDomain().first()
        assertEquals(1, remaining.size)
        assertEquals("Template 2", remaining[0].name)
    }

    // ========================================
    // duplicateTemplate tests (still uses deprecated method)
    // ========================================

    @Test
    fun `duplicateTemplate returns null for non-existent id`() = runTest {
        val result = repository.duplicateTemplate(999L).getOrNull()
        assertNull(result)
    }

    @Test
    fun `duplicateTemplate creates copy with new id`() = runTest {
        val template = createTestTemplate(name = "Original")
        val id = repository.saveTemplate(template).getOrThrow()

        val duplicate = repository.duplicateTemplate(id).getOrNull()

        assertNotNull(duplicate)
        assertNotEquals(id, duplicate?.id)
        // Fake implementation adds " (copy)" suffix
        assertEquals("Original (copy)", duplicate?.name)
    }

    @Test
    fun `duplicateTemplate copies lines to new template`() = runTest {
        val template = createTestTemplate(
            name = "WithLines",
            lines = listOf(
                createTestLine(accountName = "A:B"),
                createTestLine(accountName = "C:D")
            )
        )
        val id = repository.saveTemplate(template).getOrThrow()

        val duplicate = repository.duplicateTemplate(id).getOrNull()

        assertNotNull(duplicate)
        assertEquals(2, duplicate?.lines?.size)
        // Verify lines were copied with the expected account names
        val accountNames = duplicate?.lines?.map { it.accountName }
        assertTrue(accountNames?.contains("A:B") == true)
        assertTrue(accountNames?.contains("C:D") == true)
    }

    // ========================================
    // deleteAllTemplates tests
    // ========================================

    @Test
    fun `deleteAllTemplates removes all templates`() = runTest {
        repository.saveTemplate(createTestTemplate(name = "T1")).getOrThrow()
        repository.saveTemplate(createTestTemplate(name = "T2")).getOrThrow()
        repository.saveTemplate(createTestTemplate(name = "T3")).getOrThrow()

        repository.deleteAllTemplates().getOrThrow()

        val templates = repository.observeAllTemplatesAsDomain().first()
        assertTrue(templates.isEmpty())
    }
}
