/*
 * Copyright © 2026 Damyan Ivanov.
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

package net.ktnx.mobileledger.domain.usecase

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import logcat.LogPriority
import logcat.asLog
import logcat.logcat
import net.ktnx.mobileledger.backup.RawConfigReader
import net.ktnx.mobileledger.backup.RawConfigWriter
import net.ktnx.mobileledger.data.repository.CurrencyRepository
import net.ktnx.mobileledger.data.repository.ProfileRepository
import net.ktnx.mobileledger.data.repository.TemplateRepository
import net.ktnx.mobileledger.di.IoDispatcher

/**
 * Pure Coroutines implementation of [ConfigBackup].
 *
 * This implementation converts ConfigWriter/ConfigReader's Thread logic to pure suspend functions,
 * enabling proper integration with ViewModels and deterministic testing via TestDispatcher.
 *
 * ## Key Features
 * - No Thread usage (Thread.start(), Thread.join() not used)
 * - No runBlocking calls in production code
 * - All I/O runs via withContext(ioDispatcher)
 * - Fast cancellation via ensureActive()
 * - Result<Unit> for success/failure handling
 */
@Singleton
class ConfigBackupImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profileRepository: ProfileRepository,
    private val templateRepository: TemplateRepository,
    private val currencyRepository: CurrencyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ConfigBackup {

    override suspend fun backup(uri: Uri): Result<Unit> = withContext(ioDispatcher) {
        try {
            coroutineContext.ensureActive()

            val pfd = context.contentResolver.openFileDescriptor(uri, "w")
                ?: return@withContext Result.failure(Exception("Cannot open file for writing"))

            pfd.use { descriptor ->
                val fd = descriptor.fileDescriptor
                    ?: return@withContext Result.failure(Exception("File descriptor not available"))

                coroutineContext.ensureActive()

                val writer = RawConfigWriter(
                    FileOutputStream(fd),
                    profileRepository,
                    templateRepository,
                    currencyRepository
                )

                writer.writeConfig()
            }

            logcat { "Backup completed successfully" }
            Result.success(Unit)
        } catch (e: Exception) {
            logcat(LogPriority.WARN) { "Error during backup: ${e.asLog()}" }
            Result.failure(e)
        }
    }

    override suspend fun restore(uri: Uri): Result<Unit> = withContext(ioDispatcher) {
        try {
            coroutineContext.ensureActive()

            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                ?: return@withContext Result.failure(Exception("Cannot open file for reading"))

            pfd.use { descriptor ->
                val fd = descriptor.fileDescriptor
                    ?: return@withContext Result.failure(Exception("File descriptor not available"))

                coroutineContext.ensureActive()

                val reader = RawConfigReader(FileInputStream(fd))
                reader.readConfig()

                coroutineContext.ensureActive()

                reader.restoreAll(profileRepository, templateRepository, currencyRepository)

                // Handle current profile if needed
                val currentProfileUuid = reader.currentProfile
                if (profileRepository.currentProfile.value == null) {
                    coroutineContext.ensureActive()
                    var p = if (currentProfileUuid != null) {
                        profileRepository.getProfileByUuidSync(currentProfileUuid)
                    } else {
                        null
                    }

                    if (p == null) {
                        p = profileRepository.getAnyProfile()
                    }

                    p?.let { profileRepository.setCurrentProfile(it) }
                }
            }

            logcat { "Restore completed successfully" }
            Result.success(Unit)
        } catch (e: Exception) {
            logcat(LogPriority.WARN) { "Error during restore: ${e.asLog()}" }
            Result.failure(e)
        }
    }

    companion object {
    }
}
