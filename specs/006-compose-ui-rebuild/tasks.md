# タスク: Jetpack Compose UI Rebuild

**入力**: `/specs/006-compose-ui-rebuild/` のデザインドキュメント
**前提条件**: plan.md (必須), spec.md (必須), research.md, data-model.md, quickstart.md

**テスト**: TDDアプローチ - 各User Story完了前にCompose UIテストを追加

**構成**: タスクはUser Story単位で整理し、各ストーリーの独立した実装とテストを可能にする

## フォーマット: `[ID] [P?] [Story] 説明`

- **[P]**: 並列実行可能（異なるファイル、依存関係なし）
- **[Story]**: 対象User Story（例：US1, US2, US3, US4）
- 説明には正確なファイルパスを含める

## パス規則

- **Android**: `app/src/main/kotlin/net/ktnx/mobileledger/`
- **テスト**: `app/src/test/kotlin/net/ktnx/mobileledger/` (ユニット), `app/src/androidTest/kotlin/net/ktnx/mobileledger/` (インストルメンテーション)

---

## Phase 1: セットアップ（Compose基盤）

**目的**: Compose BOM導入とビルド設定

- [X] T001 gradle/libs.versions.toml にCompose BOM依存関係を追加
- [X] T002 app/build.gradle にComposeプラグインと依存関係を追加
- [X] T003 app/build.gradle でCompose buildFeaturesを有効化
- [X] T004 `nix run .#build` を実行してComposeコンパイルが成功することを確認
- [X] T004a app/schemas/ のRoomスキーマJSONが変更されていないことを `git diff app/schemas/` で確認（FR-007準拠）

---

## Phase 2: 基盤（テーマと共通コンポーネント）

**目的**: 全User Storyで共通して使用するテーマとコンポーネント

**⚠️ 重要**: このフェーズが完了するまでUser Storyの作業は開始できない

- [X] T005 [P] app/src/main/kotlin/net/ktnx/mobileledger/ui/theme/Color.kt にMaterial 3カラーパレットを作成
- [X] T006 [P] app/src/main/kotlin/net/ktnx/mobileledger/ui/theme/Type.kt にTypography定義を作成
- [X] T007 app/src/main/kotlin/net/ktnox/mobileledger/ui/theme/Theme.kt にMoLeTheme Composableを作成
- [X] T008 app/src/main/kotlin/net/ktnox/mobileledger/ui/theme/ProfileTheme.kt にHSLベースの動的カラー生成を作成
- [X] T009 [P] app/src/main/kotlin/net/ktnox/mobileledger/ui/components/LoadingIndicator.kt に共通コンポーネントを作成
- [X] T010 [P] app/src/main/kotlin/net/ktnox/mobileledger/ui/components/ErrorSnackbar.kt に共通コンポーネントを作成
- [X] T011 [P] app/src/main/kotlin/net/ktnox/mobileledger/ui/components/ConfirmDialog.kt に共通コンポーネントを作成
- [X] T012 `nix run .#test` を実行して既存テストが通過することを確認
- [X] T013 `nix run .#build` を実行してテーマが正しくビルドされることを確認

**チェックポイント**: 基盤準備完了 - User Story実装を開始可能

---

## Phase 3: User Story 1 - プロファイル設定画面 (優先度: P1) 🎯 MVP

**ゴール**: ProfileDetailActivityをJetpack Composeで再構築し、既存XMLと同一の操作体験を実現

**独立テスト**: プロファイルの新規作成・編集・サーバー接続テストが正常動作すること

### User Story 1 の実装

