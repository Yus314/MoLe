# タスク: Hilt 依存性注入セットアップ

**入力**: `/specs/005-hilt-di-setup/` からの設計ドキュメント
**前提条件**: plan.md, spec.md, research.md, data-model.md

**テスト**: TDDを明示的に要求しているため、テストタスクを含む

**構成**: タスクはユーザーストーリーごとにグループ化され、各ストーリーの独立した実装とテストを可能にする

## フォーマット: `[ID] [P?] [Story] 説明`

- **[P]**: 並列実行可能（異なるファイル、依存関係なし）
- **[Story]**: このタスクが属するユーザーストーリー（例: US1, US2, US3）
- 説明には正確なファイルパスを含める

## パス規約

- **Android単一モジュール**: `app/src/main/kotlin/`, `app/src/test/kotlin/`, `app/src/androidTest/kotlin/`
- plan.mdの構造に基づく

---

## Phase 1: セットアップ（共有インフラストラクチャ）

**目的**: Hilt依存関係の追加とプロジェクト初期化

- [X] T001 gradle/libs.versions.tomlにHiltバージョン定義を追加（hilt = "2.51.1", hiltAndroidX = "1.2.0"）
- [X] T002 gradle/libs.versions.tomlにHiltライブラリ定義を追加（hilt-android, hilt-compiler, hilt-android-testing, androidx-hilt-compiler）
- [X] T003 gradle/libs.versions.tomlにHiltプラグイン定義を追加
- [X] T004 build.gradleにHiltプラグイン適用を追加
- [X] T005 app/build.gradleにHilt依存関係を追加（implementation, ksp, testImplementation, androidTestImplementation）
- [X] T006 ビルド成功を確認（nix run .#build）

**チェックポイント**: Hilt依存関係が正常に解決され、ビルドが通ること

---

## Phase 2: 基盤（ブロッキング前提条件）

**目的**: すべてのユーザーストーリーの前に完了しなければならないHilt基盤インフラ

**⚠️ 重要**: このフェーズが完了するまでユーザーストーリー作業は開始できない

- [X] T007 app/src/main/kotlin/net/ktnx/mobileledger/App.ktに@HiltAndroidAppアノテーションを追加
- [X] T008 app/src/main/kotlin/net/ktnox/mobileledger/di/ディレクトリを作成
- [X] T009 [P] app/src/main/kotlin/net/ktnx/mobileledger/di/DatabaseModule.ktを作成（DB.get()をラップしてDBを@Singleton提供）
- [X] T010 [P] DatabaseModule.ktにすべてのDAO提供メソッドを追加（ProfileDAO, TransactionDAO, AccountDAO, AccountValueDAO, TemplateHeaderDAO, TemplateAccountDAO, CurrencyDAO, OptionDAO）
- [X] T011 [P] app/src/main/kotlin/net/ktnox/mobileledger/di/DataModule.ktを作成（Dataオブジェクトを@Singleton提供）
- [X] T012 ビルド成功を確認（nix run .#build）

**チェックポイント**: 基盤準備完了 - ユーザーストーリー実装を並列で開始可能

---

## Phase 3: ユーザーストーリー 1 - 開発者がモック依存関係でユニットテストを作成 (優先度: P1) 🎯 MVP

**目標**: ViewModelに依存関係を注入し、ユニットテストでモックに置き換え可能にする

**独立テスト**: MainModelのユニットテストを作成し、モック依存関係で実行できることを確認

### ユーザーストーリー 1 のテスト

> **注意: 実装前にこれらのテストを作成し、失敗することを確認**

- [X] T013 [US1] gradle/libs.versions.tomlにMockKバージョン定義を追加（mockk = "1.13.13"）
- [X] T013.1 [US1] gradle/libs.versions.tomlにMockKライブラリ定義を追加（mockk = { module = "io.mockk:mockk", version.ref = "mockk" }）
- [X] T013.2 [US1] app/build.gradleにMockK依存関係を追加（testImplementation libs.mockk）
- [X] T014 [US1] app/src/test/kotlin/net/ktnx/mobileledger/ui/ディレクトリを作成
- [X] T015 [US1] app/src/test/kotlin/net/ktnox/mobileledger/ui/MainModelTest.ktを作成（モックDAOを使用したテストクラス骨格）
- [X] T016 [US1] MainModelTest.ktに@Beforeセットアップメソッドを追加（MockKでモックDAO作成）
- [X] T017 [US1] MainModelTest.ktにサンプルテストケースを追加（テストが失敗することを確認）

