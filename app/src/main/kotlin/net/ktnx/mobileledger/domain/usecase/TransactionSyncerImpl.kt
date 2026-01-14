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

import android.os.OperationCanceledException
import com.fasterxml.jackson.databind.RuntimeJsonMappingException
import java.io.IOException
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import java.text.ParseException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import net.ktnx.mobileledger.async.RetrieveTransactionsTask
import net.ktnx.mobileledger.data.repository.AccountRepository
import net.ktnx.mobileledger.data.repository.OptionRepository
import net.ktnx.mobileledger.data.repository.TransactionRepository
import net.ktnx.mobileledger.db.Profile
import net.ktnx.mobileledger.domain.model.SyncError
import net.ktnx.mobileledger.domain.model.SyncProgress
import net.ktnx.mobileledger.domain.model.SyncResult
import net.ktnx.mobileledger.err.HTTPException
import net.ktnx.mobileledger.json.ApiNotSupportedException
import net.ktnx.mobileledger.service.AppStateService
import net.ktnx.mobileledger.service.BackgroundTaskManager
import net.ktnx.mobileledger.service.TaskProgress
import net.ktnx.mobileledger.service.TaskState
import net.ktnx.mobileledger.utils.Logger

/**
 * TransactionSyncer の実装
 *
 * RetrieveTransactionsTask をラップし、Flow<SyncProgress> として進捗を報告する。
 * キャンセル対応: invokeOnCancellation でスレッドを interrupt して接続を切断する。
 */
@Singleton
class TransactionSyncerImpl @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val optionRepository: OptionRepository,
    private val backgroundTaskManager: BackgroundTaskManager,
    private val appStateService: AppStateService
) : TransactionSyncer {

    private var _lastResult: SyncResult? = null

    override fun sync(profile: Profile): Flow<SyncProgress> = callbackFlow {
        val startTime = System.currentTimeMillis()

        // Create custom BackgroundTaskManager to capture progress
        val progressCapture = object : BackgroundTaskManager {
            override val isRunning: StateFlow<Boolean> = backgroundTaskManager.isRunning
            override val progress: StateFlow<TaskProgress?> = backgroundTaskManager.progress
            override val runningTaskCount: Int
                get() = backgroundTaskManager.runningTaskCount

            override fun taskStarted(taskId: String) {
                backgroundTaskManager.taskStarted(taskId)
            }

            override fun taskFinished(taskId: String) {
                backgroundTaskManager.taskFinished(taskId)
            }

            override fun updateProgress(taskProgress: TaskProgress) {
                backgroundTaskManager.updateProgress(taskProgress)
                // Convert to SyncProgress and emit
                val syncProgress = when (taskProgress.state) {
                    TaskState.STARTING -> SyncProgress.Starting(taskProgress.message)

                    TaskState.RUNNING -> {
                        if (taskProgress.total > 0) {
                            SyncProgress.Running(
                                current = taskProgress.current,
                                total = taskProgress.total,
                                message = taskProgress.message
                            )
                        } else {
                            SyncProgress.Indeterminate(taskProgress.message)
                        }
                    }

                    TaskState.FINISHED, TaskState.ERROR -> null
                }
                syncProgress?.let { trySend(it) }
            }
        }

        val task = RetrieveTransactionsTask(
            profile = profile,
            accountRepository = accountRepository,
            transactionRepository = transactionRepository,
            optionRepository = optionRepository,
            backgroundTaskManager = progressCapture,
            appStateService = appStateService
        )

        // Start the task
        trySend(SyncProgress.Starting("接続中..."))
        task.start()

        // Wait for task completion using suspendCancellableCoroutine
        try {
            suspendCancellableCoroutine { continuation ->
                continuation.invokeOnCancellation {
                    Logger.debug(TAG, "Cancelling sync task")
                    task.interrupt()
                }

                Thread {
                    try {
                        task.join()
                        if (continuation.isActive) {
                            continuation.resume(Unit)
                        }
                    } catch (e: InterruptedException) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(
                                CancellationException("Sync was cancelled", e)
                            )
                        }
                    }
                }.start()
            }

            // Get results from AppStateService
            val syncInfo = appStateService.lastSyncInfo.value
            val transactionCount = syncInfo.transactionCount
            val accountCount = syncInfo.accountCount

            val duration = System.currentTimeMillis() - startTime
            _lastResult = SyncResult(
                transactionCount = transactionCount,
                accountCount = accountCount,
                duration = duration
            )

            close()
        } catch (e: CancellationException) {
            close(e)
        } catch (e: Exception) {
            close(e)
        }

        awaitClose {
            if (task.isAlive) {
                task.interrupt()
            }
        }
    }

    override fun getLastResult(): SyncResult? = _lastResult

    /**
     * 例外を SyncError にマッピングする
     */
    fun mapException(e: Throwable): SyncError = when (e) {
        is SocketTimeoutException -> SyncError.TimeoutError(message = "サーバーが応答しません")

        is MalformedURLException -> SyncError.ValidationError(message = "無効なサーバーURLです")

        is HTTPException -> when (e.responseCode) {
            401 -> SyncError.AuthenticationError(message = "認証に失敗しました", httpCode = e.responseCode)
            else -> SyncError.ServerError(message = e.message ?: "サーバーエラー", httpCode = e.responseCode)
        }

        is IOException -> SyncError.NetworkError(message = e.localizedMessage ?: "ネットワークエラー", cause = e)

        is RuntimeJsonMappingException -> SyncError.ParseError(message = "JSONパースエラー", cause = e)

        is ParseException -> SyncError.ParseError(message = "データ解析エラー", cause = e)

        is OperationCanceledException -> SyncError.Cancelled

        is ApiNotSupportedException -> SyncError.ApiVersionError(message = "サポートされていないAPIバージョンです")

        is CancellationException -> SyncError.Cancelled

        else -> SyncError.UnknownError(
            message = e.localizedMessage ?: "予期しないエラーが発生しました",
            cause = e
        )
    }

    companion object {
        private const val TAG = "TransactionSyncerImpl"
    }
}
