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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import net.ktnx.mobileledger.core.domain.repository.PreferencesRepository
import net.ktnx.mobileledger.core.ui.theme.MoLeTheme
import net.ktnx.mobileledger.core.ui.theme.hslToColor

private const val HUE_STEP_DEGREES = 5

@Composable
fun HueRing(selectedHue: Int, initialHue: Int, onHueSelected: (Int) -> Unit, modifier: Modifier = Modifier) {
    val rainbowColors = remember {
        listOf(
            hslToColor(0f, 0.6f, 0.5f), // red
            hslToColor(60f, 0.6f, 0.5f), // yellow
            hslToColor(120f, 0.6f, 0.5f), // green
            hslToColor(180f, 0.6f, 0.5f), // cyan
            hslToColor(240f, 0.6f, 0.5f), // blue
            hslToColor(300f, 0.6f, 0.5f), // magenta
            hslToColor(360f, 0.6f, 0.5f) // red again
        )
    }

    val currentColor = remember(selectedHue) {
        hslToColor(selectedHue.toFloat(), 0.6f, 0.5f)
    }

    val initialColor = remember(initialHue) {
        hslToColor(initialHue.toFloat(), 0.6f, 0.5f)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val adjustedOffset = offset - center
                    val distance = sqrt(
                        adjustedOffset.x * adjustedOffset.x +
                            adjustedOffset.y * adjustedOffset.y
                    )
                    val diameter = min(size.width, size.height)
                    val outerR = diameter / 2f
                    val bandWidth = diameter / 3.5f
                    val innerR = outerR - bandWidth
                    val centerR = (innerR - bandWidth * 0.1f) * 0.5f

                    if (distance < centerR) {
                        // Tap on center - check if top or bottom half
                        if (adjustedOffset.y < 0) {
                            // Top half - reset to initial
                            onHueSelected(normalizeHue(initialHue))
                        }
                    } else {
                        // Tap on ring - calculate hue
                        val angleRad = atan2(adjustedOffset.y, adjustedOffset.x)
                        var angleDeg = (angleRad * 180f / PI).toFloat()
                        if (angleDeg < 0) angleDeg += 360f
                        onHueSelected(normalizeHue(angleDeg.toInt()))
                    }
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val adjustedOffset = change.position - center
                    val angleRad = atan2(adjustedOffset.y, adjustedOffset.x)
                    var angleDeg = (angleRad * 180f / PI).toFloat()
                    if (angleDeg < 0) angleDeg += 360f
                    onHueSelected(normalizeHue(angleDeg.toInt()))
                }
            }
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val diameter = min(size.width, size.height)
        val outerR = diameter / 2f - 16f // padding for marker
        val bandWidth = diameter / 3.5f
        val ringR = outerR - bandWidth / 2f
        val innerR = outerR - bandWidth
        val centerR = innerR * 0.5f

        // Draw rainbow ring with sweep gradient
        drawCircle(
            brush = Brush.sweepGradient(
                colors = rainbowColors,
                center = center
            ),
            radius = ringR,
            center = center,
            style = Stroke(width = bandWidth)
        )

        // Draw center circle - top half (initial color)
        drawArc(
            color = initialColor,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(center.x - centerR, center.y - centerR),
            size = Size(centerR * 2, centerR * 2)
        )

        // Draw center circle - bottom half (current color)
        drawArc(
            color = currentColor,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(center.x - centerR, center.y - centerR),
            size = Size(centerR * 2, centerR * 2)
        )

        // Draw selection marker
        drawHueMarker(
            center = center,
            outerR = outerR,
            hueDegrees = selectedHue
        )
    }
}

private fun DrawScope.drawHueMarker(center: Offset, outerR: Float, hueDegrees: Int) {
    val markerStrokeWidth = 8f
    val markerOuterEdge = outerR + markerStrokeWidth

    // Convert hue to radians (0 degrees is at 3 o'clock position)
    val angleRad = hueDegrees * PI.toFloat() / 180f

    // Draw marker arc
    val arcRadius = markerOuterEdge - markerStrokeWidth / 2f
    val halfArcDegrees = HUE_STEP_DEGREES / 2f

    val startAngle = hueDegrees - halfArcDegrees
    val sweepAngle = HUE_STEP_DEGREES.toFloat()

    // Draw the arc marker
    drawArc(
        color = Color(0xA0000000),
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
        size = Size(arcRadius * 2, arcRadius * 2),
        style = Stroke(width = markerStrokeWidth, cap = StrokeCap.Round)
    )

    // Draw small indicator circle on the ring
    val indicatorR = 6f
    val indicatorDistance = outerR - indicatorR - 4f
    val indicatorX = center.x + indicatorDistance * cos(angleRad)
    val indicatorY = center.y + indicatorDistance * sin(angleRad)

    drawCircle(
        color = Color.White,
        radius = indicatorR,
        center = Offset(indicatorX, indicatorY)
    )

    drawCircle(
        color = Color(0xA0000000),
        radius = indicatorR,
        center = Offset(indicatorX, indicatorY),
        style = Stroke(width = 2f)
    )
}

private fun normalizeHue(hueDegrees: Int): Int {
    var hue = hueDegrees
    if (hue < 0) hue += 360
    if (hue >= 360) hue %= 360

    // Round to HUE_STEP_DEGREES
    val rem = hue % HUE_STEP_DEGREES
    hue = if (rem < HUE_STEP_DEGREES / 2) {
        hue - rem
    } else {
        hue + HUE_STEP_DEGREES - rem
    }

    if (hue >= 360) hue = 0
    return hue
}

@Preview(showBackground = true)
@Composable
private fun HueRingPreview() {
    MoLeTheme {
        HueRing(
            selectedHue = PreferencesRepository.DEFAULT_HUE_DEG,
            initialHue = PreferencesRepository.DEFAULT_HUE_DEG,
            onHueSelected = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HueRingSelectedPreview() {
    MoLeTheme {
        HueRing(
            selectedHue = 120,
            initialHue = PreferencesRepository.DEFAULT_HUE_DEG,
            onHueSelected = {}
        )
    }
}
