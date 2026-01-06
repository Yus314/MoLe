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

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import net.ktnx.mobileledger.R
import net.ktnx.mobileledger.utils.Colors

class HueRingDialog(
    context: Context,
    private val initialHue: Int,
    private val currentHue: Int
) : Dialog(context) {

    private lateinit var hueRing: HueRing
    private var listener: HueSelectedListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.hue_dialog)
        hueRing = findViewById(R.id.ring)
        hueRing.setInitialHue(initialHue)
        hueRing.setHue(currentHue)

        findViewById<android.view.View>(R.id.btn_ok).setOnClickListener {
            listener?.onHueSelected(hueRing.hueDegrees)
            dismiss()
        }

        findViewById<android.view.View>(R.id.btn_default).setOnClickListener {
            hueRing.setHue(Colors.DEFAULT_HUE_DEG)
        }
    }

    fun setColorSelectedListener(listener: HueSelectedListener?) {
        this.listener = listener
    }

    fun interface HueSelectedListener {
        fun onHueSelected(hue: Int)
    }
}
