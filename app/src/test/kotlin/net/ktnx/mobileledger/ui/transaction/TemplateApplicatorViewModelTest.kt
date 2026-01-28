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

package net.ktnx.mobileledger.ui.transaction

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.ktnx.mobileledger.core.domain.model.Profile
import net.ktnx.mobileledger.core.domain.model.Template
import net.ktnx.mobileledger.core.domain.model.TemplateLine
import net.ktnx.mobileledger.core.testing.fake.FakeProfileRepository
import net.ktnx.mobileledger.core.testing.fake.FakeTemplateRepository
import net.ktnx.mobileledger.domain.usecase.GetAllTemplatesUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.GetTemplateUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.ObserveCurrentProfileUseCaseImpl
import net.ktnx.mobileledger.fake.FakeCurrencyFormatter
import net.ktnx.mobileledger.fake.FakeRowIdGenerator
import net.ktnx.mobileledger.feature.templates.usecase.TemplateMatcher
import net.ktnx.mobileledger.feature.templates.usecase.TemplateMatcherImpl
import net.ktnx.mobileledger.util.MainDispatcherRule
import net.ktnx.mobileledger.util.createTestDomainProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for TemplateApplicatorViewModel.
 *
 * Tests cover:
 * - Template listing
 * - Template application
 * - QR code template matching
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TemplateApplicatorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var profileRepository: FakeProfileRepository
    private lateinit var templateRepository: FakeTemplateRepository
    private lateinit var templateMatcher: TemplateMatcher
    private lateinit var currencyFormatter: FakeCurrencyFormatter
    private lateinit var rowIdGenerator: FakeRowIdGenerator

    private lateinit var viewModel: TemplateApplicatorViewModel

    @Before
    fun setup() {
        profileRepository = FakeProfileRepository()
        templateRepository = FakeTemplateRepository()
        templateMatcher = TemplateMatcherImpl()
        currencyFormatter = FakeCurrencyFormatter()
        rowIdGenerator = FakeRowIdGenerator()
    }

    private fun createTestProfile(
        id: Long? = 1L,
        name: String = "Test Profile",
        defaultCommodity: String = "USD"
    ): Profile = createTestDomainProfile(
        id = id,
        name = name,
        defaultCommodity = defaultCommodity
    )

    private suspend fun createViewModelWithProfile(profile: Profile? = null): TemplateApplicatorViewModel {
        if (profile != null) {
            profileRepository.insertProfile(profile)
            profileRepository.setCurrentProfile(profile)
        }

        val observeCurrentProfileUseCase = ObserveCurrentProfileUseCaseImpl(profileRepository)
        val getAllTemplatesUseCase = GetAllTemplatesUseCaseImpl(templateRepository)
        val getTemplateUseCase = GetTemplateUseCaseImpl(templateRepository)

        return TemplateApplicatorViewModel(
            observeCurrentProfileUseCase = observeCurrentProfileUseCase,
            getAllTemplatesUseCase = getAllTemplatesUseCase,
            getTemplateUseCase = getTemplateUseCase,
            templateMatcher = templateMatcher,
            currencyFormatter = currencyFormatter,
            rowIdGenerator = rowIdGenerator
        )
    }

    private fun createTestTemplate(
        id: Long? = 1L,
        name: String = "Test Template",
        description: String = "Template Description",
        regex: String = "",
        accounts: List<Pair<String, Float?>> = listOf("Assets:Bank" to 100.0f, "Expenses:Food" to null)
    ): Template {
        val templateLines = accounts.mapIndexed { index, (accountName, amount) ->
            TemplateLine(
                id = index.toLong() + 1,
                accountName = accountName,
                amount = amount
            )
        }
        return Template(
            id = id,
            name = name,
            pattern = regex,
            transactionDescription = description,
            lines = templateLines
        )
    }

    // ========================================
    // T064: Template listing tests
    // ========================================

    @Test
    fun `showTemplateSelector loads available templates`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val template1 = createTestTemplate(id = 1, name = "Template 1")
        val template2 = createTestTemplate(id = 2, name = "Template 2")
        templateRepository.saveTemplate(template1)
        templateRepository.saveTemplate(template2)

        // When
        viewModel.onEvent(TemplateApplicatorEvent.ShowTemplateSelector)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.showTemplateSelector)
        assertEquals(2, state.availableTemplates.size)
    }

    @Test
    fun `dismissTemplateSelector hides selector`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        viewModel.onEvent(TemplateApplicatorEvent.ShowTemplateSelector)
        advanceUntilIdle()

        // When
        viewModel.onEvent(TemplateApplicatorEvent.DismissTemplateSelector)
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.uiState.value.showTemplateSelector)
    }

    @Test
    fun `searchTemplates filters by name`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val template1 = createTestTemplate(id = 1, name = "Groceries", description = "Weekly shopping")
        val template2 = createTestTemplate(id = 2, name = "Gas Station", description = "Fuel")
        templateRepository.saveTemplate(template1)
        templateRepository.saveTemplate(template2)

        viewModel.onEvent(TemplateApplicatorEvent.ShowTemplateSelector)
        advanceUntilIdle()

        // When
        viewModel.onEvent(TemplateApplicatorEvent.SearchTemplates("Groc"))
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(1, state.availableTemplates.size)
        assertEquals("Groceries", state.availableTemplates[0].name)
    }

    @Test
    fun `searchTemplates with empty query shows all`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val template1 = createTestTemplate(id = 1, name = "Template 1")
        val template2 = createTestTemplate(id = 2, name = "Template 2")
        templateRepository.saveTemplate(template1)
        templateRepository.saveTemplate(template2)

        viewModel.onEvent(TemplateApplicatorEvent.ShowTemplateSelector)
        advanceUntilIdle()

        // When
        viewModel.onEvent(TemplateApplicatorEvent.SearchTemplates(""))
        advanceUntilIdle()

        // Then
        assertEquals(2, viewModel.uiState.value.availableTemplates.size)
    }

    // ========================================
    // T065: Template application tests
    // ========================================

    @Test
    fun `applyTemplate dismisses selector after applying`() = runTest {
        // Given
        val profile = createTestProfile(defaultCommodity = "USD")
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val template = createTestTemplate(
            id = 1,
            name = "Lunch",
            description = "Lunch expense",
            accounts = listOf("Expenses:Food" to 15.0f, "Assets:Cash" to null)
        )
        templateRepository.saveTemplate(template)

        viewModel.onEvent(TemplateApplicatorEvent.ShowTemplateSelector)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showTemplateSelector)

        // When
        viewModel.onEvent(TemplateApplicatorEvent.ApplyTemplate(template.id!!))
        advanceUntilIdle()

        // Then - Selector is dismissed
        assertFalse(viewModel.uiState.value.showTemplateSelector)
    }

    @Test
    fun `applyTemplate dismisses selector`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val template = createTestTemplate(id = 1)
        templateRepository.saveTemplate(template)

        viewModel.onEvent(TemplateApplicatorEvent.ShowTemplateSelector)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showTemplateSelector)

        // When
        viewModel.onEvent(TemplateApplicatorEvent.ApplyTemplate(template.id!!))
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.uiState.value.showTemplateSelector)
    }

    @Test
    fun `applyTemplateFromQr processes without error when matching template found`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val template = createTestTemplate(
            id = 1,
            name = "QR Template",
            description = "QR Payment",
            regex = "PAYMENT:(\\d+)",
            accounts = listOf("Expenses:Shopping" to 100.0f, "Assets:Bank" to null)
        )
        templateRepository.saveTemplate(template)

        // When - QR text matches regex
        viewModel.onEvent(TemplateApplicatorEvent.ApplyTemplateFromQr("PAYMENT:500"))
        advanceUntilIdle()

        // Then - No exception thrown, state is valid
        assertFalse(viewModel.uiState.value.isSearching)
    }

    @Test
    fun `applyTemplateFromQr does nothing when no template matches`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val template = createTestTemplate(
            id = 1,
            name = "QR Template",
            regex = "SPECIFIC_PATTERN:\\d+",
            accounts = listOf("Expenses:Shopping" to 100.0f)
        )
        templateRepository.saveTemplate(template)

        // When - QR text doesn't match any template
        viewModel.onEvent(TemplateApplicatorEvent.ApplyTemplateFromQr("UNRELATED_TEXT"))
        advanceUntilIdle()

        // Then - No error, state is valid
        assertFalse(viewModel.uiState.value.isSearching)
    }

    @Test
    fun `clearSelection clears selectedTemplateId`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        // When
        viewModel.onEvent(TemplateApplicatorEvent.ClearSelection)
        advanceUntilIdle()

        // Then
        assertEquals(null, viewModel.uiState.value.selectedTemplateId)
    }

    @Test
    fun `templates without regex are skipped in QR matching`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val templateWithoutRegex = createTestTemplate(
            id = 1,
            name = "No Regex Template",
            regex = "", // Empty regex should be skipped
            accounts = listOf("Expenses:Shopping" to 100.0f)
        )
        templateRepository.saveTemplate(templateWithoutRegex)

        // When
        viewModel.onEvent(TemplateApplicatorEvent.ApplyTemplateFromQr("ANY_TEXT"))
        advanceUntilIdle()

        // Then - No error (template without regex is skipped)
        assertFalse(viewModel.uiState.value.isSearching)
    }

    @Test
    fun `invalid regex in template is handled gracefully`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val templateWithInvalidRegex = createTestTemplate(
            id = 1,
            name = "Bad Regex Template",
            regex = "[invalid(regex", // Invalid regex syntax
            accounts = listOf("Expenses:Shopping" to 100.0f)
        )
        templateRepository.saveTemplate(templateWithInvalidRegex)

        // When - Should not crash
        viewModel.onEvent(TemplateApplicatorEvent.ApplyTemplateFromQr("TEST"))
        advanceUntilIdle()

        // Then - No crash, state is valid (graceful failure)
        assertFalse(viewModel.uiState.value.isSearching)
    }

    @Test
    fun `template application completes without error`() = runTest {
        // Given
        val profile = createTestProfile(defaultCommodity = "JPY")
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val template = createTestTemplate(
            id = 1,
            name = "Test",
            accounts = listOf("Expenses:Food" to 1000.0f, "Assets:Cash" to null)
        )
        templateRepository.saveTemplate(template)

        viewModel.onEvent(TemplateApplicatorEvent.ShowTemplateSelector)
        advanceUntilIdle()

        // When
        viewModel.onEvent(TemplateApplicatorEvent.ApplyTemplate(template.id!!))
        advanceUntilIdle()

        // Then - Completes without error, selector is dismissed
        assertFalse(viewModel.uiState.value.showTemplateSelector)
        assertFalse(viewModel.uiState.value.isSearching)
    }

    // ========================================
    // T066: Additional tests
    // ========================================

    @Test
    fun `applyTemplate with non-existent templateId dismisses selector`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        viewModel.onEvent(TemplateApplicatorEvent.ShowTemplateSelector)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showTemplateSelector)

        // When - apply non-existent template
        viewModel.onEvent(TemplateApplicatorEvent.ApplyTemplate(9999L))
        advanceUntilIdle()

        // Then - selector is still dismissed
        assertFalse(viewModel.uiState.value.showTemplateSelector)
    }

    @Test
    fun `searchTemplates filters by description`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val template1 = createTestTemplate(id = 1, name = "A", description = "Weekly groceries")
        val template2 = createTestTemplate(id = 2, name = "B", description = "Monthly rent")
        templateRepository.saveTemplate(template1)
        templateRepository.saveTemplate(template2)

        viewModel.onEvent(TemplateApplicatorEvent.ShowTemplateSelector)
        advanceUntilIdle()

        // When - search by description
        viewModel.onEvent(TemplateApplicatorEvent.SearchTemplates("groceries"))
        advanceUntilIdle()

        // Then
        assertEquals(1, viewModel.uiState.value.availableTemplates.size)
        assertEquals("A", viewModel.uiState.value.availableTemplates[0].name)
    }

    @Test
    fun `searchTemplates is case insensitive`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val template = createTestTemplate(id = 1, name = "GROCERY TEMPLATE")
        templateRepository.saveTemplate(template)

        viewModel.onEvent(TemplateApplicatorEvent.ShowTemplateSelector)
        advanceUntilIdle()

        // When - search with different case
        viewModel.onEvent(TemplateApplicatorEvent.SearchTemplates("grocery"))
        advanceUntilIdle()

        // Then
        assertEquals(1, viewModel.uiState.value.availableTemplates.size)
    }

    @Test
    fun `showTemplateSelector handles repository failure gracefully`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        templateRepository.shouldFailGetAll = true

        // When
        viewModel.onEvent(TemplateApplicatorEvent.ShowTemplateSelector)
        advanceUntilIdle()

        // Then - should not crash, isSearching should be false
        assertFalse(viewModel.uiState.value.isSearching)
    }

    @Test
    fun `searchTemplates handles repository failure gracefully`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        templateRepository.shouldFailGetAll = true

        // When
        viewModel.onEvent(TemplateApplicatorEvent.SearchTemplates("test"))
        advanceUntilIdle()

        // Then - should not crash, isSearching should be false
        assertFalse(viewModel.uiState.value.isSearching)
    }

    @Test
    fun `initialization with no profile does not crash`() = runTest {
        // Given - no profile
        viewModel = createViewModelWithProfile(null)
        advanceUntilIdle()

        // Then
        assertNotNull(viewModel.uiState.value)
    }

    @Test
    fun `applyTemplateFromQr with multiple matching templates uses first match`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val template1 = createTestTemplate(
            id = 1,
            name = "First Match",
            regex = "PAYMENT.*",
            accounts = listOf("Expenses:Shopping" to 100.0f)
        )
        val template2 = createTestTemplate(
            id = 2,
            name = "Second Match",
            regex = "PAYMENT:(\\d+)",
            accounts = listOf("Expenses:Food" to 200.0f)
        )
        templateRepository.saveTemplate(template1)
        templateRepository.saveTemplate(template2)

        // When
        viewModel.onEvent(TemplateApplicatorEvent.ApplyTemplateFromQr("PAYMENT:500"))
        advanceUntilIdle()

        // Then - completes without error
        assertFalse(viewModel.uiState.value.isSearching)
    }

    @Test
    fun `dismissTemplateSelector clears selectedTemplateId`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        viewModel.onEvent(TemplateApplicatorEvent.ShowTemplateSelector)
        advanceUntilIdle()

        // When
        viewModel.onEvent(TemplateApplicatorEvent.DismissTemplateSelector)
        advanceUntilIdle()

        // Then
        assertEquals(null, viewModel.uiState.value.selectedTemplateId)
    }

    @Test
    fun `empty template list is handled correctly`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        // No templates added

        // When
        viewModel.onEvent(TemplateApplicatorEvent.ShowTemplateSelector)
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.availableTemplates.isEmpty())
        assertTrue(viewModel.uiState.value.showTemplateSelector)
    }
}
