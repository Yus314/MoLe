/*
 * Copyright © 2019 Damyan Ivanov.
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
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import net.ktnx.mobileledger.R

class AutoCompleteTextViewWithClear : AppCompatAutoCompleteTextView {
    private var hadText = false

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        if (focused) {
            if (text.isNotEmpty()) {
                showClearDrawable()
            }
        } else {
            hideClearDrawable()
        }

        super.onFocusChanged(focused, direction, previouslyFocusedRect)
    }

    private fun hideClearDrawable() {
        setCompoundDrawablesRelative(null, null, null, null)
    }

    private fun showClearDrawable() {
        setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_clear_accent_24dp, 0)
    }

    override fun onTextChanged(text: CharSequence?, start: Int, lengthBefore: Int, lengthAfter: Int) {
        val hasText = (text?.length ?: 0) > 0

        if (hasFocus()) {
            if (hadText && !hasText) hideClearDrawable()
            if (!hadText && hasText) showClearDrawable()
        }

        hadText = hasText

        super.onTextChanged(text, start, lengthBefore, lengthAfter)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP && text.isNotEmpty()) {
            var clearClicked = false
            val x = event.x
            val vw = width
            // start, top, end, bottom (end == 2)
            val dwb = compoundDrawablesRelative[2]
            if (dwb != null) {
                val dw = dwb.bounds.width()
                if (layoutDirection == View.LAYOUT_DIRECTION_LTR) {
                    if (x > vw - dw) clearClicked = true
                } else {
                    if (x < vw - dw) clearClicked = true
                }
                if (clearClicked) {
                    setText("")
                    requestFocus()
                    performClick()
                    return true
                }
            }
        }

        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean = super.performClick()
}
