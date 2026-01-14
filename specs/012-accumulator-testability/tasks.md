# Tasks: TransactionAccumulator テスト可能性向上

**Input**: Design documents from `/specs/012-accumulator-testability/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, quickstart.md

**Tests**: テストタスクを含む（ユーザーストーリー 1 がテスト可能性の実現を目的としているため）

**Organization**: タスクはユーザーストーリーごとにグループ化され、独立した実装とテストを可能にする。

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 並列実行可能（異なるファイル、依存関係なし）
- **[Story]**: このタスクが属するユーザーストーリー（例: US1, US2, US3）
- 説明には正確なファイルパスを含む

## Path Conventions

- **Mobile (Android)**: `app/src/main/kotlin/`, `app/src/test/kotlin/`

---

## Phase 1: Setup (共有インフラストラクチャ)

**Purpose**: 本機能はリファクタリングのため、セットアップタスクは不要

なし。既存のプロジェクト構造、テストインフラ、Hilt DI 設定を使用。

---

## Phase 2: Foundational (ブロッキング前提条件)

**Purpose**: すべてのユーザーストーリーの実装前に完了する必要があるコア変更

**⚠️ CRITICAL**: ユーザーストーリーの作業は、このフェーズが完了するまで開始できない

- [X] T001 TransactionAccumulator のコンストラクタに `currencyFormatter: CurrencyFormatter` パラメータを追加 in `app/src/main/kotlin/net/ktnx/mobileledger/async/TransactionAccumulator.kt`
- [X] T002 `summarizeRunningTotal()` で `App.currencyFormatter()` を注入されたフォーマッターに置き換え in `app/src/main/kotlin/net/ktnx/mobileledger/async/TransactionAccumulator.kt`
- [X] T003 MainViewModel に `CurrencyFormatter` を `@Inject` で追加 in `app/src/main/kotlin/net/ktnx/mobileledger/ui/main/MainViewModel.kt`
- [X] T004 MainViewModel の TransactionAccumulator インスタンス化箇所（line 631 付近）を更新 in `app/src/main/kotlin/net/ktnx/mobileledger/ui/main/MainViewModel.kt`
- [X] T005 MainViewModel の TransactionAccumulator インスタンス化箇所（line 797 付近）を更新 in `app/src/main/kotlin/net/ktnx/mobileledger/ui/main/MainViewModel.kt`

**Checkpoint**: 基盤が準備完了 - ユーザーストーリーの実装を開始可能

---

## Phase 3: User Story 1 - 開発者が TransactionAccumulator のユニットテストを作成する (Priority: P1) 🎯 MVP

**Goal**: FakeCurrencyFormatter を使用して TransactionAccumulator のユニットテストを作成し、累計残高計算ロジックを検証する

**Independent Test**: `nix run .#test` を実行し、TransactionAccumulatorTest がパスすることを確認

### Implementation for User Story 1

- [X] T006 [US1] TransactionAccumulatorTest.kt テストファイルを作成 in `app/src/test/kotlin/net/ktnx/mobileledger/async/TransactionAccumulatorTest.kt`
- [X] T007 [US1] 単一取引の累計計算テストを追加 in `app/src/test/kotlin/net/ktnx/mobileledger/async/TransactionAccumulatorTest.kt`
- [X] T008 [US1] 複数取引の累計計算テストを追加 in `app/src/test/kotlin/net/ktnx/mobileledger/async/TransactionAccumulatorTest.kt`
- [X] T009 [US1] 単一通貨の累計フォーマットテストを追加 in `app/src/test/kotlin/net/ktnx/mobileledger/async/TransactionAccumulatorTest.kt`
- [X] T010 [US1] 複数通貨の累計フォーマットテストを追加 in `app/src/test/kotlin/net/ktnx/mobileledger/async/TransactionAccumulatorTest.kt`
- [X] T011 [US1] 注入されたフォーマッターが使用されることを検証するテストを追加 in `app/src/test/kotlin/net/ktnx/mobileledger/async/TransactionAccumulatorTest.kt`

**Checkpoint**: User Story 1 が完全に機能し、独立してテスト可能

---

## Phase 4: User Story 2 - 開発者がデバイスやエミュレーターなしでテストを実行する (Priority: P2)

**Goal**: TransactionAccumulator テストが JVM ユニットテストとして実行されることを確認

**Independent Test**: `nix run .#test` を実行し、テストが `app/src/test/` から実行されることを確認

### Implementation for User Story 2

