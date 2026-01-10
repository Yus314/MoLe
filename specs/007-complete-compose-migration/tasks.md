# タスク: Complete Compose Migration

**入力**: `/specs/007-complete-compose-migration/` のデザインドキュメント
**前提条件**: plan.md (必須), spec.md (必須), research.md, data-model.md, quickstart.md

**テスト**: 既存テスト維持 + 各User Story完了後にデバイス検証

**構成**: タスクはUser Story単位で整理し、各ストーリーの独立した実装とテストを可能にする

## フォーマット: `[ID] [P?] [Story] 説明`

- **[P]**: 並列実行可能（異なるファイル、依存関係なし）
- **[Story]**: 対象User Story（例：US1, US2, US3, US4, US5）
- 説明には正確なファイルパスを含める

## パス規則

- **Android**: `app/src/main/kotlin/net/ktnx/mobileledger/`
- **テスト**: `app/src/test/kotlin/net/ktnx/mobileledger/` (ユニット)
- **リソース**: `app/src/main/res/`

---

## Phase 1: 準備確認

**目的**: 006-compose-ui-rebuildの成果物確認とビルド検証

- [x] T001 `nix run .#build` を実行してComposeビルドが成功することを確認
- [x] T002 `nix run .#test` を実行して既存テストが通過することを確認

**チェックポイント**: 基盤準備完了 - User Story実装を開始可能 ✅

---

## Phase 2: User Story 1 - ダイアログのCompose化 (優先度: P1) 🎯 MVP

**ゴール**: DatePickerFragmentとCurrencySelectorFragmentをCompose Dialogに置換

**独立テスト**: 取引登録画面で日付選択、通貨選択を行い、ダイアログが正常に動作すること

### User Story 1 の実装

- [x] T003 [P] [US1] app/src/main/kotlin/net/ktnx/mobileledger/ui/components/DatePickerDialog.kt にMaterial3 DatePickerDialogを作成
- [x] T004 [P] [US1] app/src/main/kotlin/net/ktnx/mobileledger/ui/components/CurrencyPickerUiState.kt にUiStateとイベントクラスを作成
- [x] T005 [US1] app/src/main/kotlin/net/ktnx/mobileledger/ui/components/CurrencyPickerDialog.kt にCompose Dialogを作成
- [x] T006 [US1] DatePickerDialogに日付範囲制限（minDate, maxDate）とFutureDates対応を実装
- [x] T007 [US1] CurrencyPickerDialogに通貨追加・削除・位置設定機能を実装
- [x] T008 [US1] 既存のDatePickerFragment呼び出し箇所をDatePickerDialogに置換
- [x] T009 [US1] 既存のCurrencySelectorFragment呼び出し箇所をCurrencyPickerDialogに置換
- [x] T010 [US1] `nix run .#verify` を実行してデバイスでテスト

### User Story 1 のクリーンアップ

- [x] T011 [US1] 検証後、app/src/main/kotlin/net/ktnx/mobileledger/ui/DatePickerFragment.kt を削除
- [x] T012 [US1] 検証後、app/src/main/kotlin/net/ktnx/mobileledger/ui/CurrencySelectorFragment.kt を削除
- [x] T013 [US1] 検証後、app/src/main/kotlin/net/ktnx/mobileledger/ui/CurrencySelectorRecyclerViewAdapter.kt を削除
- [x] T014 [US1] 検証後、app/src/main/kotlin/net/ktnx/mobileledger/ui/OnCurrencySelectedListener.kt を削除
- [x] T015 [US1] 検証後、app/src/main/kotlin/net/ktnx/mobileledger/ui/OnCurrencyLongClickListener.kt を削除
- [x] T016 [P] [US1] app/src/main/res/layout/date_picker_view.xml を削除
- [x] T017 [P] [US1] app/src/main/res/layout/fragment_currency_selector_list.xml を削除
- [x] T018 [P] [US1] app/src/main/res/layout/fragment_currency_selector.xml を削除
- [x] T019 [US1] `nix run .#test` を実行して既存テストが通過することを確認

