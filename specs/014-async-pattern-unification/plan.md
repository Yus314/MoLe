# Implementation Plan: 非同期処理パターンの統一

**Branch**: `014-async-pattern-unification` | **Date**: 2026-01-15 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/014-async-pattern-unification/spec.md`

## Summary

既存の4つの非同期パターン（Thread継承、ExecutorService、古いコールバック、現代的Coroutines）から3つを削除し、Kotlin Coroutines（suspend functions + Flow）に統一する。段階的移行アプローチを採用し、複雑度の低い処理から順に移行する。各処理の外部インターフェースはCoroutinesベースの新インターフェースを導入済みのため、内部実装のみを純粋なCoroutinesに変換する。

## Technical Context

**Language/Version**: Kotlin 2.0.21 / JVM target 1.8
**Primary Dependencies**: Kotlin Coroutines 1.9.0, Hilt 2.51.1, Jetpack Compose (composeBom 2024.12.01)
**Storage**: Room Database 2.4.2 (SQLite)
**Testing**: JUnit 5 + kotlinx-coroutines-test (runTest), Kover for coverage
**Target Platform**: Android (minSdk TBD, targetSdk 34)
**Project Type**: Mobile (Android)
**Performance Goals**: 各テストが1秒以内に実行完了
**Constraints**: 既存の外部インターフェース互換性を維持、テストカバレッジ50%以上
**Scale/Scope**: 5つのバックグラウンド処理（データ同期、取引送信、バックアップ/リストア、DB初期化、バージョン検出）

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Pre-Planning Gate Check

| Principle | Status | Notes |
|-----------|--------|-------|
| I. コードの可読性とメンテナンス性 | ✅ PASS | 4つのパターンを1つに統一することでコードの理解が容易になる |
| II. テスト駆動開発 (TDD) | ✅ PASS | 各移行はテスト作成から開始。Fake実装が既に準備済み |
| III. 最小構築・段階的開発 | ✅ PASS | 複雑度順の段階的移行を採用（仕様で確定） |
| IV. パフォーマンス最適化 | ✅ PASS | パフォーマンスは副次目標。まずプロファイリングが必要な場合のみ対応 |
| V. アクセシビリティ | N/A | UI変更なし（Out of Scope） |
| VI. Kotlinコード標準 | ✅ PASS | Kotlin Coroutinesの採用は標準に完全準拠 |
| VII. Nix開発環境 | ✅ PASS | 既存のNix環境で開発可能 |
| VIII. 依存性注入 (Hilt) | ✅ PASS | 既存のUseCaseモジュールでDI設定済み |
| IX. 静的解析とリント | ✅ PASS | ktlint/detektのpre-commit hookが有効 |
| X. 階層型アーキテクチャ | ✅ PASS | UseCaseパターンに準拠。Repository経由でデータアクセス |

### Post-Design Gate Check (Phase 1完了)

- [x] データモデルが単方向データフローを維持
  - `SyncProgress` sealed classは Pending → Running → Completed/Error の単方向遷移
  - ViewModelは `StateFlow` で状態を公開、UIは収集のみ
- [x] すべてのUseCaseがsuspend/Flowを使用
  - 単純処理: `suspend fun execute(): Result<T>`
  - 進捗付き処理: `fun sync(): Flow<SyncProgress>`
- [x] テストでDispatcherを注入可能
  - `@IoDispatcher` qualifierで `CoroutineDispatcher` を注入
  - テストでは `UnconfinedTestDispatcher` を使用可能

## Project Structure

### Documentation (this feature)

```text
specs/014-async-pattern-unification/
├── plan.md              # This file
├── research.md          # Phase 0: 既存パターン調査結果
├── data-model.md        # Phase 1: 状態遷移・エンティティ定義
├── quickstart.md        # Phase 1: 開発者向け移行ガイド
├── contracts/           # Phase 1: UseCase インターフェース仕様
│   ├── transaction-syncer.md
│   ├── transaction-sender.md
│   ├── config-backup.md
│   ├── database-initializer.md
│   └── version-detector.md
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
app/src/main/kotlin/net/ktnx/mobileledger/
├── domain/                      # Domain Layer (UseCase)
│   └── usecase/
│       ├── TransactionSyncer.kt           # インターフェース（既存）
│       ├── TransactionSyncerImpl.kt       # 実装（移行対象）
│       ├── TransactionSender.kt           # インターフェース（既存）
│       ├── TransactionSenderImpl.kt       # 実装（移行対象）
│       ├── ConfigBackup.kt                # インターフェース（既存）
│       ├── ConfigBackupImpl.kt            # 実装（移行対象）
│       ├── DatabaseInitializer.kt         # インターフェース（既存）
│       ├── DatabaseInitializerImpl.kt     # 実装（移行完了）
│       ├── VersionDetector.kt             # インターフェース（既存）
│       └── VersionDetectorImpl.kt         # 実装（移行完了）
│
├── async/                       # Legacy async patterns (削除対象)
│   ├── RetrieveTransactionsTask.kt        # Thread継承（移行対象）
│   ├── SendTransactionTask.kt             # Thread継承（移行対象）
│   ├── GeneralBackgroundTasks.kt          # ExecutorService（移行対象）
│   └── TaskCallback.kt                    # コールバック（移行対象）
│
├── backup/                      # Backup/Restore (削除対象)
│   ├── ConfigIO.kt                        # Thread継承の基底クラス（移行対象）
│   ├── ConfigReader.kt                    # Thread継承（移行対象）
│   └── ConfigWriter.kt                    # Thread継承（移行対象）
│
├── ui/                          # UI Layer (変更なし、Out of Scope)
│   ├── main/
│   │   ├── MainViewModel.kt               # UseCase呼び出し（既存維持）
│   │   └── ...
│   └── ...
│
└── di/                          # Hilt DI Modules
    └── UseCaseModule.kt                   # UseCase DI（既存）

app/src/test/kotlin/net/ktnx/mobileledger/
├── domain/
│   └── usecase/
│       ├── TransactionSyncerImplTest.kt   # 新規追加
│       ├── TransactionSenderImplTest.kt   # 新規追加
│       ├── ConfigBackupImplTest.kt        # 新規追加
│       ├── DatabaseInitializerImplTest.kt # 既存
│       └── VersionDetectorImplTest.kt     # 既存
│
└── fake/                        # Fake implementations
    ├── FakeTransactionSyncer.kt           # 既存
    ├── FakeTransactionSender.kt           # 既存
    ├── FakeConfigBackup.kt                # 既存
    ├── FakeDatabaseInitializer.kt         # 既存
    └── FakeVersionDetector.kt             # 既存
```

**Structure Decision**: 既存のAndroidアーキテクチャ（domain/usecase、data/repository、ui/）を維持。
async/パッケージとbackup/パッケージのThread継承クラスを、domain/usecase/の純粋なCoroutines実装に移行する。
移行完了後、async/とbackup/パッケージは削除予定。

## Complexity Tracking

> 本機能は憲章に違反する設計判断を含まないため、このセクションは空欄。

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| (なし) | - | - |
