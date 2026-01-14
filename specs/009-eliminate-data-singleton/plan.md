# Implementation Plan: Data.kt シングルトンの廃止

**Branch**: `009-eliminate-data-singleton` | **Date**: 2026-01-12 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/009-eliminate-data-singleton/spec.md`

## Summary

Data.kt（AppStateManager のエイリアス）シングルトンを廃止し、Hilt による依存性注入を使用した Repository パターンへ完全に移行する。プロファイル管理、バックグラウンドタスク管理、通貨フォーマット機能を段階的に専用のサービスクラスへ移行し、テスト容易性と関心の分離を向上させる。

### Technical Approach

1. **既存 Repository の活用**: ProfileRepository など既存の Repository は既に完全実装済み
2. **新規サービスの導入**: BackgroundTaskManager、CurrencyFormatter を新規作成
3. **段階的移行**: 読み取り専用操作から開始し、書き込み操作へ進む
4. **検証チェックポイント**: 各フェーズ完了後に全機能テスト実施

## Technical Context

**Language/Version**: Kotlin 2.0.21 / JVM target 1.8
**Primary Dependencies**: Hilt 2.51.1, Room 2.4.2, Jetpack Compose (composeBom 2024.12.01), Coroutines 1.9.0
**Storage**: Room Database (SQLite) - 既存、変更なし
**Testing**: JUnit + MockK + Compose UI Testing (`nix run .#test`)
**Target Platform**: Android (minSdk 26, targetSdk 34)
**Project Type**: Mobile (Android)
**Performance Goals**: アプリ起動時間リファクタリング前の 10% 以内維持
**Constraints**: 既存機能の 100% 動作維持、データベーススキーマ変更なし
**Scale/Scope**: 17 ファイルが Data. を参照、MainActivityCompose に 46 参照集中

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Pre-Phase 0 Gate Evaluation

| Principle | Status | Evidence |
|-----------|--------|----------|
| I. 可読性とメンテナンス性 | ✅ PASS | Repository パターンで責務分離、自己文書化 API |
| II. TDD | ✅ PASS | 新サービスにユニットテスト追加、既存テストの実行を各段階で検証 |
| III. 段階的開発 | ✅ PASS | 3 フェーズ構成、各フェーズで検証チェックポイント |
| IV. パフォーマンス | ✅ PASS | 起動時間 10% 以内維持を成功基準として測定 |
| V. アクセシビリティ | N/A | UI 変更なし |
| VI. Kotlin 標準 | ✅ PASS | Coroutines/Flow 使用、null 安全性活用 |
| VII. Nix 環境 | ✅ PASS | 既存の Nix 環境使用 |
| VIII. 依存性注入 | ✅ PASS | Hilt で全サービス注入、コンストラクタインジェクション |
| IX. 静的解析 | ✅ PASS | 既存の ktlint/detekt 継続使用 |
| X. 階層型アーキテクチャ | ✅ PASS | Repository パターン、単方向データフロー、UiState パターン |

**Gate Result**: ✅ PASS - 全原則準拠、Phase 0 へ進む

## Project Structure

### Documentation (this feature)

```text
specs/009-eliminate-data-singleton/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output - 技術調査結果
├── data-model.md        # Phase 1 output - データモデル定義
├── quickstart.md        # Phase 1 output - 実装着手ガイド
├── contracts/           # Phase 1 output - サービスインターフェース
│   ├── BackgroundTaskManager.kt      # バックグラウンドタスク管理
│   ├── CurrencyFormatter.kt          # 通貨フォーマット
│   └── AppStateService.kt            # UIステート管理
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
app/src/main/kotlin/net/ktnx/mobileledger/
├── di/                              # Hilt DI モジュール
│   ├── DatabaseModule.kt            # 既存 - DAO 提供
│   ├── RepositoryModule.kt          # 既存 - Repository 提供
│   ├── AppStateModule.kt            # 既存 - AppStateManager 提供（削除予定）
│   └── ServiceModule.kt             # 新規 - 新サービス提供
│
├── data/
│   └── repository/                  # 既存 Repository 実装
│       ├── ProfileRepository.kt     # ✅ 既存 - 完全実装済み
│       ├── TransactionRepository.kt # ✅ 既存 - 完全実装済み
│       ├── AccountRepository.kt     # ✅ 既存
│       ├── TemplateRepository.kt    # ✅ 既存
│       ├── CurrencyRepository.kt    # ✅ 既存
│       └── OptionRepository.kt      # ✅ 既存
│
├── service/                         # 新規サービス層
│   ├── BackgroundTaskManager.kt     # 新規 - バックグラウンドタスク管理
│   ├── CurrencyFormatter.kt         # 新規 - 通貨フォーマット
│   └── AppStateService.kt           # 新規 - UIステート管理
│
├── model/
│   └── AppStateManager.kt           # 既存 - 削除対象
│
└── ui/                              # UI Layer - 変更対象
    ├── activity/
    │   ├── MainActivityCompose.kt   # 要変更 - 46参照
    │   ├── NewTransactionActivityCompose.kt # 要変更
    │   ├── ProfileDetailActivity.kt # 要変更
    │   ├── BackupsActivity.kt       # 要変更
    │   └── ProfileThemedActivity.kt # 要変更
    └── main/
        └── MainViewModel.kt         # 要変更 - AppStateManager注入あり

app/src/test/kotlin/net/ktnx/mobileledger/
├── service/                         # 新規サービスのテスト
│   ├── BackgroundTaskManagerTest.kt
│   ├── CurrencyFormatterTest.kt
│   └── AppStateServiceTest.kt
└── ui/
    └── main/
        └── MainViewModelTest.kt     # ViewModelテスト拡充
```

