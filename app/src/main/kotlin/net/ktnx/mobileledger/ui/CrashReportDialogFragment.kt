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

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import net.ktnx.mobileledger.R
import net.ktnx.mobileledger.utils.Globals

class CrashReportDialogFragment : DialogFragment() {
    private var mCrashReportText: String? = null
    private var repScroll: ScrollView? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)
        val inflater = requireActivity().layoutInflater

        if (savedInstanceState != null) {
            mCrashReportText = savedInstanceState.getString("crash_text")
        }

        val view = inflater.inflate(R.layout.crash_dialog, null)
        view.findViewById<TextView>(R.id.textCrashReport).text = mCrashReportText
        repScroll = view.findViewById(R.id.scrollText)

        builder.setTitle(R.string.crash_dialog_title)
            .setView(view)
            .setPositiveButton(R.string.btn_send_crash_report) { _, _ ->
                val email = Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(Globals.developerEmail))
                    putExtra(Intent.EXTRA_SUBJECT, "MoLe crash report")
                    putExtra(Intent.EXTRA_TEXT, mCrashReportText)
                    type = "message/rfc822"
                }
                startActivity(Intent.createChooser(email,
                    resources.getString(R.string.send_crash_via)))
            }
            .setNegativeButton(R.string.btn_not_now) { _, _ ->
                dialog?.cancel()
            }
            .setNeutralButton(R.string.btn_show_report) { _, _ -> }

        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener { v ->
                repScroll?.let { scroll ->
                    scroll.visibility = View.VISIBLE
                    v.visibility = View.GONE
                }
            }
        }
        return dialog
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("crash_text", mCrashReportText)
    }

    fun setCrashReportText(text: String) {
        mCrashReportText = text
    }
}
