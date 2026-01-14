package net.ktnx.mobileledger.domain.usecase

/**
 * データベース初期化のインターフェース
 *
 * アプリ起動時のデータベース初期化処理を管理する。
 * 既存の DatabaseInitThread（SplashActivity内）を置き換える。
 *
 * ## 使用例
 *
 * ```kotlin
 * @HiltViewModel
 * class SplashViewModel @Inject constructor(
 *     private val databaseInitializer: DatabaseInitializer
 * ) : ViewModel() {
 *
 *     private val _initState = MutableStateFlow<InitState>(InitState.Initializing)
 *     val initState: StateFlow<InitState> = _initState.asStateFlow()
 *
 *     init {
 *         initialize()
 *     }
 *
 *     private fun initialize() {
 *         viewModelScope.launch {
 *             databaseInitializer.initialize()
 *                 .onSuccess { hasProfiles ->
 *                     _initState.value = if (hasProfiles) {
 *                         InitState.Ready
 *                     } else {
 *                         InitState.NeedsSetup
 *                     }
 *                 }
 *                 .onFailure { error ->
 *                     _initState.value = InitState.Error(error.message ?: "初期化に失敗しました")
 *                 }
 *         }
 *     }
 *
 *     sealed class InitState {
 *         data object Initializing : InitState()
 *         data object Ready : InitState()
 *         data object NeedsSetup : InitState()
 *         data class Error(val message: String) : InitState()
 *     }
 * }
 * ```
 *
 * ## 初期化処理
 *
 * 1. Room データベースの初期化（マイグレーション実行）
 * 2. 初期データの確認（プロファイル存在チェック）
 * 3. 必要に応じてデフォルトデータの作成
 *
 * ## エラーハンドリング
 *
 * - マイグレーション失敗 → 致命的エラー（アプリ使用不可）
 * - DBアクセスエラー → リトライ可能だが、通常は発生しない
 */
interface DatabaseInitializer {

    /**
     * データベースを初期化する
     *
     * @return 成功時は Result.success(hasProfiles)、失敗時は Result.failure()
     *         hasProfiles: プロファイルが1つ以上存在する場合 true
     * @throws CancellationException キャンセルされた場合
     *
     * ## 処理内容
     *
     * 1. DB.get() を呼び出してRoomデータベースを初期化
     * 2. ProfileDAO.getAllProfiles() でプロファイル存在確認
     * 3. 存在有無を返却
     *
     * ## 冪等性
     *
     * - 複数回呼び出しても安全
     * - 2回目以降は初期化済みDBへのアクセスのみ
     */
    suspend fun initialize(): Result<Boolean>

    /**
     * 初期化が完了しているかどうか
     */
    val isInitialized: Boolean
}
