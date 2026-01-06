# タスク: Kotlin 2.0.21 アップデート

**入力**: `/specs/003-kotlin-update/` の設計ドキュメント
**前提条件**: plan.md（必須）、spec.md（ユーザーストーリー用、必須）、research.md、quickstart.md

**テスト**: この機能仕様ではテストの追加は明示的に要求されていない。既存テストをリグレッションテストとして使用する。

**構成**: タスクはユーザーストーリーごとにグループ化し、各ストーリーの独立した実装とテストを可能にする。

## フォーマット: `[ID] [P?] [Story] 説明`

- **[P]**: 並列実行可能（異なるファイル、依存関係なし）
- **[Story]**: このタスクが属するユーザーストーリー（例: US1, US2, US3）
- 説明には正確なファイルパスを含める

## パス規約

- **Android モバイル**: `app/src/main/kotlin/`、`gradle/libs.versions.toml`、`app/build.gradle`

---

## Phase 1: セットアップ（Version Catalog & ビルド設定）

**目的**: ビルド設定の Kotlin および依存関係バージョンを更新

- [X] T001 Kotlin バージョン 1.9.25 → 2.0.21 を `gradle/libs.versions.toml` で更新
- [X] T002 Coroutines バージョン 1.7.3 → 1.9.0 を `gradle/libs.versions.toml` で更新
- [X] T003 `app/build.gradle` で kotlinOptions を compilerOptions に移行

---

## Phase 2: 基盤（ビルド検証）

**目的**: コード変更前に新しい Kotlin バージョンでビルドが成功することを確認

**⚠️ 重要**: ビルドが通るまで deprecation 修正やコード近代化は開始不可

- [X] T004 `nix run .#build` を実行し、K2 コンパイラエラーを特定
- [X] T005 K2 breaking changes を修正（star import 競合、nullable 代入など）
- [X] T006 `nix run .#test` を実行し、既存ユニットテストがパスすることを確認
- [X] T007 Room + kapt の互換性を確認（metadata エラー発生時は Room 2.6.1 へアップグレード）

**チェックポイント**: Kotlin 2.0.21 でビルドとテストがパス - deprecation 修正を開始可能

---

## Phase 3: User Story 1 - 更新された Kotlin でビルド (Priority: P1) 🎯 MVP

**ゴール**: プロジェクトが Kotlin 2.0.21 でビルド・テストをパスし、deprecation 警告ゼロ

**独立テスト**: `nix run .#build` が成功、`nix run .#test` がパス、deprecation 警告なし

### User Story 1 の実装

#### 非推奨 API の修正（検索＆置換）

- [X] T008 [P] [US1] `appendln()` → `appendLine()` を `app/src/main/kotlin/` 配下の全 Kotlin ファイルで置換（該当なし）
- [X] T009 [P] [US1] `toUpperCase()` → `uppercase()` を `app/src/main/kotlin/` 配下の全 Kotlin ファイルで置換（該当なし）
- [X] T010 [P] [US1] `toLowerCase()` → `lowercase()` を `app/src/main/kotlin/` 配下の全 Kotlin ファイルで置換（該当なし）

#### ビルド設定の deprecation 修正

- [X] T011 [US1] `app/build.gradle` の残りの kotlinOptions deprecation 警告を修正（警告なし、現行設定維持）

#### 検証

- [X] T012 [US1] `nix run .#build` を実行し、deprecation 警告がゼロであることを確認
- [X] T013 [US1] `nix run .#test` を実行し、全ユニットテストがパスすることを確認

**チェックポイント**: 警告ゼロでビルド成功、既存テスト全パス

---

## Phase 4: User Story 2 - アプリケーション動作の保持 (Priority: P1)

**ゴール**: すべてのアプリ機能がアップデート前と同一に動作

**独立テスト**: `nix run .#verify` が成功、実機での主要機能の手動検証

### User Story 2 の実装

- [X] T014 [US2] `nix run .#verify` を実行（フル検証ワークフロー）- テスト・ビルド・インストール成功
- [X] T015 [US2] 手動テスト: 実機でのアプリ起動を確認
- [X] T016 [US2] 手動テスト: プロファイル作成・編集が動作することを確認
- [X] T017 [US2] 手動テスト: データ更新（同期）が動作することを確認
- [X] T018 [US2] 手動テスト: 取引登録が動作することを確認
- [X] T019 [US2] 手動テスト中に発見されたランタイム問題を修正（問題なし）

**チェックポイント**: アプリケーション動作がアップデート前と同一、全手動テストがパス

---

## Phase 5: User Story 3 - コード近代化 (Priority: P2)

**ゴール**: Kotlin 2.0 の安定機能を適用してコードの可読性を向上

**独立テスト**: コードレビューで新機能が適切に適用されていることを確認、全テストが引き続きパス

### User Story 3 の実装

#### data object への移行

