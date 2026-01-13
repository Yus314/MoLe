# Implementation Plan: クリティカルコンポーネントのテストカバレッジ向上

**Branch**: `011-test-coverage` | **Date**: 2026-01-13 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/011-test-coverage/spec.md`

## Summary

クリティカルな ViewModel (MainViewModel 806行, NewTransactionViewModel 985行) のテストカバレッジを 9% から 70%+ に向上させる。既存の Repository パターンと Hilt DI を活用し、テスト困難な依存関係（App シングルトン、Thread クラス）を注入可能な抽象化に置き換える。TDD アプローチでリファクタリングとテスト追加を段階的に実施。

## Technical Context

**Language/Version**: Kotlin 2.0.21 (JVM target 1.8)
**Primary Dependencies**: Hilt 2.51.1, Jetpack Compose (composeBom 2024.12.01), Room 2.4.2, Coroutines 1.9.0
**Storage**: Room Database (SQLite) - 既存、変更なし
**Testing**: JUnit 4, MockK, Turbine, kotlinx-coroutines-test (StandardTestDispatcher)
**Target Platform**: Android (minSdk 26, targetSdk 34)
**Project Type**: Mobile (Android)
**Performance Goals**: ユニットテスト実行時間 1秒以内/テストファイル、合計30秒以内
**Constraints**: Androidフレームワーク依存ゼロ（ユニットテスト）、既存機能への影響なし
**Scale/Scope**: MainViewModel 806行 (70%+)、NewTransactionViewModel 985行 (70%+)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### ゲート評価（Phase 0 前）

| 原則 | 準拠状況 | 対応 |
|------|----------|------|
| I. コードの可読性とメンテナンス性 | ✅ 準拠 | リファクタリングで可読性向上 |
| II. テスト駆動開発 (TDD) | ✅ 準拠 | TDD サイクルで実装 |
| III. 最小構築・段階的開発 | ✅ 準拠 | 段階的リファクタリング |
| IV. パフォーマンス最適化 | ✅ 準拠 | テスト実行時間目標設定 |
| V. アクセシビリティ | N/A | UIテストはスコープ外 |
| VI. Kotlinコード標準 | ✅ 準拠 | Coroutines 移行、!!演算子回避 |
| VII. Nix開発環境 | ✅ 準拠 | nix run .#test で実行 |
| VIII. 依存性注入 (Hilt) | ✅ 準拠 | 新しい抽象化も DI で提供 |
| IX. 静的解析とリント | ✅ 準拠 | pre-commit フックで自動チェック |
| X. 階層型アーキテクチャ | ✅ 準拠 | Repository パターン活用 |

**ゲート結果**: ✅ PASS - Phase 0 研究を開始可能

## Project Structure

### Documentation (this feature)

```text
specs/011-test-coverage/
├── plan.md              # This file
├── research.md          # Phase 0 output - 技術調査結果
├── data-model.md        # Phase 1 output - テスト抽象化設計
├── quickstart.md        # Phase 1 output - テスト実行ガイド
├── contracts/           # Phase 1 output - インターフェース定義
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
app/src/
├── main/kotlin/net/ktnox/mobileledger/
│   ├── data/repository/          # 既存 Repository
│   │   ├── PreferencesRepository.kt      # 既存（App シングルトン置換用）
│   │   └── PreferencesRepositoryImpl.kt  # 既存（SharedPreferences 実装）
│   ├── di/                       # Hilt DI モジュール
│   │   ├── RepositoryModule.kt   # 既存（PreferencesRepository 提供済み）
│   │   └── ServiceModule.kt      # 既存（BackgroundTaskManager 等）
│   ├── async/                    # 非同期タスク
│   │   ├── SendTransactionTask.kt        # Thread → Coroutine 移行対象
│   │   └── RetrieveTransactionsTask.kt   # Thread → Coroutine 移行対象（P2）
│   ├── domain/                   # ドメイン層
│   │   └── usecase/              # UseCase（必要に応じて追加）
│   │       └── TransactionSender.kt      # 新規：取引送信抽象化
│   └── ui/
│       ├── main/
│       │   └── MainViewModel.kt          # リファクタリング対象
│       └── transaction/
│           └── NewTransactionViewModel.kt # リファクタリング対象
│
├── test/kotlin/net/ktnox/mobileledger/
│   ├── ui/main/
│   │   ├── MainViewModelTest.kt          # 新規テスト
│   │   └── TestFakes.kt                  # 既存 Fake 拡張
│   └── ui/transaction/
│       ├── NewTransactionViewModelTest.kt # 新規/拡張テスト
│       └── TestFakes.kt                   # 新規 Fake 追加
│
└── androidTest/kotlin/  # インストルメンテーションテスト（スコープ外）
```

**Structure Decision**: 既存の階層型アーキテクチャを維持。新しい抽象化（TransactionSender）は domain/usecase/ に配置し、既存の Repository パターンと一貫性を保つ。

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| TransactionSender 抽象化 | SendTransactionTask の直接インスタンス化がテスト不可 | 直接モック不可（Thread 継承クラス） |
| PreferencesRepository 注入追加 | App.getShowZeroBalanceAccounts() がフレームワーク依存 | 静的メソッドはモック困難 |

## Current State Analysis

### テスト対象コンポーネントの依存関係

#### MainViewModel (806行) - テストブロッカー

| 依存関係 | 現状 | 問題 | 解決策 |
|----------|------|------|--------|
| ProfileRepository | ✅ 注入済み | なし | Fake 使用 |
| AccountRepository | ✅ 注入済み | なし | Fake 使用 |
| TransactionRepository | ✅ 注入済み | なし | Fake 使用 |
| OptionRepository | ✅ 注入済み | なし | Fake 使用 |
| BackgroundTaskManager | ✅ 注入済み | なし | Fake 使用 |
| AppStateService | ✅ 注入済み | なし | Fake 使用 |
| **App.getShowZeroBalanceAccounts()** | ❌ 静的参照 | フレームワーク依存 | PreferencesRepository 注入 |
| **App.storeShowZeroBalanceAccounts()** | ❌ 静的参照 | フレームワーク依存 | PreferencesRepository 注入 |
| **TransactionsDisplayedFilter** | ❌ Thread 継承 | 非同期テスト困難 | Coroutine 移行 |

#### NewTransactionViewModel (985行) - テストブロッカー

| 依存関係 | 現状 | 問題 | 解決策 |
|----------|------|------|--------|
| ProfileRepository | ✅ 注入済み | なし | Fake 使用 |
| TransactionRepository | ✅ 注入済み | なし | Fake 使用 |
| AccountRepository | ✅ 注入済み | なし | Fake 使用 |
| TemplateRepository | ✅ 注入済み | なし | Fake 使用 |
| CurrencyRepository | ✅ 注入済み | なし | Fake 使用 |
| CurrencyFormatter | ✅ 注入済み | なし | Fake/Mock 使用 |
| AppStateService | ✅ 注入済み | なし | Fake 使用 |
| **SendTransactionTask** | ❌ 直接インスタンス化 | ネットワーク依存 | TransactionSender 注入 |

### 既存テストインフラ

**テスト依存関係**: ✅ 完備
- JUnit 4
- MockK
- Turbine (Flow テスト)
- kotlinx-coroutines-test

**Fake 実装**: ✅ 350行の既存 Fake (TestFakes.kt)
- FakeProfileRepositoryForViewModel
- FakeTransactionRepositoryForViewModel
- FakeAccountRepositoryForViewModel
- FakeOptionRepositoryForViewModel
- FakeBackgroundTaskManagerForViewModel
- FakeAppStateServiceForViewModel

**不足**: FakePreferencesRepository, FakeTransactionSender

## Risk Analysis

| リスク | 影響 | 緩和策 |
|--------|------|--------|
| リファクタリングによる既存機能の破損 | 高 | 段階的移行、各段階でテスト実行 |
| Coroutine 移行での動作変更 | 中 | 同一の振る舞いを維持するテスト作成 |
| テスト実行時間の増加 | 低 | 目標 30秒以内を監視 |
| PreferencesRepository 注入の欠落 | 低 | 既に RepositoryModule で提供済み |

## Constitution Check（Phase 1 後）

*Re-check after Phase 1 design.*

### ゲート評価（Phase 1 後）

| 原則 | 準拠状況 | 詳細 |
|------|----------|------|
| I. コードの可読性とメンテナンス性 | ✅ 準拠 | TransactionSender インターフェースは明確なKDoc付き、Fake実装は単純明快 |
| II. テスト駆動開発 (TDD) | ✅ 準拠 | テストファースト: リファクタリング前にテスト作成、Red-Green-Refactor サイクル |
| III. 最小構築・段階的開発 | ✅ 準拠 | 4段階の実装フェーズ（MainViewModel → NewTransactionViewModel → P2/P3） |
| IV. パフォーマンス最適化 | ✅ 準拠 | StandardTestDispatcher で時間待機なし、テスト実行 30秒以内目標 |
| V. アクセシビリティ | N/A | UIテストはスコープ外 |
| VI. Kotlinコード標準 | ✅ 準拠 | suspend関数、Result型、!!演算子なし |
| VII. Nix開発環境 | ✅ 準拠 | nix run .#test でテスト実行可能 |
| VIII. 依存性注入 (Hilt) | ✅ 準拠 | TransactionSender は UseCaseModule でバインド、テストでは Fake を直接注入 |
| IX. 静的解析とリント | ✅ 準拠 | 新規コードも pre-commit フックでチェック |
| X. 階層型アーキテクチャ | ✅ 準拠 | TransactionSender は domain/usecase 配置、Repository パターン維持 |

**ゲート結果**: ✅ PASS - Phase 2 タスク生成へ進行可能

## Generated Artifacts

Phase 0/1 で生成された成果物:

| ファイル | 説明 | 状態 |
|----------|------|------|
| [research.md](./research.md) | 技術調査結果 | ✅ Complete |
| [data-model.md](./data-model.md) | インターフェース設計、Fake 設計 | ✅ Complete |
| [quickstart.md](./quickstart.md) | テスト実行ガイド | ✅ Complete |
| [contracts/TransactionSender.kt](./contracts/TransactionSender.kt) | TransactionSender インターフェース | ✅ Complete |

## Next Steps

1. `/speckit.tasks` コマンドで tasks.md を生成
2. 各タスクを TDD サイクルで実装:
   - Phase 1: MainViewModel テスト可能化
   - Phase 2: NewTransactionViewModel テスト可能化
   - Phase 3: バックグラウンド同期テスト（P2）
   - Phase 4: カバレッジ測定設定（P3）
