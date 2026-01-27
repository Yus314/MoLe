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

package net.ktnx.mobileledger.ui.main

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlinx.collections.immutable.persistentListOf
import net.ktnx.mobileledger.robot.main.mainScreen
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
 * Robolectric tests for MainScreen using Robot Pattern.
 *
 * Tests the main screen UI including:
 * - Tab navigation
 * - Drawer operations
 * - Profile display
 * - Welcome screen for no profile
 * - FAB visibility based on profile settings
 *
 * Uses testTag for dynamic list items per best practices.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MainScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // UI States
    private var coordinatorUiState by mutableStateOf(MainCoordinatorUiState())
    private var profileSelectionUiState by mutableStateOf(ProfileSelectionUiState())
    private var accountSummaryUiState by mutableStateOf(AccountSummaryUiState())
    private var transactionListUiState by mutableStateOf(TransactionListUiState())
    private var drawerOpen by mutableStateOf(false)

    // Captured events for verification
    private val capturedCoordinatorEvents = mutableListOf<MainCoordinatorEvent>()
    private val capturedProfileSelectionEvents = mutableListOf<ProfileSelectionEvent>()
    private val capturedAccountSummaryEvents = mutableListOf<AccountSummaryEvent>()
    private val capturedTransactionListEvents = mutableListOf<TransactionListEvent>()

    // Navigation callbacks
    private var navigatedToNewTransaction = false
    private var navigatedToProfileId: Long? = null
    private var navigatedToTemplates = false
    private var navigatedToBackups = false

    @Before
    fun setup() {
        // Initialize Colors for testing (required for ProfileRow theme colors)
        initializeThemeServiceForTesting()

        // Reset states
        coordinatorUiState = MainCoordinatorUiState(
            selectedTab = MainTab.Accounts,
            currentProfileId = 1L,
            currentProfileCanPost = true
        )
        profileSelectionUiState = ProfileSelectionUiState(
            currentProfileId = 1L,
            currentProfileName = "Test Profile",
            profiles = listOf(
                ProfileListItem(id = 1L, name = "Test Profile", theme = -1, canPost = true)
            )
        )
        accountSummaryUiState = AccountSummaryUiState(
            accounts = listOf(
                AccountSummaryListItem.Header("Last updated: 2026/01/22"),
                AccountSummaryListItem.Account(
                    id = 1L,
                    name = "Assets:Cash",
                    shortName = "Cash",
                    level = 1,
                    amounts = listOf(AccountAmount(100f, "USD", "$100.00"))
                )
            )
        )
        transactionListUiState = TransactionListUiState(
            transactions = persistentListOf()
        )
        drawerOpen = false

        // Reset captured events
        capturedCoordinatorEvents.clear()
        capturedProfileSelectionEvents.clear()
        capturedAccountSummaryEvents.clear()
        capturedTransactionListEvents.clear()

        // Reset navigation flags
        navigatedToNewTransaction = false
        navigatedToProfileId = null
        navigatedToTemplates = false
        navigatedToBackups = false
    }

    /**
     * Initialize the ThemeService for testing.
     * This is required because ThemeService.getPrimaryColorForHue depends on theme colors
     * that are normally populated by refreshColors() during app startup.
     */
    private fun initializeThemeServiceForTesting() {
        // Create a ThemeServiceImpl instance
        val themeService = net.ktnx.mobileledger.service.ThemeServiceImpl()

        // Use reflection to populate the themePrimaryColor map
        val themePrimaryColorField = net.ktnx.mobileledger.service.ThemeServiceImpl::class.java
            .getDeclaredField("themePrimaryColor")
        themePrimaryColorField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val map = themePrimaryColorField.get(themeService) as HashMap<Int, Int>
        // Add a default color for testing - the default theme ID
        // R.style.AppTheme_default maps to DEFAULT_HUE_DEG (261)
        map[net.ktnx.mobileledger.R.style.AppTheme_default] = 0xFF6200EE.toInt()
        // Add colors for common hue values used in tests
        map[net.ktnx.mobileledger.R.style.AppTheme_000] = 0xFFFF0000.toInt()
        map[net.ktnx.mobileledger.R.style.AppTheme_090] = 0xFF00FF00.toInt()
        map[net.ktnx.mobileledger.R.style.AppTheme_180] = 0xFF0000FF.toInt()
        map[net.ktnx.mobileledger.R.style.AppTheme_270] = 0xFFFFFF00.toInt()

        // Set the ThemeService in the EntryPoint via reflection
        val cachedServiceField = net.ktnx.mobileledger.di.ThemeServiceEntryPoint.Companion::class.java
            .getDeclaredField("cachedService")
        cachedServiceField.isAccessible = true
        cachedServiceField.set(net.ktnx.mobileledger.di.ThemeServiceEntryPoint.Companion, themeService)
    }

    private fun setContent() {
        composeTestRule.setContent {
            val snackbarHostState = remember { SnackbarHostState() }
            MoLeTheme {
                MainScreen(
                    coordinatorUiState = coordinatorUiState,
                    profileSelectionUiState = profileSelectionUiState,
                    accountSummaryUiState = accountSummaryUiState,
                    transactionListUiState = transactionListUiState,
                    drawerOpen = drawerOpen,
                    snackbarHostState = snackbarHostState,
                    onCoordinatorEvent = { event -> capturedCoordinatorEvents.add(event) },
                    onProfileSelectionEvent = { event -> capturedProfileSelectionEvents.add(event) },
                    onAccountSummaryEvent = { event -> capturedAccountSummaryEvents.add(event) },
                    onTransactionListEvent = { event -> capturedTransactionListEvents.add(event) },
                    onNavigateToNewTransaction = { navigatedToNewTransaction = true },
                    onNavigateToProfileSettings = { profileId -> navigatedToProfileId = profileId },
                    onNavigateToTemplates = { navigatedToTemplates = true },
                    onNavigateToBackups = { navigatedToBackups = true }
                )
            }
        }
    }

    // ========================================
    // Initial State Tests
    // ========================================

    @Test
    fun `initial state displays profile name in top bar`() {
        setContent()

        composeTestRule.onNodeWithTag("top_bar_title").assertTextEquals("Test Profile")
    }

    @Test
    fun `initial state displays tabs`() {
        setContent()

        composeTestRule.mainScreen {
            // No actions
        } verify {
            accountsTabIsDisplayed()
            transactionsTabIsDisplayed()
        }
    }

    @Test
    fun `FAB is displayed when profile can post`() {
        setContent()

        composeTestRule.mainScreen {
            // No actions
        } verify {
            newTransactionFabIsDisplayed()
        }
    }

    @Test
    fun `FAB is not displayed when profile cannot post`() {
        coordinatorUiState = coordinatorUiState.copy(currentProfileCanPost = false)
        setContent()

        composeTestRule.mainScreen {
            // No actions
        } verify {
            newTransactionFabIsNotDisplayed()
        }
    }

    // ========================================
    // Tab Navigation Tests
    // ========================================

    @Test
    fun `tapping transactions tab triggers SelectTab event`() {
        setContent()

        composeTestRule.onNodeWithText("Transactions").performClick()
        composeTestRule.waitForIdle()

        assertTrue(
            "Should capture SelectTab event for Transactions",
            capturedCoordinatorEvents.any {
                it is MainCoordinatorEvent.SelectTab && it.tab == MainTab.Transactions
            }
        )
    }

    @Test
    fun `tapping accounts tab triggers SelectTab event when on transactions`() {
        coordinatorUiState = coordinatorUiState.copy(selectedTab = MainTab.Transactions)
        setContent()

        composeTestRule.onNodeWithText("Accounts").performClick()
        composeTestRule.waitForIdle()

        assertTrue(
            "Should capture SelectTab event for Accounts",
            capturedCoordinatorEvents.any {
                it is MainCoordinatorEvent.SelectTab && it.tab == MainTab.Accounts
            }
        )
    }

    // ========================================
    // Drawer Tests
    // ========================================

    @Test
    fun `tapping menu icon triggers OpenDrawer event`() {
        setContent()

        // Use testTag to uniquely identify the menu button
        composeTestRule.onNodeWithTag("menu_button").performClick()

        assertTrue(
            "Should capture OpenDrawer event",
            capturedCoordinatorEvents.any { it is MainCoordinatorEvent.OpenDrawer }
        )
    }

    // ========================================
    // FAB Tests
    // ========================================

    @Test
    fun `tapping FAB triggers navigation to new transaction`() {
        setContent()

        composeTestRule.onNodeWithContentDescription("Plus icon").performClick()

        assertTrue("Should navigate to new transaction", navigatedToNewTransaction)
    }

    // ========================================
    // No Profile State Tests
    // ========================================

    @Test
    fun `welcome screen is displayed when no profile is selected`() {
        coordinatorUiState = coordinatorUiState.copy(currentProfileId = null)
        profileSelectionUiState = profileSelectionUiState.copy(
            currentProfileId = null,
            currentProfileName = ""
        )
        setContent()

        composeTestRule.mainScreen {
            // No actions
        } verify {
            welcomeScreenIsDisplayed()
        }
    }

    @Test
    fun `app name is displayed when no profile selected`() {
        coordinatorUiState = coordinatorUiState.copy(currentProfileId = null)
        profileSelectionUiState = profileSelectionUiState.copy(
            currentProfileId = null,
            currentProfileName = ""
        )
        setContent()

        composeTestRule.mainScreen {
            // No actions
        } verify {
            appNameIsDisplayed()
        }
    }

    // ========================================
    // Account Tab Action Tests
    // ========================================

    @Test
    fun `tapping zero balance toggle triggers ToggleZeroBalanceAccounts event`() {
        setContent()

        composeTestRule.onNodeWithContentDescription("Show zero balances").performClick()

        assertTrue(
            "Should capture ToggleZeroBalanceAccounts event",
            capturedAccountSummaryEvents.any { it is AccountSummaryEvent.ToggleZeroBalanceAccounts }
        )
    }

    // ========================================
    // Transaction Tab Action Tests
    // ========================================

    @Test
    fun `filter icon triggers ShowAccountFilterInput event on transactions tab`() {
        coordinatorUiState = coordinatorUiState.copy(selectedTab = MainTab.Transactions)
        transactionListUiState = transactionListUiState.copy(showAccountFilterInput = false)
        setContent()

        composeTestRule.onNodeWithContentDescription("Filter by account").performClick()

        assertTrue(
            "Should capture ShowAccountFilterInput event",
            capturedTransactionListEvents.any { it is TransactionListEvent.ShowAccountFilterInput }
        )
    }

    // ========================================
    // Robot Pattern Verification Tests
    // ========================================

    @Test
    fun `robot pattern verify chain works correctly`() {
        setContent()

        composeTestRule.mainScreen {
            // No actions - just verify initial state
        } verify {
            accountsTabIsDisplayed()
            transactionsTabIsDisplayed()
            profileNameIsDisplayed("Test Profile")
            newTransactionFabIsDisplayed()
        }
    }

    // ========================================
    // Account Display Tests
    // ========================================

    @Test
    fun `account is displayed in accounts tab`() {
        setContent()

        composeTestRule.mainScreen {
            // No actions
        } verify {
            accountIsDisplayed("Cash")
        }
    }

    // ========================================
    // Multiple Profiles Tests
    // ========================================

    @Test
    fun `drawer shows multiple profiles`() {
        profileSelectionUiState = profileSelectionUiState.copy(
            profiles = listOf(
                ProfileListItem(id = 1L, name = "Test Profile", theme = -1, canPost = true),
                ProfileListItem(id = 2L, name = "Work Profile", theme = -1, canPost = true),
                ProfileListItem(id = 3L, name = "Personal", theme = -1, canPost = false)
            )
        )
        drawerOpen = true
        setContent()

        composeTestRule.mainScreen {
            // Drawer is already open
        } verify {
            profileInDrawerIsDisplayed("Test Profile")
            profileInDrawerIsDisplayed("Work Profile")
            profileInDrawerIsDisplayed("Personal")
        }
    }

    @Test
    fun `selecting profile in drawer triggers SelectProfile event`() {
        profileSelectionUiState = profileSelectionUiState.copy(
            profiles = listOf(
                ProfileListItem(id = 1L, name = "Test Profile", theme = -1, canPost = true),
                ProfileListItem(id = 2L, name = "Work Profile", theme = -1, canPost = true)
            )
        )
        drawerOpen = true
        setContent()

        composeTestRule.onNodeWithText("Work Profile").performClick()

        assertTrue(
            "Should capture SelectProfile event with id 2",
            capturedProfileSelectionEvents.any {
                it is ProfileSelectionEvent.SelectProfile && it.profileId == 2L
            }
        )
    }

    // ========================================
    // Drawer Menu Tests
    // ========================================

    @Test
    fun `drawer shows templates and backup menu items`() {
        drawerOpen = true
        setContent()

        composeTestRule.mainScreen {
            // Drawer is already open
        } verify {
            templatesMenuIsDisplayed()
            backupRestoreMenuIsDisplayed()
        }
    }
}
