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
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.util.Date
import kotlinx.coroutines.launch
import net.ktnx.mobileledger.R
import net.ktnx.mobileledger.utils.SimpleDate

/**
 * Main screen composable with tab navigation and drawer.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    mainUiState: MainUiState,
    accountSummaryUiState: AccountSummaryUiState,
    transactionListUiState: TransactionListUiState,
    onMainEvent: (MainEvent) -> Unit,
    onAccountSummaryEvent: (AccountSummaryEvent) -> Unit,
    onTransactionListEvent: (TransactionListEvent) -> Unit,
    onNavigateToNewTransaction: () -> Unit,
    onNavigateToProfileSettings: (Long) -> Unit,
    onNavigateToTemplates: () -> Unit,
    onNavigateToBackups: () -> Unit,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val pagerState = rememberPagerState(
        initialPage = if (mainUiState.selectedTab == MainTab.Accounts) 0 else 1,
        pageCount = { 2 }
    )

    val accountsListState = rememberLazyListState()
    val transactionsListState = rememberLazyListState()

    // Date picker dialog state
    var showDatePicker by remember { mutableStateOf(false) }

    // Sync pager state with UI state
    LaunchedEffect(mainUiState.selectedTab) {
        val targetPage = if (mainUiState.selectedTab == MainTab.Accounts) 0 else 1
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // Keep reference to latest selectedTab for use in collect lambda
    val currentSelectedTab by rememberUpdatedState(mainUiState.selectedTab)

    // Update UI state when pager changes
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            val newTab = if (page == 0) MainTab.Accounts else MainTab.Transactions
            if (currentSelectedTab != newTab) {
                onMainEvent(MainEvent.SelectTab(newTab))
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
                profiles = mainUiState.profiles,
                currentProfileId = mainUiState.currentProfileId,
                onProfileSelected = { profileId ->
                    onMainEvent(MainEvent.SelectProfile(profileId))
                    scope.launch { drawerState.close() }
                },
                onEditProfile = { profileId ->
                    onNavigateToProfileSettings(profileId)
                    scope.launch { drawerState.close() }
                },
                onCreateNewProfile = {
                    onNavigateToProfileSettings(-1)
                    scope.launch { drawerState.close() }
                },
                onNavigateToTemplates = {
                    onNavigateToTemplates()
                    scope.launch { drawerState.close() }
                },
                onNavigateToBackups = {
                    onNavigateToBackups()
                    scope.launch { drawerState.close() }
                },
                onProfilesReordered = { orderedProfiles ->
                    onMainEvent(MainEvent.ReorderProfiles(orderedProfiles))
                },
                modifier = Modifier.fillMaxWidth(0.85f)
            )
        }
    ) {
        Scaffold(
            modifier = modifier,
            topBar = {
                TopAppBar(
                    title = { Text(mainUiState.currentProfileName.ifEmpty { stringResource(R.string.app_name) }) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = stringResource(R.string.nav_header_desc)
                            )
                        }
                    },
                    actions = {
                        // Show zero balance toggle only on Accounts tab
                        if (mainUiState.selectedTab == MainTab.Accounts) {
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
                        // Show go-to-date button only on Transactions tab
                        if (mainUiState.selectedTab == MainTab.Transactions) {
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
                if (mainUiState.currentProfileCanPost) {
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
            if (mainUiState.currentProfileId == null) {
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

                    // Pull-to-refresh state with explicit control
                    val pullToRefreshState = rememberPullToRefreshState()

                    // Hide the indicator when refresh completes
                    LaunchedEffect(mainUiState.isRefreshing) {
                        if (!mainUiState.isRefreshing) {
                            pullToRefreshState.animateToHidden()
                        }
                    }

                    // Pager with pull-to-refresh
                    PullToRefreshBox(
                        state = pullToRefreshState,
                        isRefreshing = mainUiState.isRefreshing,
                        onRefresh = { onMainEvent(MainEvent.RefreshData) },
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

@Composable
private fun WelcomeScreen(onCreateProfile: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = stringResource(R.string.text_welcome),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.text_welcome_profile_needed),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp)
            )
            androidx.compose.material3.Button(
                onClick = onCreateProfile,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Text(stringResource(R.string.new_profile_title))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoToDatePickerDialog(
    minDate: SimpleDate?,
    maxDate: SimpleDate?,
    onDateSelected: (SimpleDate) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = SimpleDate.today().toDate().time,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                if (minDate == null || maxDate == null) return true
                val date = SimpleDate.fromDate(Date(utcTimeMillis))
                return date >= minDate && date <= maxDate
            }
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(SimpleDate.fromDate(Date(millis)))
                    }
                }
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
