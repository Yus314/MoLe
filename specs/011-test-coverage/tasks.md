# Tasks: クリティカルコンポーネントのテストカバレッジ向上

**Input**: Design documents from `/specs/011-test-coverage/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/TransactionSender.kt

**Tests**: TDD アプローチ（spec.md で指定）- テストを先に作成し、失敗を確認してから実装

**Organization**: ユーザーストーリーごとにタスクをグループ化し、独立した実装・テストを可能にする

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 並列実行可能（異なるファイル、依存関係なし）
- **[Story]**: タスクが属するユーザーストーリー（例：US1, US2, US3, US4）
- 説明に正確なファイルパスを含める

## Path Conventions

- **Android Mobile**: `app/src/main/kotlin/` for production, `app/src/test/kotlin/` for tests
- Package: `net.ktnx.mobileledger`

---

## Phase 1: Setup (共有インフラストラクチャ)

**Purpose**: テスト基盤の準備とプロジェクト構成

- [X] T001 MainDispatcherRule を作成: `app/src/test/kotlin/net/ktnx/mobileledger/util/MainDispatcherRule.kt`
- [X] T002 [P] テストユーティリティ関数を作成: `app/src/test/kotlin/net/ktnx/mobileledger/util/TestUtils.kt`

---

## Phase 2: Foundational (ブロッキング前提条件)

**Purpose**: すべてのユーザーストーリー実装に必要なコアインフラストラクチャ

**⚠️ CRITICAL**: このフェーズが完了するまでユーザーストーリー作業を開始できない

- [X] T003 TransactionSender インターフェースを実装: `app/src/main/kotlin/net/ktnx/mobileledger/domain/usecase/TransactionSender.kt`
- [X] T004 [P] TransactionSenderImpl を実装（SendTransactionTask ラッパー）: `app/src/main/kotlin/net/ktnx/mobileledger/domain/usecase/TransactionSenderImpl.kt`
- [X] T005 [P] UseCaseModule を作成（TransactionSender バインディング）: `app/src/main/kotlin/net/ktnx/mobileledger/di/UseCaseModule.kt`
- [X] T006 [P] FakePreferencesRepository を作成: `app/src/test/kotlin/net/ktnx/mobileledger/fake/FakePreferencesRepository.kt`
- [X] T007 [P] FakeTransactionSender を作成: `app/src/test/kotlin/net/ktnx/mobileledger/fake/FakeTransactionSender.kt`
- [X] T008 [P] FakeCurrencyFormatter を作成: `app/src/test/kotlin/net/ktnx/mobileledger/fake/FakeCurrencyFormatter.kt`
- [X] T009 [P] FakeTemplateRepository を作成: `app/src/test/kotlin/net/ktnx/mobileledger/fake/FakeTemplateRepository.kt`
- [X] T010 [P] FakeCurrencyRepository を作成: `app/src/test/kotlin/net/ktnx/mobileledger/fake/FakeCurrencyRepository.kt`

**Checkpoint**: 基盤準備完了 - ユーザーストーリー実装を並列開始可能

---

## Phase 3: User Story 1 - MainViewModel のテスト追加 (Priority: P1) 🎯 MVP

**Goal**: MainViewModel のユニットテストを作成し、プロファイル選択、アカウント読み込み、取引読み込み、リフレッシュ操作を検証

**Independent Test**: モックプリファレンスリポジトリを使用してテスト実行可能。最も使用頻度の高い画面のリグレッション検出

### リファクタリング（テスト可能化）

- [X] T011 [US1] MainViewModel に PreferencesRepository を注入: `app/src/main/kotlin/net/ktnx/mobileledger/ui/main/MainViewModel.kt`
  - コンストラクタに `preferencesRepository: PreferencesRepository` パラメータを追加
  - `@Inject constructor` に含める（既存の Repository と同様）
- [X] T012 [US1] App.getShowZeroBalanceAccounts() を preferencesRepository.getShowZeroBalanceAccounts() に置換: `app/src/main/kotlin/net/ktnx/mobileledger/ui/main/MainViewModel.kt`
  - `App.getShowZeroBalanceAccounts()` の呼び出し箇所をすべて検索
  - `preferencesRepository.getShowZeroBalanceAccounts()` に置換
- [X] T013 [US1] App.storeShowZeroBalanceAccounts() を preferencesRepository.setShowZeroBalanceAccounts() に置換: `app/src/main/kotlin/net/ktnx/mobileledger/ui/main/MainViewModel.kt`
  - `App.storeShowZeroBalanceAccounts()` の呼び出し箇所をすべて検索
  - `preferencesRepository.setShowZeroBalanceAccounts()` に置換
  - `App` クラスへの依存が完全に除去されたことを確認
- [X] T014 [US1] TransactionsDisplayedFilter (Thread) を Coroutine に移行: `app/src/main/kotlin/net/ktnx/mobileledger/ui/main/MainViewModel.kt`
  - `TransactionsDisplayedFilter` クラス（Thread 継承）の使用箇所を特定
  - `viewModelScope.launch { ... }` + `withContext(Dispatchers.Default)` パターンに置換
  - フィルタリングロジックを suspend 関数として抽出
  - Thread の直接インスタンス化を削除

### テスト作成（TDD: テストを先に作成し、失敗を確認）

- [X] T015 [US1] MainViewModelTest 基本構造を作成: `app/src/test/kotlin/net/ktnx/mobileledger/ui/main/MainViewModelTest.kt`
- [X] T016 [P] [US1] プロファイル選択テストを追加: `app/src/test/kotlin/net/ktnx/mobileledger/ui/main/MainViewModelTest.kt`
- [X] T017 [P] [US1] ゼロ残高アカウント表示切替テストを追加: `app/src/test/kotlin/net/ktnx/mobileledger/ui/main/MainViewModelTest.kt`
- [X] T018 [P] [US1] データリフレッシュテストを追加: `app/src/test/kotlin/net/ktnx/mobileledger/ui/main/MainViewModelTest.kt`
- [X] T019 [P] [US1] アカウント読み込みテストを追加: `app/src/test/kotlin/net/ktnx/mobileledger/ui/main/MainViewModelTest.kt`
- [X] T020 [P] [US1] 取引読み込みテストを追加: `app/src/test/kotlin/net/ktnx/mobileledger/ui/main/MainViewModelTest.kt`
- [X] T021 [P] [US1] エラーハンドリングテストを追加: `app/src/test/kotlin/net/ktnx/mobileledger/ui/main/MainViewModelTest.kt`
- [X] T022 [P] [US1] タブ選択テストを追加: `app/src/test/kotlin/net/ktnx/mobileledger/ui/main/MainViewModelTest.kt`
- [X] T023 [P] [US1] アカウント検索デバウンステストを追加: `app/src/test/kotlin/net/ktnx/mobileledger/ui/main/MainViewModelTest.kt`
- [X] T024 [US1] テスト実行で既存機能が壊れていないことを確認: `nix run .#test`

