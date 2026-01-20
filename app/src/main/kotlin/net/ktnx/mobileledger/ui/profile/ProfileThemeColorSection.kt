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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import net.ktnx.mobileledger.ui.components.HueRing
import net.ktnx.mobileledger.ui.theme.hslToColor

@Composable
internal fun ProfileThemeColorSection(
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
