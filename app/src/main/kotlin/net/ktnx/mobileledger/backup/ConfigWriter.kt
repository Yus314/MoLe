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

package net.ktnx.mobileledger.backup

import android.content.Context
import android.net.Uri
import net.ktnx.mobileledger.utils.Misc
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class ConfigWriter @Throws(FileNotFoundException::class) constructor(
    context: Context,
    uri: Uri,
    onErrorListener: OnErrorListener?,
    private val onDoneListener: OnDoneListener?
) : ConfigIO(context, uri, onErrorListener) {

    private lateinit var w: RawConfigWriter

    override fun getStreamMode(): String = "w"

    override fun initStream() {
        w = RawConfigWriter(FileOutputStream(pfd!!.fileDescriptor))
    }

    @Throws(IOException::class)
    override fun processStream() {
        w.writeConfig()
        onDoneListener?.let { Misc.onMainThread { it.done() } }
    }

    abstract class OnDoneListener {
        abstract fun done()
    }
}