- [X] T020 [P] [US3] `TemplateDivider` を data object に変換 `app/src/main/kotlin/net/ktnx/mobileledger/ui/templates/TemplatesRecyclerViewAdapter.kt`
- [X] T021 [P] [US3] sealed 階層内の他の object 宣言を data object に変換する候補を特定・変換（TransactionIdType.kt, StyleConfigurer.kt）

#### enumEntries への移行

- [X] T022 [P] [US3] `enumValues<T>()` → `enumEntries<T>()` を全 Kotlin ファイルで置換（該当なし）

#### 検証

- [X] T023 [US3] `nix run .#test` を実行し、近代化後も全テストがパスすることを確認
- [X] T024 [US3] `nix run .#build` を実行し、ビルドが成功することを確認

**チェックポイント**: コード近代化完了、全テストパス

---

## Phase 6: 仕上げ & 最終検証

**目的**: 最終クリーンアップと包括的な検証

- [X] T025 `nix run .#buildRelease` を実行し、リリースビルドが成功することを確認
- [X] T026 全変更をレビューし、見落とした deprecation 警告がないか確認（Kotlin stdlib警告なし、K2型推論警告のみ）
- [X] T027 quickstart.md の検証チェックリストを実行（ビルド・テスト・デバイスインストール成功）
- [X] T028 新しいパターンや規約が確立された場合は CLAUDE.md を更新（Kotlin 2.0.21 情報追加）

---

## 依存関係 & 実行順序

### Phase 依存関係

- **セットアップ (Phase 1)**: 依存なし - 即座に開始可能
- **基盤 (Phase 2)**: セットアップ完了に依存 - 全ユーザーストーリーをブロック
- **User Story 1 (Phase 3)**: 基盤フェーズの完了に依存
- **User Story 2 (Phase 4)**: User Story 1 の完了に依存（検証前にビルド必須）
- **User Story 3 (Phase 5)**: User Story 2 の完了に依存（近代化前に動作保持を確認）
- **仕上げ (Phase 6)**: 全ユーザーストーリーの完了に依存

### User Story 依存関係

- **User Story 1 (P1)**: 基盤に依存 - まずビルドが成功する必要あり
- **User Story 2 (P1)**: US1 に依存 - 実機検証前にビルドとテストがパス必須
- **User Story 3 (P2)**: US2 に依存 - コード変更前に動作保持を確認

### 各 User Story 内

- [P] マークの非推奨 API 修正は並列実行可能（異なるパターン、異なるファイル）
- [P] マークの data object 変換は並列実行可能（異なるファイル）
- 検証タスクは実装タスクの後に実行必須

### 並列実行の機会

- T001、T002 は並列実行可能（同一ファイル内の異なる行だが、順次実行が安全）
- T008、T009、T010 は並列実行可能（異なる検索パターン）
- T020、T021、T022 は並列実行可能（異なるファイル、異なるパターン）

---

## 並列実行例: User Story 1 非推奨 API 修正

```bash
# 全ての非推奨 API 修正を同時に起動:
Task: "appendln() → appendLine() を全 Kotlin ファイルで置換"
Task: "toUpperCase() → uppercase() を全 Kotlin ファイルで置換"
Task: "toLowerCase() → lowercase() を全 Kotlin ファイルで置換"
```

---

## 並列実行例: User Story 3 コード近代化

```bash
# 全ての data object 変換を同時に起動:
Task: "TemplateDivider を data object に変換"
Task: "他の object 宣言を data object に変換"
Task: "enumValues<T>() → enumEntries<T>() を置換"
```

---

## 実装戦略

### MVP 優先（User Story 1 & 2 のみ）

1. Phase 1 完了: セットアップ（バージョン更新）
2. Phase 2 完了: 基盤（ビルド検証）
3. Phase 3 完了: User Story 1（deprecation 修正）
4. Phase 4 完了: User Story 2（動作検証）
5. **停止して検証**: 実機でテスト、同一動作を確認
6. コミットして安定版としてタグ付け

### 完全デリバリー（コード近代化含む）

1. MVP 完了（上記）
2. Phase 5 完了: User Story 3（コード近代化）
3. Phase 6 完了: 仕上げ
4. 最終検証とリリース

### ロールバック戦略

いずれかのフェーズで問題が発生した場合:
```bash
git checkout master -- gradle/libs.versions.toml app/build.gradle
nix run .#build
```

---

## 備考

- [P] タスク = 異なるファイルまたはパターン、依存関係なし
- [Story] ラベルはタスクを特定のユーザーストーリーにマッピング（トレーサビリティ用）
- US1 と US2 は両方 P1 優先度だが、順次依存関係あり（検証前にビルド必須）
- US3 はオプションの機能強化 - MVP ではスキップ可能
- 既存テストがリグレッション検出として機能
- 完全な動作検証には実機での手動テストが必要
- 避けるべき: 実験的 Kotlin 機能（`@OptIn`）、ksp 移行（スコープ外）
