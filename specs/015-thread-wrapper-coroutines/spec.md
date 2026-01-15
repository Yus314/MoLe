# Feature Specification: Thread Wrapper to Coroutines Migration

**Feature Branch**: `015-thread-wrapper-coroutines`
**Created**: 2026-01-15
**Status**: Draft
**Input**: User description: "現在のthreadが残っているwrapperをpure coroutinesに移行することでテストをしやすくしてください。"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Pure Coroutine Transaction Sync (Priority: P1)

開発者として、取引同期処理（RetrieveTransactionsTask）がpure coroutineで実装されることで、単体テストでの検証が容易になり、テストの実行時間が短縮される。

**Why this priority**: RetrieveTransactionsTaskは取引同期の中核であり、現在rawスレッド内で`runBlocking`を使用するアンチパターンが存在する。これがテストの複雑性と実行時間の主な原因となっている。

**Independent Test**: RetrieveTransactionsTaskの同期処理をFake repositoryとTestDispatcherで完全にテストでき、実際のスレッドを起動せずに同期結果を検証できる。

**Acceptance Scenarios**:

1. **Given** TestDispatcherを使用したテスト環境、**When** 取引同期処理を実行する、**Then** `advanceUntilIdle()`で同期完了まで時間を進めることができ、実際の待機時間なしでテストが完了する
2. **Given** 同期処理中、**When** キャンセル信号を送信する、**Then** コルーチンが適切にキャンセルされ、リソースがクリーンアップされる
3. **Given** サーバーエラーが発生した場合、**When** 同期処理が失敗する、**Then** 例外がcoroutineのコンテキストで適切に伝播され、テストで捕捉できる

---

### User Story 2 - Testable Transaction Sender (Priority: P1)

開発者として、取引送信処理（SendTransactionTask）がpure coroutineで実装されることで、送信成功・失敗・タイムアウトのシナリオをFakeを使用して即座にテストできる。

**Why this priority**: 取引送信はユーザーにとって最も重要な操作であり、現在のスレッドベースの実装ではThread.sleep()によりテストが遅くなる。

**Independent Test**: FakeTransactionSenderの動作をTestDispatcherで制御し、成功・失敗・タイムアウトの全シナリオを即座にテストできる。

**Acceptance Scenarios**:

1. **Given** TestDispatcherを使用したテスト環境、**When** 取引送信処理をテストする、**Then** Thread.sleep()なしで送信完了を検証できる
2. **Given** ネットワークエラーが発生した場合、**When** 送信処理が失敗する、**Then** リトライロジックをTestDispatcherの時間操作でテストできる
3. **Given** Fakeの送信処理、**When** 任意のエラー条件を設定する、**Then** ViewModelのUI状態更新を検証できる

---

### User Story 3 - Coroutine-Based Backup Operations (Priority: P2)

開発者として、バックアップ/リストア処理（ConfigWriter/ConfigReader）がsuspend関数で実装されることで、バックアップ処理のテストが容易になる。

**Why this priority**: バックアップ操作は重要だが使用頻度は同期・送信より低い。ただし、現在の実装はrunBlockingを使用しており、コルーチンの利点を活かせていない。

**Independent Test**: Fake URIとTestDispatcherを使用して、バックアップの読み書きを即座にテストできる。

**Acceptance Scenarios**:

1. **Given** テスト用のFakeリポジトリ、**When** バックアップ処理を実行する、**Then** runBlockingなしでリポジトリへの保存を検証できる
2. **Given** バックアップファイルが破損している場合、**When** リストア処理を実行する、**Then** 適切な例外がcoroutineコンテキストで発生する

---

### User Story 4 - ViewModel Thread Elimination (Priority: P2)

開発者として、ViewModel内のスレッド（TransactionsDisplayedFilter、VersionDetectionThread）がviewModelScope.launchに置き換わることで、ViewModelのテストがシンプルになる。

**Why this priority**: ViewModelのテストは開発サイクルで頻繁に行われるため、スレッドの排除によりテストの信頼性と速度が向上する。

**Independent Test**: ViewModelのライフサイクルに連動したコルーチンスコープをテストで完全に制御できる。

**Acceptance Scenarios**:

1. **Given** TestDispatcherを使用したViewModelテスト、**When** フィルタ処理を実行する、**Then** Thread.interrupt()を使わずにキャンセルをテストできる
2. **Given** ViewModelのライフサイクル終了時、**When** スコープがキャンセルされる、**Then** 全ての進行中のコルーチンがキャンセルされる

---

### User Story 5 - Legacy Executor Removal (Priority: P3)