- [X] T014 [US1] app/src/main/kotlin/net/ktnx/mobileledger/ui/components/HueRing.kt にCanvas APIを使用したHueRingカスタムComposableを作成
- [X] T015 [US1] app/src/main/kotlin/net/ktnx/mobileledger/ui/profile/ProfileDetailUiState.kt にデータクラスを作成
- [X] T016 [US1] app/src/main/kotlin/net/ktnx/mobileledger/ui/profile/ProfileDetailViewModel.kt にStateFlowを使用したViewModelを作成
- [X] T017 [US1] app/src/main/kotlin/net/ktnx/mobileledger/ui/profile/ProfileDetailScreen.kt にメインComposableを作成
- [X] T018 [US1] ProfileDetailScreen.kt にフォームフィールド（名前、URL、認証）を追加
- [X] T019 [US1] ProfileDetailScreen.kt にテーマカラー選択用HueRing統合を追加
- [X] T020 [US1] ProfileDetailViewModel.kt にローディング状態付き接続テスト機能を実装
- [X] T021 [US1] ProfileDetailScreen.kt に未保存変更確認ダイアログを追加
- [X] T022 [US1] app/src/main/kotlin/net/ktnox/mobileledger/ui/profiles/ProfileDetailActivity.kt をsetContentとMoLeThemeを使用するよう更新
- [X] T023 [US1] `nix run .#verify` を実行してデバイスでテスト

### User Story 1 のクリーンアップ

- [ ] T024 [US1] 検証後、app/src/main/kotlin/net/ktnx/mobileledger/ui/profiles/ProfileDetailFragment.kt を削除
- [ ] T025 [US1] app/src/main/res/layout/ のactivity_profile_detail.xmlと関連レイアウトファイルを削除
- [ ] T026 [US1] `nix run .#test` を実行して既存テストが通過することを確認

**チェックポイント**: User Story 1 完了 - ProfileDetailActivityのCompose移行完了

---

## Phase 4: User Story 2 - テンプレート管理画面 (優先度: P2)

**ゴール**: TemplatesActivityをJetpack Composeで再構築し、リスト表示・編集・削除機能を実現

**独立テスト**: テンプレートの一覧表示、作成、編集、削除が正常動作すること

### User Story 2 の実装

- [ ] T027 [P] [US2] app/src/main/kotlin/net/ktnx/mobileledger/ui/templates/TemplateListUiState.kt にデータクラスを作成
- [ ] T028 [P] [US2] app/src/main/kotlin/net/ktnx/mobileledger/ui/templates/TemplateDetailUiState.kt にデータクラスを作成
- [ ] T029 [US2] app/src/main/kotlin/net/ktnx/mobileledger/ui/templates/TemplateListViewModel.kt にStateFlowを使用したViewModelを作成
- [ ] T030 [US2] app/src/main/kotlin/net/ktnx/mobileledger/ui/templates/TemplateDetailViewModel.kt にStateFlowを使用したViewModelを作成
- [ ] T031 [US2] app/src/main/kotlin/net/ktnx/mobileledger/ui/templates/TemplateListScreen.kt にLazyColumnを使用した画面を作成
- [ ] T031a [US2] TemplateListScreen で1000件以上のテンプレートデータでスクロールパフォーマンスを確認（SC-003: 60fps維持）
- [ ] T032 [US2] app/src/main/kotlin/net/ktnx/mobileledger/ui/templates/TemplateDetailScreen.kt にフォームフィールド付き画面を作成
- [ ] T033 [US2] app/src/main/kotlin/net/ktnx/mobileledger/ui/templates/TemplatesNavigation.kt にCompose Navigationを作成
- [ ] T034 [US2] TemplateListScreen.kt にFABとスライド遷移アニメーションを実装
- [ ] T035 [US2] TemplateListScreen.kt に長押し削除と確認ダイアログを実装
- [ ] T036 [US2] app/src/main/kotlin/net/ktnx/mobileledger/ui/activity/TemplatesActivity.kt をsetContentとMoLeThemeを使用するよう更新
- [ ] T037 [US2] `nix run .#verify` を実行してデバイスでテスト

### User Story 2 のクリーンアップ