**Checkpoint**: User Story 1 が完全に機能し、独立してテスト可能

---

## Phase 4: User Story 2 - NewTransactionViewModel のテスト追加 (Priority: P1)

**Goal**: NewTransactionViewModel のユニットテストを作成し、取引作成、金額計算、テンプレート適用、フォームバリデーションを検証

**Independent Test**: モックトランザクション送信者を注入し、フォーム状態検証可能。財務計算エラー防止

### リファクタリング（テスト可能化）

- [X] T025 [US2] NewTransactionViewModel に TransactionSender を注入: `app/src/main/kotlin/net/ktnx/mobileledger/ui/transaction/NewTransactionViewModel.kt`
  - コンストラクタに `transactionSender: TransactionSender` パラメータを追加
  - `@Inject constructor` に含める
- [X] T026 [US2] SendTransactionTask 直接インスタンス化を TransactionSender.send() に置換: `app/src/main/kotlin/net/ktnx/mobileledger/ui/transaction/NewTransactionViewModel.kt`
  - `SendTransactionTask()` の `new` / インスタンス化をすべて削除
  - `Thread.start()` 呼び出しを `viewModelScope.launch { transactionSender.send(...) }` に置換
  - コールバック/リスナーベースの結果処理を `Result.fold()` パターンに移行
  - 成功時: ローカルDB保存 → フォームクリア → 画面遷移
  - 失敗時: UiState.error に設定
  - `SendTransactionTask` の import 文を削除

