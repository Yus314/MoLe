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
import java.io.PrintWriter
import java.io.StringWriter
import net.ktnx.mobileledger.ui.CrashReportDialogFragment
import net.ktnx.mobileledger.utils.Logger.debug

abstract class CrashReportingActivity : AppCompatActivity() {
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

            val df = CrashReportDialogFragment()
            df.setCrashReportText(sw.toString())
            df.show(supportFragmentManager, "crash_report")
        }
        debug("crash", "Uncaught exception handler set")
    }
}