- [ ] T038 [US2] 検証後、app/src/main/kotlin/net/ktnx/mobileledger/ui/templates/TemplateListFragment.kt を削除
- [ ] T039 [US2] 検証後、app/src/main/kotlin/net/ktnx/mobileledger/ui/templates/TemplateDetailsFragment.kt を削除
- [ ] T040 [US2] app/src/main/res/layout/ のfragment_template_list.xmlと関連レイアウトファイルを削除
- [ ] T041 [US2] app/src/main/res/navigation/ のtemplate_list_navigation.xml を削除
- [ ] T042 [US2] `nix run .#test` を実行して既存テストが通過することを確認

**チェックポイント**: User Story 2 完了 - TemplatesActivityのCompose移行完了

---

## Phase 5: User Story 3 - メイン画面 (優先度: P3)

**ゴール**: MainActivityをJetpack Composeで再構築し、タブ・ドロワー・リスト表示を実現

**独立テスト**: アカウント一覧・取引一覧のタブ切り替え、ドロワーからのプロファイル切り替え、プルリフレッシュが正常動作すること

### User Story 3 の実装

- [ ] T043 [P] [US3] app/src/main/kotlin/net/ktnx/mobileledger/ui/main/MainUiState.kt にデータクラスを作成
- [ ] T044 [P] [US3] app/src/main/kotlin/net/ktnx/mobileledger/ui/main/AccountSummaryUiState.kt にデータクラスを作成
- [ ] T045 [P] [US3] app/src/main/kotlin/net/ktnx/mobileledger/ui/main/TransactionListUiState.kt にデータクラスを作成
- [ ] T046 [US3] app/src/main/kotlin/net/ktnx/mobileledger/ui/main/MainViewModel.kt にStateFlowを使用したViewModel（MainModelから移行）を作成
- [ ] T047 [US3] app/src/main/kotlin/net/ktnx/mobileledger/ui/main/NavigationDrawer.kt にプロファイルリスト付きドロワーを作成
- [ ] T048 [US3] app/src/main/kotlin/net/ktnx/mobileledger/ui/main/AccountSummaryTab.kt にLazyColumnとkey最適化を使用したタブを作成
- [ ] T049 [US3] app/src/main/kotlin/net/ktnx/mobileledger/ui/main/TransactionListTab.kt にLazyColumnとグループヘッダーを使用したタブを作成
- [ ] T049a [US3] AccountSummaryTab/TransactionListTab で1000件以上のデータでスクロールパフォーマンスを確認（SC-003: 60fps維持）
- [ ] T050 [US3] app/src/main/kotlin/net/ktnx/mobileledger/ui/main/MainScreen.kt にHorizontalPagerを使用したタブ構成を作成
- [ ] T051 [US3] MainScreen.kt にSwipeRefreshを使用したプルリフレッシュを実装
- [ ] T052 [US3] MainScreen.kt にNewTransactionActivityへのナビゲーション付きFABを実装
- [ ] T053 [US3] MainScreen.kt にプロファイル未設定時のウェルカムメッセージを実装
- [ ] T054 [US3] app/src/main/kotlin/net/ktnx/mobileledger/ui/activity/MainActivity.kt をsetContentとMoLeThemeを使用するよう更新
- [ ] T055 [US3] `nix run .#verify` を実行してデバイスでテスト

### User Story 3 のクリーンアップ

- [ ] T056 [US3] 検証後、app/src/main/kotlin/net/ktnx/mobileledger/ui/account_summary/AccountSummaryFragment.kt を削除
- [ ] T057 [US3] 検証後、app/src/main/kotlin/net/ktnx/mobileledger/ui/transaction_list/TransactionListFragment.kt を削除
- [ ] T058 [US3] app/src/main/kotlin/net/ktnx/mobileledger/ui/ の関連RecyclerViewアダプター（AccountSummaryAdapter, TransactionListAdapter）を削除
- [ ] T059 [US3] app/src/main/res/layout/ のactivity_main.xmlと関連レイアウトファイルを削除
- [ ] T060 [US3] `nix run .#test` を実行して既存テストが通過することを確認