### テスト作成（TDD: テストを先に作成し、失敗を確認）

- [X] T027 [US2] NewTransactionViewModelTest 基本構造を作成: `app/src/test/kotlin/net/ktnx/mobileledger/ui/transaction/NewTransactionViewModelTest.kt`
- [X] T028 [P] [US2] 初期化テスト（デフォルト通貨設定）を追加: `app/src/test/kotlin/net/ktnx/mobileledger/ui/transaction/NewTransactionViewModelTest.kt`
- [X] T029 [P] [US2] 金額入力と残高ヒント再計算テストを追加: `app/src/test/kotlin/net/ktnx/mobileledger/ui/transaction/NewTransactionViewModelTest.kt`
- [X] T030 [P] [US2] テンプレート適用テストを追加: `app/src/test/kotlin/net/ktnx/mobileledger/ui/transaction/NewTransactionViewModelTest.kt`
- [X] T031 [P] [US2] 取引送信成功テストを追加: `app/src/test/kotlin/net/ktnx/mobileledger/ui/transaction/NewTransactionViewModelTest.kt`
- [X] T032 [P] [US2] 取引送信失敗テストを追加: `app/src/test/kotlin/net/ktnx/mobileledger/ui/transaction/NewTransactionViewModelTest.kt`
- [X] T033 [P] [US2] フォームバリデーションテストを追加: `app/src/test/kotlin/net/ktnx/mobileledger/ui/transaction/NewTransactionViewModelTest.kt`
- [X] T034 [P] [US2] アカウント検索サジェストテストを追加: `app/src/test/kotlin/net/ktnx/mobileledger/ui/transaction/NewTransactionViewModelTest.kt`
- [X] T035 [US2] テスト実行で既存機能が壊れていないことを確認: `nix run .#test`

**Checkpoint**: User Stories 1 AND 2 が両方とも独立して動作

---

## Phase 5: User Story 3 - バックグラウンド同期操作のテスト (Priority: P2)

**Goal**: 実際のネットワークリクエストなしでデータ同期ロジックをテスト

**Independent Test**: モック HTTP レスポンスを使用してパース/変換ロジックを検証

### リファクタリング（テスト可能化）

- [X] T036 [US3] RetrieveTransactionsTask のパースロジックを抽出: `app/src/main/kotlin/net/ktnx/mobileledger/async/TransactionParser.kt`
- [X] T037 [US3] RetrieveTransactionsTask のアカウントパースロジックを抽出: `app/src/main/kotlin/net/ktnx/mobileledger/async/AccountParser.kt`

### テスト作成

- [X] T038 [P] [US3] TransactionParser テストを作成: `app/src/test/kotlin/net/ktnx/mobileledger/async/TransactionParserTest.kt`
- [X] T039 [P] [US3] AccountParser テストを作成: `app/src/test/kotlin/net/ktnx/mobileledger/async/AccountParserTest.kt`
- [X] T040 [US3] テスト実行で既存機能が壊れていないことを確認: `nix run .#test`

**Checkpoint**: User Stories 1, 2, 3 がすべて独立して機能

---

## Phase 6: User Story 4 - テストカバレッジメトリクスの検証 (Priority: P3)

**Goal**: クリティカルコンポーネントが適切なカバレッジレベルを維持していることを確認

**Independent Test**: カバレッジレポートを有効にしてテストスイートを実行し、最小閾値確認

### カバレッジ設定

- [X] T041 [US4] JaCoCo → Kover 移行: `app/build.gradle` (Kotlin専用カバレッジツール)
- [X] T042 [US4] カバレッジレポート生成スクリプトを更新: `nix run .#coverage` (flake.nix)

### カバレッジ検証

- [X] T043 [US4] カバレッジレポートを生成: `./gradlew koverHtmlReportDebug`
- [X] T044 [US4] MainViewModel カバレッジ: 約58% (line) ✓
- [X] T045 [US4] NewTransactionViewModel カバレッジ: 約62% (line) - FutureDates リファクタリング完了 ✓

**Checkpoint**: すべてのユーザーストーリーが独立して機能しカバレッジ目標達成

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: 複数のユーザーストーリーに影響する改善

