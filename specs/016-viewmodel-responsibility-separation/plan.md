# Implementation Plan: ViewModel 責務分離

**Branch**: `016-viewmodel-responsibility-separation` | **Date**: 2026-01-16 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/016-viewmodel-responsibility-separation/spec.md`

## Summary

肥大化した MainViewModel（830行）と NewTransactionViewModel（961行）を、単一責任原則に基づいた専門化 ViewModel に分離する内部リファクタリング。既存の専門化 ViewModel（ProfileSelectionViewModel、AccountSummaryViewModel、TransactionListViewModel、MainCoordinatorViewModel）にロジックを移行し、各 ViewModel を目安300行以下に収める。ProfileDetailModel は StateFlow ベースに移行する。

## Technical Context

**Language/Version**: Kotlin 2.0.21 / JVM target 1.8
**Primary Dependencies**: Hilt 2.51.1, Jetpack Compose (composeBom 2024.12.01), Coroutines 1.9.0, Room 2.4.2
**Storage**: Room Database (SQLite) - 既存、変更なし
**Testing**: JUnit 5, Kover for coverage, MainDispatcherRule, runTest, advanceUntilIdle()
**Target Platform**: Android (minSdk 22, targetSdk 34)
**Project Type**: Mobile (Android)
**Performance Goals**: UI操作は100ms以内で完了（既存要件継続）
**Constraints**: 分離により画面の動作や UI に変更が生じないこと（リファクタリングのみ）
**Scale/Scope**:
- MainViewModel: 830行 → 4つの専門化 ViewModel（各300行以下目標）へ移行、MainViewModel削除/最小化
- NewTransactionViewModel: 961行 → 3つの専門化 ViewModel（各300行以下目標）へ分離
- ProfileDetailModel: 574行 → StateFlow ベースに移行

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| 原則 | 状態 | 確認内容 |
|------|------|----------|
| I. コードの可読性とメンテナンス性 | ✅ PASS | 分離により各 ViewModel の責務が明確になり、可読性が向上する |
| II. テスト駆動開発 (TDD) | ✅ PASS | 分離後の各 ViewModel は独立してユニットテスト可能であり、既存の Fake 実装パターンを継続使用する |
| III. 最小構築・段階的開発 | ✅ PASS | P1（MainViewModel）→ P2（NewTransactionViewModel）→ P3（ProfileDetailModel）の段階的実装 |
| IV. パフォーマンス最適化 | ✅ PASS | リファクタリングのみで動作変更なし、パフォーマンス影響なし |
| V. アクセシビリティ | ✅ PASS | UI変更なしのため影響なし |
| VI. Kotlinコード標準 | ✅ PASS | StateFlow、Coroutines、data class を継続使用 |
| VII. Nix開発環境 | ✅ PASS | 既存環境で対応可能 |
| VIII. 依存性注入 (Hilt) | ✅ PASS | @HiltViewModel と @Inject constructor パターンを継続使用 |
| IX. 静的解析とリント | ✅ PASS | ktlint/detekt チェックを通過するコードを生成 |
| X. 階層型アーキテクチャ | ✅ PASS | ViewModel は UI Layer に属し、Repository 経由でデータアクセス（既存パターン継続） |

## Project Structure

### Documentation (this feature)

```text
specs/016-viewmodel-responsibility-separation/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
# Android Mobile Application
app/src/main/kotlin/net/ktnx/mobileledger/
├── ui/                      # UI Layer (ViewModel分離対象)
│   ├── main/                # メイン画面
│   │   ├── MainScreen.kt                    # Compose UI
│   │   ├── MainViewModel.kt                 # 830行 → 削除/最小化
│   │   ├── MainUiState.kt                   # 統合状態
│   │   ├── ProfileSelectionViewModel.kt     # 138行 (既存、移行先)
│   │   ├── ProfileSelectionUiState.kt       # プロファイル選択状態
│   │   ├── AccountSummaryViewModel.kt       # 258行 (既存、移行先)
│   │   ├── AccountSummaryUiState.kt         # アカウント一覧状態
│   │   ├── TransactionListViewModel.kt      # 423行 (既存、移行先)
│   │   ├── TransactionListUiState.kt        # 取引一覧状態
│   │   ├── MainCoordinatorViewModel.kt      # 307行 (既存、移行先+同期責務)
│   │   └── MainCoordinatorUiState.kt        # UI調整状態
│   ├── transaction/         # 取引登録画面
│   │   ├── NewTransactionScreen.kt          # Compose UI
│   │   ├── NewTransactionViewModel.kt       # 961行 → 分離対象
│   │   ├── NewTransactionUiState.kt         # 取引登録状態
│   │   ├── TransactionFormViewModel.kt      # 新規 (フォーム+送信)
│   │   ├── TransactionFormUiState.kt        # 新規
│   │   ├── AccountRowsViewModel.kt          # 新規 (行管理+通貨)
│   │   ├── AccountRowsUiState.kt            # 新規
│   │   ├── TemplateApplicatorViewModel.kt   # 新規 (テンプレート)
│   │   └── TemplateApplicatorUiState.kt     # 新規
│   └── profile/             # プロファイル画面
│       ├── ProfileDetailViewModel.kt        # 574行 → StateFlow移行
│       └── ProfileDetailUiState.kt          # プロファイル詳細状態
├── data/                    # Data Layer (変更なし)
│   └── repository/          # Repository実装
└── di/                      # Hilt DI モジュール (変更なし)

