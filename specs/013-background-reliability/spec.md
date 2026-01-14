# Feature Specification: バックグラウンド処理アーキテクチャの技術的負債解消

**Feature Branch**: `013-background-reliability`
**Created**: 2026-01-14
**Status**: Draft
**Input**: User description: "バックグラウンド処理アーキテクチャの技術的負債解消 - テスト容易性、Coroutines移行、Hilt DI、アーキテクチャ一貫性"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 同期処理のユニットテスト (Priority: P1)

開発者がサーバー同期処理（RetrieveTransactionsTask）のビジネスロジックに対するユニットテストを作成できる。現在814行のThread実装であり、ネットワーク通信から分離されたテストが不可能な状態を解消する。

**Why this priority**: 同期処理は最も複雑なコンポーネント（APIバージョン検出、複数パーサー、進捗報告）であり、テストなしでのリファクタリングはリスクが高い。テスト可能にすることで、安全な段階的改善が可能になる。

**Independent Test**: FakeTransactionSyncer を使用したユニットテストを作成し、ネットワーク接続なしで同期ロジック（パース、エラーハンドリング、進捗計算）をテストできることを確認する。

**Acceptance Scenarios**:

1. **Given** TransactionSyncer インターフェースが定義されている状態, **When** 開発者が FakeTransactionSyncer を作成する, **Then** ネットワーク接続なしで同期処理のテストが実行できる
2. **Given** 同期処理のテストコードが存在する状態, **When** CI でユニットテストを実行する, **Then** 5分以内にテスト結果が得られる（外部依存なし）
3. **Given** 同期処理でエラーが発生するシナリオ, **When** FakeTransactionSyncer でエラーをシミュレート, **Then** エラーハンドリングロジックがテストできる

---

### User Story 2 - Coroutines によるキャンセル制御 (Priority: P1)

バックグラウンド処理が Kotlin Coroutines で実装され、構造化された並行処理（Structured Concurrency）により適切にキャンセル可能になる。現在の Thread.interrupt() パターンから移行し、ViewModel の lifecycle に連動した処理制御を実現する。

**Why this priority**: Thread は interrupt() でのキャンセルが信頼性に欠け、アプリ終了時のデータ不整合リスクがある。Coroutines への移行は constitution 原則 VI への準拠に必須。

**Independent Test**: ViewModel の scope がキャンセルされた際に、実行中の同期処理が適切に停止することを確認する。

**Acceptance Scenarios**:

1. **Given** Coroutines で実装された同期処理が実行中, **When** ViewModel がクリアされる（画面遷移）, **Then** 処理が5秒以内に停止し、リソースが解放される
2. **Given** 同期処理が suspend function として実装されている状態, **When** isActive をチェックしながら処理を進行, **Then** キャンセル要求が各チェックポイントで反映される
3. **Given** 複数のバックグラウンド処理が同時実行中, **When** アプリがバックグラウンドに移動, **Then** supervisorScope により各処理が独立してキャンセル処理される

---

### User Story 3 - Hilt DI によるテスト用依存性注入 (Priority: P1)

すべてのバックグラウンド処理コンポーネントが Hilt DI で管理され、テスト時に Fake 実装への差し替えが可能になる。現在手動インスタンス化されているコンポーネント（RetrieveTransactionsTask、ConfigIO系）を DI コンテナで管理する。

**Why this priority**: DI なしではテスト時のモック注入ができず、ユニットテストが事実上不可能。constitution 原則 X の階層型アーキテクチャ準拠に必須。

**Independent Test**: テストコードで @HiltAndroidTest を使用し、Fake 実装がプロダクション実装と差し替わることを確認する。

**Acceptance Scenarios**:

1. **Given** TransactionSyncer が Hilt で @Singleton として提供される状態, **When** テストモジュールで FakeTransactionSyncer をバインド, **Then** テスト実行時に Fake が使用される
2. **Given** ViewModel が TransactionSyncer を @Inject で受け取る状態, **When** ViewModel のテストを作成, **Then** コンストラクタ注入で Fake を渡せる
3. **Given** BackupService が Hilt で管理される状態, **When** BackupsViewModel をテスト, **Then** バックアップ処理の成功/失敗をシミュレートできる

---

### User Story 4 - エラー伝播の一貫性 (Priority: P2)

バックグラウンド処理で発生したエラーが Result 型で一貫して UI レイヤーまで伝播される。現在のコールバックベースのエラーハンドリング（TaskCallback）から、Kotlin の Result<T> または sealed class を使用したエラーハンドリングに移行する。

**Why this priority**: P1（テスト容易性、Coroutines、DI）が達成された後に、エラーハンドリングパターンを統一することで保守性が向上する。

**Independent Test**: 各エラータイプ（ネットワーク、認証、タイムアウト、パース）をシミュレートし、UI レイヤーで適切なエラー状態が設定されることを確認する。