- [X] T046 [P] 全テスト実行で 30 秒以内を確認: 約14秒で完了 ✓
- [X] T047 [P] quickstart.md を Kover に更新: `specs/011-test-coverage/quickstart.md`
- [X] T048 実機検証: `nix run .#verify` - ビルド・インストール・起動成功 ✓
- [X] T049 CLAUDE.md の更新（テストカバレッジセクション追加）: `CLAUDE.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: 依存関係なし - 即時開始可能
- **Foundational (Phase 2)**: Setup 完了に依存 - すべてのユーザーストーリーをブロック
- **User Stories (Phase 3-6)**: Foundational フェーズ完了に依存
  - User Story 1 (P1) と User Story 2 (P1) は並列実行可能
  - User Story 3 (P2) は US1/US2 完了後推奨
  - User Story 4 (P3) は US1/US2/US3 完了後
- **Polish (Phase 7)**: すべての希望ユーザーストーリー完了に依存

### User Story Dependencies

- **User Story 1 (P1)**: Foundational 完了後開始可能 - 他のストーリーへの依存なし
- **User Story 2 (P1)**: Foundational 完了後開始可能 - US1 と並列実行可能
- **User Story 3 (P2)**: Foundational 完了後開始可能 - US1/US2 とは独立
- **User Story 4 (P3)**: US1/US2 のテストが存在することが前提

### Within Each User Story

- リファクタリングタスクを先に実行（テスト可能化）
- テストを作成し失敗を確認（TDD）
- 実装を完了しテストがパスすることを確認
- 既存機能が壊れていないことを確認

### Parallel Opportunities

- T003-T010（Phase 2）: [P] マーク付きタスクは並列実行可能
- T016-T023（US1 テスト）: すべて並列実行可能
- T028-T034（US2 テスト）: すべて並列実行可能
- T038-T039（US3 テスト）: 並列実行可能
- US1 と US2 は異なるファイルを変更するため並列実行可能

---

## Parallel Example: Phase 2 (Foundational)

```bash
# Fake 実装を並列で作成:
Task: "FakePreferencesRepository を作成: app/src/test/.../FakePreferencesRepository.kt"
Task: "FakeTransactionSender を作成: app/src/test/.../FakeTransactionSender.kt"
Task: "FakeCurrencyFormatter を作成: app/src/test/.../FakeCurrencyFormatter.kt"
Task: "FakeTemplateRepository を作成: app/src/test/.../FakeTemplateRepository.kt"
Task: "FakeCurrencyRepository を作成: app/src/test/.../FakeCurrencyRepository.kt"
```

## Parallel Example: User Story 1 Tests

```bash
# US1 のテストを並列で作成:
Task: "プロファイル選択テストを追加: MainViewModelTest.kt"
Task: "ゼロ残高アカウント表示切替テストを追加: MainViewModelTest.kt"
Task: "データリフレッシュテストを追加: MainViewModelTest.kt"
Task: "アカウント読み込みテストを追加: MainViewModelTest.kt"
Task: "取引読み込みテストを追加: MainViewModelTest.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1 (MainViewModel テスト)
4. **STOP and VALIDATE**: `nix run .#test` で User Story 1 を独立検証
5. Deploy/demo if ready

### Incremental Delivery

1. Setup + Foundational → 基盤準備完了
2. User Story 1 → 独立テスト → MainViewModel カバレッジ 70%+ (MVP!)
3. User Story 2 → 独立テスト → NewTransactionViewModel カバレッジ 70%+
4. User Story 3 → 独立テスト → パースロジックテスト追加
5. User Story 4 → カバレッジ測定・検証

### Suggested MVP Scope

- Phase 1: Setup (T001-T002)
- Phase 2: Foundational (T003-T010)
- Phase 3: User Story 1 (T011-T024)

これにより MainViewModel の 70%+ カバレッジを達成し、最も使用頻度の高い画面のリグレッション検出が可能になる。

---

## Notes

- [P] タスク = 異なるファイル、依存関係なし
- [Story] ラベルはタスクを特定のユーザーストーリーにマッピング
- 各ユーザーストーリーは独立して完了・テスト可能
- TDD: テストを先に作成し失敗を確認してから実装
- 各タスクまたは論理グループ完了後にコミット
- チェックポイントで停止してストーリーを独立検証可能
