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

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.ktnx.mobileledger.R
import net.ktnx.mobileledger.ui.theme.MoLeTheme
import net.ktnx.mobileledger.utils.Globals

@Composable
fun CrashReportDialog(crashReportText: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    var isReportVisible by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = {
            Text(
                text = stringResource(R.string.crash_dialog_title),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.crash_send_question),
                    style = MaterialTheme.typography.bodyMedium
                )

                AnimatedVisibility(
                    visible = isReportVisible,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.shapes.small
                            )
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        SelectionContainer {
                            Text(
                                text = crashReportText,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isReportVisible) {
                    TextButton(onClick = { isReportVisible = true }) {
                        Text(
                            text = stringResource(R.string.btn_show_report),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                TextButton(
                    onClick = {
                        sendCrashReportEmail(context, crashReportText)
                    }
                ) {
                    Text(
                        text = stringResource(R.string.btn_send_crash_report),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.btn_not_now))
            }
        }
    )
}

private fun sendCrashReportEmail(context: Context, crashReportText: String) {
    val email = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_EMAIL, arrayOf(Globals.developerEmail))
        putExtra(Intent.EXTRA_SUBJECT, "MoLe crash report")
        putExtra(Intent.EXTRA_TEXT, crashReportText)
        type = "message/rfc822"
    }
    context.startActivity(
        Intent.createChooser(
            email,
            context.getString(R.string.send_crash_via)
        )
    )
}

@Preview
@Composable
private fun CrashReportDialogPreview() {
    MoLeTheme {
        CrashReportDialog(
            crashReportText = """
                MoLe version: 1.0.0
                OS version: 14; API level 34

                java.lang.NullPointerException: Attempt to invoke virtual method on a null object reference
                    at com.example.app.MainActivity.onCreate(MainActivity.kt:42)
                    at android.app.Activity.performCreate(Activity.java:8051)
            """.trimIndent(),
            onDismiss = {}
        )
    }
}
