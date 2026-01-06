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

package net.ktnx.mobileledger.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import net.ktnx.mobileledger.ui.theme.MoLeTheme

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "OK",
    dismissText: String = "キャンセル",
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
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = {
            Text(
                text = "未保存の変更",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = "変更が保存されていません。保存しますか？",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text(
                    text = "保存",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDiscard) {
                Text(
                    text = "破棄",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    )
}

@Composable
fun DeleteConfirmDialog(itemName: String, onConfirm: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    ConfirmDialog(
        title = "削除の確認",
        message = "「$itemName」を削除しますか？この操作は取り消せません。",
        confirmText = "削除",
        dismissText = "キャンセル",
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
            title = "確認",
            message = "この操作を実行しますか？",
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
            itemName = "テストプロファイル",
            onConfirm = {},
            onDismiss = {}
        )
    }
}
