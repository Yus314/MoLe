/*
 * Copyright © 2022 Damyan Ivanov.
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

package net.ktnx.mobileledger

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import net.ktnx.mobileledger.backup.ConfigIO
import net.ktnx.mobileledger.backup.ConfigReader
import net.ktnx.mobileledger.backup.ConfigWriter
import net.ktnx.mobileledger.databinding.FragmentBackupsBinding
import net.ktnx.mobileledger.model.Data

class BackupsActivity : AppCompatActivity() {
    private lateinit var b: FragmentBackupsBinding
    private lateinit var backupChooserLauncher: ActivityResultLauncher<String>
    private lateinit var restoreChooserLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = FragmentBackupsBinding.inflate(layoutInflater)
        setContentView(b.root)

        setSupportActionBar(b.toolbar)
        // Show the Up button in the action bar.
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        b.backupButton.setOnClickListener { view -> backupClicked(view) }
        b.restoreButton.setOnClickListener { view -> restoreClicked(view) }

        backupChooserLauncher = registerForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { result -> storeConfig(result) }

        restoreChooserLauncher = registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { result -> readConfig(result) }

        Data.observeProfile(this) { p ->
            if (p == null) {
                b.backupButton.isEnabled = false
                b.backupExplanationText.isEnabled = false
            } else {
                b.backupButton.isEnabled = true
                b.backupExplanationText.isEnabled = true
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun storeConfig(result: Uri?) {
        if (result == null) return

        try {
            val saver = ConfigWriter(
                baseContext,
                result,
                object : ConfigIO.OnErrorListener() {
                    override fun error(e: Exception) {
                        Snackbar.make(
                            b.backupButton,
                            e.toString(),
                            BaseTransientBottomBar.LENGTH_LONG
                        ).show()
                    }
                },
                object : ConfigWriter.OnDoneListener() {
                    override fun done() {
                        Snackbar.make(
                            b.backupButton,
                            R.string.config_saved,
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            )
            saver.start()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun readConfig(result: Uri?) {
        if (result == null) return

        try {
            val reader = ConfigReader(
                baseContext,
                result,
                object : ConfigIO.OnErrorListener() {
                    override fun error(e: Exception) {
                        Snackbar.make(
                            b.backupButton,
                            e.toString(),
                            BaseTransientBottomBar.LENGTH_LONG
                        ).show()
                    }
                },
                object : ConfigReader.OnDoneListener() {
                    override fun done() {
                        Snackbar.make(
                            b.backupButton,
                            R.string.config_restored,
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            )
            reader.start()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun backupClicked(view: View) {
        val now = Date()
        val df = SimpleDateFormat("y-MM-dd HH:mm", Locale.getDefault())
        backupChooserLauncher.launch(String.format("MoLe-%s.json", df.format(now)))
    }

    private fun restoreClicked(view: View) {
        restoreChooserLauncher.launch(arrayOf("application/json"))
    }

    companion object {
        @JvmStatic
        fun start(context: Context) {
            val starter = Intent(context, BackupsActivity::class.java)
            context.startActivity(starter)
        }
    }
}
