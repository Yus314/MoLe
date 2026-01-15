/*
 * Copyright © 2020 Damyan Ivanov.
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

package net.ktnx.mobileledger.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import logcat.logcat
import net.ktnx.mobileledger.utils.Colors
import net.ktnx.mobileledger.utils.DimensionUtils

class HueRing : View {
    private lateinit var ringPaint: Paint
    private lateinit var initialPaint: Paint
    private lateinit var currentPaint: Paint
    private lateinit var markerPaint: Paint
    private var center = 0
    private var padding = 0
    private var initialHueDegrees = 0
    private var _color = 0
    private var _hueDegrees = 0
    private var outerR = 0f
    private var innerR = 0f
    private var bandWidth = 0f
    private var centerR = 0f
    private val centerRect = RectF()
    private val ringRect = RectF()

    @Suppress("unused")
    private var markerOverflow = 0
    private var markerStrokeWidth = 0

    val color: Int
        get() = _color

    val hueDegrees: Int
        get() = _hueDegrees

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(Colors.DEFAULT_HUE_DEG)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(Colors.DEFAULT_HUE_DEG)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) :
        super(context, attrs, defStyleAttr, defStyleRes) {
        init(Colors.DEFAULT_HUE_DEG)
    }

    constructor(context: Context) : super(context) {
        init(Colors.DEFAULT_HUE_DEG)
    }

    constructor(context: Context, initialHueDegrees: Int) : super(context) {
        init(initialHueDegrees)
    }

    private fun init(initialHueDegrees: Int) {
        val steps = intArrayOf(
            Colors.getPrimaryColorForHue(0), // red
            Colors.getPrimaryColorForHue(60), // yellow
            Colors.getPrimaryColorForHue(120), // green
            Colors.getPrimaryColorForHue(180), // cyan
            Colors.getPrimaryColorForHue(240), // blue
            Colors.getPrimaryColorForHue(300), // magenta
            Colors.getPrimaryColorForHue(360) // red, again
        )
        val rainbow = SweepGradient(0f, 0f, steps, null)

        ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = rainbow
            style = Paint.Style.STROKE
        }

        initialPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        currentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        setInitialHue(initialHueDegrees)
        setHue(initialHueDegrees)

        markerStrokeWidth = DimensionUtils.dp2px(context, 4f)

        padding = markerStrokeWidth * 2 + 2

        markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = 0xa0000000.toInt()
            strokeWidth = markerStrokeWidth.toFloat()
        }
    }

    fun setHue(hueDegrees: Int) {
        var adjustedHue = hueDegrees
        if (adjustedHue == -1) {
            adjustedHue = Colors.DEFAULT_HUE_DEG
        }

        if (adjustedHue != Colors.DEFAULT_HUE_DEG) {
            // round to hueStepDegrees
            val rem = adjustedHue % hueStepDegrees
            adjustedHue = if (rem < hueStepDegrees / 2) {
                adjustedHue - rem
            } else {
                adjustedHue + hueStepDegrees - rem
            }
        }

        this._hueDegrees = adjustedHue
        this._color = Colors.getPrimaryColorForHue(adjustedHue)
        currentPaint.color = this._color
        invalidate()
    }

    private fun setHue(hue: Float) {
        setHue((360f * hue).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val center = width / 2f
        ringPaint.strokeWidth = bandWidth.toInt().toFloat()

        canvas.save()
        canvas.translate(center, center)
        canvas.drawOval(ringRect, ringPaint)

        canvas.drawArc(centerRect, 180f, 180f, true, initialPaint)
        canvas.drawArc(centerRect, 0f, 180f, true, currentPaint)

        canvas.restore()
        drawMarker(canvas, center)
    }

    private fun drawMarker(canvas: Canvas, center: Float) {
        val leftRadians = Math.toRadians((-hueStepDegrees / 2f).toDouble()).toFloat()
        val rightRadians = Math.toRadians((hueStepDegrees / 2f).toDouble()).toFloat()

        @Suppress("UNUSED_VARIABLE")
        val sl = sin(leftRadians)

        @Suppress("UNUSED_VARIABLE")
        val sr = sin(rightRadians)

        @Suppress("UNUSED_VARIABLE")
        val cl = cos(leftRadians)

        @Suppress("UNUSED_VARIABLE")
        val cr = cos(rightRadians)

        @Suppress("UNUSED_VARIABLE")
        val innerEdge = innerR - 1.5f * markerStrokeWidth
        val outerEdge = outerR + 1.5f + markerStrokeWidth
        val p = Path()
        p.arcTo(
            -outerEdge,
            -outerEdge,
            outerEdge,
            outerEdge,
            hueStepDegrees / 2f,
            (-hueStepDegrees).toFloat(),
            false
        )
        canvas.save()
        canvas.translate(center, center)
        canvas.rotate(_hueDegrees.toFloat(), 0f, 0f)
        canvas.drawPath(p, markerPaint)
        canvas.restore()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        var diameter: Int
        if (widthMode == MeasureSpec.AT_MOST && heightMode == MeasureSpec.AT_MOST) {
            diameter = min(widthSize, heightSize)
        } else {
            setMeasuredDimension(MEASURED_STATE_TOO_SMALL, MEASURED_STATE_TOO_SMALL)
            return
        }

        setMeasuredDimension(diameter, diameter)

        diameter -= 2 * padding
        outerR = diameter / 2f
        center = padding + outerR.toInt()

        bandWidth = diameter / 3.5f
        val ringR = outerR - bandWidth / 2f
        innerR = outerR - bandWidth

        ringRect.set(-ringR, -ringR, ringR, ringR)

        val innerDiameter = diameter - 2 * bandWidth
        centerR = innerDiameter * 0.5f
        centerRect.set(-centerR, -centerR, centerR, centerR)
    }

    override fun performClick(): Boolean = super.performClick()

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val x = event.x - center
                val y = event.y - center

                val dist = hypot(x.toDouble(), y.toDouble()).toFloat()

                if (dist < centerR) {
                    if (y < 0) {
                        setHue(initialHueDegrees)
                    }
                } else {
                    val angleRad = atan2(y.toDouble(), x.toDouble()).toFloat()
                    // angleRad is [-𝜋; +𝜋]
                    var hue = (angleRad / (2 * PI)).toFloat()
                    if (hue < 0) {
                        hue += 1
                    }
                    logcat {
                        "x=${"%.3f".format(
                            x
                        )}, y=${"%.3f".format(
                            y
                        )}, angle=${"%.3f".format(angleRad)} rad, hueDegrees=${"%.3f".format(hue)}"
                    }
                    setHue(hue)
                }
            }

            MotionEvent.ACTION_UP -> {
                performClick()
            }
        }
        return true
    }

    fun setInitialHue(initialHue: Int) {
        var hue = initialHue
        if (hue == -1) {
            hue = Colors.DEFAULT_HUE_DEG
        }
        this.initialHueDegrees = hue
        this.initialPaint.color = Colors.getPrimaryColorForHue(hue)
        invalidate()
    }

    companion object {
        @JvmField
        val hueStepDegrees = 5
    }
}