- [X] T012 [US2] テストファイルが `app/src/test/kotlin/` に配置されていることを確認（androidTest ではなく）in `app/src/test/kotlin/net/ktnx/mobileledger/async/TransactionAccumulatorTest.kt`
- [X] T013 [US2] テストが Android 依存関係なしで実行されることを `nix run .#test` で確認

**Checkpoint**: User Stories 1 AND 2 が両方とも独立して動作

---

## Phase 5: User Story 3 - 既存のアプリケーション動作が変更されない (Priority: P1)

**Goal**: 本番環境での通貨フォーマット出力がリファクタリング前後で同一であることを確認

**Independent Test**: アプリをデバイスにインストールし、取引一覧の通貨表示を確認

### Implementation for User Story 3

- [X] T014 [US3] `nix run .#build` でビルドが成功することを確認
- [X] T015 [US3] `nix run .#lint` で新しいエラーがないことを確認
- [X] T016 [US3] `nix run .#verify` でアプリをインストールし、取引一覧の通貨表示を視覚的に確認
- [X] T017 [US3] TransactionAccumulator.kt に `App.currencyFormatter()` への参照がないことを確認 in `app/src/main/kotlin/net/ktnx/mobileledger/async/TransactionAccumulator.kt`

**Checkpoint**: すべてのユーザーストーリーが独立して機能する

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: 複数のユーザーストーリーに影響する改善

- [X] T018 既存の MainViewModelTest がパスすることを確認 in `app/src/test/kotlin/net/ktnx/mobileledger/ui/main/MainViewModelTest.kt`
- [X] T019 quickstart.md の成功基準チェックリストをすべて検証

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 2)**: セットアップなし - 即座に開始可能
- **User Story 1 (Phase 3)**: Foundational フェーズの完了に依存
- **User Story 2 (Phase 4)**: User Story 1 の完了に依存（テストファイルが必要）
- **User Story 3 (Phase 5)**: Foundational フェーズの完了に依存（US1 と並行可能）
- **Polish (Phase 6)**: すべてのユーザーストーリーの完了に依存

### User Story Dependencies

- **User Story 1 (P1)**: Foundational (Phase 2) 完了後に開始可能 - 他のストーリーへの依存なし
- **User Story 2 (P2)**: User Story 1 完了後に開始 - US1 のテストファイルに依存
- **User Story 3 (P1)**: Foundational (Phase 2) 完了後に開始可能 - US1 と並行して実行可能

### Within Each Phase

- T001 → T002（summarizeRunningTotal はコンストラクタ変更後に修正）
- T003 → T004/T005（MainViewModel に CurrencyFormatter を追加後にインスタンス化を更新）
- T006 → T007-T011（テストファイル作成後にテストケースを追加）

### Parallel Opportunities

- **Foundational Phase**: T004 と T005 は同じファイル内だが、T003 の後に順次実行
- **User Story 1 Phase**: T007-T011 は同じファイルのため順次実行
- **User Story 3 Phase**: T014, T015 は並列実行可能（異なるコマンド）

---

## Parallel Example: Foundational Phase

```bash
# T001 と T002 は同じファイルのため順次実行
Task: "TransactionAccumulator コンストラクタ変更"
Task: "summarizeRunningTotal のフォーマッター置き換え"

# T003, T004, T005 は同じファイルのため順次実行
Task: "MainViewModel に CurrencyFormatter 注入"
Task: "TransactionAccumulator インスタンス化更新 (line 631)"
Task: "TransactionAccumulator インスタンス化更新 (line 797)"
```

---

## Implementation Strategy

### MVP First (User Story 1 + User Story 3)

1. Complete Phase 2: Foundational（コンストラクタ変更と呼び出し元の更新）
2. Complete Phase 3: User Story 1（テスト作成）
3. Complete Phase 5: User Story 3（動作確認）
4. **STOP and VALIDATE**: `nix run .#verify` で全体検証
5. Deploy/demo if ready

### Incremental Delivery

1. Foundational 完了 → コード変更完了
2. User Story 1 追加 → テスト可能性達成 → Test pass (MVP!)
3. User Story 2 追加 → JVM テスト確認
4. User Story 3 追加 → 動作確認 → Deploy ready
5. 各ストーリーが前のストーリーを壊さずに価値を追加

---

## Notes

- [P] タスク = 異なるファイル、依存関係なし
- [Story] ラベルはタスクを特定のユーザーストーリーにマッピング
- 各ユーザーストーリーは独立して完了・テスト可能
- 各タスクまたは論理グループの後にコミット
- 任意のチェックポイントで停止してストーリーを独立して検証可能
- 回避: 曖昧なタスク、同じファイルの競合、独立性を壊すクロスストーリー依存
