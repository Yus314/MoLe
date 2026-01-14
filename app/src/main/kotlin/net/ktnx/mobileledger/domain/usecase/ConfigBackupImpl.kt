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
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import net.ktnx.mobileledger.backup.ConfigIO
import net.ktnx.mobileledger.backup.ConfigReader
import net.ktnx.mobileledger.backup.ConfigWriter
import net.ktnx.mobileledger.utils.Logger

/**
 * ConfigBackup の実装
 *
 * ConfigWriter と ConfigReader をラップし、suspend 関数として提供する。
 * キャンセル対応: invokeOnCancellation でスレッドを interrupt する。
 */
@Singleton
class ConfigBackupImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ConfigBackup {

    override suspend fun backup(uri: Uri): Result<Unit> = suspendCancellableCoroutine { cont ->
        try {
            val writer = ConfigWriter(
                context,
                uri,
                object : ConfigIO.OnErrorListener() {
                    override fun error(e: Exception) {
                        if (cont.isActive) {
                            cont.resumeWithException(e)
                        }
                    }
                },
                object : ConfigWriter.OnDoneListener() {
                    override fun done() {
                        if (cont.isActive) {
                            cont.resume(Result.success(Unit))
                        }
                    }
                }
            )

            cont.invokeOnCancellation {
                Logger.debug(TAG, "Cancelling backup task")
                writer.interrupt()
            }

            writer.start()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.warn(TAG, "Error starting backup", e)
            if (cont.isActive) {
                cont.resume(Result.failure(e))
            }
        }
    }

    override suspend fun restore(uri: Uri): Result<Unit> = suspendCancellableCoroutine { cont ->
        try {
            val reader = ConfigReader(
                context,
                uri,
                object : ConfigIO.OnErrorListener() {
                    override fun error(e: Exception) {
                        if (cont.isActive) {
                            cont.resumeWithException(e)
                        }
                    }
                },
                object : ConfigReader.OnDoneListener() {
                    override fun done() {
                        if (cont.isActive) {
                            cont.resume(Result.success(Unit))
                        }
                    }
                }
            )

            cont.invokeOnCancellation {
                Logger.debug(TAG, "Cancelling restore task")
                reader.interrupt()
            }

            reader.start()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.warn(TAG, "Error starting restore", e)
            if (cont.isActive) {
                cont.resume(Result.failure(e))
            }
        }
    }

    companion object {
        private const val TAG = "ConfigBackupImpl"
    }
}
