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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.launch

/**
 * Weak overscroll effect that applies a subtle stretch when scrolling past bounds.
 * The stretch factor controls how much the content stretches (0.0 = no stretch, 1.0 = full stretch).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WeakOverscrollContainer(
    modifier: Modifier = Modifier,
    stretchFactor: Float = 0.3f,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val overscrollOffset = remember { Animatable(0f) }

    val nestedScrollConnection = remember(stretchFactor) {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                // available.y > 0 means scrolling down past top
                // available.y < 0 means scrolling up past bottom
                if (available.y != 0f) {
                    scope.launch {
                        val newOffset = (overscrollOffset.value + available.y * stretchFactor)
                            .coerceIn(-200f, 200f)
                        overscrollOffset.snapTo(newOffset)
                    }
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                // Animate back to 0 when fling ends
                overscrollOffset.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = 0.6f,
                        stiffness = 400f
                    )
                )
                return Velocity.Zero
            }
        }
    }

    CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
        androidx.compose.foundation.layout.Box(
            modifier = modifier
                .nestedScroll(nestedScrollConnection)
                .graphicsLayer {
                    translationY = overscrollOffset.value * 0.5f
                }
        ) {
            content()
        }
    }
}
