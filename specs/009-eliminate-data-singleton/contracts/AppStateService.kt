/**
 * AppStateService - UIレベルのアプリケーション状態管理サービスの契約
 *
 * このファイルは実装の契約（インターフェース）を定義する。
 * 実際の実装は app/src/main/kotlin/net/ktnx/mobileledger/service/ に配置する。
 *
 * Date: 2026-01-12
 * Feature: 009-eliminate-data-singleton
 */
package net.ktnx.mobileledger.service

import java.util.Date
import kotlinx.coroutines.flow.StateFlow

/**
 * データ同期の結果情報
 *
 * @property date 同期完了日時
 * @property transactionCount 取得した取引数
 * @property accountCount 表示対象のアカウント数
 * @property totalAccountCount 全アカウント数
 */
data class SyncInfo(val date: Date?, val transactionCount: Int, val accountCount: Int, val totalAccountCount: Int) {
    companion object {
        /**
         * 同期未実行時の初期値
         */
        val EMPTY = SyncInfo(
            date = null,
            transactionCount = 0,
            accountCount = 0,
            totalAccountCount = 0
        )
    }

    /**
     * 同期が実行されたことがあるか
     */
    val hasSynced: Boolean
        get() = date != null

    /**
     * 同期情報の要約テキストを生成
     *
     * @param transactionLabel 取引のラベル (例: "transactions", "件")
     * @param accountLabel アカウントのラベル (例: "accounts", "アカウント")
     * @return フォーマット済み文字列
     */
    fun formatSummary(transactionLabel: String, accountLabel: String): String =
        "$transactionCount $transactionLabel, $accountCount/$totalAccountCount $accountLabel"
}

/**
 * UIレベルのアプリケーション状態管理サービスのインターフェース
 *
 * ナビゲーションドロワーの状態や同期情報など、UI全体で共有される状態を管理する。
 * @Singleton スコープで管理され、Activity 遷移を超えて状態を保持する。
 *
 * ## 使用例
 *
 * ```kotlin
 * // ViewModel での使用
 * @HiltViewModel
 * class MainViewModel @Inject constructor(
 *     private val appStateService: AppStateService
 * ) : ViewModel() {
 *     val lastSyncInfo = appStateService.lastSyncInfo
 *         .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SyncInfo.EMPTY)
 *
 *     val drawerOpen = appStateService.drawerOpen
 *         .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
 *
 *     fun openDrawer() {
 *         appStateService.setDrawerOpen(true)
 *     }
 *
 *     fun closeDrawer() {
 *         appStateService.setDrawerOpen(false)
 *     }
 * }
 *
 * // Compose での使用
 * @Composable
 * fun MainScreen(viewModel: MainViewModel) {
 *     val drawerOpen by viewModel.drawerOpen.collectAsState()
 *     val syncInfo by viewModel.lastSyncInfo.collectAsState()
 *
 *     ModalNavigationDrawer(
 *         drawerState = rememberDrawerState(if (drawerOpen) DrawerValue.Open else DrawerValue.Closed),
 *         // ...
 *     ) {
 *         // Content
 *         if (syncInfo.hasSynced) {
 *             Text("Last sync: ${syncInfo.date}")
 *         }
 *     }
 * }
 * ```
 *
 * ## 同期完了時の更新
 *
 * ```kotlin
 * class RetrieveTransactionsTask @Inject constructor(
 *     private val appStateService: AppStateService
 * ) {
 *     fun onSyncComplete(transactionCount: Int, accountCount: Int, totalAccountCount: Int) {
 *         appStateService.updateSyncInfo(
 *             SyncInfo(
 *                 date = Date(),
 *                 transactionCount = transactionCount,
 *                 accountCount = accountCount,
 *                 totalAccountCount = totalAccountCount
 *             )
 *         )
 *     }
 * }
 * ```
 */
interface AppStateService {
    /**
     * 最後の同期情報
     *
     * 同期が一度も実行されていない場合は SyncInfo.EMPTY を返す。
     */
    val lastSyncInfo: StateFlow<SyncInfo>

    /**
     * ナビゲーションドロワーの開閉状態
     */
    val drawerOpen: StateFlow<Boolean>

    /**
     * 同期情報を更新する
     *
     * @param info 新しい同期情報
     */
    fun updateSyncInfo(info: SyncInfo)

    /**
     * 同期情報をクリアする
     *
     * プロファイル切り替え時などに使用する。
     */
    fun clearSyncInfo()

    /**
     * ドロワーの開閉状態を設定する
     *
     * @param open true: 開く, false: 閉じる
     */
    fun setDrawerOpen(open: Boolean)

    /**
     * ドロワーの開閉状態をトグルする
     */
    fun toggleDrawer()
}