**チェックポイント**: User Story 3 完了 - MainActivityのCompose移行完了

---

## Phase 6: User Story 4 - 取引登録画面 (優先度: P4)

**ゴール**: NewTransactionActivityをJetpack Composeで再構築し、動的フォーム・オートコンプリートを実現

**独立テスト**: 取引の日付・説明・複数アカウント行を入力して保存が成功すること

### User Story 4 の実装

- [ ] T061 [P] [US4] app/src/main/kotlin/net/ktnx/mobileledger/ui/transaction/NewTransactionUiState.kt にデータクラスを作成
- [ ] T062 [US4] app/src/main/kotlin/net/ktnx/mobileledger/ui/transaction/NewTransactionViewModel.kt にStateFlowを使用したViewModelを作成
- [ ] T063 [US4] app/src/main/kotlin/net/ktnx/mobileledger/ui/transaction/AccountAutocomplete.kt にExposedDropdownMenuを使用したコンポーネントを作成
- [ ] T064 [US4] app/src/main/kotlin/net/ktnx/mobileledger/ui/transaction/TransactionRowItem.kt に動的アカウント行コンポーネントを作成
- [ ] T065 [US4] app/src/main/kotlin/net/ktnx/mobileledger/ui/transaction/NewTransactionScreen.kt に動的フォームを作成
- [ ] T066 [US4] NewTransactionScreen.kt に日付ピッカーダイアログ統合を実装
- [ ] T067 [US4] NewTransactionScreen.kt にAnimatedVisibilityを使用した行追加/削除を実装
- [ ] T068 [US4] NewTransactionScreen.kt にテンプレート選択ダイアログを実装
- [ ] T069 [US4] NewTransactionViewModel.kt にフォームバリデーションとバランスチェックを実装
- [ ] T070 [US4] NewTransactionScreen.kt にプログレスインジケーター付き保存機能を実装
- [ ] T071 [US4] app/src/main/kotlin/net/ktnx/mobileledger/ui/activity/NewTransactionActivity.kt をsetContentとMoLeThemeを使用するよう更新
- [ ] T072 [US4] `nix run .#verify` を実行してデバイスでテスト

### User Story 4 のクリーンアップ

- [ ] T073 [US4] 検証後、app/src/main/kotlin/net/ktnx/mobileledger/ui/new_transaction/NewTransactionFragment.kt を削除
- [ ] T074 [US4] 検証後、app/src/main/kotlin/net/ktnx/mobileledger/ui/new_transaction/NewTransactionSavingFragment.kt を削除
- [ ] T075 [US4] app/src/main/kotlin/net/ktnx/mobileledger/ui/ の関連アダプター（NewTransactionItemsAdapter）を削除
- [ ] T076 [US4] app/src/main/res/layout/ のfragment_new_transaction.xmlと関連レイアウトファイルを削除
- [ ] T077 [US4] app/src/main/res/navigation/ のnew_transaction_navigation.xml を削除
- [ ] T078 [US4] `nix run .#test` を実行して既存テストが通過することを確認

**チェックポイント**: User Story 4 完了 - NewTransactionActivityのCompose移行完了

---

## Phase 7: 仕上げとクロスカッティング

**目的**: 全User Story完了後の最終調整

- [ ] T079 [P] app/src/main/res/layout/ の未使用XMLレイアウトファイルを削除
- [ ] T080 [P] app/src/main/res/navigation/ の未使用Navigation XMLファイルを削除
- [ ] T081 [P] app/src/main/res/drawable/ の未使用drawableリソースをクリーンアップ
- [ ] T082 View Bindingが不要になった場合、参照を削除
- [ ] T083 CLAUDE.md をCompose関連の開発ガイドラインで更新
- [ ] T084 `nix run .#verify` を実行して最終デバイス検証
- [ ] T085 APKサイズ増加を測定し、<10%制約を検証
- [ ] T086 アプリ起動時間を測定し、±200ms制約を検証
- [ ] T087 パフォーマンスプロファイリングを実行し、60fpsスクロールパフォーマンスを検証

