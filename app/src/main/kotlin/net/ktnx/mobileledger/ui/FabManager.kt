/*
 * Copyright © 2021 Damyan Ivanov.
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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.TimeInterpolator
import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.animation.AnimationUtils
import com.google.android.material.floatingactionbutton.FloatingActionButton
import net.ktnx.mobileledger.utils.DimensionUtils
import timber.log.Timber

class FabManager(private val fab: FloatingActionButton) {
    private var wantedFabState = FAB_SHOWN
    private var fabSlideAnimator: ViewPropertyAnimator? = null
    private var fabVerticalOffset = 0

    private fun slideFabTo(target: Int, duration: Long, interpolator: TimeInterpolator) {
        fabSlideAnimator = fab.animate()
            .translationY(target.toFloat())
            .setInterpolator(interpolator)
            .setDuration(duration)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    fabSlideAnimator = null
                }
            })
    }

    fun showFab() {
        if (wantedFabState == FAB_SHOWN) {
            return
        }

        fabSlideAnimator?.let {
            it.cancel()
            fab.clearAnimation()
        }

        Timber.d("Showing FAB")
        wantedFabState = FAB_SHOWN
        slideFabTo(0, 200L, AnimationUtils.LINEAR_OUT_SLOW_IN_INTERPOLATOR)
    }

    fun hideFab() {
        if (wantedFabState == FAB_HIDDEN) {
            return
        }

        calcVerticalFabOffset()

        fabSlideAnimator?.let {
            it.cancel()
            fab.clearAnimation()
        }

        Timber.d("Hiding FAB")
        wantedFabState = FAB_HIDDEN
        slideFabTo(fabVerticalOffset, 150L, AnimationUtils.FAST_OUT_LINEAR_IN_INTERPOLATOR)
    }

    private fun calcVerticalFabOffset() {
        if (fabVerticalOffset > 0) return // already calculated
        fab.measure(0, 0)

        val height = fab.measuredHeight

        val bottomMargin: Int

        val layoutParams = fab.layoutParams
        bottomMargin = if (layoutParams is ViewGroup.MarginLayoutParams) {
            layoutParams.bottomMargin
        } else {
            throw RuntimeException("Unsupported layout params " + layoutParams.javaClass.canonicalName)
        }

        fabVerticalOffset = height + bottomMargin
    }

    interface FabHandler {
        fun getContext(): Context
        fun showManagedFab()
        fun hideManagedFab()
    }

    class ScrollFabHandler
    @SuppressLint("ClickableViewAccessibility")
    constructor(
        private val fabHandler: FabHandler,
        recyclerView: RecyclerView
    ) {
        private var generation = 0

        init {
            val triggerAbsolutePixels = DimensionUtils.dp2px(fabHandler.getContext(), 20f).toFloat()
            recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy <= 0) {
                        showFab()
                    } else {
                        hideFab()
                    }
                    super.onScrolled(recyclerView, dx, dy)
                }
            })
            recyclerView.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
                private var absoluteAnchor = -1f

                override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                    when (e.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            absoluteAnchor = e.rawY
                        }

                        MotionEvent.ACTION_MOVE -> {
                            if (absoluteAnchor < 0) return false

                            val absoluteY = e.rawY

                            if (absoluteY > absoluteAnchor + triggerAbsolutePixels) {
                                // swipe down
                                showFab()
                                absoluteAnchor = absoluteY
                            } else if (absoluteY < absoluteAnchor - triggerAbsolutePixels) {
                                // swipe up
                                hideFab()
                                absoluteAnchor = absoluteY
                            }
                        }
                    }
                    return false
                }
            })
        }

        private fun hideFab() {
            generation++
            val thisGeneration = generation
            fabHandler.hideManagedFab()
            Handler(Looper.getMainLooper()).postDelayed({
                if (generation != thisGeneration) return@postDelayed
                showFab()
            }, AUTO_SHOW_DELAY_MILLS.toLong())
        }

        private fun showFab() {
            generation++
            fabHandler.showManagedFab()
        }
    }

    companion object {
        private const val FAB_SHOWN = true
        private const val FAB_HIDDEN = false
        private const val AUTO_SHOW_DELAY_MILLS = 4000

        @JvmStatic
        fun handle(fabHandler: FabHandler, recyclerView: RecyclerView) {
            ScrollFabHandler(fabHandler, recyclerView)
        }
    }
}
