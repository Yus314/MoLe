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

import android.annotation.SuppressLint
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import net.ktnx.mobileledger.R

class TextViewClearHelper {
    private var hadText = false
    private var hasFocus = false
    private var view: EditText? = null
    private var textWatcher: TextWatcher? = null
    private var prevOnFocusChangeListener: View.OnFocusChangeListener? = null

    fun detachFromView() {
        val v = view ?: return
        v.removeTextChangedListener(textWatcher)
        prevOnFocusChangeListener = null
        textWatcher = null
        hasFocus = false
        hadText = false
        view = null
    }

    @SuppressLint("ClickableViewAccessibility")
    fun attachToTextView(view: EditText) {
        if (this.view != null) {
            detachFromView()
        }
        this.view = view
        textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                val hasText = s.isNotEmpty()

                if (hasFocus) {
                    if (hadText && !hasText) hideClearDrawable()
                    if (!hadText && hasText) showClearDrawable()
                }

                hadText = hasText
            }
        }
        view.addTextChangedListener(textWatcher)
        prevOnFocusChangeListener = view.onFocusChangeListener
        view.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                if (view.text.isNotEmpty()) {
                    showClearDrawable()
                }
            } else {
                hideClearDrawable()
            }

            this.hasFocus = hasFocus

            prevOnFocusChangeListener?.onFocusChange(v, hasFocus)
        }

        view.setOnTouchListener { _, event -> onTouchEvent(view, event) }
    }

    private fun hideClearDrawable() {
        view?.setCompoundDrawablesRelative(null, null, null, null)
    }

    private fun showClearDrawable() {
        view?.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_clear_accent_24dp, 0)
    }

    fun onTouchEvent(view: EditText, event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP && view.text.isNotEmpty()) {
            var clearClicked = false
            val x = event.x
            val vw = view.width
            // start, top, end, bottom (end == 2)
            val dwb = view.compoundDrawablesRelative[2]
            if (dwb != null) {
                val dw = dwb.bounds.width()
                if (view.layoutDirection == View.LAYOUT_DIRECTION_LTR) {
                    if (x > vw - dw) clearClicked = true
                } else {
                    if (x < vw - dw) clearClicked = true
                }
                if (clearClicked) {
                    view.setText("")
                    view.requestFocus()
                    return true
                }
            }
        }

        return false
    }
}
