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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import net.ktnx.mobileledger.R
import net.ktnx.mobileledger.json.API
import net.ktnx.mobileledger.model.FutureDates
import net.ktnx.mobileledger.ui.components.ConfirmDialog
import net.ktnx.mobileledger.ui.components.HueRing
import net.ktnx.mobileledger.ui.components.LoadingIndicator
import net.ktnx.mobileledger.ui.components.UnsavedChangesDialog
import net.ktnx.mobileledger.ui.theme.MoLeTheme
import net.ktnx.mobileledger.ui.theme.hslToColor
import net.ktnx.mobileledger.utils.Colors

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
private fun ProfileDetailContent(
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
                    ThemeColorSection(
                        themeHue = uiState.themeHue,
                        initialThemeHue = uiState.initialThemeHue,
                        showHueRingDialog = uiState.showHueRingDialog,
                        onShowHueRingDialog = { onEvent(ProfileDetailEvent.ShowHueRingDialog) },
                        onDismissHueRingDialog = { onEvent(ProfileDetailEvent.DismissHueRingDialog) },
                        onHueSelected = { onEvent(ProfileDetailEvent.UpdateThemeHue(it)) }
                    )

                    HorizontalDivider()

                    // Authentication Section
                    AuthenticationSection(
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
                    ServerDetectionSection(
                        detectedVersion = uiState.detectedVersion?.toString(),
                        isTestingConnection = uiState.isTestingConnection,
                        connectionTestResult = uiState.connectionTestResult,
                        onTestConnection = { onEvent(ProfileDetailEvent.TestConnection) }
                    )

                    HorizontalDivider()

                    // API Version
                    ApiVersionSection(
                        apiVersion = uiState.apiVersion,
                        onApiVersionSelected = { onEvent(ProfileDetailEvent.UpdateApiVersion(it)) },
                        resources = resources
                    )

                    // Future Dates
                    FutureDatesSection(
                        futureDates = uiState.futureDates,
                        onFutureDatesSelected = { onEvent(ProfileDetailEvent.UpdateFutureDates(it)) },
                        resources = resources
                    )

                    HorizontalDivider()

                    // Posting Options
                    PostingOptionsSection(
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

@Composable
private fun ThemeColorSection(
    themeHue: Int,
    initialThemeHue: Int,
    showHueRingDialog: Boolean,
    onShowHueRingDialog: () -> Unit,
    onDismissHueRingDialog: () -> Unit,
    onHueSelected: (Int) -> Unit
) {
    val themeColor = remember(themeHue) {
        hslToColor(themeHue.toFloat(), 0.6f, 0.5f)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "テーマカラー",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(themeColor)
                .clickable { onShowHueRingDialog() }
        )
    }

    if (showHueRingDialog) {
        Dialog(onDismissRequest = onDismissHueRingDialog) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "テーマカラーを選択",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    HueRing(
                        selectedHue = themeHue,
                        initialHue = initialThemeHue,
                        onHueSelected = onHueSelected,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )

                    TextButton(
                        onClick = onDismissHueRingDialog,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("完了")
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthenticationSection(
    useAuthentication: Boolean,
    authUser: String,
    authPassword: String,
    showInsecureWarning: Boolean,
    validationErrors: Map<ProfileField, String>,
    onUseAuthenticationChanged: (Boolean) -> Unit,
    onAuthUserChanged: (String) -> Unit,
    onAuthPasswordChanged: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.pref_title_use_http_auth),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = useAuthentication,
                onCheckedChange = onUseAuthenticationChanged
            )
        }

        AnimatedVisibility(
            visible = useAuthentication,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (showInsecureWarning) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.errorContainer,
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.insecure_scheme_with_auth),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                OutlinedTextField(
                    value = authUser,
                    onValueChange = onAuthUserChanged,
                    label = { Text(stringResource(R.string.pref_title_backend_auth_user)) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = validationErrors.containsKey(ProfileField.AUTH_USER),
                    supportingText = validationErrors[ProfileField.AUTH_USER]?.let {
                        { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                OutlinedTextField(
                    value = authPassword,
                    onValueChange = onAuthPasswordChanged,
                    label = { Text(stringResource(R.string.pref_title_backend_auth_password)) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = validationErrors.containsKey(ProfileField.AUTH_PASSWORD),
                    supportingText = validationErrors[ProfileField.AUTH_PASSWORD]?.let {
                        { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    )
                )
            }
        }
    }
}

@Composable
private fun ServerDetectionSection(
    detectedVersion: String?,
    isTestingConnection: Boolean,
    connectionTestResult: ConnectionTestResult?,
    onTestConnection: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.server_version_title),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = detectedVersion ?: stringResource(R.string.server_version_unknown_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(
                onClick = onTestConnection,
                enabled = !isTestingConnection
            ) {
                if (isTestingConnection) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.detect_button))
            }
        }

        connectionTestResult?.let { result ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        when (result) {
                            is ConnectionTestResult.Success -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                            is ConnectionTestResult.Error -> MaterialTheme.colorScheme.errorContainer
                        },
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (result) {
                    is ConnectionTestResult.Success -> {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "接続成功",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4CAF50)
                        )
                    }

                    is ConnectionTestResult.Error -> {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = result.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ApiVersionSection(apiVersion: API, onApiVersionSelected: (API) -> Unit, resources: Resources) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.api_version_label),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = apiVersion.getDescription(resources),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            listOf(API.auto, API.html, *API.allVersions).forEach { api ->
                DropdownMenuItem(
                    text = { Text(api.getDescription(resources)) },
                    onClick = {
                        onApiVersionSelected(api)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun FutureDatesSection(
    futureDates: FutureDates,
    onFutureDatesSelected: (FutureDates) -> Unit,
    resources: Resources
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.future_dates_label),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = futureDates.getText(resources),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            FutureDates.entries.forEach { fd ->
                DropdownMenuItem(
                    text = { Text(fd.getText(resources)) },
                    onClick = {
                        onFutureDatesSelected(fd)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun PostingOptionsSection(
    permitPosting: Boolean,
    showCommentsByDefault: Boolean,
    showCommodityByDefault: Boolean,
    preferredAccountsFilter: String,
    defaultCommodity: String?,
    onPermitPostingChanged: (Boolean) -> Unit,
    onShowCommentsByDefaultChanged: (Boolean) -> Unit,
    onShowCommodityByDefaultChanged: (Boolean) -> Unit,
    onPreferredAccountsFilterChanged: (String) -> Unit,
    onDefaultCommodityChanged: (String?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.posting_permitted),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = permitPosting,
                onCheckedChange = onPermitPostingChanged
            )
        }

        AnimatedVisibility(
            visible = permitPosting,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.show_comments_by_default),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Checkbox(
                        checked = showCommentsByDefault,
                        onCheckedChange = onShowCommentsByDefaultChanged
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.show_commodity_by_default),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Checkbox(
                        checked = showCommodityByDefault,
                        onCheckedChange = onShowCommodityByDefaultChanged
                    )
                }

                OutlinedTextField(
                    value = preferredAccountsFilter,
                    onValueChange = onPreferredAccountsFilterChanged,
                    label = { Text(stringResource(R.string.preferred_accounts_filter_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // TODO: Open currency selector dialog
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.default_commodity_label),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = defaultCommodity ?: stringResource(R.string.btn_no_currency),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (defaultCommodity == null) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }
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
                themeHue = Colors.DEFAULT_HUE_DEG,
                initialThemeHue = Colors.DEFAULT_HUE_DEG
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
