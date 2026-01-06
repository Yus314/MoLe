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

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt

internal class TemplateListDivider(
    context: Context,
    orientation: Int
) : DividerItemDecoration(context, orientation) {

    private val mBounds = Rect()
    private var mOrientation: Int = orientation

    override fun setOrientation(orientation: Int) {
        super.setOrientation(orientation)
        mOrientation = orientation
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (parent.layoutManager == null || drawable == null) {
            return
        }
        when (mOrientation) {
            VERTICAL -> drawVertical(c, parent)
            else -> drawHorizontal(c, parent)
        }
    }

    private fun drawVertical(canvas: Canvas, parent: RecyclerView) {
        canvas.save()
        val left: Int
        val right: Int
        // NewApi lint fails to handle overrides.
        if (parent.clipToPadding) {
            left = parent.paddingLeft
            right = parent.width - parent.paddingRight
            canvas.clipRect(
                left,
                parent.paddingTop,
                right,
                parent.height - parent.paddingBottom
            )
        } else {
            left = 0
            right = parent.width
        }

        val divider = checkNotNull(drawable)
        val childCount = parent.childCount
        val adapter = checkNotNull(parent.adapter) as TemplatesRecyclerViewAdapter
        val itemCount = adapter.itemCount

        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            val childAdapterPosition = parent.getChildAdapterPosition(child)
            if (shouldSkipDivider(adapter, childAdapterPosition, itemCount)) {
                continue
            }
            parent.getDecoratedBoundsWithMargins(child, mBounds)
            val bottom = mBounds.bottom + child.translationY.roundToInt()
            val top = bottom - divider.intrinsicHeight
            divider.setBounds(left, top, right, bottom)
            divider.draw(canvas)
        }
        canvas.restore()
    }

    private fun drawHorizontal(canvas: Canvas, parent: RecyclerView) {
        val layoutManager = parent.layoutManager ?: return

        canvas.save()
        val top: Int
        val bottom: Int
        // NewApi lint fails to handle overrides.
        if (parent.clipToPadding) {
            top = parent.paddingTop
            bottom = parent.height - parent.paddingBottom
            canvas.clipRect(
                parent.paddingLeft,
                top,
                parent.width - parent.paddingRight,
                bottom
            )
        } else {
            top = 0
            bottom = parent.height
        }

        val divider = checkNotNull(drawable)
        val childCount = parent.childCount
        val adapter = checkNotNull(parent.adapter) as TemplatesRecyclerViewAdapter
        val itemCount = adapter.itemCount

        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            val childAdapterPosition = parent.getChildAdapterPosition(child)
            if (shouldSkipDivider(adapter, childAdapterPosition, itemCount)) {
                continue
            }
            layoutManager.getDecoratedBoundsWithMargins(child, mBounds)
            val right = mBounds.right + child.translationX.roundToInt()
            val left = right - divider.intrinsicWidth
            divider.setBounds(left, top, right, bottom)
            divider.draw(canvas)
        }
        canvas.restore()
    }

    override fun getItemOffsets(
        outRect: Rect,
        child: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val childAdapterPosition = parent.getChildAdapterPosition(child)
        val adapter = checkNotNull(parent.adapter) as TemplatesRecyclerViewAdapter
        val itemCount = adapter.itemCount

        if (shouldSkipDivider(adapter, childAdapterPosition, itemCount)) {
            return
        }

        super.getItemOffsets(outRect, child, parent, state)
    }

    private fun shouldSkipDivider(
        adapter: TemplatesRecyclerViewAdapter,
        childAdapterPosition: Int,
        itemCount: Int
    ): Boolean = when {
            childAdapterPosition == RecyclerView.NO_POSITION -> true

            adapter.getItemViewType(childAdapterPosition) ==
                TemplatesRecyclerViewAdapter.ITEM_TYPE_DIVIDER -> true

            childAdapterPosition + 1 < itemCount &&
                adapter.getItemViewType(childAdapterPosition + 1) ==
                TemplatesRecyclerViewAdapter.ITEM_TYPE_DIVIDER -> true

            else -> false
        }
}
