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

package net.ktnx.mobileledger.ui.profile

import android.content.res.Resources
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
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import net.ktnx.mobileledger.robot.profile.profileDetailScreen
import net.ktnx.mobileledger.service.ThemeService
import net.ktnx.mobileledger.ui.theme.MoLeTheme
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric tests for ProfileDetailScreen using Robot Pattern.
 *
 * Tests the profile detail UI including:
 * - Initial state for new vs existing profiles
 * - Name and URL input fields
 * - Authentication settings
 * - Save and delete buttons
 * - Dialog interactions
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ProfileDetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // UI State
    private var uiState by mutableStateOf(ProfileDetailUiState())

    // Captured events for verification
    private val capturedEvents = mutableListOf<ProfileDetailEvent>()

    // Resources for the screen
    private lateinit var resources: Resources

    @Before
    fun setup() {
        // Reset state
        uiState = ProfileDetailUiState(
            themeHue = ThemeService.DEFAULT_HUE_DEG,
            initialThemeHue = ThemeService.DEFAULT_HUE_DEG
        )
        capturedEvents.clear()
        resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
    }

    private fun setContent() {
        composeTestRule.setContent {
            MoLeTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                ProfileDetailContent(
                    uiState = uiState,
                    snackbarHostState = snackbarHostState,
                    onEvent = { event -> capturedEvents.add(event) },
                    resources = resources
                )
            }
        }
    }

    // ========================================
    // Initial State Tests (New Profile)
    // ========================================

    @Test
    fun `new profile shows correct title`() {
        setContent()

        composeTestRule.profileDetailScreen {
            // No actions
        } verify {
            newProfileTitle()
        }
    }

    @Test
    fun `new profile shows name field`() {
        setContent()

        composeTestRule.profileDetailScreen {
            // No actions
        } verify {
            nameFieldIsDisplayed()
        }
    }

    @Test
    fun `new profile shows URL field`() {
        setContent()

        // URL field uses "URL" label from strings.xml
        composeTestRule.onNodeWithText("URL").assertIsDisplayed()
    }

    @Test
    fun `new profile shows save FAB`() {
        setContent()

        composeTestRule.profileDetailScreen {
            // No actions
        } verify {
            saveFabIsDisplayed()
        }
    }

    @Test
    fun `new profile does not show delete button`() {
        setContent()

        // New profile should not have delete button
        val deleteNodes = composeTestRule.onAllNodesWithText("削除").fetchSemanticsNodes()
        assertTrue("Delete button should not be shown for new profile", deleteNodes.isEmpty())
    }

    // ========================================
    // Existing Profile State Tests
    // ========================================

    @Test
    fun `existing profile shows profile name as title`() {
        uiState = uiState.copy(
            profileId = 1L,
            name = "Test Profile"
        )
        setContent()

        composeTestRule.profileDetailScreen {
            // No actions
        } verify {
            profileTitleIs("Test Profile")
        }
    }

    @Test
    fun `existing profile shows delete button`() {
        uiState = uiState.copy(profileId = 1L, name = "Test")
        setContent()

        composeTestRule.profileDetailScreen {
            // No actions
        } verify {
            deleteButtonIsDisplayed()
        }
    }

    @Test
    fun `existing profile shows pre-filled name`() {
        uiState = uiState.copy(
            profileId = 1L,
            name = "Test Profile"
        )
        setContent()

        composeTestRule.onNodeWithText("Profile name")
            .assertTextContains("Test Profile")
    }

    @Test
    fun `existing profile shows pre-filled URL`() {
        uiState = uiState.copy(
            profileId = 1L,
            url = "https://example.com"
        )
        setContent()

        composeTestRule.onNodeWithText("URL")
            .assertTextContains("https://example.com")
    }

    // ========================================
    // Form Interaction Tests
    // ========================================

    @Test
    fun `typing name triggers UpdateName event`() {
        setContent()

        composeTestRule.onNodeWithText("Profile name").performTextInput("New Name")
        composeTestRule.waitForIdle()

        assertTrue(
            "Should capture UpdateName event",
            capturedEvents.any { it is ProfileDetailEvent.UpdateName }
        )
    }

    @Test
    fun `typing URL triggers UpdateUrl event`() {
        setContent()

        composeTestRule.onNodeWithText("URL").performTextInput("example.com")
        composeTestRule.waitForIdle()

        assertTrue(
            "Should capture UpdateUrl event",
            capturedEvents.any { it is ProfileDetailEvent.UpdateUrl }
        )
    }

    // ========================================
    // Action Button Tests
    // ========================================

    @Test
    fun `tapping save FAB triggers Save event`() {
        setContent()

        composeTestRule.profileDetailScreen {
            tapSave()
        }
        composeTestRule.waitForIdle()

        assertTrue(
            "Should capture Save event",
            capturedEvents.any { it is ProfileDetailEvent.Save }
        )
    }

    @Test
    fun `tapping delete button triggers ShowDeleteConfirmDialog event`() {
        uiState = uiState.copy(profileId = 1L, name = "Test")
        setContent()

        composeTestRule.profileDetailScreen {
            tapDelete()
        }
        composeTestRule.waitForIdle()

        assertTrue(
            "Should capture ShowDeleteConfirmDialog event",
            capturedEvents.any { it is ProfileDetailEvent.ShowDeleteConfirmDialog }
        )
    }

    @Test
    fun `tapping back button triggers NavigateBack event`() {
        setContent()

        composeTestRule.profileDetailScreen {
            tapBack()
        }
        composeTestRule.waitForIdle()

        assertTrue(
            "Should capture NavigateBack event",
            capturedEvents.any { it is ProfileDetailEvent.NavigateBack }
        )
    }

    // ========================================
    // Dialog Tests
    // ========================================

    @Test
    fun `delete confirmation dialog is displayed`() {
        uiState = uiState.copy(
            profileId = 1L,
            name = "Test Profile",
            showDeleteConfirmDialog = true
        )
        setContent()

        composeTestRule.profileDetailScreen {
            // No actions
        } verify {
            deleteConfirmDialogIsDisplayed()
        }
    }

    @Test
    fun `confirming delete triggers ConfirmDelete event`() {
        uiState = uiState.copy(
            profileId = 1L,
            name = "Test Profile",
            showDeleteConfirmDialog = true
        )
        setContent()

        composeTestRule.profileDetailScreen {
            confirmDelete()
        }
        composeTestRule.waitForIdle()

        assertTrue(
            "Should capture ConfirmDelete event",
            capturedEvents.any { it is ProfileDetailEvent.ConfirmDelete }
        )
    }

    @Test
    fun `unsaved changes dialog is displayed`() {
        uiState = uiState.copy(showUnsavedChangesDialog = true)
        setContent()

        composeTestRule.profileDetailScreen {
            // No actions
        } verify {
            unsavedChangesDialogIsDisplayed()
        }
    }

    @Test
    fun `saving in unsaved changes dialog triggers Save event`() {
        uiState = uiState.copy(showUnsavedChangesDialog = true)
        setContent()

        composeTestRule.profileDetailScreen {
            saveChangesInDialog()
        }
        composeTestRule.waitForIdle()

        assertTrue(
            "Should capture Save event",
            capturedEvents.any { it is ProfileDetailEvent.Save }
        )
    }

    @Test
    fun `discarding in unsaved changes dialog triggers ConfirmDiscardChanges event`() {
        uiState = uiState.copy(showUnsavedChangesDialog = true)
        setContent()

        composeTestRule.profileDetailScreen {
            discardChangesInDialog()
        }
        composeTestRule.waitForIdle()

        assertTrue(
            "Should capture ConfirmDiscardChanges event",
            capturedEvents.any { it is ProfileDetailEvent.ConfirmDiscardChanges }
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
        val nodes = composeTestRule.onAllNodesWithText("Profile name").fetchSemanticsNodes()
        assertTrue("Profile name field should not be visible during loading", nodes.isEmpty())
    }

    // ========================================
    // Robot Pattern Verification Tests
    // ========================================

    @Test
    fun `robot pattern verify chain works correctly`() {
        uiState = uiState.copy(
            profileId = 1L,
            name = "Test Profile",
            url = "https://example.com"
        )
        setContent()

        composeTestRule.profileDetailScreen {
            // No actions - just verify
        } verify {
            profileTitleIs("Test Profile")
            saveFabIsDisplayed()
            deleteButtonIsDisplayed()
            nameFieldIsDisplayed()
            urlFieldIsDisplayed()
        }
    }
}
