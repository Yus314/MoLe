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

package net.ktnx.mobileledger.ui.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import net.ktnx.mobileledger.R
import net.ktnx.mobileledger.db.DB
import net.ktnx.mobileledger.utils.Logger
import java.util.Locale

class SplashActivity : CrashReportingActivity() {
    private var startupTime: Long = 0
    private var running = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme_default)
        setContentView(R.layout.splash_activity_layout)
        Logger.debug("splash", "onCreate()")

        DB.initComplete.setValue(false)
        DB.initComplete.observe(this) { done -> onDbInitDoneChanged(done) }
    }

    override fun onStart() {
        super.onStart()
        Logger.debug("splash", "onStart()")
        running = true

        startupTime = System.currentTimeMillis()

        val dbInitThread = DatabaseInitThread()
        Logger.debug("splash", "starting dbInit task")
        dbInitThread.start()
    }

    override fun onPause() {
        super.onPause()
        Logger.debug("splash", "onPause()")
        running = false
    }

    override fun onResume() {
        super.onResume()
        Logger.debug("splash", "onResume()")
        running = true
    }

    private fun onDbInitDoneChanged(done: Boolean) {
        if (!done) {
            Logger.debug("splash", "DB not yet initialized")
            return
        }

        Logger.debug("splash", "DB init done")
        val now = System.currentTimeMillis()
        if (now > startupTime + KEEP_ACTIVE_FOR_MS) {
            startMainActivity()
        } else {
            val delay = KEEP_ACTIVE_FOR_MS - (now - startupTime)
            Logger.debug("splash",
                String.format(Locale.ROOT, "Scheduling main activity start in %d milliseconds",
                    delay))
            Handler(Looper.getMainLooper()).postDelayed({ startMainActivity() }, delay)
        }
    }

    private fun startMainActivity() {
        if (running) {
            Logger.debug("splash", "still running, launching main activity")
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                        Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.fade_in_slowly, R.anim.fade_out_slowly)
        } else {
            Logger.debug("splash", "Not running, finish and go away")
            finish()
        }
    }

    private class DatabaseInitThread : Thread() {
        override fun run() {
            DB.get().getProfileDAO().getProfileCountSync()

            DB.initComplete.postValue(true)
        }
    }

    companion object {
        private const val KEEP_ACTIVE_FOR_MS = 400L
    }
}
