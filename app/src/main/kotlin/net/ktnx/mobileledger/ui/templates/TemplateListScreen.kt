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

package net.ktnx.mobileledger.ui.templates

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import net.ktnx.mobileledger.R
import net.ktnx.mobileledger.ui.components.LoadingIndicator
import net.ktnx.mobileledger.ui.theme.MoLeTheme

/**
 * Template list screen using Jetpack Compose.
 * Displays a list of templates with long-press context menu for delete/duplicate.
 */
@Composable
fun TemplateListScreen(viewModel: TemplateListViewModelCompose = hiltViewModel(), onNavigateToDetail: (Long?) -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is TemplateListEffect.NavigateToDetail -> {
                    onNavigateToDetail(effect.templateId)
                }

                is TemplateListEffect.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = effect.message,
                        duration = SnackbarDuration.Long
                    )
                }

                is TemplateListEffect.ShowUndoSnackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "「${effect.templateName}」を削除しました",
                        actionLabel = "元に戻す",
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onEvent(TemplateListEvent.UndoDelete(effect.templateId))
                    }
                }
            }
        }
    }

    TemplateListContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent
    )
}

@Composable
internal fun TemplateListContent(
    uiState: TemplateListUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (TemplateListEvent) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf<TemplateListItem?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onEvent(TemplateListEvent.CreateNewTemplate) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_button),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingIndicator(modifier = Modifier.align(Alignment.Center))
                }

                uiState.error != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.error,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                uiState.templates.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.no_templates),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "右下の＋ボタンでテンプレートを追加できます",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = uiState.templates,
                            key = { it.id }
                        ) { template ->
                            TemplateListItemCard(
                                template = template,
                                onClick = { onEvent(TemplateListEvent.EditTemplate(template.id)) },
                                onDeleteClick = { showDeleteDialog = template },
                                onDuplicateClick = { onEvent(TemplateListEvent.DuplicateTemplate(template.id)) }
                            )
                            HorizontalDivider()
                        }

                        // Bottom spacing for FAB
                        item {
                            Spacer(modifier = Modifier.height(88.dp))
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    showDeleteDialog?.let { template ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(template.name.ifEmpty { "テンプレート" }) },
            text = { Text(stringResource(R.string.remove_template_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEvent(TemplateListEvent.DeleteTemplate(template.id))
                        showDeleteDialog = null
                    }
                ) {
                    Text(
                        text = stringResource(R.string.Remove),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TemplateListScreenPreview() {
    MoLeTheme {
        TemplateListContent(
            uiState = TemplateListUiState(
                templates = listOf(
                    TemplateListItem(
                        id = 1,
                        name = "クレジットカード決済",
                        pattern = "\\d{4}/\\d{2}/\\d{2}.*",
                        isFallback = false
                    ),
                    TemplateListItem(
                        id = 2,
                        name = "銀行振込",
                        pattern = "振込.*\\d+円",
                        isFallback = true
                    )
                ),
                isLoading = false
            ),
            snackbarHostState = SnackbarHostState(),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TemplateListScreenEmptyPreview() {
    MoLeTheme {
        TemplateListContent(
            uiState = TemplateListUiState(
                templates = emptyList(),
                isLoading = false
            ),
            snackbarHostState = SnackbarHostState(),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TemplateListScreenLoadingPreview() {
    MoLeTheme {
        TemplateListContent(
            uiState = TemplateListUiState(isLoading = true),
            snackbarHostState = SnackbarHostState(),
            onEvent = {}
        )
    }
}