**チェックポイント**: User Story 1 完了 - ダイアログのCompose移行完了

---

## Phase 3: User Story 2 - スプラッシュ画面のCompose化 (優先度: P2)

**ゴール**: SplashActivityをCompose実装に置換

**独立テスト**: アプリを起動し、スプラッシュ画面が表示され、メイン画面への遷移が正常に行われること

### User Story 2 の実装

- [x] T020 [P] [US2] app/src/main/kotlin/net/ktnx/mobileledger/ui/splash/SplashUiState.kt にUiStateとエフェクトクラスを作成
- [x] T021 [US2] app/src/main/kotlin/net/ktnx/mobileledger/ui/splash/SplashScreen.kt にスプラッシュ画面Composableを作成
- [x] T022 [US2] SplashScreenにアプリロゴとローディングインジケーターを実装
- [x] T023 [US2] app/src/main/kotlin/net/ktnx/mobileledger/ui/activity/SplashActivity.kt をsetContentとMoLeThemeを使用するよう更新
- [x] T024 [US2] DB初期化完了と最小表示時間（400ms）のロジックを維持
- [x] T025 [US2] MainActivityComposeへの遷移アニメーション（フェードイン/アウト）を実装
- [x] T026 [US2] `nix run .#verify` を実行してデバイスでテスト

### User Story 2 のクリーンアップ

- [x] T027 [US2] 検証後、app/src/main/res/layout/splash_activity_layout.xml を削除
- [x] T028 [US2] `nix run .#test` を実行して既存テストが通過することを確認

**チェックポイント**: User Story 2 完了 - SplashActivityのCompose移行完了

---

## Phase 4: User Story 3 - バックアップ画面のCompose化 (優先度: P3)

**ゴール**: BackupsActivityをCompose実装に置換

**独立テスト**: バックアップ画面を開き、バックアップの作成・リストア操作が正常に動作すること

### User Story 3 の実装

- [x] T029 [P] [US3] app/src/main/kotlin/net/ktnx/mobileledger/ui/backups/BackupsUiState.kt にUiState、イベント、エフェクトクラスを作成
- [x] T030 [US3] app/src/main/kotlin/net/ktnx/mobileledger/ui/backups/BackupsViewModel.kt にStateFlowを使用したViewModelを作成
- [x] T031 [US3] app/src/main/kotlin/net/ktnx/mobileledger/ui/backups/BackupsScreen.kt にバックアップ画面Composableを作成
- [x] T032 [US3] BackupsScreenにバックアップ/リストアボタンと説明テキストを実装
- [x] T033 [US3] BackupsViewModelにConfigWriter/ConfigReaderとの連携を実装
- [x] T034 [US3] BackupsScreenにSnackbarHostでステータスメッセージ表示を実装
- [x] T035 [US3] app/src/main/kotlin/net/ktnx/mobileledger/BackupsActivity.kt をsetContentとMoLeThemeを使用するよう更新
- [x] T036 [US3] `nix run .#verify` を実行してデバイスでテスト

### User Story 3 のクリーンアップ

- [x] T037 [US3] 検証後、app/src/main/res/layout/fragment_backups.xml を削除
- [x] T038 [US3] BackupsActivityからViewBinding参照を削除
- [x] T039 [US3] `nix run .#test` を実行して既存テストが通過することを確認

**チェックポイント**: User Story 3 完了 - BackupsActivityのCompose移行完了 ✅

---

## Phase 5: User Story 4 - レガシーアダプター削除 (優先度: P4)

**ゴール**: ProfilesRecyclerViewAdapterをCompose化し、未使用アダプターを削除

**独立テスト**: NavigationDrawerでプロファイル一覧表示、選択、並べ替えが正常に動作すること

### User Story 4 の実装

