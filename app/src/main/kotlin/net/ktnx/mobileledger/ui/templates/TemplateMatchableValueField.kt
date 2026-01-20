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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

@Composable
internal fun TemplateMatchableValueField(
    label: String,
    value: MatchableValue,
    patternGroupCount: Int,
    onValueChanged: (MatchableValue) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))

            // Toggle between literal and match group
            TextButton(
                onClick = {
                    if (value.isLiteral()) {
                        onValueChanged(MatchableValue.MatchGroup(1))
                    } else {
                        onValueChanged(MatchableValue.Literal(""))
                    }
                }
            ) {
                Text(
                    text = if (value.isLiteral()) "グループ" else "リテラル",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        when (value) {
            is MatchableValue.Literal -> {
                OutlinedTextField(
                    value = value.value,
                    onValueChange = { onValueChanged(MatchableValue.Literal(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = keyboardType,
                        imeAction = ImeAction.Next
                    )
                )
            }

            is MatchableValue.MatchGroup -> {
                OutlinedTextField(
                    value = value.group.toString(),
                    onValueChange = { text ->
                        val group = text.toIntOrNull() ?: 1
                        onValueChanged(MatchableValue.MatchGroup(group.coerceIn(1, patternGroupCount.coerceAtLeast(1))))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    label = { Text("グループ番号") },
                    isError = value.group > patternGroupCount && patternGroupCount > 0,
                    supportingText = if (value.group > patternGroupCount && patternGroupCount > 0) {
                        { Text("グループ番号が範囲外です (1-$patternGroupCount)") }
                    } else {
                        null
                    }
                )
            }
        }
    }
}