**Acceptance Scenarios**:

1. **Given** 同期処理が Result<SyncResult> を返す状態, **When** ネットワークエラーが発生, **Then** Result.failure(NetworkError) が返され、ViewModel が適切な UiState を設定
2. **Given** 取引送信が Result<Unit> を返す状態, **When** 認証エラーが発生, **Then** Result.failure(AuthenticationError) が返され、エラーメッセージが表示可能
3. **Given** バックアップ処理が Result を返す状態, **When** ファイル破損エラーが発生, **Then** Result.failure(FileCorruptedError) が返され、具体的なエラー情報が含まれる

---

### User Story 5 - バックアップ処理のテスト可能化 (Priority: P2)

バックアップ/リストア処理（ConfigReader/ConfigWriter）がインターフェース抽象化され、ユニットテスト可能になる。現在 Thread を直接継承し runBlocking で DB アクセスしているパターンから、suspend function とインターフェースベースの実装に移行する。

**Why this priority**: バックアップ処理は頻度が低いが、失敗時のデータロスリスクがある。テスト可能にすることで信頼性を向上させる。

**Independent Test**: FakeBackupService を使用して、バックアップ/リストアのロジック（JSON シリアライズ、データ検証）をテストする。

**Acceptance Scenarios**:

1. **Given** BackupService インターフェースが定義されている状態, **When** FakeBackupService を作成, **Then** ファイルシステムアクセスなしでバックアップロジックをテストできる
2. **Given** リストア処理がテスト可能な状態, **When** 不正な JSON をシミュレート, **Then** 適切なエラーが返され、データベースは変更されない
3. **Given** BackupsViewModel がテスト可能な状態, **When** バックアップ完了をシミュレート, **Then** 成功状態が UiState に反映される

---

### User Story 6 - データベース初期化の Coroutines 移行 (Priority: P3)

データベース初期化処理（DatabaseInitThread）が Coroutines で実装され、SplashActivity から適切に完了待ちができる。現在の LiveData + Thread パターンから、suspend function + StateFlow パターンに移行する。

**Why this priority**: 初期化処理は起動時の一度のみで、Thread でも動作している。他のコンポーネントの移行後に対応しても問題ない。

**Independent Test**: テストで初期化処理の完了を await でき、タイムアウトテストが可能になることを確認する。

**Acceptance Scenarios**:

1. **Given** DatabaseInitializer が suspend function として実装されている状態, **When** SplashActivity が初期化を呼び出す, **Then** lifecycleScope で完了を await できる
2. **Given** 初期化処理がテスト可能な状態, **When** Robolectric/テスト環境で実行, **Then** in-memory DB で初期化ロジックをテストできる
3. **Given** 初期化が StateFlow で状態を公開する状態, **When** 初期化完了, **Then** isInitialized が true になり、メイン画面に遷移できる

---

### Edge Cases

- 同期処理中に ViewModel がクリアされた場合、処理はどうなるか？
  → Coroutines の structured concurrency により自動キャンセル。進行中の DB トランザクションはロールバック。
- Hilt によるインスタンス生成でメモリリークは起きないか？
  → @Singleton スコープの適切な使用と、ViewModel スコープの分離により防止。
- 既存のコールバックベースのコードとの互換性は？
  → 移行期間中は suspendCancellableCoroutine でラップする（TransactionSender パターン）。
- テスト環境で Room DB が正しく動作するか？
  → in-memory Room DB または Robolectric で対応。FakeRepository も提供。
- API バージョン自動検出ロジックを分離した場合、既存動作に影響は？
  → VersionDetector インターフェースとして抽出し、既存ロジックをそのまま実装に移行。

## Requirements *(mandatory)*

### Functional Requirements

#### インターフェース抽象化

- **FR-001**: システムは TransactionSyncer インターフェースを提供し、サーバー同期処理のビジネスロジックをネットワーク通信から分離しなければならない
- **FR-002**: システムは BackupService インターフェースを提供し、バックアップ/リストア処理をファイルシステムから分離しなければならない
- **FR-003**: システムは VersionDetector インターフェースを提供し、API バージョン検出ロジックを独立してテスト可能にしなければならない
- **FR-004**: システムは DatabaseInitializer インターフェースを提供し、初期化処理をテスト可能にしなければならない

#### Coroutines 移行

- **FR-005**: すべてのバックグラウンド処理は suspend function として実装され、Dispatchers.IO で実行されなければならない
- **FR-006**: すべてのバックグラウンド処理は isActive をチェックし、キャンセル要求に5秒以内に応答しなければならない
- **FR-007**: 長時間実行される処理（同期、バックアップ）は進捗を Flow<Progress> で公開しなければならない
- **FR-008**: エラーは Result<T> 型で返され、呼び出し元で適切にハンドリングされなければならない

