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

package net.ktnx.mobileledger.ui.activity

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.PrintWriter
import java.io.StringWriter
import logcat.LogPriority
import logcat.logcat

abstract class CrashReportingActivity : AppCompatActivity() {
    protected var crashReportText: String? by mutableStateOf(null)
        private set

    protected fun dismissCrashReport() {
        crashReportText = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            val sw = StringWriter()
            val pw = PrintWriter(sw)

            try {
                val pm = applicationContext.packageManager
                val pi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(packageName, 0)
                }
                pw.format("MoLe version: %s\n", pi.versionName)
            } catch (oh: Exception) {
                pw.print("Error getting package version:\n")
                oh.printStackTrace(pw)
                pw.print("\n")
            }
            pw.format(
                "OS version: %s; API level %d\n\n",
                Build.VERSION.RELEASE,
                Build.VERSION.SDK_INT
            )
            e.printStackTrace(pw)

            Log.e(null, sw.toString())

            crashReportText = sw.toString()
        }
        logcat { "Uncaught exception handler set" }
    }
}
