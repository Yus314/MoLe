/**
 * BackgroundTaskManager - バックグラウンドタスク管理サービスの契約
 *
 * このファイルは実装の契約（インターフェース）を定義する。
 * 実際の実装は app/src/main/kotlin/net/ktnx/mobileledger/service/ に配置する。
 *
 * Date: 2026-01-12
 * Feature: 009-eliminate-data-singleton
 */
package net.ktnx.mobileledger.service

import kotlinx.coroutines.flow.StateFlow

/**
 * バックグラウンドタスクの状態
 */
enum class TaskState {
    /** タスク開始準備中 */
    STARTING,

    /** タスク実行中 */
    RUNNING,

    /** タスク正常完了 */
    FINISHED,

    /** タスクエラー終了 */
    ERROR
}

/**
 * バックグラウンドタスクの進捗情報
 *
 * @property taskId タスクの一意識別子
 * @property state タスクの状態
 * @property message ユーザー表示用メッセージ
 * @property current 現在の進捗 (オプション)
 * @property total 合計 (オプション)
 */
data class TaskProgress(
    val taskId: String,
    val state: TaskState,
    val message: String,
    val current: Int = 0,
    val total: Int = 0
) {
    init {
        require(taskId.isNotBlank()) { "taskId must not be blank" }
    }

    /**
     * 進捗率を 0.0〜1.0 で返す
     * total が 0 の場合は 0.0 を返す
     */
    val progressFraction: Float
        get() = if (total > 0) current.coerceAtMost(total).toFloat() / total else 0f

    /**
     * 進捗率を百分率で返す (0〜100)
     */
    val progressPercent: Int
        get() = (progressFraction * 100).toInt()

    /**
     * タスクが完了したかどうか
     */
    val isCompleted: Boolean
        get() = state == TaskState.FINISHED || state == TaskState.ERROR
}

/**
 * バックグラウンドタスク管理サービスのインターフェース
 *
 * アプリケーション全体でバックグラウンドタスク（データ同期など）の
 * 実行状態と進捗を管理する。@Singleton スコープで Activity 遷移を超えて
 * 状態を保持する。
 *
 * ## 使用例
 *
 * ```kotlin
 * // ViewModel での使用
 * @HiltViewModel
 * class MainViewModel @Inject constructor(
 *     private val backgroundTaskManager: BackgroundTaskManager
 * ) : ViewModel() {
 *     val isTaskRunning = backgroundTaskManager.isRunning
 *         .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
 *
 *     val taskProgress = backgroundTaskManager.progress
 *         .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
 * }
 *
 * // Compose での使用
 * @Composable
 * fun SyncIndicator(viewModel: MainViewModel) {
 *     val isRunning by viewModel.isTaskRunning.collectAsState()
 *     val progress by viewModel.taskProgress.collectAsState()
 *
 *     if (isRunning) {
 *         LinearProgressIndicator(progress = progress?.progressFraction ?: 0f)
 *     }
 * }
 * ```
 *
 * ## タスク実行側での使用
 *
 * ```kotlin
 * class RetrieveTransactionsTask @Inject constructor(
 *     private val backgroundTaskManager: BackgroundTaskManager
 * ) : Runnable {
 *     override fun run() {
 *         val taskId = "sync-${System.currentTimeMillis()}"
 *         backgroundTaskManager.taskStarted(taskId)
 *
 *         try {
 *             // 処理
 *             backgroundTaskManager.updateProgress(
 *                 TaskProgress(taskId, TaskState.RUNNING, "同期中...", current, total)
 *             )
 *
 *             backgroundTaskManager.taskFinished(taskId)
 *         } catch (e: Exception) {
 *             backgroundTaskManager.updateProgress(
 *                 TaskProgress(taskId, TaskState.ERROR, e.message ?: "エラー")
 *             )
 *             backgroundTaskManager.taskFinished(taskId)
 *         }
 *     }
 * }
 * ```
 */
interface BackgroundTaskManager {
    /**
     * バックグラウンドタスクが実行中かどうか
     *
     * 複数のタスクが同時に実行される可能性があるため、
     * 1つ以上のタスクが実行中の場合は true を返す。
     */
    val isRunning: StateFlow<Boolean>

    /**
     * 現在実行中のタスクの進捗情報
     *
     * タスクが実行されていない場合は null を返す。
     * 複数のタスクが実行中の場合は、最も最近更新されたタスクの進捗を返す。
     */
    val progress: StateFlow<TaskProgress?>

    /**
     * タスクの開始を通知する
     *
     * @param taskId タスクの一意識別子
     */
    fun taskStarted(taskId: String)

    /**
     * タスクの完了を通知する
     *
     * @param taskId タスクの一意識別子
     */
    fun taskFinished(taskId: String)

    /**
     * タスクの進捗を更新する
     *
     * @param progress 進捗情報
     */
    fun updateProgress(progress: TaskProgress)

    /**
     * 現在実行中のタスク数を取得する
     */
    val runningTaskCount: Int
}
