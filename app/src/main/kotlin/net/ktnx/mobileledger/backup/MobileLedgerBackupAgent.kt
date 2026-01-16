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

package net.ktnx.mobileledger.backup

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.os.ParcelFileDescriptor
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlinx.coroutines.runBlocking
import logcat.asLog
import logcat.logcat
import net.ktnx.mobileledger.di.BackupEntryPoint

class MobileLedgerBackupAgent : BackupAgent() {

    @Throws(IOException::class)
    override fun onBackup(oldState: ParcelFileDescriptor, data: BackupDataOutput, newState: ParcelFileDescriptor) {
        logcat { "onBackup()" }
        backupSettings(data)
        newState.close()
    }

    @Throws(IOException::class)
    private fun backupSettings(data: BackupDataOutput) {
        logcat { "Starting cloud backup" }
        val entryPoint = BackupEntryPoint.get(this)
        val output = ByteArrayOutputStream(4096)
        val saver = RawConfigWriter(
            output,
            entryPoint.profileRepository(),
            entryPoint.templateRepository(),
            entryPoint.currencyRepository()
        )
        // runBlocking is required here because BackupAgent.onBackup() is called
        // synchronously by the Android system and cannot be made a suspend function
        runBlocking { saver.writeConfig() }
        val bytes = output.toByteArray()
        data.writeEntityHeader(SETTINGS_KEY, bytes.size)
        data.writeEntityData(bytes, bytes.size)
        logcat { "Done writing backup data" }
    }

    @Throws(IOException::class)
    override fun onRestore(data: BackupDataInput, appVersionCode: Int, newState: ParcelFileDescriptor) {
        logcat { "Starting cloud restore" }
        if (data.readNextHeader()) {
            val key = data.key
            if (key == SETTINGS_KEY) {
                restoreSettings(data)
            }
        }
    }

    @Throws(IOException::class)
    private fun restoreSettings(data: BackupDataInput) {
        val entryPoint = BackupEntryPoint.get(this)
        val bytes = ByteArray(data.dataSize)
        data.readEntityData(bytes, 0, bytes.size)
        val reader = RawConfigReader(ByteArrayInputStream(bytes))
        reader.readConfig()
        logcat { "Successfully read restore data. Wiping database" }
        entryPoint.db().deleteAllSync()
        logcat { "Database wiped" }
        // runBlocking is required here because BackupAgent.onRestore() is called
        // synchronously by the Android system and cannot be made a suspend function
        runBlocking {
            reader.restoreAll(
                entryPoint.profileRepository(),
                entryPoint.templateRepository(),
                entryPoint.currencyRepository(),
                entryPoint.preferencesRepository()
            )
        }
        logcat { "All data restored from the cloud" }
    }

    companion object {
        @JvmField
        var SETTINGS_KEY = "settings"
    }
}
