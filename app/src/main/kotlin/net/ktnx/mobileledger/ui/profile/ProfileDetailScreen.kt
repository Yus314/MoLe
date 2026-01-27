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

package net.ktnx.mobileledger.ui.profile

import android.content.res.Resources
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import net.ktnx.mobileledger.R
import net.ktnx.mobileledger.service.ThemeService
import net.ktnx.mobileledger.ui.components.ConfirmDialog
import net.ktnx.mobileledger.ui.components.LoadingIndicator
import net.ktnx.mobileledger.ui.components.UnsavedChangesDialog
import net.ktnx.mobileledger.ui.theme.MoLeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDetailScreen(
    viewModel: ProfileDetailViewModel = hiltViewModel(),
    profileId: Long = 0L,
    initialThemeHue: Int = -1,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(profileId, initialThemeHue) {
        viewModel.initialize(profileId, initialThemeHue)
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ProfileDetailEffect.NavigateBack -> onNavigateBack()

                ProfileDetailEffect.ProfileSaved -> {
                    snackbarHostState.showSnackbar(
                        message = "プロファイルを保存しました",
                        duration = SnackbarDuration.Short
                    )
                }

                ProfileDetailEffect.ProfileDeleted -> {
                    snackbarHostState.showSnackbar(
                        message = "プロファイルを削除しました",
                        duration = SnackbarDuration.Short
                    )
                }

                is ProfileDetailEffect.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = effect.message,
                        duration = SnackbarDuration.Long
                    )
                }
            }
        }
    }

    BackHandler(enabled = uiState.hasUnsavedChanges) {
        viewModel.onEvent(ProfileDetailEvent.NavigateBack)
    }

    ProfileDetailContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
        resources = context.resources
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileDetailContent(
    uiState: ProfileDetailUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (ProfileDetailEvent) -> Unit,
    resources: Resources
) {
    Scaffold(
        contentWindowInsets = WindowInsets.ime,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isNewProfile) {
                            stringResource(R.string.new_profile_title)
                        } else {
                            uiState.name.ifEmpty { stringResource(R.string.new_profile_title) }
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onEvent(ProfileDetailEvent.NavigateBack) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る"
                        )
                    }
                },
                actions = {
                    if (uiState.canDelete) {
                        IconButton(onClick = { onEvent(ProfileDetailEvent.ShowDeleteConfirmDialog) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "削除"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onEvent(ProfileDetailEvent.Save) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "保存",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                LoadingIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Profile Name
                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = { onEvent(ProfileDetailEvent.UpdateName(it)) },
                        label = { Text(stringResource(R.string.profile_name_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = uiState.validationErrors.containsKey(ProfileField.NAME),
                        supportingText = uiState.validationErrors[ProfileField.NAME]?.let {
                            { Text(it, color = MaterialTheme.colorScheme.error) }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    // Server URL
                    OutlinedTextField(
                        value = uiState.url,
                        onValueChange = { onEvent(ProfileDetailEvent.UpdateUrl(it)) },
                        label = { Text(stringResource(R.string.url_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = uiState.validationErrors.containsKey(ProfileField.URL),
                        supportingText = uiState.validationErrors[ProfileField.URL]?.let {
                            { Text(it, color = MaterialTheme.colorScheme.error) }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next
                        )
                    )

                    // Theme Color
                    ProfileThemeColorSection(
                        themeHue = uiState.themeHue,
                        initialThemeHue = uiState.initialThemeHue,
                        showHueRingDialog = uiState.showHueRingDialog,
                        onShowHueRingDialog = { onEvent(ProfileDetailEvent.ShowHueRingDialog) },
                        onDismissHueRingDialog = { onEvent(ProfileDetailEvent.DismissHueRingDialog) },
                        onHueSelected = { onEvent(ProfileDetailEvent.UpdateThemeHue(it)) }
                    )

                    HorizontalDivider()

                    // Authentication Section
                    ProfileAuthenticationSection(
                        useAuthentication = uiState.useAuthentication,
                        authUser = uiState.authUser,
                        authPassword = uiState.authPassword,
                        showInsecureWarning = uiState.showInsecureWarning,
                        validationErrors = uiState.validationErrors,
                        onUseAuthenticationChanged = { onEvent(ProfileDetailEvent.UpdateUseAuthentication(it)) },
                        onAuthUserChanged = { onEvent(ProfileDetailEvent.UpdateAuthUser(it)) },
                        onAuthPasswordChanged = { onEvent(ProfileDetailEvent.UpdateAuthPassword(it)) }
                    )

                    HorizontalDivider()

                    // Server Detection Section
                    ProfileServerDetectionSection(
                        detectedVersion = uiState.detectedVersion?.toString(),
                        isTestingConnection = uiState.isTestingConnection,
                        connectionTestResult = uiState.connectionTestResult,
                        onTestConnection = { onEvent(ProfileDetailEvent.TestConnection) }
                    )

                    HorizontalDivider()

                    // API Version
                    ProfileApiVersionSection(
                        apiVersion = uiState.apiVersion,
                        onApiVersionSelected = { onEvent(ProfileDetailEvent.UpdateApiVersion(it)) }
                    )

                    // Future Dates
                    ProfileFutureDatesSection(
                        futureDates = uiState.futureDates,
                        onFutureDatesSelected = { onEvent(ProfileDetailEvent.UpdateFutureDates(it)) },
                        resources = resources
                    )

                    HorizontalDivider()

                    // Posting Options
                    ProfilePostingOptionsSection(
                        permitPosting = uiState.permitPosting,
                        showCommentsByDefault = uiState.showCommentsByDefault,
                        showCommodityByDefault = uiState.showCommodityByDefault,
                        preferredAccountsFilter = uiState.preferredAccountsFilter,
                        defaultCommodity = uiState.defaultCommodity,
                        onPermitPostingChanged = { onEvent(ProfileDetailEvent.UpdatePermitPosting(it)) },
                        onShowCommentsByDefaultChanged = {
                            onEvent(ProfileDetailEvent.UpdateShowCommentsByDefault(it))
                        },
                        onShowCommodityByDefaultChanged = {
                            onEvent(ProfileDetailEvent.UpdateShowCommodityByDefault(it))
                        },
                        onPreferredAccountsFilterChanged = {
                            onEvent(ProfileDetailEvent.UpdatePreferredAccountsFilter(it))
                        },
                        onDefaultCommodityChanged = { onEvent(ProfileDetailEvent.UpdateDefaultCommodity(it)) }
                    )

                    Spacer(modifier = Modifier.height(72.dp)) // Space for FAB
                }
            }
        }
    }

    // Dialogs
    if (uiState.showUnsavedChangesDialog) {
        UnsavedChangesDialog(
            onSave = { onEvent(ProfileDetailEvent.Save) },
            onDiscard = { onEvent(ProfileDetailEvent.ConfirmDiscardChanges) },
            onDismiss = { onEvent(ProfileDetailEvent.DismissUnsavedChangesDialog) }
        )
    }

    if (uiState.showDeleteConfirmDialog) {
        ConfirmDialog(
            title = uiState.name.ifEmpty { "プロファイル" },
            message = stringResource(R.string.remove_profile_dialog_message),
            confirmText = stringResource(R.string.Remove),
            dismissText = stringResource(android.R.string.cancel),
            onConfirm = { onEvent(ProfileDetailEvent.ConfirmDelete) },
            onDismiss = { onEvent(ProfileDetailEvent.DismissDeleteConfirmDialog) },
            isDestructive = true
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileDetailScreenPreview() {
    MoLeTheme {
        ProfileDetailContent(
            uiState = ProfileDetailUiState(
                name = "Test Profile",
                url = "https://example.com",
                themeHue = ThemeService.DEFAULT_HUE_DEG,
                initialThemeHue = ThemeService.DEFAULT_HUE_DEG
            ),
            snackbarHostState = SnackbarHostState(),
            onEvent = {},
            resources = LocalContext.current.resources
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileDetailScreenNewProfilePreview() {
    MoLeTheme {
        ProfileDetailContent(
            uiState = ProfileDetailUiState(),
            snackbarHostState = SnackbarHostState(),
            onEvent = {},
            resources = LocalContext.current.resources
        )
    }
}
