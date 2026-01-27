/*
 * Copyright © 2024 Damyan Ivanov.
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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import net.ktnx.mobileledger.R

// Import specialized ViewModel states and events

/**
 * Main screen composable with tab navigation and drawer.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    coordinatorUiState: MainCoordinatorUiState,
    profileSelectionUiState: ProfileSelectionUiState,
    accountSummaryUiState: AccountSummaryUiState,
    transactionListUiState: TransactionListUiState,
    drawerOpen: Boolean,
    snackbarHostState: SnackbarHostState,
    onCoordinatorEvent: (MainCoordinatorEvent) -> Unit,
    onProfileSelectionEvent: (ProfileSelectionEvent) -> Unit,
    onAccountSummaryEvent: (AccountSummaryEvent) -> Unit,
    onTransactionListEvent: (TransactionListEvent) -> Unit,
    onNavigateToNewTransaction: () -> Unit,
    onNavigateToProfileSettings: (Long) -> Unit,
    onNavigateToTemplates: () -> Unit,
    onNavigateToBackups: () -> Unit,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = if (drawerOpen) DrawerValue.Open else DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Keep updated reference for use in collect lambda
    val currentDrawerOpen by rememberUpdatedState(drawerOpen)

    // T043: Sync ViewModel -> Compose drawer state
    LaunchedEffect(drawerOpen) {
        if (drawerOpen && !drawerState.isOpen) {
            drawerState.open()
        } else if (!drawerOpen && drawerState.isOpen) {
            drawerState.close()
        }
    }

    // Sync Compose -> ViewModel drawer state (when user closes by tapping outside)
    LaunchedEffect(drawerState) {
        snapshotFlow { drawerState.isClosed }
            .collect { isClosed ->
                if (isClosed && currentDrawerOpen) {
                    onCoordinatorEvent(MainCoordinatorEvent.CloseDrawer)
                }
            }
    }

    val pagerState = rememberPagerState(
        initialPage = if (coordinatorUiState.selectedTab == MainTab.Accounts) 0 else 1,
        pageCount = { 2 }
    )

    val transactionsListState = rememberLazyListState()

    // Date picker dialog state
    var showDatePicker by remember { mutableStateOf(false) }

    // Sync pager state with UI state
    LaunchedEffect(coordinatorUiState.selectedTab) {
        val targetPage = if (coordinatorUiState.selectedTab == MainTab.Accounts) 0 else 1
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // Keep reference to latest selectedTab for use in collect lambda
    val currentSelectedTab by rememberUpdatedState(coordinatorUiState.selectedTab)

    // Update UI state when pager changes
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            val newTab = if (page == 0) MainTab.Accounts else MainTab.Transactions
            if (currentSelectedTab != newTab) {
                onCoordinatorEvent(MainCoordinatorEvent.SelectTab(newTab))
            }
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        GoToDatePickerDialog(
            minDate = transactionListUiState.firstTransactionDate,
            maxDate = transactionListUiState.lastTransactionDate,
            onDateSelected = { date ->
                showDatePicker = false
                onTransactionListEvent(TransactionListEvent.GoToDate(date))
            },
            onDismiss = { showDatePicker = false }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NavigationDrawerContent(
                profiles = profileSelectionUiState.profiles,
                currentProfileId = profileSelectionUiState.currentProfileId,
                onProfileSelected = { profileId ->
                    onProfileSelectionEvent(ProfileSelectionEvent.SelectProfile(profileId))
                    onCoordinatorEvent(MainCoordinatorEvent.CloseDrawer)
                },
                onEditProfile = { profileId ->
                    onNavigateToProfileSettings(profileId)
                    onCoordinatorEvent(MainCoordinatorEvent.CloseDrawer)
                },
                onCreateNewProfile = {
                    onNavigateToProfileSettings(-1)
                    onCoordinatorEvent(MainCoordinatorEvent.CloseDrawer)
                },
                onNavigateToTemplates = {
                    onNavigateToTemplates()
                    onCoordinatorEvent(MainCoordinatorEvent.CloseDrawer)
                },
                onNavigateToBackups = {
                    onNavigateToBackups()
                    onCoordinatorEvent(MainCoordinatorEvent.CloseDrawer)
                },
                onProfilesReordered = { orderedProfiles ->
                    onProfileSelectionEvent(ProfileSelectionEvent.ReorderProfiles(orderedProfiles))
                },
                modifier = Modifier.fillMaxWidth(0.85f)
            )
        }
    ) {
        Scaffold(
            modifier = modifier,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = profileSelectionUiState.currentProfileName.ifEmpty {
                                stringResource(R.string.app_name)
                            },
                            modifier = Modifier.testTag("top_bar_title")
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { onCoordinatorEvent(MainCoordinatorEvent.OpenDrawer) },
                            modifier = Modifier.testTag("menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = stringResource(R.string.nav_header_desc)
                            )
                        }
                    },
                    actions = {
                        // Show zero balance toggle only on Accounts tab
                        if (coordinatorUiState.selectedTab == MainTab.Accounts) {
                            IconButton(
                                onClick = {
                                    onAccountSummaryEvent(AccountSummaryEvent.ToggleZeroBalanceAccounts)
                                }
                            ) {
                                Icon(
                                    imageVector = if (accountSummaryUiState.showZeroBalanceAccounts) {
                                        Icons.Default.Visibility
                                    } else {
                                        Icons.Default.VisibilityOff
                                    },
                                    contentDescription = stringResource(
                                        R.string.accounts_menu_show_zero
                                    )
                                )
                            }
                        }
                        // Show filter and go-to-date buttons only on Transactions tab
                        if (coordinatorUiState.selectedTab == MainTab.Transactions) {
                            // Search icon to show filter bar (only when filter bar is hidden)
                            if (!transactionListUiState.showAccountFilterInput) {
                                IconButton(
                                    onClick = {
                                        onTransactionListEvent(TransactionListEvent.ShowAccountFilterInput)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = stringResource(R.string.filter_by_account)
                                    )
                                }
                            }
                            // Go to date button
                            IconButton(
                                onClick = { showDatePicker = true },
                                enabled = transactionListUiState.firstTransactionDate != null
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = stringResource(R.string.go_to_date_menu_title)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            },
            floatingActionButton = {
                if (coordinatorUiState.currentProfileCanPost) {
                    FloatingActionButton(
                        onClick = onNavigateToNewTransaction,
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.new_transaction_fab_description)
                        )
                    }
                }
            }
        ) { paddingValues ->
            if (coordinatorUiState.currentProfileId == null) {
                // No profile selected - show welcome message
                WelcomeScreen(
                    onCreateProfile = { onNavigateToProfileSettings(-1) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Tab row
                    TabRow(
                        selectedTabIndex = pagerState.currentPage
                    ) {
                        Tab(
                            selected = pagerState.currentPage == 0,
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(0) }
                            },
                            text = { Text(stringResource(R.string.account_summary_title)) }
                        )
                        Tab(
                            selected = pagerState.currentPage == 1,
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(1) }
                            },
                            text = { Text(stringResource(R.string.title_latest_transactions)) }
                        )
                    }

                    // Pull-to-refresh state
                    // Note: PullToRefreshBox automatically hides the indicator when
                    // isRefreshing becomes false. Explicit animateToHidden() calls can
                    // cause race conditions with quick consecutive swipes.
                    val pullToRefreshState = rememberPullToRefreshState()

                    // Pager with pull-to-refresh
                    PullToRefreshBox(
                        state = pullToRefreshState,
                        isRefreshing = coordinatorUiState.isRefreshing,
                        onRefresh = { onCoordinatorEvent(MainCoordinatorEvent.RefreshData) },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            when (page) {
                                0 -> AccountSummaryTab(
                                    uiState = accountSummaryUiState,
                                    onToggleExpanded = { accountId ->
                                        onAccountSummaryEvent(AccountSummaryEvent.ToggleAccountExpanded(accountId))
                                    },
                                    onToggleAmountsExpanded = { accountId ->
                                        onAccountSummaryEvent(AccountSummaryEvent.ToggleAmountsExpanded(accountId))
                                    },
                                    onAccountClick = { accountName ->
                                        onAccountSummaryEvent(AccountSummaryEvent.ShowAccountTransactions(accountName))
                                    }
                                )

                                1 -> TransactionListTab(
                                    uiState = transactionListUiState,
                                    onAccountFilterChanged = { filter ->
                                        onTransactionListEvent(TransactionListEvent.SetAccountFilter(filter))
                                    },
                                    onSuggestionSelected = { accountName ->
                                        onTransactionListEvent(TransactionListEvent.SelectSuggestion(accountName))
                                    },
                                    onClearFilter = {
                                        onTransactionListEvent(TransactionListEvent.ClearAccountFilter)
                                    },
                                    listState = transactionsListState
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
