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

@file:Suppress("UNUSED", "MatchingDeclarationName")

package net.ktnx.mobileledger.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Re-export from core:ui for backward compatibility
// New code should import from net.ktnx.mobileledger.core.ui.components directly

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
) = net.ktnx.mobileledger.core.ui.components.ConfirmDialog(
    title = title,
    message = message,
    confirmText = confirmText,
    dismissText = dismissText,
    onConfirm = onConfirm,
    onDismiss = onDismiss,
    modifier = modifier,
    isDestructive = isDestructive
)

@Composable
fun UnsavedChangesDialog(
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) = net.ktnx.mobileledger.core.ui.components.UnsavedChangesDialog(
    onSave = onSave,
    onDiscard = onDiscard,
    onDismiss = onDismiss,
    modifier = modifier,
    title = "未保存の変更",
    message = "変更が保存されていません。保存しますか？",
    saveText = "保存",
    discardText = "破棄"
)

@Composable
fun DeleteConfirmDialog(itemName: String, onConfirm: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) =
    net.ktnx.mobileledger.core.ui.components.DeleteConfirmDialog(
        itemName = itemName,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        modifier = modifier,
        title = "削除の確認",
        messageTemplate = "「%s」を削除しますか？この操作は取り消せません。",
        confirmText = "削除",
        dismissText = "キャンセル"
    )
