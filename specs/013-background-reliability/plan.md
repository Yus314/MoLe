# Implementation Plan: バックグラウンド処理アーキテクチャの技術的負債解消

**Branch**: `013-background-reliability` | **Date**: 2026-01-14 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/013-background-reliability/spec.md`

## Summary

5つのバックグラウンド処理コンポーネント（RetrieveTransactionsTask, SendTransactionTask, ConfigIO, VersionDetectionThread, DatabaseInitThread）を、テスト可能なインターフェースベースのアーキテクチャに移行する。既存の TransactionSender パターン（suspendCancellableCoroutine + Hilt DI）を標準として適用し、constitution 原則 II（TDD）、VI（Coroutines）、X（階層型アーキテクチャ）への準拠を達成する。

## Technical Context

**Language/Version**: Kotlin 2.0.21 / JVM target 1.8
**Primary Dependencies**: Coroutines 1.9.0, Hilt 2.51.1, Room 2.4.2, Jetpack Compose (composeBom 2024.12.01)
**Storage**: Room Database（SQLite）- 既存、変更なし
**Testing**: JUnit 4, MockK, Turbine, Kover（カバレッジ）
**Target Platform**: Android (minSdk 24)
**Project Type**: Android Mobile
**Performance Goals**: キャンセル応答 5秒以内（FR-006）、テストカバレッジ 50%以上（SC-004）
**Constraints**: 既存 Thread 実装との後方互換性維持、HttpURLConnection 継続（OkHttp 移行は別機能）
**Scale/Scope**: 5コンポーネント移行、推定 20-30 ファイル変更

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| 原則 | 現状 | 移行後 | 対応 |
|------|------|--------|------|
| II. TDD | ❌ バックグラウンド処理のテストカバレッジ 0% | ✅ 50%以上達成 | インターフェース抽象化 + Fake 提供 |
| VI. Kotlin/Coroutines | ❌ Thread + runBlocking | ✅ suspend function + Flow | suspendCancellableCoroutine でラップ |
| VIII. Hilt DI | ❌ 手動インスタンス化 | ✅ @Inject コンストラクタ | UseCaseModule 拡張 |
| X. 階層型アーキテクチャ | ❌ DAO 直接アクセスあり | ✅ Repository 経由 | 既存 Repository 活用 |

**GATE Result**: PASS（移行計画で全原則への準拠を達成可能）

## Project Structure

### Documentation (this feature)

```text
specs/013-background-reliability/
├── plan.md              # This file
├── research.md          # Phase 0 output - 技術調査結果
├── data-model.md        # Phase 1 output - エンティティ定義
├── quickstart.md        # Phase 1 output - 開発者ガイド
├── contracts/           # Phase 1 output - インターフェース定義
│   ├── TransactionSyncer.kt
│   ├── TransactionSender.kt
│   ├── ConfigBackup.kt
│   ├── VersionDetector.kt
│   ├── DatabaseInitializer.kt
│   └── FakeImplementations.kt
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
app/src/main/kotlin/net/ktnx/mobileledger/
├── domain/
│   ├── model/                    # 新規作成
│   │   ├── SyncError.kt          # 共通エラー型
│   │   ├── SyncProgress.kt       # 進捗報告型
│   │   ├── SyncState.kt          # UI状態
│   │   ├── SyncResult.kt         # 同期結果
│   │   ├── SendState.kt          # 送信UI状態
│   │   └── BackupState.kt        # バックアップUI状態
│   └── usecase/                  # 拡張
│       ├── TransactionSender.kt  # 既存
│       ├── TransactionSenderImpl.kt  # 既存
│       ├── TransactionSyncer.kt  # 新規
│       ├── TransactionSyncerImpl.kt  # 新規
│       ├── ConfigBackup.kt       # 新規
│       ├── ConfigBackupImpl.kt   # 新規
│       ├── VersionDetector.kt    # 新規
│       ├── VersionDetectorImpl.kt  # 新規
│       ├── DatabaseInitializer.kt  # 新規
│       └── DatabaseInitializerImpl.kt  # 新規
├── di/
│   └── UseCaseModule.kt          # 拡張（新規バインディング追加）
├── async/                        # 既存（移行期間中維持）
│   ├── RetrieveTransactionsTask.kt  # 既存（Impl からラップ）
│   └── SendTransactionTask.kt    # 既存（Impl からラップ）
├── backup/                       # 既存（移行期間中維持）
│   ├── ConfigIO.kt
│   ├── RawConfigReader.kt
│   └── RawConfigWriter.kt
└── service/                      # 既存
    ├── BackgroundTaskManager.kt  # 既存（変更なし）
    └── BackgroundTaskManagerImpl.kt

