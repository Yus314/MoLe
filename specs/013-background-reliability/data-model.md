# Data Model: バックグラウンド処理アーキテクチャの技術的負債解消

**Feature**: 013-background-reliability
**Date**: 2026-01-14
**Status**: Complete

## エンティティ一覧

| エンティティ | 用途 | 永続化 |
|--------------|------|--------|
| SyncError | 共通エラー型 | No |
| SyncProgress | 進捗報告 | No |
| SyncState | UI状態管理 | No |
| SyncResult | 同期結果データ | No |

---

## SyncError

### 概要

バックグラウンド処理で発生するエラーを表現する sealed class。
UIでのエラー表示、リトライ判定、ログ出力に使用。

### 定義

```kotlin
package net.ktnx.mobileledger.domain.model

/**
 * バックグラウンド処理で発生するエラーの共通型
 *
 * すべてのエラーは以下の情報を持つ:
 * - message: ユーザーに表示するメッセージ（日本語）
 * - isRetryable: リトライ可能かどうか
 */
sealed class SyncError {
    abstract val message: String
    abstract val isRetryable: Boolean

    /**
     * ネットワーク接続エラー
     * - WiFi/モバイルデータ未接続
     * - DNS解決失敗
     * - サーバー到達不可
     */
    data class NetworkError(
        override val message: String = "ネットワークに接続できません",
        val cause: Throwable? = null
    ) : SyncError() {
        override val isRetryable = true
    }

    /**
     * タイムアウトエラー
     * - 接続タイムアウト（30秒）
     * - 読み取りタイムアウト（30秒）
     */
    data class TimeoutError(
        override val message: String = "サーバーが応答しません",
        val timeoutMs: Long = 30_000
    ) : SyncError() {
        override val isRetryable = true
    }

    /**
     * 認証エラー
     * - HTTP 401 Unauthorized
     * - 無効なユーザー名/パスワード
     */
    data class AuthenticationError(
        override val message: String = "認証に失敗しました。ユーザー名とパスワードを確認してください",
        val httpCode: Int = 401
    ) : SyncError() {
        override val isRetryable = false
    }

    /**
     * サーバーエラー
     * - HTTP 4xx クライアントエラー（401以外）
     * - HTTP 5xx サーバーエラー
     */
    data class ServerError(
        override val message: String,
        val httpCode: Int,
        val serverMessage: String? = null
    ) : SyncError() {
        override val isRetryable = httpCode >= 500
    }

    /**
     * バリデーションエラー
     * - 取引送信時のサーバー側バリデーション失敗
     * - 勘定科目不存在、金額エラーなど
     */
    data class ValidationError(
        override val message: String,
        val field: String? = null,
        val details: List<String> = emptyList()
    ) : SyncError() {
        override val isRetryable = false
    }

    /**
     * パースエラー
     * - JSONパース失敗
     * - 予期しないレスポンス形式
     */
    data class ParseError(
        override val message: String = "データの解析に失敗しました",
        val cause: Throwable? = null
    ) : SyncError() {
        override val isRetryable = false
    }

    /**
     * APIバージョンエラー
     * - サポートされていないAPIバージョン
     */
    data class ApiVersionError(
        override val message: String = "サポートされていないAPIバージョンです",
        val detectedVersion: String? = null,
        val supportedVersions: List<String> = emptyList()
    ) : SyncError() {
        override val isRetryable = false
    }

    /**
     * キャンセル
     * - ユーザーによる明示的キャンセル
     * - バックグラウンド30秒超過による自動キャンセル
     */
    data object Cancelled : SyncError() {
        override val message = "処理がキャンセルされました"
        override val isRetryable = false
    }

    /**
     * 不明なエラー
     * - 予期しない例外
     */
    data class UnknownError(
        override val message: String = "予期しないエラーが発生しました",
        val cause: Throwable? = null
    ) : SyncError() {
        override val isRetryable = false
    }
}
```

### バリデーションルール

- `message` は空文字列不可
- `httpCode` は 100-599 の範囲
- `timeoutMs` は正の値

### 状態遷移

N/A（状態を持たない値オブジェクト）

---

## SyncProgress

### 概要

バックグラウンド処理の進捗を報告するためのデータクラス。
Flow経由でUIに通知される。

### 定義

