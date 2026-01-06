# Implementation Plan: Jetpack Compose UI Rebuild

**Branch**: `006-compose-ui-rebuild` | **Date**: 2026-01-06 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/006-compose-ui-rebuild/spec.md`

## Summary

既存のXML/View Binding実装からJetpack Composeへの段階的UI移行。4つのPhaseに分けて実施し、各Phase完了後に既存XMLを削除する。Material 3テーマを採用しつつ現行の外観を維持し、StateFlow/Flowベースの状態管理に移行する。

## Technical Context

**Language/Version**: Kotlin 2.0.21 / Java 8互換 (JVM target 1.8)
**Primary Dependencies**:
- Jetpack Compose BOM 2024.12.01（最新安定版）
- Material 3 (androidx.compose.material3)
- Compose Navigation 2.8.x
- Hilt 2.51.1（既存）
- Room 2.4.2（既存）
- Coroutines 1.9.0（既存）

**Storage**: Room Database（既存、変更なし）
**Testing**: JUnit 4, MockK, Compose Testing (ui-test-junit4)
**Target Platform**: Android minSdk 26, targetSdk 34
**Project Type**: Mobile (Android)
**Performance Goals**: 60fps維持、スクロール遅延なし、起動時間±200ms以内
**Constraints**: APKサイズ増加10%以内、既存テスト通過必須
**Scale/Scope**: 6画面（4 Phase）、約20のComposable関数

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. コードの可読性とメンテナンス性 | ✅ PASS | Composeの宣言的UIは可読性向上に貢献 |
| II. テスト駆動開発 (TDD) | ✅ PASS | Compose UIテストフレームワークを使用 |
| III. 最小構築・段階的開発 | ✅ PASS | 4 Phaseに分けて段階的に移行 |
| IV. パフォーマンス最適化 | ✅ PASS | 60fps目標、LazyColumnで効率的リスト表示 |
| V. アクセシビリティ | ✅ PASS | Composeは自動的にアクセシビリティサポート提供 |
| VI. Kotlinコード標準 | ✅ PASS | 100% Kotlin、コルーチン活用 |
| VII. Nix開発環境 | ✅ PASS | 既存のNix環境を継続使用 |
| VIII. 依存性注入 (Hilt) | ✅ PASS | hiltViewModel()でCompose対応 |
| IX. 静的解析とリント | ✅ PASS | ktlint/detektの既存設定を継続 |

## Project Structure

### Documentation (this feature)

```text
specs/006-compose-ui-rebuild/
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
├── ui/                              # 新規: Compose UI層
│   ├── theme/                       # Material 3テーマ定義
│   │   ├── Theme.kt                 # MoLeTheme Composable
│   │   ├── Color.kt                 # カラーパレット
│   │   ├── Type.kt                  # Typography定義
│   │   └── ProfileTheme.kt          # HSLベースの動的テーマ
│   ├── components/                  # 再利用可能なコンポーネント
│   │   ├── HueRing.kt               # カスタムHueRingピッカー
│   │   ├── LoadingIndicator.kt      # ローディング表示
│   │   ├── ErrorSnackbar.kt         # エラー表示
│   │   └── ConfirmDialog.kt         # 確認ダイアログ
│   ├── profile/                     # Phase 1: プロファイル画面
│   │   ├── ProfileDetailScreen.kt
│   │   └── ProfileDetailViewModel.kt # 既存ViewModelのCompose対応版
│   ├── templates/                   # Phase 2: テンプレート画面
│   │   ├── TemplateListScreen.kt
│   │   ├── TemplateDetailScreen.kt
│   │   └── TemplatesNavigation.kt
│   ├── main/                        # Phase 3: メイン画面
│   │   ├── MainScreen.kt
│   │   ├── AccountSummaryTab.kt
│   │   ├── TransactionListTab.kt
│   │   └── NavigationDrawer.kt
│   └── transaction/                 # Phase 4: 取引登録画面
│       ├── NewTransactionScreen.kt
│       ├── TransactionRowItem.kt
│       └── AccountAutocomplete.kt
├── model/                           # 既存: データモデル（変更なし）
├── db/                              # 既存: Room DB（変更なし）
├── di/                              # 既存: Hiltモジュール
│   └── ComposeModule.kt             # 新規: Compose用追加提供
└── async/                           # 既存: 非同期処理（変更なし）

app/src/test/kotlin/net/ktnx/mobileledger/
└── ui/                              # Compose UIテスト
    ├── theme/
    ├── components/
    ├── profile/
    ├── templates/
    ├── main/
    └── transaction/

app/src/androidTest/kotlin/net/ktnx/mobileledger/
└── ui/                              # Compose Instrumentation Tests
```

**Structure Decision**: 既存のプロジェクト構造を維持しつつ、`ui/`パッケージ配下にCompose関連コードを集約。既存のmodel/db/async層は変更なし。

## Complexity Tracking

> 本移行では憲章違反は発生しない。段階的移行アプローチにより複雑性を管理。

| 考慮事項 | 対応 |
|----------|------|
| Compose/XML共存期間 | 各Phase完了後にXML削除で技術的負債を解消 |
| Material 3への移行 | HSLベーステーマを保持しつつM3に対応 |
| テスト戦略 | 既存テスト維持 + 新規Composeテスト追加 |

## キーボード対応方針

spec.md Edge Case「キーボード表示時、入力フィールドが適切にスクロールされる」に対応するための実装方針。

### 実装ルール

| 対象 | 実装方法 |
|------|----------|
| フォーム画面全体 | `Scaffold`の`contentWindowInsets`に`WindowInsets.ime`を設定 |
| スクロール可能領域 | `Modifier.imePadding()`を適用 |
| フォーカス時の可視性 | `BringIntoViewRequester`で入力フィールドを画面内にスクロール |
| Activity設定 | `android:windowSoftInputMode="adjustResize"`を維持 |

### 適用対象画面

- **ProfileDetailScreen** (Phase 1): フォーム入力あり
- **TemplateDetailScreen** (Phase 2): フォーム入力あり
- **NewTransactionScreen** (Phase 4): 動的フォーム、最も複雑

### 共通パターン

```kotlin
// すべてのフォーム画面で使用
Scaffold(
    contentWindowInsets = WindowInsets.ime
) { innerPadding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .imePadding()
    ) {
        // フォームコンテンツ
    }
}
```

## Phase Overview

### Phase 1: 基盤構築 + ProfileDetailActivity
- Compose BOM/依存関係追加
- Material 3テーマ基盤構築
- HueRingカスタムComposable実装
- ProfileDetailScreen実装
- 検証後、既存ProfileDetailFragment/XML削除

### Phase 2: TemplatesActivity
- TemplateListScreen実装
- TemplateDetailScreen実装
- Compose Navigation適用
- 検証後、既存Templates関連Fragment/XML削除

### Phase 3: MainActivity
- MainScreen（タブ構成）実装
- AccountSummaryTab実装
- TransactionListTab実装
- NavigationDrawer実装
- 検証後、既存MainActivity関連Fragment/XML削除

### Phase 4: NewTransactionActivity
- NewTransactionScreen実装
- 動的フォーム（行追加/削除）実装
- AccountAutocomplete実装
- 検証後、既存NewTransaction関連Fragment/XML削除