---

## 依存関係と実行順序

### フェーズ依存関係

- **セットアップ (Phase 1)**: 依存関係なし - 即座に開始可能
- **基盤 (Phase 2)**: セットアップ完了に依存 - 全User Storyをブロック
- **User Stories (Phase 3-6)**: 基盤フェーズ完了に依存
  - User Storyは順番に進める（P1 → P2 → P3 → P4）
  - 各ストーリーはクリーンアップ前にデバイスで検証必須
- **仕上げ (Phase 7)**: 全User Story完了に依存

### User Story依存関係

- **User Story 1 (P1)**: 基盤（Phase 2）完了後に開始可能 - 他ストーリーへの依存なし
- **User Story 2 (P2)**: US1クリーンアップ後に開始可能 - 確立されたComposeパターンを活用
- **User Story 3 (P3)**: US2クリーンアップ後に開始可能 - US2のナビゲーションとリストパターンを活用
- **User Story 4 (P4)**: US3クリーンアップ後に開始可能 - 最も複雑、全ての先行パターンから恩恵

### 各User Story内の順序

- UiState → ViewModel → Screen → 統合
- クリーンアップ前に `nix run .#verify` でデバイステスト
- デバイス検証成功後のみXML/Fragmentを削除
- クリーンアップ後に `nix run .#test` でリグレッションがないことを確認

### 並列実行の機会

- Phase 1: 全セットアップタスクは順次実行
- Phase 2: T005, T006, T009, T010, T011 は並列実行可能
- Phase 3-6: 各ストーリー内の [P] マーク付きUiStateデータクラスは並列実行可能
- Phase 7: [P] マーク付きクリーンアップタスクは並列実行可能

---

## 並列実行例: Phase 2 基盤

```bash
# 並列タスクを起動:
Task: "ui/theme/Color.kt にColor.ktを作成"
Task: "ui/theme/Type.kt にType.ktを作成"
Task: "ui/components/LoadingIndicator.kt にLoadingIndicator.ktを作成"
Task: "ui/components/ErrorSnackbar.kt にErrorSnackbar.ktを作成"
Task: "ui/components/ConfirmDialog.kt にConfirmDialog.ktを作成"
```

---

## 実装戦略

### MVPファースト（User Story 1のみ）

1. Phase 1: セットアップを完了
2. Phase 2: 基盤を完了
3. Phase 3: User Story 1 (ProfileDetailActivity) を完了
4. **停止して検証**: デバイスでテスト、全受け入れシナリオを確認
5. MVPとしてデプロイ/デモ

### インクリメンタルデリバリー

1. セットアップ + 基盤 → 基盤準備完了
2. User Story 1 追加 → デバイス検証 → XMLクリーンアップ (ProfileDetail MVP!)
3. User Story 2 追加 → デバイス検証 → XMLクリーンアップ (テンプレート追加)
4. User Story 3 追加 → デバイス検証 → XMLクリーンアップ (メイン画面追加)
5. User Story 4 追加 → デバイス検証 → XMLクリーンアップ (完全移行完了!)
6. 仕上げフェーズ → 最終検証

### 主要検証ポイント

各User Story完了後:
1. `nix run .#verify` を実行してデバイスにインストール
2. spec.md の全受け入れシナリオを手動テスト
3. オリジナルXML実装とUI外観を比較
4. 検証通過後のみクリーンアップに進む

---

## 備考

- [P] タスク = 異なるファイル、依存関係なし
- [Story] ラベル = 特定User Storyへのタスクマッピング（追跡用）
- 各User Storyはクリーンアップ前にデバイス検証必須
- クリーンアップタスクはオリジナルXML/Fragmentファイルを削除
- クリーンアップ後に `nix run .#test` で既存テストが通過することを確認
- 各タスクまたは論理的グループ後にコミット
- 避けるべき: 同一ファイル競合、デバイス検証スキップ