app/src/test/kotlin/net/ktnx/mobileledger/
├── domain/
│   └── usecase/                  # 新規
│       ├── TransactionSyncerTest.kt
│       └── ConfigBackupTest.kt
└── fake/                         # 拡張
    ├── FakeTransactionSender.kt  # 既存
    ├── FakeTransactionSyncer.kt  # 新規
    ├── FakeConfigBackup.kt       # 新規
    ├── FakeVersionDetector.kt    # 新規
    └── FakeDatabaseInitializer.kt  # 新規
```

**Structure Decision**: 既存の Android モバイルアプリ構造を維持。domain/usecase/ に新規インターフェースを追加し、既存の async/ と backup/ は移行期間中維持する（suspendCancellableCoroutine でラップ）。

## Migration Strategy

### Phase 1: Interface-First Wrapper（低リスク）

1. 各コンポーネントにインターフェースを定義
2. 既存 Thread 実装を suspendCancellableCoroutine でラップ
3. Hilt DI でバインド
4. Fake 実装を提供しテスト可能に

### Phase 2: 段階的内部移行（将来）

1. Thread 内部のロジックを suspend 関数に移行
2. runBlocking を除去
3. 最終的に Thread クラスを削除

### 移行順序（spec.md で確定）

1. **TransactionSyncer** (RetrieveTransactionsTask) - 最も複雑、814行
2. **ConfigBackup** (ConfigIO) - 中程度、runBlocking 複数箇所
3. **VersionDetector** (埋め込み) - 単純、抽出のみ
4. **DatabaseInitializer** (DatabaseInitThread) - 最も単純、7行

（TransactionSender は既に移行済み、参照パターンとして使用）

## Key Design Decisions

### 1. エラー型

**Decision**: `SyncError` sealed class を採用

```kotlin
sealed class SyncError {
    abstract val message: String
    abstract val isRetryable: Boolean

    data class NetworkError(...) : SyncError()
    data class TimeoutError(...) : SyncError()
    data class AuthenticationError(...) : SyncError()
    // ...
}
```

**Rationale**: UI での exhaustive when チェック、リトライ可能判定に最適

### 2. 進捗報告

**Decision**: `Flow<SyncProgress>` + 既存 `BackgroundTaskManager` との統合

```kotlin
interface TransactionSyncer {
    fun sync(profile: Profile): Flow<SyncProgress>
}
```

**Rationale**: BackgroundTaskManager は既にテスト可能なインターフェースとして存在

### 3. キャンセル処理

**Decision**: 構造化並行性 + `invokeOnCancellation`

```kotlin
suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation {
        connection?.disconnect()
        task.interrupt()
    }
    // ...
}
```

**Rationale**: 5秒以内のキャンセル応答を達成可能

### 4. ネットワーク

**Decision**: HttpURLConnection を維持（OkHttp 移行は別機能）

**Rationale**: スコープ制限、リスク分離

## Complexity Tracking

> 本機能に constitution 違反はなし。全ての設計判断は constitution 原則に準拠。

| 項目 | 判断 | 根拠 |
|------|------|------|
| Interface-First Wrapper | 採用 | 段階的移行でリスク最小化、constitution VIII に準拠 |
| sealed class エラー型 | 採用 | 型安全性とテスト容易性、constitution II に準拠 |
| Thread 維持（移行期間） | 採用 | 後方互換性、constitution III（段階的開発）に準拠 |

## Generated Artifacts

| ファイル | 目的 |
|---------|------|
| `research.md` | 技術調査結果（runBlocking 除去、suspendCancellableCoroutine パターン） |
| `data-model.md` | エンティティ定義（SyncError, SyncProgress, SyncState, SyncResult） |
| `quickstart.md` | 開発者向けクイックスタートガイド |
| `contracts/` | 5つのインターフェース定義 + Fake 実装パターン |

## Next Steps

1. `/speckit.tasks` でタスク分解を実行
2. `SyncError.kt` から実装開始（TDD）
3. `TransactionSyncer` インターフェース + Fake + テスト
4. `TransactionSyncerImpl`（既存 Thread ラップ）
5. 残りのコンポーネントを順次移行
