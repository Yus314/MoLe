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

package net.ktnx.mobileledger.ui.backups

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createComposeRule
import dagger.hilt.android.testing.HiltTestApplication
import net.ktnx.mobileledger.robot.backups.backupsScreen
import net.ktnx.mobileledger.ui.theme.MoLeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Compose UI tests for BackupsScreen.
 *
 * Tests cover:
 * - Screen structure and element visibility
 * - Button enabled/disabled states based on UiState
 * - Event triggering on user interactions
 * - Navigation callback
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [34],
    application = HiltTestApplication::class
)
class BackupsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var capturedEvents: MutableList<BackupsEvent>
    private var navigateBackCalled = false

    @Before
    fun setup() {
        capturedEvents = mutableListOf()
        navigateBackCalled = false
    }

    // ========================================
    // Helper
    // ========================================

    private fun setupScreen(uiState: BackupsUiState = BackupsUiState()) {
        composeTestRule.setContent {
            MoLeTheme {
                BackupsScreen(
                    uiState = uiState,
                    snackbarHostState = remember { SnackbarHostState() },
                    onEvent = { capturedEvents.add(it) },
                    onNavigateBack = { navigateBackCalled = true }
                )
            }
        }
    }

    // ========================================
    // Screen Structure Tests
    // ========================================

    @Test
    fun `screen displays title`() {
        setupScreen()

        composeTestRule.backupsScreen {} verify {
            titleIsDisplayed()
        }
    }

    @Test
    fun `screen displays back button`() {
        setupScreen()

        composeTestRule.backupsScreen {} verify {
            backButtonIsDisplayed()
        }
    }

    // ========================================
    // Backup Section Tests
    // ========================================

    @Test
    fun `backup section displays header`() {
        setupScreen()

        composeTestRule.backupsScreen {} verify {
            backupHeaderIsDisplayed()
        }
    }

    @Test
    fun `backup section displays explanation`() {
        setupScreen()

        composeTestRule.backupsScreen {} verify {
            backupExplanationIsDisplayed()
        }
    }

    @Test
    fun `backup button enabled when profile exists`() {
        setupScreen(BackupsUiState(backupEnabled = true))

        composeTestRule.backupsScreen {} verify {
            backupButtonIsEnabled()
        }
    }

    @Test
    fun `backup button disabled when no profile`() {
        setupScreen(BackupsUiState(backupEnabled = false))

        composeTestRule.backupsScreen {} verify {
            backupButtonIsDisabled()
        }
    }

    @Test
    fun `backup button disabled during backup operation`() {
        setupScreen(BackupsUiState(backupEnabled = true, isBackingUp = true))

        composeTestRule.backupsScreen {} verify {
            backupButtonIsDisabled()
        }
    }

    // ========================================
    // Restore Section Tests
    // ========================================

    @Test
    fun `restore section displays header`() {
        setupScreen()

        composeTestRule.backupsScreen {} verify {
            restoreHeaderIsDisplayed()
        }
    }

    @Test
    fun `restore section displays explanation`() {
        setupScreen()

        composeTestRule.backupsScreen {} verify {
            restoreExplanationIsDisplayed()
        }
    }

    @Test
    fun `restore button enabled by default`() {
        setupScreen(BackupsUiState())

        composeTestRule.backupsScreen {} verify {
            restoreButtonIsEnabled()
        }
    }

    @Test
    fun `restore button disabled during restore operation`() {
        setupScreen(BackupsUiState(isRestoring = true))

        composeTestRule.backupsScreen {} verify {
            restoreButtonIsDisabled()
        }
    }

    // ========================================
    // Event Triggering Tests
    // ========================================

    @Test
    fun `backup button click triggers BackupClicked event`() {
        setupScreen(BackupsUiState(backupEnabled = true))

        composeTestRule.backupsScreen {
            tapBackupButton()
        }

        assertEquals(1, capturedEvents.size)
        assertTrue(capturedEvents[0] is BackupsEvent.BackupClicked)
    }

    @Test
    fun `restore button click triggers RestoreClicked event`() {
        setupScreen(BackupsUiState())

        composeTestRule.backupsScreen {
            tapRestoreButton()
        }

        assertEquals(1, capturedEvents.size)
        assertTrue(capturedEvents[0] is BackupsEvent.RestoreClicked)
    }

    @Test
    fun `back button click triggers navigation callback`() {
        setupScreen()

        composeTestRule.backupsScreen {
            tapBackButton()
        }

        assertTrue(navigateBackCalled)
    }

    // ========================================
    // Combined State Tests
    // ========================================

    @Test
    fun `both sections are visible`() {
        setupScreen(BackupsUiState(backupEnabled = true))

        composeTestRule.backupsScreen {} verify {
            backupHeaderIsDisplayed()
            backupExplanationIsDisplayed()
            backupButtonIsEnabled()
            restoreHeaderIsDisplayed()
            restoreExplanationIsDisplayed()
            restoreButtonIsEnabled()
        }
    }
}