```kotlin
package net.ktnx.mobileledger.domain.model

/**
 * 同期処理の進捗情報
 */
sealed class SyncProgress {
    /**
     * 処理開始
     */
    data class Starting(
        val message: String = "接続中..."
    ) : SyncProgress()

    /**
     * 処理実行中（進捗計算可能）
     *
     * @param current 現在の処理済み件数
     * @param total 総件数（0の場合は不明）
     * @param message 進捗メッセージ
     */
    data class Running(
        val current: Int,
        val total: Int,
        val message: String
    ) : SyncProgress() {
        /**
         * 進捗率（0.0-1.0）
         * totalが0の場合は-1.0（不確定）
         */
        val progressFraction: Float
            get() = if (total > 0) current.toFloat() / total else -1f

        /**
         * 進捗パーセント（0-100）
         * totalが0の場合は-1（不確定）
         */
        val progressPercent: Int
            get() = if (total > 0) (current * 100 / total) else -1
    }

    /**
     * 処理実行中（進捗計算不可）
     * サーバー応答待ちなど、総件数が不明な場合
     */
    data class Indeterminate(
        val message: String
    ) : SyncProgress()
}
```

### バリデーションルール

- `Running.current` は 0 以上
- `Running.total` は 0 以上（0 = 不明）
- `Running.current <= Running.total`（total > 0 の場合）
- `message` は空文字列不可

---

## SyncState

### 概要

UI層での同期処理状態を表現する sealed class。
ViewModelからUIに公開されるStateFlow<SyncState>の型。

### 定義

```kotlin
package net.ktnx.mobileledger.domain.model

/**
 * 同期処理のUI状態
 */
sealed class SyncState {
    /**
     * アイドル状態（同期処理なし）
     */
    data object Idle : SyncState()

    /**
     * 同期処理実行中
     */
    data class InProgress(
        val progress: SyncProgress
    ) : SyncState()

    /**
     * 同期処理完了
     */
    data class Completed(
        val result: SyncResult
    ) : SyncState()

    /**
     * 同期処理失敗
     */
    data class Failed(
        val error: SyncError
    ) : SyncState()

    /**
     * 同期処理キャンセル済み
     */
    data object Cancelled : SyncState()
}
```

### 状態遷移図

```
                 startSync()
     Idle ─────────────────────► InProgress
       ▲                              │
       │                              │
       │  dismiss()                   │
       │                              ▼
       │ ◄─────────────── Completed / Failed / Cancelled
       │                              │
       │          retry()             │
       └──────────────────────────────┘
                (Failed のみ)
```

---

## SyncResult

### 概要

同期処理の完了結果を表現するデータクラス。

### 定義

```kotlin
package net.ktnx.mobileledger.domain.model

/**
 * 同期処理の結果データ
 *
 * @param transactionCount 同期された取引数
 * @param accountCount 同期された勘定科目数
 * @param duration 処理時間（ミリ秒）
 */
data class SyncResult(
    val transactionCount: Int,
    val accountCount: Int,
    val duration: Long
) {
    /**
     * 結果サマリーメッセージ
     */
    val summaryMessage: String
        get() = "${transactionCount}件の取引、${accountCount}件の勘定科目を同期しました"
}
```

### バリデーションルール

- `transactionCount` は 0 以上
- `accountCount` は 0 以上
- `duration` は 0 以上

---

## 関連型

### SendState（取引送信用）

```kotlin
package net.ktnx.mobileledger.domain.model

/**
 * 取引送信のUI状態
 */
sealed class SendState {
    /**
     * アイドル状態
     */
    data object Idle : SendState()

    /**
     * 送信中
     */
    data class Sending(
        val message: String = "送信中..."
    ) : SendState()

    /**
     * 送信完了
     */
    data object Completed : SendState()

    /**
     * 送信失敗
     */
    data class Failed(
        val error: SyncError
    ) : SendState()
}
```

### BackupState（バックアップ用）

```kotlin
package net.ktnx.mobileledger.domain.model

/**
 * バックアップ/リストアのUI状態
 */
sealed class BackupState {
    data object Idle : BackupState()
    data class InProgress(val message: String) : BackupState()
    data object Completed : BackupState()
    data class Failed(val error: SyncError) : BackupState()
}
```

---

## パッケージ構成

```text
app/src/main/kotlin/net/ktnx/mobileledger/domain/model/
├── SyncError.kt       # 共通エラー型
├── SyncProgress.kt    # 進捗報告型
├── SyncState.kt       # 同期UI状態
├── SyncResult.kt      # 同期結果
├── SendState.kt       # 送信UI状態
└── BackupState.kt     # バックアップUI状態
```
