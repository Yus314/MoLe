# Implementation Plan: Complete Compose Migration

**Branch**: `007-complete-compose-migration` | **Date**: 2026-01-10 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/007-complete-compose-migration/spec.md`

## Summary

006-compose-ui-rebuildで残存したView/XMLベースのコンポーネント（ダイアログ、スプラッシュ画面、バックアップ画面、レガシーアダプター）を全てJetpack Composeに移行し、Viewベースのコードを完全に削除する。これによりコードベースの一貫性を向上させ、技術的負債を解消する。

## Technical Context

**Language/Version**: Kotlin 2.0.21 / Java 8互換 (JVM target 1.8)
**Primary Dependencies**:
- Jetpack Compose BOM 2024.12.01（006で導入済み）
- Material 3 (androidx.compose.material3)
- Compose Navigation 2.8.x
- Hilt 2.51.1（既存）
- Room 2.4.2（既存、変更なし）
- Coroutines 1.9.0（既存）

**Storage**: Room Database（既存、変更なし - FR-007）
**Testing**: JUnit 4, MockK, Compose Testing (ui-test-junit4)
**Target Platform**: Android minSdk 26, targetSdk 34
**Project Type**: Mobile (Android)
**Performance Goals**: 起動時間526ms±20%以内（SC-006）、60fps維持
**Constraints**: APKサイズ27MB±5%以内（SC-005）、既存テスト全通過（SC-004）
**Scale/Scope**: 3画面 + 3ダイアログ + 5アダプター削除、約15のComposable関数

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. コードの可読性とメンテナンス性 | ✅ PASS | Fragment/XML削除でコードベース簡素化 |
| II. テスト駆動開発 (TDD) | ✅ PASS | 既存テスト維持、新規Composeテスト追加 |
| III. 最小構築・段階的開発 | ✅ PASS | User Story優先度順（P1→P5）で段階的移行 |
| IV. パフォーマンス最適化 | ✅ PASS | 起動時間・APKサイズの目標値を設定 |
| V. アクセシビリティ | ✅ PASS | Composeの自動アクセシビリティサポート活用 |
| VI. Kotlinコード標準 | ✅ PASS | 100% Kotlin、コルーチン活用 |
| VII. Nix開発環境 | ✅ PASS | 既存のNix環境を継続使用 |
| VIII. 依存性注入 (Hilt) | ✅ PASS | hiltViewModel()でCompose対応 |
| IX. 静的解析とリント | ✅ PASS | ktlint/detektの既存設定を継続 |

## Project Structure

### Documentation (this feature)

```text
specs/007-complete-compose-migration/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # N/A (UI migration, no API changes)
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
app/src/main/kotlin/net/ktnx/mobileledger/
├── ui/                              # Compose UI層（006で構築済み）
│   ├── theme/                       # 既存: Material 3テーマ
│   ├── components/                  # 既存 + 新規追加
│   │   ├── DatePickerDialog.kt      # 新規: 日付選択ダイアログ
│   │   ├── CurrencyPickerDialog.kt  # 新規: 通貨選択ダイアログ
│   │   └── CrashReportDialog.kt     # 新規: クラッシュレポートダイアログ
│   ├── splash/                      # 新規: スプラッシュ画面
│   │   ├── SplashScreen.kt
│   │   └── SplashViewModel.kt
│   ├── backups/                     # 新規: バックアップ画面
│   │   ├── BackupsScreen.kt
│   │   ├── BackupsViewModel.kt
│   │   └── BackupsUiState.kt
│   └── main/                        # 既存: 拡張
│       └── ProfileListComposable.kt # 新規: ProfilesRecyclerViewAdapter置換
├── db/                              # 既存: 削除対象アダプター
│   ├── AccountAutocompleteAdapter.kt          # 削除
│   ├── AccountWithAmountsAutocompleteAdapter.kt # 削除
│   └── TransactionDescriptionAutocompleteAdapter.kt # 削除
└── ui/                              # 既存: 削除対象Fragment
    ├── DatePickerFragment.kt        # 削除
    ├── CurrencySelectorFragment.kt  # 削除
    ├── CurrencySelectorRecyclerViewAdapter.kt # 削除
    ├── QRScanCapableFragment.kt     # 削除
    ├── CrashReportDialogFragment.kt # 削除
    ├── profiles/
    │   └── ProfilesRecyclerViewAdapter.kt # 削除（Compose置換後）
    └── activity/
        ├── SplashActivity.kt        # Compose化
        └── BackupsActivity.kt       # Compose化

app/src/main/res/layout/              # 全ファイル削除（SC-001）
├── splash_activity_layout.xml       # 削除
├── fragment_backups.xml             # 削除
├── crash_dialog.xml                 # 削除
├── date_picker_view.xml             # 削除
├── fragment_currency_selector*.xml  # 削除
├── profile_list_content.xml         # 削除
├── account_autocomplete_row.xml     # 削除
└── hue_dialog.xml                   # 削除
```

**Structure Decision**: 006で確立したui/パッケージ構造を維持。新規画面はsplash/、backups/サブパッケージに追加。ダイアログはcomponents/に集約。レガシーコードは段階的に削除。

## Complexity Tracking

> 本移行では憲章違反は発生しない。006の延長として同じパターンを適用。

| 考慮事項 | 対応 |
|----------|------|
| ProfilesRecyclerViewAdapterの移行 | NavigationDrawer内でCompose LazyColumnに置換 |
| Fragment依存の完全削除 | 全DialogFragmentをCompose Dialogに置換 |
| XMLレイアウト完全削除 | app/src/main/res/layout/を空にする（SC-001） |

## Phase Overview

### Phase 1: ダイアログのCompose化 (US1 - P1)
- DatePickerFragmentをCompose DatePickerDialogに置換
- CurrencySelectorFragmentをCompose CurrencyPickerDialogに置換
- 関連XMLレイアウト削除

### Phase 2: スプラッシュ画面のCompose化 (US2 - P2)
- SplashActivityをCompose実装に置換
- 起動フローのテスト
- splash_activity_layout.xml削除

### Phase 3: バックアップ画面のCompose化 (US3 - P3)
- BackupsActivityをCompose実装に置換
- ViewBinding削除
- fragment_backups.xml削除

### Phase 4: レガシーアダプター削除 (US4 - P4)
- ProfilesRecyclerViewAdapterをCompose LazyColumnに置換
- 未使用アダプター（AccountAutocomplete系、CurrencySelector系）削除
- 関連XMLレイアウト削除

### Phase 5: クラッシュレポートダイアログ (US5 - P5)
- CrashReportDialogFragmentをCompose Dialogに置換
- QRScanCapableFragment削除
- 残存XMLレイアウト全削除
- SC-001〜SC-007の最終検証