app/src/test/kotlin/net/ktnx/mobileledger/
├── ui/
│   ├── main/
│   │   ├── MainViewModelTest.kt             # 既存 → 移行先VM用に分割
│   │   ├── ProfileSelectionViewModelTest.kt # 拡充
│   │   ├── AccountSummaryViewModelTest.kt   # 拡充
│   │   ├── TransactionListViewModelTest.kt  # 拡充
│   │   ├── MainCoordinatorViewModelTest.kt  # 拡充
│   │   └── TestFakes.kt                     # 既存 Fake 実装
│   └── transaction/
│       ├── NewTransactionViewModelTest.kt   # 既存 → 分離VMごとにテスト
│       ├── TransactionFormViewModelTest.kt  # 新規
│       ├── AccountRowsViewModelTest.kt      # 新規
│       └── TemplateApplicatorViewModelTest.kt # 新規
└── fake/                    # Fake 実装 (既存継続)
```

**Structure Decision**: 既存の Android Mobile Application 構造を維持し、ui/main/ と ui/transaction/ 内の ViewModel ファイルを分離・拡充する。新規 ViewModel と対応する UiState ファイルを追加する。テストは対応する test/ ディレクトリに配置する。

## Complexity Tracking

> 該当なし - Constitution Check にて全項目 PASS

## Constitution Check (Post-Design)

*Re-evaluated after Phase 1 design completion*

| 原則 | 状態 | 再確認内容 |
|------|------|----------|
| I. コードの可読性とメンテナンス性 | ✅ PASS | data-model.md で各 ViewModel の責務を明確に定義。各 ViewModel は150-400行の範囲で単一責務を担当 |
| II. テスト駆動開発 (TDD) | ✅ PASS | quickstart.md に TDD サイクルを文書化。既存の Fake 実装パターンを継続使用 |
| III. 最小構築・段階的開発 | ✅ PASS | P1→P2→P3 の段階的実装計画を維持。各フェーズでこまめなコミットを実施 |
| IV. パフォーマンス最適化 | ✅ PASS | StateFlow と viewModelScope.launch を使用し、既存のパフォーマンス特性を維持 |
| V. アクセシビリティ | ✅ PASS | UI 変更なし（リファクタリングのみ） |
| VI. Kotlinコード標準 | ✅ PASS | StateFlow、sealed class、data class を活用した設計 |
| VII. Nix開発環境 | ✅ PASS | 既存環境で対応可能、追加依存なし |
| VIII. 依存性注入 (Hilt) | ✅ PASS | contracts/viewmodel-interfaces.md で各 ViewModel の依存関係を明確に定義 |
| IX. 静的解析とリント | ✅ PASS | pre-commit hook で ktlint/detekt チェックを継続 |
| X. 階層型アーキテクチャ | ✅ PASS | ViewModel は UI Layer のみを担当、Repository 経由でデータアクセス（DAO 直接アクセスなし） |

---

## 生成済み成果物

| 成果物 | パス | 説明 |
|--------|------|------|
| plan.md | `/specs/016-viewmodel-responsibility-separation/plan.md` | 本ファイル |
| research.md | `/specs/016-viewmodel-responsibility-separation/research.md` | ViewModel 分離パターンの調査結果 |
| data-model.md | `/specs/016-viewmodel-responsibility-separation/data-model.md` | ViewModel エンティティと UiState 定義 |
| quickstart.md | `/specs/016-viewmodel-responsibility-separation/quickstart.md` | 開発ガイド |
| viewmodel-interfaces.md | `/specs/016-viewmodel-responsibility-separation/contracts/viewmodel-interfaces.md` | ViewModel 間のインターフェース契約 |

---

## 次のステップ

`/speckit.tasks` コマンドを実行して `tasks.md` を生成してください。