- [x] T040 [P] [US4] NavigationDrawer.ktにProfileListItem UiState統合済み
- [x] T041 [US4] NavigationDrawer.ktにProfileRow Composable統合済み
- [x] T042 [US4] NavigationDrawer.ktでLazyColumn実装済み
- [x] T043 [US4] NavigationDrawer.ktで編集ボタン表示実装済み
- [x] T044 [US4] NavigationDrawer.ktでreorderableライブラリによるドラッグ&ドロップ実装済み
- [x] T045 [US4] NavigationDrawer.ktでプロファイル選択とカラータグ表示実装済み
- [x] T046 [US4] NavigationDrawer.kt 既にCompose化済み
- [x] T047 [US4] `nix run .#verify` を実行してデバイスでテスト

### User Story 4 のクリーンアップ

- [x] T048 [US4] ProfilesRecyclerViewAdapter.kt を削除
- [x] T049 [P] [US4] AccountAutocompleteAdapter.kt 既に削除済み
- [x] T050 [P] [US4] AccountWithAmountsAutocompleteAdapter.kt 既に削除済み
- [x] T051 [P] [US4] TransactionDescriptionAutocompleteAdapter.kt 既に削除済み
- [x] T052 [P] [US4] app/src/main/res/layout/profile_list_content.xml を削除
- [x] T053 [P] [US4] app/src/main/res/layout/account_autocomplete_row.xml を削除
- [x] T054 [US4] `nix run .#test` を実行して既存テストが通過することを確認

**チェックポイント**: User Story 4 完了 - レガシーアダプター削除完了 ✅

---

## Phase 6: User Story 5 - クラッシュレポートダイアログ (優先度: P5)

**ゴール**: CrashReportDialogFragmentをCompose Dialogに置換し、残存Fragment/XMLを全削除

**独立テスト**: クラッシュレポートダイアログが表示され、メール送信Intentが正常に動作すること

### User Story 5 の実装

- [x] T055 [P] [US5] app/src/main/kotlin/net/ktnx/mobileledger/ui/components/CrashReportUiState.kt にUiStateクラスを作成
- [x] T056 [US5] app/src/main/kotlin/net/ktnx/mobileledger/ui/components/CrashReportDialog.kt にCompose Dialogを作成
- [x] T057 [US5] CrashReportDialogにクラッシュレポートテキスト表示（スクロール可能）を実装
- [x] T058 [US5] CrashReportDialogにShow Report/Hide Reportトグルを実装
- [x] T059 [US5] CrashReportDialogにメール送信Intent起動を実装
- [x] T060 [US5] app/src/main/kotlin/net/ktnx/mobileledger/ui/activity/CrashReportingActivity.kt をCompose Dialogを使用するよう更新
- [x] T061 [US5] `nix run .#verify` を実行してデバイスでテスト

### User Story 5 のクリーンアップ

- [x] T062 [US5] 検証後、app/src/main/kotlin/net/ktnx/mobileledger/ui/CrashReportDialogFragment.kt を削除
- [x] T063 [US5] 検証後、app/src/main/kotlin/net/ktnx/mobileledger/ui/QRScanCapableFragment.kt を削除
- [x] T064 [P] [US5] app/src/main/res/layout/crash_dialog.xml を削除
- [x] T065 [P] [US5] app/src/main/res/layout/hue_dialog.xml を削除（残存XML）
- [x] T066 [US5] `nix run .#test` を実行して既存テストが通過することを確認

**チェックポイント**: User Story 5 完了 - CrashReportDialogのCompose移行完了

---

## Phase 7: 仕上げとクロスカッティング

**目的**: 全User Story完了後の最終検証とSuccess Criteria達成確認

- [ ] T067 SC-001検証: `ls app/src/main/res/layout/` でディレクトリが空であることを確認
- [ ] T068 SC-002検証: Fragment/DialogFragment依存コードが存在しないことをgrep検索で確認
- [ ] T069 SC-003検証: ViewBinding使用箇所が存在しないことをgrep検索で確認
- [ ] T070 SC-004検証: `nix run .#test` で全テスト通過を確認
- [ ] T071 SC-005検証: APKサイズを測定し27MB±5%以内であることを確認
- [ ] T072 SC-006検証: アプリ起動時間を測定し526ms±20%以内であることを確認
- [ ] T073 SC-007検証: 全画面でプロファイルカラーテーマが適用されることを手動確認
- [ ] T074 `nix run .#verify` を実行して最終デバイス検証
- [ ] T075 CLAUDE.md を007関連の開発ガイドラインで更新