開発者として、レガシーのExecutorパターン（BaseDAO.asyncRunner、GeneralBackgroundTasks）が削除されることで、非同期処理の一貫性が向上する。

**Why this priority**: これらは直接的なテスト困難性の原因ではないが、コードベースの一貫性を保つために移行が必要。

**Independent Test**: 全てのDAO操作がsuspend関数として実装され、Repositoryを通じてテストできる。

**Acceptance Scenarios**:

1. **Given** TemplateRepositoryのテスト、**When** テンプレートを取得する、**Then** Executorを使用せずにFlow/suspend関数でデータが返される
2. **Given** GeneralBackgroundTasks、**When** コードベースを検索する、**Then** 使用箇所が存在しない（削除済み）

---

### Edge Cases

- コルーチンがキャンセル中にリソース（ネットワーク接続、ファイルハンドル）を適切に解放するか？
- 同時に複数の同期処理が開始された場合 → Mutexで単一実行を保証し、後続リクエストはキュー待ち
- ネットワーク接続が不安定な状態 → 指数バックオフ付きリトライ（最大3回、1s→2s→4s）で自動復旧を試行。リトライ回数超過時は `SyncError.NetworkError` を返す
- ViewModelのonClearedが呼ばれた際に進行中の処理が確実にキャンセルされるか？
- 長時間実行される同期処理中にアプリがバックグラウンドに移行した場合 → viewModelScopeで実行継続、Activity破棄時にキャンセル

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST replace all raw Thread subclasses (RetrieveTransactionsTask, SendTransactionTask, ConfigWriter, ConfigReader) with suspend functions
- **FR-002**: System MUST remove all runBlocking calls inside Thread.run() methods
- **FR-003**: System MUST replace Thread.interrupt() patterns with CancellationException-based coroutine cancellation
- **FR-004**: System MUST eliminate Thread.sleep() calls by using delay() in coroutines
- **FR-005**: System MUST replace ViewModel内のThread subclasses (TransactionsDisplayedFilter, VersionDetectionThread) with viewModelScope.launch
- **FR-006**: System MUST remove or deprecate Executor-based patterns (BaseDAO.asyncRunner, GeneralBackgroundTasks)
- **FR-007**: System MUST replace callback patterns (TaskCallback, AsyncResultCallback) with Flow or suspend functions
- **FR-008**: System MUST ensure all migrated code supports structured concurrency with proper cancellation propagation
- **FR-009**: System MUST maintain backward compatibility with existing Fake implementations for testing
- **FR-010**: System MUST provide CoroutineDispatcher injection for testability

### Key Entities

- **RetrieveTransactionsTask**: 取引同期の主要コンポーネント。サーバーから取引を取得し、ローカルDBに保存する
- **SendTransactionTask**: 取引送信コンポーネント。ローカルで作成した取引をサーバーに送信する
- **ConfigWriter/ConfigReader**: バックアップ/リストアコンポーネント。設定とデータのエクスポート/インポートを行う
- **TransactionSyncerImpl**: 同期処理のラッパー。現在Thread.join()をsuspendCancellableCoroutineで待機している
- **TransactionSenderImpl**: 送信処理のラッパー。現在callbackパターンでThreadの完了を待機している

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 全ての同期・送信処理のユニットテストがTestDispatcherを使用して実行でき、実時間待機なしで完了する
- **SC-002**: コードベースにおけるrawスレッド使用箇所（Thread subclass）が0件になる
- **SC-003**: runBlocking使用箇所がテスト以外のプロダクションコードで0件になる
- **SC-004**: Thread.sleep()使用箇所がプロダクションコードで0件になる
- **SC-005**: ViewModelのユニットテストカバレッジが現在の58%から70%以上に向上する
- **SC-006**: ユニットテストの実行時間が現在より30%以上短縮される

## Clarifications

### Session 2026-01-15

- Q: 同時に複数の同期処理が開始された場合の制御方法は？ → A: Mutexで同時に1つの同期のみ実行（後続はキュー待ち）
- Q: ネットワークエラー時のリトライ戦略は？ → A: 指数バックオフ付きリトライ（最大3回、1s→2s→4s）、リトライ回数超過時は `SyncError.NetworkError` を返す
- Q: バックグラウンド移行時の同期処理の動作は？ → A: viewModelScopeで実行継続（Activity破棄でキャンセル）

## Assumptions

- 既存のHiltモジュール構造（UseCaseModule、ServiceModule）を活用してDispatcherを注入する
- 既存のFake実装パターン（FakeTransactionSyncer、FakeTransactionSender等）を継続使用する
- Room DAOのsuspend関数サポートを活用する
- Coroutines 1.9.0の機能（特にcancellationサポート）を活用する
