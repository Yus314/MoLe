package net.ktnx.mobileledger.domain.usecase

import net.ktnx.mobileledger.db.Profile

/**
 * hledger-web バージョン検出のインターフェース
 *
 * プロファイルのサーバーに接続し、hledger-webのバージョンを検出する。
 * 既存の VersionDetectionThread を置き換える。
 *
 * ## 使用例
 *
 * ```kotlin
 * @HiltViewModel
 * class ProfileDetailViewModel @Inject constructor(
 *     private val versionDetector: VersionDetector
 * ) : ViewModel() {
 *
 *     private val _detectedVersion = MutableStateFlow<String?>(null)
 *     val detectedVersion: StateFlow<String?> = _detectedVersion.asStateFlow()
 *
 *     private val _isDetecting = MutableStateFlow(false)
 *     val isDetecting: StateFlow<Boolean> = _isDetecting.asStateFlow()
 *
 *     fun detectVersion(url: String, useAuth: Boolean, user: String?, password: String?) {
 *         viewModelScope.launch {
 *             _isDetecting.value = true
 *             _detectedVersion.value = null
 *
 *             versionDetector.detect(url, useAuth, user, password)
 *                 .onSuccess { version ->
 *                     _detectedVersion.value = version
 *                 }
 *                 .onFailure {
 *                     _detectedVersion.value = null
 *                 }
 *
 *             _isDetecting.value = false
 *         }
 *     }
 * }
 * ```
 *
 * ## キャンセル対応
 *
 * - `detect()` はキャンセル対応
 * - URLやパスワード入力中の連続呼び出しで前のリクエストをキャンセル可能
 *
 * ## エラーハンドリング
 *
 * - 接続失敗 → Result.failure() で静かに失敗（UIにはバージョン非表示）
 * - バージョン解析失敗 → Result.failure()
 */
interface VersionDetector {

    /**
     * hledger-web のバージョンを検出する
     *
     * @param url サーバーのベースURL
     * @param useAuth 認証を使用するかどうか
     * @param user 認証ユーザー名（useAuth=true の場合に使用）
     * @param password 認証パスワード（useAuth=true の場合に使用）
     * @return 成功時はバージョン文字列（例: "1.32"）、失敗時は Result.failure()
     * @throws CancellationException キャンセルされた場合
     *
     * ## 検出方法
     *
     * 1. `{url}/version` にGETリクエスト
     * 2. レスポンスから "hledger-web X.Y" パターンを抽出
     * 3. バージョン番号を返却
     *
     * ## タイムアウト
     *
     * - 接続: 10秒
     * - 読み取り: 10秒
     *
     * バージョン検出は補助機能のため、同期処理より短いタイムアウトを設定。
     */
    suspend fun detect(url: String, useAuth: Boolean, user: String?, password: String?): Result<String>

    /**
     * プロファイルからバージョンを検出する（便利メソッド）
     *
     * @param profile 検出対象のプロファイル
     * @return 成功時はバージョン文字列、失敗時は Result.failure()
     */
    suspend fun detect(profile: Profile): Result<String> = detect(
        url = profile.url,
        useAuth = profile.useAuthentication,
        user = profile.authUser,
        password = profile.authPassword
    )
}