### ユーザーストーリー 1 の実装

- [X] T018 [US1] app/src/main/kotlin/net/ktnox/mobileledger/ui/MainModel.ktを分析し、現在の直接依存関係を特定
- [X] T019 [US1] MainModel.ktに@HiltViewModelアノテーションを追加
- [X] T020 [US1] MainModel.ktにコンストラクタを追加し、@Injectで依存関係を宣言（ProfileDAO, TransactionDAO, AccountDAO, Data）
- [X] T021 [US1] MainModel.kt内のDB.get().getXxxDAO()呼び出しを注入された依存関係に置き換え
- [X] T022 [US1] MainModel.kt内のData直接参照を注入された依存関係に置き換え
- [X] T023 [US1] app/src/main/kotlin/net/ktnx/mobileledger/ui/activity/MainActivity.ktに@AndroidEntryPointアノテーションを追加
- [X] T024 [US1] MainActivity.ktのViewModelProvider呼び出しをby viewModels()に変更
- [X] T025 [US1] ユニットテストを実行し、成功することを確認（nix run .#test）
- [X] T026 [US1] アプリをビルドして実機で動作確認（nix run .#verify）

**チェックポイント**: ユーザーストーリー1が完全に機能し、独立してテスト可能

---

## Phase 4: ユーザーストーリー 2 - 開発者が適切な依存関係管理で新機能を追加 (優先度: P2)

**目標**: 依存関係を宣言するだけで自動的に提供されることを実証

**独立テスト**: 新しいViewModelを依存関係付きで作成し、手動配線なしで動作することを確認

### ユーザーストーリー 2 の実装

- [X] T027 [US2] quickstart.mdのパターンに従い、既存機能が移行後も同一に動作することを確認
- [X] T028 [US2] CLAUDE.mdを更新し、Hilt DIのベストプラクティスを追加（新規ViewModelの作成方法）

**チェックポイント**: ユーザーストーリー1と2が両方とも独立して動作

---

## Phase 5: ユーザーストーリー 3 - 開発者がテストデータベースでインストルメンテーションテストを実行 (優先度: P3)

**目標**: インストルメンテーションテストで本番DBの代わりにインメモリDBを使用可能にする

**独立テスト**: インストルメンテーションテストを実行し、テストDBを使用していることを確認

### ユーザーストーリー 3 のテスト

- [X] T029 [US3] app/src/androidTest/kotlin/net/ktnx/mobileledger/ディレクトリ構造を確認/作成
- [X] T030 [US3] app/src/androidTest/kotlin/net/ktnx/mobileledger/HiltTestRunner.ktを作成（HiltTestApplicationを使用するカスタムテストランナー）
- [X] T031 [US3] app/build.gradleのtestInstrumentationRunnerをHiltTestRunnerに変更

### ユーザーストーリー 3 の実装

- [X] T032 [US3] app/src/androidTest/kotlin/net/ktnx/mobileledger/di/TestDatabaseModule.ktを作成（@TestInstallInでDatabaseModuleを置換）
- [X] T033 [US3] TestDatabaseModule.ktにインメモリDB提供メソッドを追加（Room.inMemoryDatabaseBuilder使用）
- [X] T034 [US3] app/src/androidTest/kotlin/net/ktnx/mobileledger/MainActivityInstrumentationTest.ktを作成（@HiltAndroidTestサンプル）
- [X] T035 [US3] インストルメンテーションテストを実行し、成功することを確認

**チェックポイント**: すべてのユーザーストーリーが独立して機能

---

## Phase 6: ユーザーストーリー 4 - チームが一貫した依存関係設定を維持 (優先度: P4)

**目標**: 依存関係設定が集中化され、アーキテクチャが理解しやすいことを確認

**独立テスト**: 依存関係作成ロジックがdi/ディレクトリに集中していることを確認

### ユーザーストーリー 4 の実装