**チェックポイント**: Phase 7 完了 - 007-complete-compose-migration 全タスク完了

---

## 依存関係と実行順序

### フェーズ依存関係

- **準備確認 (Phase 1)**: 依存関係なし - 即座に開始可能
- **User Stories (Phase 2-6)**: 準備確認完了に依存
  - User Storyは順番に進める（P1 → P2 → P3 → P4 → P5）
  - 各ストーリーはクリーンアップ前にデバイスで検証必須
- **仕上げ (Phase 7)**: 全User Story完了に依存

### User Story依存関係

- **User Story 1 (P1)**: 準備確認完了後に開始可能 - 他ストーリーへの依存なし
- **User Story 2 (P2)**: US1クリーンアップ後に開始可能
- **User Story 3 (P3)**: US2クリーンアップ後に開始可能
- **User Story 4 (P4)**: US3クリーンアップ後に開始可能
- **User Story 5 (P5)**: US4クリーンアップ後に開始可能

### 各User Story内の順序

- UiState → Composable → 統合 → 検証
- クリーンアップ前に `nix run .#verify` でデバイステスト
- デバイス検証成功後のみFragment/Adapter/XMLを削除
- クリーンアップ後に `nix run .#test` でリグレッションがないことを確認

### 並列実行の機会

- Phase 2: T003, T004 は並列実行可能
- Phase 2: T016, T017, T018 は並列実行可能
- Phase 5: T049, T050, T051, T052, T053 は並列実行可能
- Phase 6: T064, T065 は並列実行可能

---

## 並列実行例: Phase 2 User Story 1

```bash
# 並列タスクを起動:
Task: "ui/components/DatePickerDialog.kt にDatePickerDialogを作成"
Task: "ui/components/CurrencyPickerUiState.kt にUiStateを作成"

# クリーンアップの並列タスク:
Task: "layout/date_picker_view.xml を削除"
Task: "layout/fragment_currency_selector_list.xml を削除"
Task: "layout/fragment_currency_selector.xml を削除"
```

---

## 実装戦略

### MVPファースト（User Story 1のみ）

1. Phase 1: 準備確認を完了
2. Phase 2: User Story 1 (ダイアログ) を完了
3. **停止して検証**: デバイスでテスト、全受け入れシナリオを確認
4. MVPとしてデプロイ/デモ

### インクリメンタルデリバリー

1. 準備確認 → 基盤準備完了
2. User Story 1 追加 → デバイス検証 → Fragment/XMLクリーンアップ (ダイアログ MVP!)
3. User Story 2 追加 → デバイス検証 → XMLクリーンアップ (スプラッシュ追加)
4. User Story 3 追加 → デバイス検証 → XMLクリーンアップ (バックアップ追加)
5. User Story 4 追加 → デバイス検証 → Adapter/XMLクリーンアップ (レガシー削除)
6. User Story 5 追加 → デバイス検証 → Fragment/XMLクリーンアップ (完全移行完了!)
7. 仕上げフェーズ → 最終検証

### 主要検証ポイント

各User Story完了後:
1. `nix run .#verify` を実行してデバイスにインストール
2. spec.md の全受け入れシナリオを手動テスト
3. 検証通過後のみクリーンアップに進む

---

## 備考

- [P] タスク = 異なるファイル、依存関係なし
- [Story] ラベル = 特定User Storyへのタスクマッピング（追跡用）
- 各User Storyはクリーンアップ前にデバイス検証必須
- クリーンアップタスクはFragment/Adapter/XMLファイルを削除
- クリーンアップ後に `nix run .#test` で既存テストが通過することを確認
- 各タスクまたは論理的グループ後にコミット
- 避けるべき: 同一ファイル競合、デバイス検証スキップ
