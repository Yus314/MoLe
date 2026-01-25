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

package net.ktnx.mobileledger.ui.templates

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.ktnx.mobileledger.domain.model.Template
import net.ktnx.mobileledger.domain.usecase.DeleteTemplateUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.DuplicateTemplateUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.GetTemplateUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.ObserveTemplatesUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.SaveTemplateUseCaseImpl
import net.ktnx.mobileledger.fake.FakeTemplateRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [TemplateListViewModelCompose].
 *
 * Tests cover:
 * - Initialization and template loading
 * - Template operations (delete, duplicate, undo)
 * - Navigation effects
 * - Error handling
 * - Flow updates
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TemplateListViewModelComposeTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var templateRepository: FakeTemplateRepository
    private lateinit var viewModel: TemplateListViewModelCompose

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        templateRepository = FakeTemplateRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========================================
    // Helper methods
    // ========================================

    private fun createTestTemplate(
        id: Long? = null,
        name: String = "Test Template",
        pattern: String = ".*",
        isFallback: Boolean = false
    ): Template = Template(
        id = id,
        name = name,
        pattern = pattern,
        isFallback = isFallback,
        lines = emptyList()
    )

    private fun createViewModel(): TemplateListViewModelCompose {
        val observeTemplatesUseCase = ObserveTemplatesUseCaseImpl(templateRepository)
        val getTemplateUseCase = GetTemplateUseCaseImpl(templateRepository)
        val saveTemplateUseCase = SaveTemplateUseCaseImpl(templateRepository)
        val deleteTemplateUseCase = DeleteTemplateUseCaseImpl(templateRepository)
        val duplicateTemplateUseCase = DuplicateTemplateUseCaseImpl(templateRepository)
        return TemplateListViewModelCompose(
            observeTemplatesUseCase = observeTemplatesUseCase,
            getTemplateUseCase = getTemplateUseCase,
            saveTemplateUseCase = saveTemplateUseCase,
            deleteTemplateUseCase = deleteTemplateUseCase,
            duplicateTemplateUseCase = duplicateTemplateUseCase
        )
    }

    // ========================================
    // Initialization tests
    // ========================================

    @Test
    fun `initialization loads templates from repository`() = runTest {
        // Given
        val template1 = createTestTemplate(name = "Template 1", pattern = "pattern1")
        val template2 = createTestTemplate(name = "Template 2", pattern = "pattern2")
        templateRepository.saveTemplate(template1)
        templateRepository.saveTemplate(template2)

        // When
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.templates.size)
        assertEquals("Template 1", state.templates[0].name)
        assertEquals("Template 2", state.templates[1].name)
    }

    @Test
    fun `initialization shows loading state then content`() = runTest {
        // Given
        val template = createTestTemplate(name = "Test")
        templateRepository.saveTemplate(template)

        // When
        viewModel = createViewModel()

        // Then - initially loading
        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()

        // Then - eventually loaded
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(1, viewModel.uiState.value.templates.size)
    }

    @Test
    fun `initialization handles empty list`() = runTest {
        // Given - no templates

        // When
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.templates.isEmpty())
        assertNull(state.error)
    }

    // ========================================
    // Navigation tests
    // ========================================

    @Test
    fun `createNewTemplate emits navigation effect with null id`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When/Then
        viewModel.effects.test {
            viewModel.onEvent(TemplateListEvent.CreateNewTemplate)
            advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is TemplateListEffect.NavigateToDetail)
            assertNull((effect as TemplateListEffect.NavigateToDetail).templateId)
        }
    }

    @Test
    fun `editTemplate emits navigation effect with template id`() = runTest {
        // Given
        val template = createTestTemplate(name = "Edit Me")
        val savedId = templateRepository.saveTemplate(template).getOrThrow()
        viewModel = createViewModel()
        advanceUntilIdle()

        // When/Then
        viewModel.effects.test {
            viewModel.onEvent(TemplateListEvent.EditTemplate(savedId))
            advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is TemplateListEffect.NavigateToDetail)
            assertEquals(savedId, (effect as TemplateListEffect.NavigateToDetail).templateId)
        }
    }

    // ========================================
    // Delete tests
    // ========================================

    @Test
    fun `deleteTemplate removes from list and shows undo snackbar`() = runTest {
        // Given
        val template = createTestTemplate(name = "To Delete")
        val savedId = templateRepository.saveTemplate(template).getOrThrow()
        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.templates.size)

        // When/Then
        viewModel.effects.test {
            viewModel.onEvent(TemplateListEvent.DeleteTemplate(savedId))
            advanceUntilIdle()

            // Verify undo snackbar effect
            val effect = awaitItem()
            assertTrue(effect is TemplateListEffect.ShowUndoSnackbar)
            assertEquals("To Delete", (effect as TemplateListEffect.ShowUndoSnackbar).templateName)
            assertEquals(savedId, effect.templateId)
        }

        // Verify template is removed from list
        assertTrue(viewModel.uiState.value.templates.isEmpty())
    }

    @Test
    fun `undoDelete restores deleted template`() = runTest {
        // Given
        val template = createTestTemplate(name = "To Restore")
        val savedId = templateRepository.saveTemplate(template).getOrThrow()
        viewModel = createViewModel()
        advanceUntilIdle()

        // Delete the template first
        viewModel.onEvent(TemplateListEvent.DeleteTemplate(savedId))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.templates.isEmpty())

        // When - undo delete
        viewModel.onEvent(TemplateListEvent.UndoDelete(savedId))
        advanceUntilIdle()

        // Then - template is restored
        assertEquals(1, viewModel.uiState.value.templates.size)
        assertEquals("To Restore", viewModel.uiState.value.templates[0].name)
    }

    // ========================================
    // Duplicate tests
    // ========================================

    @Test
    fun `duplicateTemplate creates copy in repository`() = runTest {
        // Given
        val template = createTestTemplate(name = "Original")
        val savedId = templateRepository.saveTemplate(template).getOrThrow()
        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.templates.size)

        // When
        viewModel.onEvent(TemplateListEvent.DuplicateTemplate(savedId))
        advanceUntilIdle()

        // Then - should have original + copy
        assertEquals(2, viewModel.uiState.value.templates.size)
        assertTrue(viewModel.uiState.value.templates.any { it.name.contains("copy") })
    }

    // ========================================
    // Flow update tests
    // ========================================

    @Test
    fun `template list updates when repository changes externally`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.templates.isEmpty())

        // When - add template directly to repository
        val template = createTestTemplate(name = "External Add")
        templateRepository.saveTemplate(template)
        advanceUntilIdle()

        // Then - viewModel should reflect the change
        assertEquals(1, viewModel.uiState.value.templates.size)
        assertEquals("External Add", viewModel.uiState.value.templates[0].name)
    }

    @Test
    fun `fallback templates are sorted last`() = runTest {
        // Given
        val regularTemplate = createTestTemplate(name = "A Regular", isFallback = false)
        val fallbackTemplate = createTestTemplate(name = "B Fallback", isFallback = true)
        templateRepository.saveTemplate(fallbackTemplate)
        templateRepository.saveTemplate(regularTemplate)

        // When
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then - regular templates should come before fallback
        val templates = viewModel.uiState.value.templates
        assertEquals(2, templates.size)
        assertEquals("A Regular", templates[0].name)
        assertFalse(templates[0].isFallback)
        assertEquals("B Fallback", templates[1].name)
        assertTrue(templates[1].isFallback)
    }

    // ========================================
    // Error handling tests
    // ========================================

    @Test
    fun `deleteTemplate failure emits ShowError effect`() = runTest {
        // Given
        val template = createTestTemplate(name = "To Delete")
        val savedId = templateRepository.saveTemplate(template).getOrThrow()
        viewModel = createViewModel()
        advanceUntilIdle()

        // Configure failure for get (which is called during delete)
        templateRepository.shouldFailGetTemplate = true

        // When/Then
        viewModel.effects.test {
            viewModel.onEvent(TemplateListEvent.DeleteTemplate(savedId))
            advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is TemplateListEffect.ShowError)
            assertTrue((effect as TemplateListEffect.ShowError).message.contains("削除"))
        }

        // Template should still exist (delete failed)
        assertEquals(1, viewModel.uiState.value.templates.size)
    }

    @Test
    fun `duplicateTemplate failure emits ShowError effect`() = runTest {
        // Given
        val template = createTestTemplate(name = "To Duplicate")
        val savedId = templateRepository.saveTemplate(template).getOrThrow()
        viewModel = createViewModel()
        advanceUntilIdle()

        // Configure failure
        templateRepository.shouldFailDuplicate = true

        // When/Then
        viewModel.effects.test {
            viewModel.onEvent(TemplateListEvent.DuplicateTemplate(savedId))
            advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is TemplateListEffect.ShowError)
            assertTrue((effect as TemplateListEffect.ShowError).message.contains("複製"))
        }

        // Only original should exist (duplicate failed)
        assertEquals(1, viewModel.uiState.value.templates.size)
    }

    @Test
    fun `undoDelete failure emits ShowError effect`() = runTest {
        // Given
        val template = createTestTemplate(name = "To Delete Then Fail Undo")
        val savedId = templateRepository.saveTemplate(template).getOrThrow()
        viewModel = createViewModel()
        advanceUntilIdle()

        // When/Then - collect all effects in one test block
        viewModel.effects.test {
            // Delete successfully first
            viewModel.onEvent(TemplateListEvent.DeleteTemplate(savedId))
            advanceUntilIdle()

            // Consume the ShowUndoSnackbar effect from delete
            val undoSnackbarEffect = awaitItem()
            assertTrue(undoSnackbarEffect is TemplateListEffect.ShowUndoSnackbar)

            assertTrue(viewModel.uiState.value.templates.isEmpty())

            // Configure failure for save (used during undo)
            templateRepository.shouldFailSave = true

            // Attempt undo which should fail
            viewModel.onEvent(TemplateListEvent.UndoDelete(savedId))
            advanceUntilIdle()

            val errorEffect = awaitItem()
            assertTrue(errorEffect is TemplateListEffect.ShowError)
            assertTrue((errorEffect as TemplateListEffect.ShowError).message.contains("復元"))
        }

        // Template should still be deleted (undo failed)
        assertTrue(viewModel.uiState.value.templates.isEmpty())
    }

    @Test
    fun `undoDelete with no deleted template is no-op`() = runTest {
        // Given - create viewModel with template but don't delete anything
        val template = createTestTemplate(name = "Not Deleted")
        templateRepository.saveTemplate(template)
        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.templates.size)

        // When - try to undo without prior delete
        viewModel.onEvent(TemplateListEvent.UndoDelete(999L))
        advanceUntilIdle()

        // Then - no change, no error
        assertEquals(1, viewModel.uiState.value.templates.size)
    }

    // ========================================
    // Sequence and edge case tests
    // ========================================

    @Test
    fun `full delete and undo sequence works correctly`() = runTest {
        // Given
        val template1 = createTestTemplate(name = "Template 1")
        val template2 = createTestTemplate(name = "Template 2")
        val id1 = templateRepository.saveTemplate(template1).getOrThrow()
        val id2 = templateRepository.saveTemplate(template2).getOrThrow()
        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.templates.size)

        // When - delete template 1
        viewModel.effects.test {
            viewModel.onEvent(TemplateListEvent.DeleteTemplate(id1))
            advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is TemplateListEffect.ShowUndoSnackbar)
        }

        // Then - only template 2 remains
        assertEquals(1, viewModel.uiState.value.templates.size)
        assertEquals("Template 2", viewModel.uiState.value.templates[0].name)

        // When - undo delete
        viewModel.onEvent(TemplateListEvent.UndoDelete(id1))
        advanceUntilIdle()

        // Then - both templates restored
        assertEquals(2, viewModel.uiState.value.templates.size)
    }

    @Test
    fun `delete non-existent template is graceful`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.templates.isEmpty())

        // When - delete non-existent template
        viewModel.onEvent(TemplateListEvent.DeleteTemplate(999L))
        advanceUntilIdle()

        // Then - no error, still empty
        assertTrue(viewModel.uiState.value.templates.isEmpty())
    }

    @Test
    fun `duplicate non-existent template returns null gracefully`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.templates.isEmpty())

        // When - duplicate non-existent template
        viewModel.onEvent(TemplateListEvent.DuplicateTemplate(999L))
        advanceUntilIdle()

        // Then - still empty (no crash)
        assertTrue(viewModel.uiState.value.templates.isEmpty())
    }

    @Test
    fun `rapid duplicate operations create multiple copies`() = runTest {
        // Given
        val template = createTestTemplate(name = "Original")
        val savedId = templateRepository.saveTemplate(template).getOrThrow()
        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.templates.size)

        // When - duplicate twice rapidly
        viewModel.onEvent(TemplateListEvent.DuplicateTemplate(savedId))
        viewModel.onEvent(TemplateListEvent.DuplicateTemplate(savedId))
        advanceUntilIdle()

        // Then - original + 2 copies
        assertEquals(3, viewModel.uiState.value.templates.size)
        assertEquals(2, viewModel.uiState.value.templates.count { it.name.contains("copy") })
    }

    @Test
    fun `delete then create new navigates correctly`() = runTest {
        // Given
        val template = createTestTemplate(name = "To Delete")
        val savedId = templateRepository.saveTemplate(template).getOrThrow()
        viewModel = createViewModel()
        advanceUntilIdle()

        // When - delete then create new
        viewModel.effects.test {
            viewModel.onEvent(TemplateListEvent.DeleteTemplate(savedId))
            advanceUntilIdle()

            // First effect: undo snackbar
            val undoEffect = awaitItem()
            assertTrue(undoEffect is TemplateListEffect.ShowUndoSnackbar)

            viewModel.onEvent(TemplateListEvent.CreateNewTemplate)
            advanceUntilIdle()

            // Second effect: navigate to detail
            val navEffect = awaitItem()
            assertTrue(navEffect is TemplateListEffect.NavigateToDetail)
            assertNull((navEffect as TemplateListEffect.NavigateToDetail).templateId)
        }
    }

    @Test
    fun `multiple deletes only undo most recent`() = runTest {
        // Given
        val template1 = createTestTemplate(name = "Template 1")
        val template2 = createTestTemplate(name = "Template 2")
        val id1 = templateRepository.saveTemplate(template1).getOrThrow()
        val id2 = templateRepository.saveTemplate(template2).getOrThrow()
        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.templates.size)

        // When - delete both
        viewModel.onEvent(TemplateListEvent.DeleteTemplate(id1))
        advanceUntilIdle()
        viewModel.onEvent(TemplateListEvent.DeleteTemplate(id2))
        advanceUntilIdle()

        // Then - both deleted
        assertTrue(viewModel.uiState.value.templates.isEmpty())

        // When - undo (should only restore template 2, the most recent delete)
        viewModel.onEvent(TemplateListEvent.UndoDelete(id2))
        advanceUntilIdle()

        // Then - only template 2 restored (most recent delete)
        assertEquals(1, viewModel.uiState.value.templates.size)
        assertEquals("Template 2", viewModel.uiState.value.templates[0].name)
    }
}
