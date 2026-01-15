# Implementation Plan: Thread Wrapper to Coroutines Migration

**Branch**: `015-thread-wrapper-coroutines` | **Date**: 2026-01-15 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/015-thread-wrapper-coroutines/spec.md`

## Summary

Thread ベースの非同期処理を pure Coroutines に完全移行し、テスト容易性を向上させる。現在、`TransactionSyncerImpl`、`TransactionSenderImpl`、`ConfigBackupImpl` は Thread を wrap する形で Coroutines に対応しているが、内部では依然として `Thread.start()` + `suspendCancellableCoroutine` パターンを使用している。この移行では Thread クラスを完全に排除し、suspend 関数として再実装することで、TestDispatcher を使用した即座のテスト実行を可能にする。

## Technical Context

**Language/Version**: Kotlin 2.0.21 / JVM target 1.8
**Primary Dependencies**: Kotlin Coroutines 1.9.0, Hilt 2.51.1, Jetpack Compose (composeBom 2024.12.01)
**Storage**: Room Database 2.4.2 (SQLite)
**Testing**: JUnit 5, kotlinx-coroutines-test, Kover for coverage
**Target Platform**: Android (minSdk 23, targetSdk 34)
**Project Type**: Android mobile application
**Performance Goals**: テスト実行時間30%短縮、TestDispatcher による即時テスト完了
**Constraints**: 既存機能の破壊禁止、Fake実装の互換性維持
**Scale/Scope**: 6クラスのThread移行、5つのrunBlocking排除

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Pre-Design Check (Phase 0)

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Readability | ✅ PASS | Coroutines は Thread より宣言的で可読性向上 |
| II. TDD | ✅ PASS | 本機能の目的がテスト容易性向上、既存 Fake 維持 |
| III. Incremental Development | ✅ PASS | 段階的移行（P1→P2→P3）、機能ごとにコミット |
| IV. Performance | ✅ PASS | スレッド生成コスト削減、Coroutines の軽量性 |
| V. Accessibility | N/A | UI変更なし |
| VI. Kotlin Standards | ✅ PASS | Kotlin Coroutines 活用、GlobalScope禁止 |
| VII. Nix Environment | ✅ PASS | 既存 flake.nix で対応 |
| VIII. Hilt DI | ✅ PASS | CoroutineDispatcher 注入パターン継続 |
| IX. Static Analysis | ✅ PASS | ktlint/detekt 継続 |
| X. Layered Architecture | ✅ PASS | UseCase パターン継続、Repository 経由 |

### Post-Design Check (Phase 1)

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Readability | ✅ PASS | suspend関数は宣言的、Thread.run()より明確 |
| II. TDD | ✅ PASS | TestDispatcher注入でテスト容易、Fake互換維持 |
| III. Incremental Development | ✅ PASS | 3フェーズ移行、各コンポーネント独立コミット |
| IV. Performance | ✅ PASS | Thread生成オーバーヘッド削減、delay()は仮想時間 |
| V. Accessibility | N/A | UI変更なし |
| VI. Kotlin Standards | ✅ PASS | ensureActive()でキャンセル、viewModelScope使用 |
| VII. Nix Environment | ✅ PASS | 依存関係変更なし |
| VIII. Hilt DI | ✅ PASS | @IoDispatcher注入、TestDispatcher差し替え可能 |
| IX. Static Analysis | ✅ PASS | Coroutines専用lint有効化推奨 |
| X. Layered Architecture | ✅ PASS | UseCase実装のみ変更、インターフェース維持 |

**Post-Design Conclusion**: 全原則に適合。設計により以下が改善される:
- テスト実行時間: Thread.sleep() → delay() で TestDispatcher 時間操作可能
- コード量: Thread wrapper 削除により約500行削減見込み
- キャンセル処理: Thread.interrupt() → 構造化並行性による自動キャンセル

## Project Structure

### Documentation (this feature)

```text
specs/015-thread-wrapper-coroutines/
├── spec.md              # Feature specification (complete)
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output (interface contracts)
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output (API contracts)
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
app/src/main/kotlin/net/ktnx/mobileledger/
├── domain/
│   └── usecase/
│       ├── TransactionSyncer.kt          # Interface (既存)
│       ├── TransactionSyncerImpl.kt      # 移行対象: Thread wrapper → pure suspend
│       ├── TransactionSender.kt          # Interface (既存)
│       ├── TransactionSenderImpl.kt      # 移行対象: Thread wrapper → pure suspend
│       ├── ConfigBackup.kt               # Interface (既存)
│       ├── ConfigBackupImpl.kt           # 移行対象: Thread wrapper → pure suspend
│       ├── VersionDetector.kt            # Interface (既存)
│       └── VersionDetectorImpl.kt        # 移行対象: 新規 pure suspend 実装
├── async/
│   ├── RetrieveTransactionsTask.kt       # 削除対象: Thread 実装
│   ├── SendTransactionTask.kt            # 削除対象: Thread 実装 (deprecated)
│   ├── GeneralBackgroundTasks.kt         # 削除対象: Executor パターン
│   ├── TaskCallback.kt                   # 削除対象: Callback interface
│   └── parsers/                          # 維持: Parser ロジックを抽出
│       ├── TransactionParser.kt          # 新規: JSON/HTML パース suspend 関数
│       └── AccountParser.kt              # 新規: アカウントパース suspend 関数
├── backup/
│   ├── ConfigIO.kt                       # 削除対象: Thread 基底クラス
│   ├── ConfigWriter.kt                   # 移行対象: suspend 関数化
│   ├── ConfigReader.kt                   # 移行対象: suspend 関数化
│   ├── RawConfigWriter.kt                # 移行対象: runBlocking 排除
│   └── RawConfigReader.kt                # 移行対象: runBlocking 排除
├── dao/
│   ├── BaseDAO.kt                        # 移行対象: asyncRunner 削除
│   └── AsyncResultCallback.kt            # 削除対象: Callback interface
└── ui/
    ├── main/
    │   └── TransactionListViewModel.kt   # 移行対象: TransactionsDisplayedFilter
    └── profiles/
        └── ProfileDetailModel.kt         # 移行対象: VersionDetectionThread

