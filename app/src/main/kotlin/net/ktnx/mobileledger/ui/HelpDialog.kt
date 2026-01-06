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

import android.app.AlertDialog
import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.widget.TextView
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import net.ktnx.mobileledger.R
import java.util.regex.Pattern

object HelpDialog {
    private val MARKDOWN_LINK_PATTERN: Pattern = Pattern.compile("\\[([^\\[]+)]\\(([^)]*)\\)")

    @JvmStatic
    fun show(context: Context, @StringRes title: Int, @ArrayRes content: Int) {
        val adb = AlertDialog.Builder(context)
        adb.setTitle(title)
        var message = TextUtils.join("\n\n", context.resources.getStringArray(content))

        val richTextMessage = SpannableStringBuilder()
        while (true) {
            val m = MARKDOWN_LINK_PATTERN.matcher(message)
            if (m.find()) {
                richTextMessage.append(message.substring(0, m.start()))
                // Groups 1 and 2 are guaranteed to exist when find() returns true
                var linkText = m.group(1) ?: ""
                val linkURL = m.group(2) ?: ""

                if (linkText.isEmpty()) {
                    linkText = linkURL
                }

                val spanStart = richTextMessage.length
                richTextMessage.append(linkText)
                richTextMessage.setSpan(
                    URLSpan(linkURL),
                    spanStart,
                    spanStart + linkText.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                message = message.substring(m.end())
            } else {
                richTextMessage.append(message)
                break
            }
        }
        adb.setMessage(richTextMessage)
        adb.setPositiveButton(R.string.close_button) { dialog, _ -> dialog.dismiss() }
        val dialog = adb.create()
        dialog.show()
        (dialog.findViewById<TextView>(android.R.id.message))?.movementMethod =
            LinkMovementMethod.getInstance()
    }
}
