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

package net.ktnx.mobileledger.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import net.ktnx.mobileledger.core.ui.components.ConfirmDialog
import net.ktnx.mobileledger.core.ui.components.DeleteConfirmDialog
import net.ktnx.mobileledger.core.ui.components.UnsavedChangesDialog
import net.ktnx.mobileledger.ui.theme.MoLeTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric tests for ConfirmDialog, UnsavedChangesDialog, and DeleteConfirmDialog.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ConfirmDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ========================================
    // ConfirmDialog tests
    // ========================================

    @Test
    fun `ConfirmDialog displays title and message`() {
        composeTestRule.setContent {
            MoLeTheme {
                ConfirmDialog(
                    title = "テストタイトル",
                    message = "テストメッセージ",
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("テストタイトル").assertIsDisplayed()
        composeTestRule.onNodeWithText("テストメッセージ").assertIsDisplayed()
    }

    @Test
    fun `ConfirmDialog displays default button text`() {
        composeTestRule.setContent {
            MoLeTheme {
                ConfirmDialog(
                    title = "タイトル",
                    message = "メッセージ",
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("OK").assertIsDisplayed()
        composeTestRule.onNodeWithText("キャンセル").assertIsDisplayed()
    }

    @Test
    fun `ConfirmDialog displays custom button text`() {
        composeTestRule.setContent {
            MoLeTheme {
                ConfirmDialog(
                    title = "タイトル",
                    message = "メッセージ",
                    confirmText = "はい",
                    dismissText = "いいえ",
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("はい").assertIsDisplayed()
        composeTestRule.onNodeWithText("いいえ").assertIsDisplayed()
    }

    @Test
    fun `ConfirmDialog confirm button triggers callback`() {
        var confirmClicked = false

        composeTestRule.setContent {
            MoLeTheme {
                ConfirmDialog(
                    title = "タイトル",
                    message = "メッセージ",
                    onConfirm = { confirmClicked = true },
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("OK").performClick()
        assertTrue("Confirm callback should be triggered", confirmClicked)
    }

    @Test
    fun `ConfirmDialog dismiss button triggers callback`() {
        var dismissClicked = false

        composeTestRule.setContent {
            MoLeTheme {
                ConfirmDialog(
                    title = "タイトル",
                    message = "メッセージ",
                    onConfirm = {},
                    onDismiss = { dismissClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("キャンセル").performClick()
        assertTrue("Dismiss callback should be triggered", dismissClicked)
    }

    // ========================================
    // UnsavedChangesDialog tests
    // ========================================

    @Test
    fun `UnsavedChangesDialog displays title and message`() {
        composeTestRule.setContent {
            MoLeTheme {
                UnsavedChangesDialog(
                    onSave = {},
                    onDiscard = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("未保存の変更").assertIsDisplayed()
        composeTestRule.onNodeWithText("変更が保存されていません。保存しますか？").assertIsDisplayed()
    }

    @Test
    fun `UnsavedChangesDialog displays save and discard buttons`() {
        composeTestRule.setContent {
            MoLeTheme {
                UnsavedChangesDialog(
                    onSave = {},
                    onDiscard = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("保存").assertIsDisplayed()
        composeTestRule.onNodeWithText("破棄").assertIsDisplayed()
    }

    @Test
    fun `UnsavedChangesDialog save button triggers callback`() {
        var saveClicked = false

        composeTestRule.setContent {
            MoLeTheme {
                UnsavedChangesDialog(
                    onSave = { saveClicked = true },
                    onDiscard = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("保存").performClick()
        assertTrue("Save callback should be triggered", saveClicked)
    }

    @Test
    fun `UnsavedChangesDialog discard button triggers callback`() {
        var discardClicked = false

        composeTestRule.setContent {
            MoLeTheme {
                UnsavedChangesDialog(
                    onSave = {},
                    onDiscard = { discardClicked = true },
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("破棄").performClick()
        assertTrue("Discard callback should be triggered", discardClicked)
    }

    // ========================================
    // DeleteConfirmDialog tests
    // ========================================

    @Test
    fun `DeleteConfirmDialog displays title`() {
        composeTestRule.setContent {
            MoLeTheme {
                DeleteConfirmDialog(
                    itemName = "テストアイテム",
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("削除の確認").assertIsDisplayed()
    }

    @Test
    fun `DeleteConfirmDialog includes item name in message`() {
        composeTestRule.setContent {
            MoLeTheme {
                DeleteConfirmDialog(
                    itemName = "テストプロファイル",
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("「テストプロファイル」を削除しますか？この操作は取り消せません。")
            .assertIsDisplayed()
    }

    @Test
    fun `DeleteConfirmDialog displays delete and cancel buttons`() {
        composeTestRule.setContent {
            MoLeTheme {
                DeleteConfirmDialog(
                    itemName = "アイテム",
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("削除").assertIsDisplayed()
        composeTestRule.onNodeWithText("キャンセル").assertIsDisplayed()
    }

    @Test
    fun `DeleteConfirmDialog delete button triggers callback`() {
        var deleteClicked = false

        composeTestRule.setContent {
            MoLeTheme {
                DeleteConfirmDialog(
                    itemName = "アイテム",
                    onConfirm = { deleteClicked = true },
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("削除").performClick()
        assertTrue("Delete callback should be triggered", deleteClicked)
    }

    @Test
    fun `DeleteConfirmDialog cancel button triggers callback`() {
        var cancelClicked = false

        composeTestRule.setContent {
            MoLeTheme {
                DeleteConfirmDialog(
                    itemName = "アイテム",
                    onConfirm = {},
                    onDismiss = { cancelClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("キャンセル").performClick()
        assertTrue("Cancel callback should be triggered", cancelClicked)
    }
}
