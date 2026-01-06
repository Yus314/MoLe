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

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData

abstract class QRScanCapableFragment : Fragment() {
    protected val scanQrLauncher: ActivityResultLauncher<Void?> = QR.registerLauncher(this) { text ->
        onQrScanned(text)
    }

    protected abstract fun onQrScanned(text: String?)

    override fun onAttach(context: Context) {
        super.onAttach(context)
        qrScanTrigger.observe(this) {
            scanQrLauncher.launch(null)
        }
    }

    companion object {
        private val qrScanTrigger = MutableLiveData<Int>()

        @JvmStatic
        fun triggerQRScan() {
            qrScanTrigger.value = 1
        }
    }
}
