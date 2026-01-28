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

package net.ktnx.mobileledger.core.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import net.ktnx.mobileledger.core.ui.theme.MoLeTheme

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "OK",
    dismissText: String = "Cancel",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmText,
                    color = if (isDestructive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = dismissText)
            }
        }
    )
}

@Composable
fun UnsavedChangesDialog(
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Unsaved Changes",
    message: String = "You have unsaved changes. Do you want to save them?",
    saveText: String = "Save",
    discardText: String = "Discard"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text(
                    text = saveText,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDiscard) {
                Text(
                    text = discardText,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    )
}

@Composable
fun DeleteConfirmDialog(
    itemName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Confirm Delete",
    messageTemplate: String = "Delete \"%s\"? This action cannot be undone.",
    confirmText: String = "Delete",
    dismissText: String = "Cancel"
) {
    ConfirmDialog(
        title = title,
        message = messageTemplate.format(itemName),
        confirmText = confirmText,
        dismissText = dismissText,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        modifier = modifier,
        isDestructive = true
    )
}

@Preview
@Composable
private fun ConfirmDialogPreview() {
    MoLeTheme {
        ConfirmDialog(
            title = "Confirm",
            message = "Do you want to proceed with this action?",
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@Preview
@Composable
private fun UnsavedChangesDialogPreview() {
    MoLeTheme {
        UnsavedChangesDialog(
            onSave = {},
            onDiscard = {},
            onDismiss = {}
        )
    }
}

@Preview
@Composable
private fun DeleteConfirmDialogPreview() {
    MoLeTheme {
        DeleteConfirmDialog(
            itemName = "Test Profile",
            onConfirm = {},
            onDismiss = {}
        )
    }
}
