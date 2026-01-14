# Implementation Plan: TransactionAccumulator テスト可能性向上

**Branch**: `012-accumulator-testability` | **Date**: 2026-01-14 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/012-accumulator-testability/spec.md`

## Summary

TransactionAccumulator クラスの `App.currencyFormatter()` への静的グローバルアクセスをコンストラクタインジェクションに置き換え、Android アプリケーションコンテキストなしでのユニットテストを可能にする。既存の `CurrencyFormatter` インターフェース、`FakeCurrencyFormatter` テスト実装、および Hilt DI セットアップを活用する。

## Technical Context

**Language/Version**: Kotlin 2.0.21 / JVM target 1.8
**Primary Dependencies**: Hilt 2.51.1, Jetpack Compose (composeBom 2024.12.01), Coroutines 1.9.0
**Storage**: Room 2.4.2 (既存、本機能では変更なし)
**Testing**: JUnit 4, kotlinx-coroutines-test, StandardTestDispatcher, MainDispatcherRule
**Target Platform**: Android (minSdk 21+)
**Project Type**: Mobile (Android)
**Performance Goals**: ユニットテストは 100ms 以内で完了
**Constraints**: 既存の通貨フォーマット出力を変更しない
**Scale/Scope**: TransactionAccumulator の 2 箇所のインスタンス化を更新

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| 原則 | 状態 | 判定 |
|------|------|------|
| I. コードの可読性とメンテナンス性 | コンストラクタインジェクションで依存関係を明示 | ✅ PASS |
| II. テスト駆動開発 | TDD サイクルでテストを先に作成 | ✅ PASS |
| III. 最小構築・段階的開発 | 単一責務のリファクタリング、機能変更なし | ✅ PASS |
| IV. パフォーマンス最適化 | 変更なし（パフォーマンス影響なし） | ✅ PASS |
| V. アクセシビリティ | UI 変更なし | ✅ N/A |
| VI. Kotlin コード標準 | Kotlin のモダンな機能を使用 | ✅ PASS |
| VII. Nix 開発環境 | 既存環境を使用 | ✅ PASS |
| VIII. 依存性注入 (Hilt) | コンストラクタインジェクションを使用 | ✅ PASS |
| IX. 静的解析とリント | pre-commit フックで自動チェック | ✅ PASS |
| X. 階層型アーキテクチャ | DI パターンに準拠 | ✅ PASS |

**ゲート判定**: ✅ 全原則に準拠。Phase 0 に進行可能。

## Project Structure

### Documentation (this feature)

```text
specs/012-accumulator-testability/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
app/src/main/kotlin/net/ktnx/mobileledger/
├── async/
│   └── TransactionAccumulator.kt    # 変更対象: コンストラクタに CurrencyFormatter 追加
├── ui/main/
│   ├── MainViewModel.kt             # 変更対象: CurrencyFormatter を注入して渡す
│   └── TransactionListViewModel.kt  # 確認対象: TransactionAccumulator 使用有無
├── service/
│   ├── CurrencyFormatter.kt         # 変更なし: 既存インターフェース
│   └── CurrencyFormatterImpl.kt     # 変更なし: 既存実装
├── di/
│   └── ServiceModule.kt             # 変更なし: 既存 Hilt バインディング
└── App.kt                           # 変更なし: 静的メソッドは残す（後方互換）

app/src/test/kotlin/net/ktnx/mobileledger/
├── fake/
│   └── FakeCurrencyFormatter.kt     # 変更なし: 既存テスト実装
├── async/
│   └── TransactionAccumulatorTest.kt # 新規: ユニットテスト
└── util/
    └── MainDispatcherRule.kt        # 変更なし: 既存テストルール
```

**Structure Decision**: 既存の階層構造を維持し、テストファイルを `async/` パッケージに配置

## Complexity Tracking

> **Constitution Check に違反がないため、このセクションは空**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| - | - | - |