#### Hilt DI 統合

- **FR-009**: すべてのバックグラウンド処理インターフェースは Hilt @Module で提供されなければならない
- **FR-010**: 各インターフェースには @Singleton スコープの実装クラスがバインドされなければならない
- **FR-011**: テスト用に各インターフェースの Fake 実装が提供されなければならない
- **FR-012**: ViewModel は @Inject コンストラクタでバックグラウンド処理インターフェースを受け取らなければならない

#### エラーハンドリング

- **FR-013**: システムは sealed class でエラータイプを定義し、NetworkError, AuthenticationError, TimeoutError, ParseError, FileError を含まなければならない
- **FR-014**: 各エラータイプはユーザー向けメッセージリソースID と技術的詳細を含まなければならない
- **FR-015**: ViewModel は Result.failure をキャッチし、UiState.error に適切なエラー情報を設定しなければならない

#### 後方互換性

- **FR-016**: 既存の BackgroundTaskManager インターフェースとの互換性を維持しなければならない
- **FR-017**: 既存の AppStateService インターフェースとの互換性を維持しなければならない
- **FR-018**: 移行期間中、既存の Thread ベース実装は suspend function でラップして使用できなければならない

### Key Entities

- **TransactionSyncer**: サーバー同期処理を抽象化するインターフェース。fun sync(profile: Profile): Flow<SyncProgress> を提供
- **SyncResult**: 同期結果を表すデータクラス。同期された取引数、勘定科目数、検出された API バージョンを含む
- **BackupService**: バックアップ/リストア処理を抽象化するインターフェース。suspend fun backup/restore を提供
- **VersionDetector**: API バージョン検出を抽象化するインターフェース。suspend fun detectVersion(profile: Profile): Result<API> を提供
- **DatabaseInitializer**: データベース初期化を抽象化するインターフェース。suspend fun initialize(): Result<Unit> を提供
- **SyncError**: エラータイプを表す sealed class。サブタイプとして NetworkError, AuthenticationError, TimeoutError, ParseError, FileError を含む
- **Progress**: 進捗情報を表すデータクラス。current: Int, total: Int, message: String? を含む

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 対象の5コンポーネント（TransactionSyncer, ConfigBackup, VersionDetector, DatabaseInitializer, TransactionSender）すべてがインターフェース抽象化される
- **SC-002**: 対象の5コンポーネントすべてが Hilt DI で管理され、@Inject で取得可能になる
- **SC-003**: 対象の5コンポーネントすべてに対応する Fake 実装が存在し、テストで使用可能になる
- **SC-004**: バックグラウンド処理の主要ロジックに対するユニットテストカバレッジが 50% 以上達成される
- **SC-005**: すべてのバックグラウンド処理が suspend function として実装され、Coroutines でキャンセル可能になる
- **SC-006**: ViewModel の scope キャンセル時に、実行中の処理が5秒以内に停止することが確認される
- **SC-007**: constitution 原則 II（テスト駆動開発）、VI（Coroutines ルール）、X（階層型アーキテクチャ）への違反が解消される

## Assumptions

- 既存の TransactionSender パターン（インターフェース + suspendCancellableCoroutine ラップ）を他のコンポーネントにも適用する
- TransactionSyncer の移行は「ラップ後段階的移行」戦略を採用：まず suspendCancellableCoroutine で Thread をラップしてインターフェースを公開し、その後内部を段階的に Coroutines 化する
- 既存の BackgroundTaskManager, AppStateService, CurrencyFormatter インターフェースは変更せず維持する
- パーサー（AccountParser, TransactionParser）は既にテスト可能な純粋関数として実装されており、変更不要
- 移行は段階的に行い、各コンポーネントごとにブランチを分けることも可能
- 移行順序は複雑度順：TransactionSyncer → ConfigBackup → VersionDetector → DatabaseInitializer（TransactionSender は既存パターンとして参照）
- Room DAO の suspend function 化は既に完了しており、追加作業は不要

## Out of Scope

- UI/UX デザインの変更（エラー表示方法の改善は別機能）
- 新しいバックグラウンド機能の追加
- サーバー API 仕様の変更
- パフォーマンス最適化（本機能の範囲外だが、副次的に改善される可能性あり）
- 既存の Thread ベース実装の完全削除（移行期間中は共存可能）
- AndroidX WorkManager への移行（将来の検討事項）

## Clarifications

### Session 2026-01-14

- Q: SyncService（814行）の Coroutines 移行戦略は？ → A: suspendCancellableCoroutine でラップ後、内部を段階的に Coroutines 化
- Q: 5コンポーネントの移行順序は？ → A: 複雑度順（SyncService → BackupService → VersionDetector → DatabaseInitializer）
- Q: Coroutines 移行後のログ出力戦略は？ → A: 現状維持（Android Log 使用、変更なし）
