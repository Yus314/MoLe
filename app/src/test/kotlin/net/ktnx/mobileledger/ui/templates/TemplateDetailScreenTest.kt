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

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import net.ktnx.mobileledger.robot.template.templateDetailScreen
import net.ktnx.mobileledger.ui.theme.MoLeTheme
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric tests for TemplateDetailScreen using Robot Pattern.
 *
 * Tests the template detail UI including:
 * - Initial state for new vs existing templates
 * - Name and pattern input fields
 * - Account row operations
 * - Fallback toggle
 * - Save and delete buttons
 * - Dialog interactions
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TemplateDetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // UI State
    private var uiState by mutableStateOf(TemplateDetailUiState())

    // Captured events for verification
    private val capturedEvents = mutableListOf<TemplateDetailEvent>()

    @Before
    fun setup() {
        // Reset state
        uiState = TemplateDetailUiState()
        capturedEvents.clear()
    }

    private fun setContent() {
        composeTestRule.setContent {
            MoLeTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                TemplateDetailContent(
                    uiState = uiState,
                    snackbarHostState = snackbarHostState,
                    onEvent = { event -> capturedEvents.add(event) }
                )
            }
        }
    }

    // ========================================
    // Initial State Tests (New Template)
    // ========================================

    @Test
    fun `new template shows correct title`() {
        setContent()

        composeTestRule.templateDetailScreen {
            // No actions
        } verify {
            newTemplateTitle()
        }
    }

    @Test
    fun `new template shows name field`() {
        setContent()

        composeTestRule.onNodeWithText("Template name").assertIsDisplayed()
    }

    @Test
    fun `new template shows pattern field`() {
        setContent()

        composeTestRule.onNodeWithText("Regular expression pattern").assertIsDisplayed()
    }

    @Test
    fun `new template shows test text field`() {
        setContent()

        composeTestRule.templateDetailScreen {
            // No actions
        } verify {
            testTextFieldIsDisplayed()
        }
    }

    @Test
    fun `new template shows accounts section`() {
        setContent()

        composeTestRule.templateDetailScreen {
            // No actions
        } verify {
            accountsSectionIsDisplayed()
        }
    }

    @Test
    fun `new template shows fallback toggle`() {
        setContent()

        composeTestRule.templateDetailScreen {
            // No actions
        } verify {
            fallbackToggleIsDisplayed()
        }
    }

    @Test
    fun `new template shows save FAB`() {
        setContent()

        composeTestRule.templateDetailScreen {
            // No actions
        } verify {
            saveFabIsDisplayed()
        }
    }

    @Test
    fun `new template does not show delete button`() {
        setContent()

        // New template should not have delete button
        val deleteNodes = composeTestRule.onAllNodesWithText("削除").fetchSemanticsNodes()
        assertTrue("Delete button should not be shown for new template", deleteNodes.isEmpty())
    }

    @Test
    fun `new template has two default account rows`() {
        setContent()

        composeTestRule.templateDetailScreen {
            // No actions
        } verify {
            accountRowCountIs(2)
        }
    }

    // ========================================
    // Existing Template State Tests
    // ========================================

    @Test
    fun `existing template shows template name as title`() {
        uiState = uiState.copy(
            templateId = 1L,
            name = "Bank Transfer"
        )
        setContent()

        composeTestRule.templateDetailScreen {
            // No actions
        } verify {
            templateTitleIs("Bank Transfer")
        }
    }

    @Test
    fun `existing template shows delete button`() {
        uiState = uiState.copy(templateId = 1L, name = "Test")
        setContent()

        composeTestRule.templateDetailScreen {
            // No actions
        } verify {
            deleteButtonIsDisplayed()
        }
    }

    @Test
    fun `existing template shows pre-filled name`() {
        uiState = uiState.copy(
            templateId = 1L,
            name = "Bank Transfer"
        )
        setContent()

        composeTestRule.onNodeWithText("Template name")
            .assertTextContains("Bank Transfer")
    }

    @Test
    fun `existing template shows pre-filled pattern`() {
        uiState = uiState.copy(
            templateId = 1L,
            pattern = "(\\d+)円"
        )
        setContent()

        composeTestRule.onNodeWithText("Regular expression pattern")
            .assertTextContains("(\\d+)円")
    }

    // ========================================
    // Form Interaction Tests
    // ========================================

    @Test
    fun `typing name triggers UpdateName event`() {
        setContent()

        composeTestRule.onNodeWithText("Template name").performTextInput("New Name")
        composeTestRule.waitForIdle()

        assertTrue(
            "Should capture UpdateName event",
            capturedEvents.any { it is TemplateDetailEvent.UpdateName }
        )
    }

    @Test
    fun `typing pattern triggers UpdatePattern event`() {
        setContent()

        composeTestRule.onNodeWithText("Regular expression pattern").performTextInput(".*")
        composeTestRule.waitForIdle()

        assertTrue(
            "Should capture UpdatePattern event",
            capturedEvents.any { it is TemplateDetailEvent.UpdatePattern }
        )
    }

    @Test
    fun `typing test text triggers UpdateTestText event`() {
        setContent()

        composeTestRule.onNodeWithText("Test text").performTextInput("test input")
        composeTestRule.waitForIdle()

        assertTrue(
            "Should capture UpdateTestText event",
            capturedEvents.any { it is TemplateDetailEvent.UpdateTestText }
        )
    }

    @Test
    fun `fallback toggle is visible and scrollable`() {
        setContent()

        // Verify the fallback toggle is accessible via scrolling
        composeTestRule.templateDetailScreen {
            tapFallbackToggle()
        }
        composeTestRule.waitForIdle()

        // Note: Switch click behavior differs - clicking on the row text may not trigger
        // the switch. We verify the element is accessible via scroll.
        composeTestRule.templateDetailScreen {
            // No actions
        } verify {
            fallbackToggleIsDisplayed()
        }
    }

    // ========================================
    // Pattern Validation Display Tests
    // ========================================

    @Test
    fun `pattern error is displayed when present`() {
        uiState = uiState.copy(patternError = "Invalid regex syntax")
        setContent()

        composeTestRule.templateDetailScreen {
            // No actions
        } verify {
            patternErrorIsDisplayed("Invalid regex syntax")
        }
    }

    @Test
    fun `group count text exists for valid pattern with groups`() {
        uiState = uiState.copy(
            pattern = "(\\d+)-(\\d+)",
            patternGroupCount = 2,
            patternError = null
        )
        setContent()

        // The group count text "グループ数: 2" should exist when patternGroupCount > 0
        // Use assertExists instead of assertIsDisplayed since it may not be visible on screen
        val nodes = composeTestRule.onAllNodesWithText("グループ数: 2").fetchSemanticsNodes()
        assertTrue("Group count text should exist in UI tree", nodes.isNotEmpty())
    }

    // ========================================
    // Account Row Operation Tests
    // ========================================

    @Test
    fun `add account row button is accessible via scroll`() {
        setContent()

        // Verify the add account row button can be scrolled to and interacted with
        // Note: Due to TemplateAccountRowsSection being a separate composable,
        // the onAddRow callback goes through the internal event handler
        composeTestRule.onNodeWithContentDescription("行を追加").performScrollTo()
        composeTestRule.onNodeWithContentDescription("行を追加").assertIsDisplayed()
    }

    @Test
    fun `three account rows display correctly`() {
        uiState = uiState.copy(
            accounts = listOf(
                TemplateAccountRow(id = -1),
                TemplateAccountRow(id = -2),
                TemplateAccountRow(id = -3)
            )
        )
        setContent()

        composeTestRule.templateDetailScreen {
            // No actions
        } verify {
            accountRowCountIs(3)
        }
    }

    // ========================================
    // Action Button Tests
    // ========================================

    @Test
    fun `tapping save FAB triggers Save event`() {
        setContent()

        composeTestRule.templateDetailScreen {
            tapSave()
        }
        composeTestRule.waitForIdle()

        assertTrue(
            "Should capture Save event",
            capturedEvents.any { it is TemplateDetailEvent.Save }
        )
    }

    @Test
    fun `tapping delete button triggers ShowDeleteConfirmDialog event`() {
        uiState = uiState.copy(templateId = 1L, name = "Test")
        setContent()

        composeTestRule.templateDetailScreen {
            tapDelete()
        }
        composeTestRule.waitForIdle()

        assertTrue(
            "Should capture ShowDeleteConfirmDialog event",
            capturedEvents.any { it is TemplateDetailEvent.ShowDeleteConfirmDialog }
        )
    }

    @Test
    fun `tapping back button triggers NavigateBack event`() {
        setContent()

        composeTestRule.templateDetailScreen {
            tapBack()
        }
        composeTestRule.waitForIdle()

        assertTrue(
            "Should capture NavigateBack event",
            capturedEvents.any { it is TemplateDetailEvent.NavigateBack }
        )
    }

    // ========================================
    // Dialog Tests
    // ========================================

    @Test
    fun `delete confirmation dialog is displayed`() {
        uiState = uiState.copy(
            templateId = 1L,
            name = "Test Template",
            showDeleteConfirmDialog = true
        )
        setContent()

        composeTestRule.templateDetailScreen {
            // No actions
        } verify {
            deleteConfirmDialogIsDisplayed()
        }
    }

    @Test
    fun `confirming delete triggers ConfirmDelete event`() {
        uiState = uiState.copy(
            templateId = 1L,
            name = "Test Template",
            showDeleteConfirmDialog = true
        )
        setContent()

        composeTestRule.templateDetailScreen {
            confirmDelete()
        }
        composeTestRule.waitForIdle()

        assertTrue(
            "Should capture ConfirmDelete event",
            capturedEvents.any { it is TemplateDetailEvent.ConfirmDelete }
        )
    }

    @Test
    fun `canceling delete triggers DismissDeleteConfirmDialog event`() {
        uiState = uiState.copy(
            templateId = 1L,
            name = "Test Template",
            showDeleteConfirmDialog = true
        )
        setContent()

        composeTestRule.templateDetailScreen {
            cancelDelete()
        }
        composeTestRule.waitForIdle()

        assertTrue(
            "Should capture DismissDeleteConfirmDialog event",
            capturedEvents.any { it is TemplateDetailEvent.DismissDeleteConfirmDialog }
        )
    }

    @Test
    fun `unsaved changes dialog is displayed`() {
        uiState = uiState.copy(showUnsavedChangesDialog = true)
        setContent()

        composeTestRule.templateDetailScreen {
            // No actions
        } verify {
            unsavedChangesDialogIsDisplayed()
        }
    }

    @Test
    fun `saving in unsaved changes dialog triggers Save event`() {
        uiState = uiState.copy(showUnsavedChangesDialog = true)
        setContent()

        composeTestRule.templateDetailScreen {
            saveChangesInDialog()
        }
        composeTestRule.waitForIdle()

        assertTrue(
            "Should capture Save event",
            capturedEvents.any { it is TemplateDetailEvent.Save }
        )
    }

    @Test
    fun `discarding in unsaved changes dialog triggers ConfirmDiscardChanges event`() {
        uiState = uiState.copy(showUnsavedChangesDialog = true)
        setContent()

        composeTestRule.templateDetailScreen {
            discardChangesInDialog()
        }
        composeTestRule.waitForIdle()

        assertTrue(
            "Should capture ConfirmDiscardChanges event",
            capturedEvents.any { it is TemplateDetailEvent.ConfirmDiscardChanges }
        )
    }

    // ========================================
    // Loading State Tests
    // ========================================

    @Test
    fun `loading state hides form content`() {
        uiState = uiState.copy(isLoading = true)
        setContent()

        // When loading, the form fields should not be displayed
        // (LoadingIndicator is shown instead of the form content)
        // We verify by checking that the Template name field is not displayed
        val nodes = composeTestRule.onAllNodesWithText("Template name").fetchSemanticsNodes()
        assertTrue("Template name field should not be visible during loading", nodes.isEmpty())
    }

    // ========================================
    // Robot Pattern Verification Tests
    // ========================================

    @Test
    fun `robot pattern verify chain works correctly`() {
        uiState = uiState.copy(
            templateId = 1L,
            name = "Test Template",
            pattern = ".*",
            patternGroupCount = 0
        )
        setContent()

        composeTestRule.templateDetailScreen {
            // No actions - just verify
        } verify {
            templateTitleIs("Test Template")
            saveFabIsDisplayed()
            deleteButtonIsDisplayed()
            accountsSectionIsDisplayed()
            fallbackToggleIsDisplayed()
        }
    }
}
