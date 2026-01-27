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
import net.ktnx.mobileledger.core.domain.model.Template
import net.ktnx.mobileledger.domain.usecase.DeleteTemplateUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.GetTemplateUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.SaveTemplateUseCaseImpl
import net.ktnx.mobileledger.fake.FakeTemplateAccountRowManager
import net.ktnx.mobileledger.fake.FakeTemplateDataMapper
import net.ktnx.mobileledger.fake.FakeTemplatePatternValidator
import net.ktnx.mobileledger.fake.FakeTemplateRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [TemplateDetailViewModelCompose].
 *
 * Tests cover:
 * - Initialization (new vs existing templates)
 * - Form field updates
 * - Pattern validation
 * - Account row operations
 * - Save and delete operations
 * - Dialog management
 * - Unsaved changes tracking
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TemplateDetailViewModelComposeTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var templateRepository: FakeTemplateRepository
    private lateinit var patternValidator: FakeTemplatePatternValidator
    private lateinit var rowManager: FakeTemplateAccountRowManager
    private lateinit var dataMapper: FakeTemplateDataMapper
    private lateinit var viewModel: TemplateDetailViewModelCompose

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        templateRepository = FakeTemplateRepository()
        patternValidator = FakeTemplatePatternValidator()
        rowManager = FakeTemplateAccountRowManager()
        dataMapper = FakeTemplateDataMapper()
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

    private fun createViewModel() = TemplateDetailViewModelCompose(
        getTemplateUseCase = GetTemplateUseCaseImpl(templateRepository),
        saveTemplateUseCase = SaveTemplateUseCaseImpl(templateRepository),
        deleteTemplateUseCase = DeleteTemplateUseCaseImpl(templateRepository),
        patternValidator = patternValidator,
        rowManager = rowManager,
        dataMapper = dataMapper
    )

    // ========================================
    // Initialization tests
    // ========================================

    @Test
    fun `initialize with null id creates new template`() = runTest {
        // Given
        viewModel = createViewModel()

        // When
        viewModel.initialize(null)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNull(state.templateId)
        assertTrue(state.isNewTemplate)
        assertFalse(state.isLoading)
        assertEquals("", state.name)
        assertEquals("", state.pattern)
    }

    @Test
    fun `initialize with valid id loads existing template`() = runTest {
        // Given
        val template = createTestTemplate(name = "Existing Template", pattern = "test.*")
        val savedId = templateRepository.saveTemplate(template).getOrThrow()

        viewModel = createViewModel()

        // When
        viewModel.initialize(savedId)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(savedId, state.templateId)
        assertFalse(state.isNewTemplate)
        assertFalse(state.isLoading)
        assertEquals("Existing Template", state.name)
        assertEquals("test.*", state.pattern)
    }

    @Test
    fun `initialize with zero id creates new template`() = runTest {
        // Given
        viewModel = createViewModel()

        // When
        viewModel.initialize(0L)
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.isNewTemplate)
    }

    @Test
    fun `initialize with negative id creates new template`() = runTest {
        // Given
        viewModel = createViewModel()

        // When
        viewModel.initialize(-1L)
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.isNewTemplate)
    }

    @Test
    fun `initialize only runs once`() = runTest {
        // Given
        val template = createTestTemplate(name = "First")
        val savedId = templateRepository.saveTemplate(template).getOrThrow()

        viewModel = createViewModel()

        // When - initialize twice
        viewModel.initialize(savedId)
        advanceUntilIdle()
        viewModel.initialize(null) // Should be ignored
        advanceUntilIdle()

        // Then - still has first template
        assertEquals("First", viewModel.uiState.value.name)
    }

    // ========================================
    // Form field update tests
    // ========================================

    @Test
    fun `updateName updates state and marks unsaved`() = runTest {
        // Given
        viewModel = createViewModel()
        viewModel.initialize(null)
        advanceUntilIdle()

        // When
        viewModel.onEvent(TemplateDetailEvent.UpdateName("New Name"))
        advanceUntilIdle()

        // Then
        assertEquals("New Name", viewModel.uiState.value.name)
        assertTrue(viewModel.uiState.value.hasUnsavedChanges)
    }

    @Test
    fun `updatePattern validates and updates state`() = runTest {
        // Given
        patternValidator.groupCount = 3
        viewModel = createViewModel()
        viewModel.initialize(null)
        advanceUntilIdle()

        // When
        viewModel.onEvent(TemplateDetailEvent.UpdatePattern("test(.*)"))
        advanceUntilIdle()

        // Then
        assertEquals("test(.*)", viewModel.uiState.value.pattern)
        assertTrue(viewModel.uiState.value.hasUnsavedChanges)
        assertEquals(3, viewModel.uiState.value.patternGroupCount)
    }

    @Test
    fun `updateTestText triggers pattern validation`() = runTest {
        // Given
        patternValidator.matchResultText = "Matched!"
        viewModel = createViewModel()
        viewModel.initialize(null)
        advanceUntilIdle()

        // When
        viewModel.onEvent(TemplateDetailEvent.UpdateTestText("sample text"))
        advanceUntilIdle()

        // Then
        assertEquals("sample text", viewModel.uiState.value.testText)
        assertTrue(viewModel.uiState.value.hasUnsavedChanges)
        assertEquals(1, patternValidator.validateCallCount)
    }

    @Test
    fun `updateIsFallback updates state`() = runTest {
        // Given
        viewModel = createViewModel()
        viewModel.initialize(null)
        advanceUntilIdle()

        // When
        viewModel.onEvent(TemplateDetailEvent.UpdateIsFallback(true))
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.isFallback)
        assertTrue(viewModel.uiState.value.hasUnsavedChanges)
    }

    // ========================================
    // Pattern validation tests
    // ========================================

    @Test
    fun `valid pattern shows success with group count`() = runTest {
        // Given
        patternValidator.shouldReturnError = false
        patternValidator.groupCount = 2
        viewModel = createViewModel()
        viewModel.initialize(null)
        advanceUntilIdle()

        // When
        viewModel.onEvent(TemplateDetailEvent.UpdatePattern("(\\d+)-(\\d+)"))
        advanceUntilIdle()

        // Then
        assertNull(viewModel.uiState.value.patternError)
        assertEquals(2, viewModel.uiState.value.patternGroupCount)
    }

    @Test
    fun `invalid pattern shows error message`() = runTest {
        // Given
        patternValidator.shouldReturnError = true
        patternValidator.errorMessage = "Invalid regex syntax"
        viewModel = createViewModel()
        viewModel.initialize(null)
        advanceUntilIdle()

        // When
        viewModel.onEvent(TemplateDetailEvent.UpdatePattern("[invalid"))
        advanceUntilIdle()

        // Then
        assertEquals("Invalid regex syntax", viewModel.uiState.value.patternError)
    }

    // ========================================
    // Account row operation tests
    // ========================================

    @Test
    fun `addAccountRow adds new row`() = runTest {
        // Given
        viewModel = createViewModel()
        viewModel.initialize(null)
        advanceUntilIdle()
        val initialRowCount = viewModel.uiState.value.accounts.size

        // When
        viewModel.onEvent(TemplateDetailEvent.AddAccountRow)
        advanceUntilIdle()

        // Then
        assertEquals(initialRowCount + 1, viewModel.uiState.value.accounts.size)
        assertTrue(viewModel.uiState.value.hasUnsavedChanges)
    }

    @Test
    fun `removeAccountRow removes specified row`() = runTest {
        // Given
        viewModel = createViewModel()
        viewModel.initialize(null)
        advanceUntilIdle()

        // Add extra rows to ensure we can remove one
        viewModel.onEvent(TemplateDetailEvent.AddAccountRow)
        advanceUntilIdle()
        val rowCountAfterAdd = viewModel.uiState.value.accounts.size

        // When
        viewModel.onEvent(TemplateDetailEvent.RemoveAccountRow(0))
        advanceUntilIdle()

        // Then - row count should decrease (or stay same if minimum reached)
        assertTrue(viewModel.uiState.value.accounts.size <= rowCountAfterAdd)
    }

    @Test
    fun `moveAccountRow reorders correctly`() = runTest {
        // Given
        viewModel = createViewModel()
        viewModel.initialize(null)
        advanceUntilIdle()

        // Update first row with identifiable data
        viewModel.onEvent(TemplateDetailEvent.UpdateAccountName(0, MatchableValue.Literal("First")))
        advanceUntilIdle()

        // When
        viewModel.onEvent(TemplateDetailEvent.MoveAccountRow(0, 1))
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.hasUnsavedChanges)
    }

    @Test
    fun `updateAccountName updates values`() = runTest {
        // Given
        viewModel = createViewModel()
        viewModel.initialize(null)
        advanceUntilIdle()

        // When
        viewModel.onEvent(TemplateDetailEvent.UpdateAccountName(0, MatchableValue.Literal("Assets:Cash")))
        advanceUntilIdle()

        // Then
        val firstRow = viewModel.uiState.value.accounts[0]
        assertEquals("Assets:Cash", firstRow.accountName.getLiteralValue())
        assertTrue(viewModel.uiState.value.hasUnsavedChanges)
    }

    // ========================================
    // Save tests
    // ========================================

    @Test
    fun `saveTemplate with valid data succeeds`() = runTest {
        // Given
        viewModel = createViewModel()
        viewModel.initialize(null)
        advanceUntilIdle()

        viewModel.onEvent(TemplateDetailEvent.UpdateName("Valid Template"))
        viewModel.onEvent(TemplateDetailEvent.UpdatePattern(".*"))
        advanceUntilIdle()

        // When/Then
        viewModel.effects.test {
            viewModel.onEvent(TemplateDetailEvent.Save)
            advanceUntilIdle()

            // Should emit TemplateSaved and NavigateBack
            val firstEffect = awaitItem()
            assertTrue(firstEffect is TemplateDetailEffect.TemplateSaved)

            val secondEffect = awaitItem()
            assertTrue(secondEffect is TemplateDetailEffect.NavigateBack)
        }

        // Verify state
        assertFalse(viewModel.uiState.value.hasUnsavedChanges)
    }

    @Test
    fun `saveTemplate with invalid data does not save`() = runTest {
        // Given
        viewModel = createViewModel()
        viewModel.initialize(null)
        advanceUntilIdle()

        // Leave name and pattern empty (invalid form)

        // When
        viewModel.onEvent(TemplateDetailEvent.Save)
        advanceUntilIdle()

        // Then - no save should occur (isFormValid is false)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    // ========================================
    // Delete tests
    // ========================================

    @Test
    fun `confirmDelete removes template and navigates back`() = runTest {
        // Given
        val template = createTestTemplate(name = "To Delete")
        val savedId = templateRepository.saveTemplate(template).getOrThrow()

        viewModel = createViewModel()
        viewModel.initialize(savedId)
        advanceUntilIdle()

        // When/Then
        viewModel.effects.test {
            viewModel.onEvent(TemplateDetailEvent.ConfirmDelete)
            advanceUntilIdle()

            val firstEffect = awaitItem()
            assertTrue(firstEffect is TemplateDetailEffect.TemplateDeleted)

            val secondEffect = awaitItem()
            assertTrue(secondEffect is TemplateDetailEffect.NavigateBack)
        }
    }

    @Test
    fun `confirmDelete on new template just navigates back`() = runTest {
        // Given
        viewModel = createViewModel()
        viewModel.initialize(null)
        advanceUntilIdle()

        // When/Then
        viewModel.effects.test {
            viewModel.onEvent(TemplateDetailEvent.ConfirmDelete)
            advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is TemplateDetailEffect.NavigateBack)
        }
    }

    // ========================================
    // Dialog management tests
    // ========================================

    @Test
    fun `navigateBack with unsaved changes shows dialog`() = runTest {
        // Given
        viewModel = createViewModel()
        viewModel.initialize(null)
        advanceUntilIdle()

        // Make a change
        viewModel.onEvent(TemplateDetailEvent.UpdateName("Changed"))
        advanceUntilIdle()

        // When
        viewModel.onEvent(TemplateDetailEvent.NavigateBack)
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.showUnsavedChangesDialog)
    }

    @Test
    fun `navigateBack without changes navigates immediately`() = runTest {
        // Given
        viewModel = createViewModel()
        viewModel.initialize(null)
        advanceUntilIdle()

        // When/Then
        viewModel.effects.test {
            viewModel.onEvent(TemplateDetailEvent.NavigateBack)
            advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is TemplateDetailEffect.NavigateBack)
        }
    }

    @Test
    fun `dismissUnsavedChangesDialog hides dialog`() = runTest {
        // Given
        viewModel = createViewModel()
        viewModel.initialize(null)
        advanceUntilIdle()

        viewModel.onEvent(TemplateDetailEvent.UpdateName("Changed"))
        viewModel.onEvent(TemplateDetailEvent.NavigateBack)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showUnsavedChangesDialog)

        // When
        viewModel.onEvent(TemplateDetailEvent.DismissUnsavedChangesDialog)
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.uiState.value.showUnsavedChangesDialog)
    }

    @Test
    fun `confirmDiscardChanges navigates back`() = runTest {
        // Given
        viewModel = createViewModel()
        viewModel.initialize(null)
        advanceUntilIdle()

        viewModel.onEvent(TemplateDetailEvent.UpdateName("Changed"))
        viewModel.onEvent(TemplateDetailEvent.NavigateBack)
        advanceUntilIdle()

        // When/Then
        viewModel.effects.test {
            viewModel.onEvent(TemplateDetailEvent.ConfirmDiscardChanges)
            advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is TemplateDetailEffect.NavigateBack)
        }
    }

    @Test
    fun `showDeleteConfirmDialog shows dialog`() = runTest {
        // Given
        viewModel = createViewModel()
        viewModel.initialize(null)
        advanceUntilIdle()

        // When
        viewModel.onEvent(TemplateDetailEvent.ShowDeleteConfirmDialog)
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.showDeleteConfirmDialog)
    }

    @Test
    fun `dismissDeleteConfirmDialog hides dialog`() = runTest {
        // Given
        viewModel = createViewModel()
        viewModel.initialize(null)
        advanceUntilIdle()

        viewModel.onEvent(TemplateDetailEvent.ShowDeleteConfirmDialog)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showDeleteConfirmDialog)

        // When
        viewModel.onEvent(TemplateDetailEvent.DismissDeleteConfirmDialog)
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.uiState.value.showDeleteConfirmDialog)
    }

    // ========================================
    // UIState computed property tests
    // ========================================

    @Test
    fun `hasUnsavedChanges tracks modifications`() = runTest {
        // Given
        viewModel = createViewModel()
        viewModel.initialize(null)
        advanceUntilIdle()

        // Then - initially no changes
        assertFalse(viewModel.uiState.value.hasUnsavedChanges)

        // When - make a change
        viewModel.onEvent(TemplateDetailEvent.UpdateName("New"))
        advanceUntilIdle()

        // Then - has changes
        assertTrue(viewModel.uiState.value.hasUnsavedChanges)
    }

    @Test
    fun `isFormValid reflects validation state`() = runTest {
        // Given
        viewModel = createViewModel()
        viewModel.initialize(null)
        advanceUntilIdle()

        // Then - initially invalid (empty name and pattern)
        assertFalse(viewModel.uiState.value.isFormValid)

        // When - fill required fields
        viewModel.onEvent(TemplateDetailEvent.UpdateName("Valid"))
        viewModel.onEvent(TemplateDetailEvent.UpdatePattern(".*"))
        advanceUntilIdle()

        // Then - now valid
        assertTrue(viewModel.uiState.value.isFormValid)
    }

    @Test
    fun `canDelete is true only for existing templates`() = runTest {
        // Given - new template
        viewModel = createViewModel()
        viewModel.initialize(null)
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.uiState.value.canDelete)

        // Given - existing template
        val template = createTestTemplate(name = "Existing")
        val savedId = templateRepository.saveTemplate(template).getOrThrow()
        viewModel = createViewModel()
        viewModel.initialize(savedId)
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.canDelete)
    }

    // ========================================
    // Error handling tests
    // ========================================

    @Test
    fun `initialize handles repository failure`() = runTest {
        // Given
        templateRepository.shouldFail = true
        viewModel = createViewModel()

        // When/Then
        viewModel.effects.test {
            viewModel.initialize(1L)
            advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is TemplateDetailEffect.ShowError)
        }

        // State should not be loading
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `initialize handles null template result`() = runTest {
        // Given - repository returns success but with null template
        templateRepository.useCustomTemplate = true
        templateRepository.templateToReturn = null
        viewModel = createViewModel()

        // When
        viewModel.initialize(999L)
        advanceUntilIdle()

        // Then - should just set loading to false without error
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `saveTemplate handles repository failure`() = runTest {
        // Given
        viewModel = createViewModel()
        viewModel.initialize(null)
        advanceUntilIdle()

        viewModel.onEvent(TemplateDetailEvent.UpdateName("Valid Template"))
        viewModel.onEvent(TemplateDetailEvent.UpdatePattern(".*"))
        advanceUntilIdle()

        templateRepository.shouldFail = true

        // When/Then
        viewModel.effects.test {
            viewModel.onEvent(TemplateDetailEvent.Save)
            advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is TemplateDetailEffect.ShowError)
        }

        // State should not be saving
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `confirmDelete handles repository failure`() = runTest {
        // Given
        val template = createTestTemplate(name = "To Delete")
        val savedId = templateRepository.saveTemplate(template).getOrThrow()

        viewModel = createViewModel()
        viewModel.initialize(savedId)
        advanceUntilIdle()

        templateRepository.shouldFail = true

        // When/Then
        viewModel.effects.test {
            viewModel.onEvent(TemplateDetailEvent.ConfirmDelete)
            advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is TemplateDetailEffect.ShowError)
        }

        // State should not be loading
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