app/src/test/kotlin/net/ktnx/mobileledger/
├── domain/usecase/
│   ├── TransactionSyncerImplTest.kt      # 新規: pure Coroutines テスト
│   ├── TransactionSenderImplTest.kt      # 新規: pure Coroutines テスト
│   └── ConfigBackupImplTest.kt           # 新規: pure Coroutines テスト
└── fake/
    ├── FakeTransactionSyncer.kt          # 維持: 互換性確保
    ├── FakeTransactionSender.kt          # 維持: 互換性確保
    └── FakeConfigBackup.kt               # 維持: 互換性確保
```

**Structure Decision**: 既存の階層型アーキテクチャを維持しつつ、`async/` パッケージの Thread 実装を `domain/usecase/` の pure suspend 実装に統合する。Parser ロジックは `async/parsers/` に分離して再利用可能にする。

## Migration Strategy

### Phase 1: Core UseCase 移行 (P1)

1. **TransactionSyncer の pure Coroutines 化**
   - `RetrieveTransactionsTask` のロジックを `TransactionSyncerImpl` に統合
   - `runBlocking` を排除し、全て suspend 関数として実装
   - Parser ロジックを `TransactionParser.kt` に分離
   - 既存 `FakeTransactionSyncer` との互換性維持

2. **TransactionSender の pure Coroutines 化**
   - `SendTransactionTask` のロジックを `TransactionSenderImpl` に統合
   - `Thread.sleep()` を `delay()` に置換
   - `TaskCallback` を排除し、`Result<Unit>` を返す suspend 関数に
   - 既存 `FakeTransactionSender` との互換性維持

### Phase 2: Backup/ViewModel 移行 (P2)

3. **ConfigBackup の pure Coroutines 化**
   - `ConfigIO`, `ConfigWriter`, `ConfigReader` を suspend 関数に統合
   - `RawConfigWriter`/`RawConfigReader` の `runBlocking` を排除
   - 既存 `FakeConfigBackup` との互換性維持

4. **ViewModel Thread 排除**
   - `TransactionsDisplayedFilter` → `viewModelScope.launch` + Flow
   - `VersionDetectionThread` → `VersionDetectorImpl` 統合

### Phase 3: レガシー削除 (P3)

5. **Executor パターン削除**
   - `BaseDAO.asyncRunner` 削除（既存 suspend DAO メソッド使用）
   - `GeneralBackgroundTasks` 削除
   - `TaskCallback`, `AsyncResultCallback` 削除

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| なし | - | - |

本機能は既存アーキテクチャに準拠し、複雑性を追加しない。