**Structure Decision**: 既存の Android プロジェクト構成を維持。新規サービスは `service/` パッケージに配置し、DI モジュールで提供。Repository は既存の `data/repository/` を継続使用。

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| なし | - | - |

本リファクタリングは Constitution の全原則に準拠しており、複雑性の正当化は不要。

## Implementation Strategy

### フェーズ概要

```
Phase 1: プロファイル管理移行 (Read-Only First)
├── ProfileRepository.currentProfile への移行
├── Data.observeProfile() の廃止
└── 検証: プロファイル切り替えが動作

Phase 2: バックグラウンドタスク管理移行
├── BackgroundTaskManager サービス作成
├── RetrieveTransactionsTask の修正
├── MainActivityCompose の更新
└── 検証: データ同期が動作

Phase 3: 通貨フォーマット・UIステート移行
├── CurrencyFormatter サービス作成
├── AppStateService サービス作成
├── Data.kt (AppStateManager) 削除
└── 検証: 全機能動作、grep 結果ゼロ
```

### 検証チェックポイント

各フェーズ完了後に実行:

```bash
# 1. ユニットテスト
nix run .#test

# 2. ビルド・インストール
nix run .#verify

# 3. 手動検証
# - アプリ起動確認
# - プロファイル切り替え
# - データ同期トリガー
# - 取引一覧表示
```

### 成功基準 (spec.md より)

- **SC-001**: `Data.` / `AppStateManager.` への静的参照がゼロ件
- **SC-002**: 全既存ユニットテストがパス
- **SC-003**: 全ユーザー向け機能が同一に動作
- **SC-004**: 新 ViewModel がモック依存関係のみでテスト可能
- **SC-005**: Data.kt 削除後もビルド成功
- **SC-006**: 起動時間が 10% 以内維持

---

## Post-Design Constitution Check

*Re-evaluation after Phase 1 design completion*

### Post-Phase 1 Gate Evaluation

| Principle | Status | Evidence |
|-----------|--------|----------|
| I. 可読性とメンテナンス性 | ✅ PASS | 新サービスは単一責任を持ち、contracts/ でインターフェースを明確に定義 |
| II. TDD | ✅ PASS | contracts/ に定義したインターフェースに対してテスト可能、モック化容易 |
| III. 段階的開発 | ✅ PASS | 3 フェーズ構成で各フェーズが独立して検証可能、quickstart.md で手順明確化 |
| IV. パフォーマンス | ✅ PASS | StateFlow 使用で効率的な状態配信、不要な再計算を回避 |
| V. アクセシビリティ | N/A | UI 変更なし |
| VI. Kotlin 標準 | ✅ PASS | data class 使用、sealed class/enum 適切使用、null 安全性確保 |
| VII. Nix 環境 | ✅ PASS | 既存の Nix 環境で全テスト・ビルド実行可能 |
| VIII. 依存性注入 | ✅ PASS | 全新規サービスが @Singleton + @Inject constructor パターン、ServiceModule で一元管理 |
| IX. 静的解析 | ✅ PASS | contracts/ のコードは ktlint/detekt 準拠 |
| X. 階層型アーキテクチャ | ✅ PASS | Service 層を追加、ViewModel → Service/Repository の依存方向を維持 |

**Gate Result**: ✅ PASS - 設計完了、実装フェーズへ進む準備完了

### テスト容易性の検証

設計されたサービスは以下の点でテスト容易性を確保:

1. **インターフェース分離**: 全サービスがインターフェースを持ち、モック差し替え可能
2. **StateFlow 公開**: 状態がリアクティブに公開され、テストでの検証が容易
3. **副作用の分離**: 副作用（DB アクセス等）は Repository に委譲済み
4. **コンストラクタ注入**: 依存関係が明示的で、テスト時の差し替えが容易

### 関心の分離の検証

| 責務 | 配置 | 依存関係 |
|------|------|----------|
| データ永続化 | Repository (既存) | DAO |
| プロファイル状態 | ProfileRepository (既存) | ProfileDAO |
| タスク進捗管理 | BackgroundTaskManager (新規) | なし |
| 通貨フォーマット | CurrencyFormatter (新規) | なし |
| UI 共有状態 | AppStateService (新規) | なし |
| 画面状態 | ViewModel | Repository, Service |
| 画面表示 | Compose | ViewModel |

各コンポーネントが単一責任を持ち、依存方向が単方向（UI → Domain → Data）を維持している。

---

## Generated Artifacts

| Artifact | Path | Description |
|----------|------|-------------|
| plan.md | `/specs/009-eliminate-data-singleton/plan.md` | 実装計画 (本ファイル) |
| research.md | `/specs/009-eliminate-data-singleton/research.md` | 技術調査結果 |
| data-model.md | `/specs/009-eliminate-data-singleton/data-model.md` | データモデル定義 |
| quickstart.md | `/specs/009-eliminate-data-singleton/quickstart.md` | 実装着手ガイド |
| BackgroundTaskManager.kt | `/specs/009-eliminate-data-singleton/contracts/` | タスク管理契約 |
| CurrencyFormatter.kt | `/specs/009-eliminate-data-singleton/contracts/` | 通貨フォーマット契約 |
| AppStateService.kt | `/specs/009-eliminate-data-singleton/contracts/` | UI ステート管理契約 |

## Next Steps

1. `/speckit.tasks` コマンドで tasks.md を生成
2. Phase 1 のタスクから実装開始
3. 各フェーズ完了後に検証チェックポイント実施