- [X] T036 [US4] di/ディレクトリのモジュール構造をレビューし、命名規則の一貫性を確認
- [X] T037 [US4] 各モジュールにKDocコメントを追加（モジュールの目的と提供物を説明）
- [X] T038 [US4] quickstart.mdが最新であることを確認し、必要に応じて更新

**チェックポイント**: 依存関係設定が集中化され、文書化されている

---

## Phase 7: ポリッシュ＆横断的関心事

**目的**: 複数のユーザーストーリーに影響する改善

- [X] T039 [P] specs/005-hilt-di-setup/のドキュメントを最終確認
- [X] T040 コードクリーンアップ（未使用インポート削除、フォーマット統一）
- [X] T041 [P] 実機での最終動作確認（nix run .#verify）
- [X] T042 CLAUDE.mdのRecent Changesセクションを更新

---

## 依存関係と実行順序

### フェーズ依存関係

- **セットアップ (Phase 1)**: 依存関係なし - 即座に開始可能
- **基盤 (Phase 2)**: セットアップ完了に依存 - すべてのユーザーストーリーをブロック
- **ユーザーストーリー (Phase 3+)**: 基盤フェーズ完了に依存
  - ユーザーストーリーは並列で進行可能（チームメンバーがいる場合）
  - または優先度順に順次進行（P1 → P2 → P3 → P4）
- **ポリッシュ (最終フェーズ)**: 希望するすべてのユーザーストーリーの完了に依存

### ユーザーストーリー依存関係

- **ユーザーストーリー 1 (P1)**: 基盤（Phase 2）後に開始可能 - 他のストーリーへの依存なし
- **ユーザーストーリー 2 (P2)**: US1完了後が推奨（パターン確立のため）
- **ユーザーストーリー 3 (P3)**: 基盤（Phase 2）後に開始可能 - US1と独立してテスト可能
- **ユーザーストーリー 4 (P4)**: すべてのモジュール作成後が推奨

### 各ユーザーストーリー内

- テストは実装前に作成し、失敗することを確認
- 分析 → アノテーション追加 → 依存関係注入 → 既存コード更新
- ストーリー完了後に次の優先度に移動

### 並列機会

- すべてのセットアップタスク（T001-T006）は順次実行（依存関係あり）
- 基盤タスクのT009-T011は[P]マークで並列実行可能
- 基盤フェーズ完了後、US1とUS3は並列開始可能
- US1内のテストタスク（T013-T016）は順次実行（テスト構造構築）

---

## 並列実行例: Phase 2 基盤

```bash
# Phase 2の並列タスクを同時に起動:
Task: "app/src/main/kotlin/net/ktnx/mobileledger/di/DatabaseModule.ktを作成"
Task: "app/src/main/kotlin/net/ktnx/mobileledger/di/DataModule.ktを作成"
```

---

## 実装戦略

### MVP優先（ユーザーストーリー1のみ）

1. Phase 1: セットアップを完了
2. Phase 2: 基盤を完了（重要 - すべてのストーリーをブロック）
3. Phase 3: ユーザーストーリー1を完了
4. **停止して検証**: ユーザーストーリー1を独立してテスト
5. 準備ができたらデプロイ/デモ

### 段階的デリバリー

1. セットアップ + 基盤を完了 → 基盤準備完了
2. ユーザーストーリー1を追加 → 独立してテスト → デプロイ/デモ（MVP!）
3. ユーザーストーリー3を追加 → 独立してテスト → デプロイ/デモ
4. ユーザーストーリー2を追加 → 独立してテスト → デプロイ/デモ
5. ユーザーストーリー4を追加 → 独立してテスト → デプロイ/デモ
6. 各ストーリーは前のストーリーを壊さずに価値を追加

---

## 注意事項

- [P] タスク = 異なるファイル、依存関係なし
- [Story] ラベルはタスクを特定のユーザーストーリーにマッピング
- 各ユーザーストーリーは独立して完了・テスト可能であるべき
- 実装前にテストが失敗することを確認
- 各タスクまたは論理グループ後にコミット
- 任意のチェックポイントで停止してストーリーを独立して検証可能
- 避けるべき: 曖昧なタスク、同一ファイルの競合、独立性を損なうストーリー間依存関係
