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
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener

class RecyclerItemListener(
    ctx: Context,
    rv: RecyclerView,
    listener: RecyclerTouchListener
) : OnItemTouchListener {

    private val gd: GestureDetector = GestureDetector(ctx, object : GestureDetector.SimpleOnGestureListener() {
        override fun onLongPress(e: MotionEvent) {
            val v = rv.findChildViewUnder(e.x, e.y)
            listener.onLongClickItem(v, rv.getChildAdapterPosition(v!!))
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val v = rv.findChildViewUnder(e.x, e.y)
            listener.onClickItem(v, rv.getChildAdapterPosition(v!!))
            return true
        }
    })

    override fun onInterceptTouchEvent(recyclerView: RecyclerView, motionEvent: MotionEvent): Boolean {
        val v = recyclerView.findChildViewUnder(motionEvent.x, motionEvent.y)
        return v != null && gd.onTouchEvent(motionEvent)
    }

    override fun onTouchEvent(recyclerView: RecyclerView, motionEvent: MotionEvent) {
    }

    override fun onRequestDisallowInterceptTouchEvent(b: Boolean) {
    }

    interface RecyclerTouchListener {
        fun onClickItem(v: View?, position: Int)
        fun onLongClickItem(v: View?, position: Int)
    }
}
