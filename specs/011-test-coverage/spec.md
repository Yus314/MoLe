# Feature Specification: クリティカルコンポーネントのテストカバレッジ向上

**Feature Branch**: `011-test-coverage`
**Created**: 2026-01-13
**Status**: Draft
**Input**: User description: "MoLe アプリのテストカバレッジを向上させたい。現状: テストカバレッジ9%、MainViewModel (853行) と NewTransactionViewModel (985行) にテストなし。問題: Thread クラス使用、App シングルトン直接参照、重複コードがテスト作成を困難にしている。目標: クリティカルコンポーネントのテストカバレッジ80%以上を達成するため、テスト可能な構造にリファクタリングしてからテストを追加する。"

## Clarifications

### Session 2026-01-13

- Q: ViewModelの操作中にリポジトリ例外が発生した場合、どのように処理すべきか？ → A: キャッチしてUI状態にエラーを発行（UiState.error経由）
- Q: 長時間実行される操作中にコルーチンがキャンセルされた場合、どのように処理すべきか？ → A: CancellationExceptionを再スロー（標準コルーチン動作）
- Q: リポジトリが期待されるデータに対してnullを返した場合、どのように処理すべきか？ → A: 空リスト/デフォルト値を使用（null回避）
- Q: このフィーチャーで明示的にスコープ外とすべきものは？ → A: UIテスト（Compose/Instrumentation）はスコープ外
- Q: TransactionSenderのインターフェース設計は？ → A: suspend fun send(): Result<Unit>（コルーチン + Result型）

## User Scenarios & Testing *(mandatory)*

### User Story 1 - MainViewModelのテスト追加 (Priority: P1)

開発者として、MainViewModelのユニットテストを作成し、プロファイル選択、アカウント読み込み、取引読み込み、リフレッシュ操作が正しく動作することを検証したい。現在、ViewModelは `App.getShowZeroBalanceAccounts()` と `App.storeShowZeroBalanceAccounts()` を直接参照しており、Androidフレームワークのコンテキストが必要なため、インストルメンテーションなしでのユニットテストが不可能である。

**Why this priority**: MainViewModelはメイン画面の状態を管理する中心的なコンポーネント。807行のこのコンポーネントに対するテストがなければ、変更によりアカウント表示、取引表示、データ更新といったコアユーザーワークフローに影響するリグレッションが発生するリスクがある。

**Independent Test**: モックプリファレンスリポジトリを作成し、プロファイル選択がUI状態を正しく更新することを検証することで完全にテスト可能。最も使用頻度の高い画面のリグレッションを検出する価値を提供。

**Acceptance Scenarios**:

1. **Given** 依存関係が注入されたMainViewModel, **When** 開発者がユニットテストを実行する, **Then** Androidフレームワークや実際のデータベースなしでテストが実行される
2. **Given** モックリポジトリを持つMainViewModel, **When** `selectProfile(profileId)` が呼ばれる, **Then** UI状態が選択されたプロファイルを反映し、ドロワーが閉じる
3. **Given** モックリポジトリを持つMainViewModel, **When** `toggleZeroBalanceAccounts()` が呼ばれる, **Then** プリファレンスが永続化され、アカウントが再読み込みされる

---

### User Story 2 - NewTransactionViewModelのテスト追加 (Priority: P1)

開発者として、NewTransactionViewModelのユニットテストを作成し、取引作成、金額計算、テンプレート適用、フォームバリデーションが正しく動作することを検証したい。現在、ViewModelは `SendTransactionTask`（Threadのサブクラス）を直接インスタンス化しており、実際のネットワーク呼び出しなしでは送信フローのテストが不可能である。

**Why this priority**: NewTransactionViewModelは取引入力ワークフローを処理する（985行）。金額計算やバリデーションのバグは財務データの整合性に直接影響する。

**Independent Test**: モックトランザクション送信者を注入し、フォーム状態が正しく更新されることを検証することで完全にテスト可能。財務計算エラーを防ぐ価値を提供。

**Acceptance Scenarios**:

1. **Given** 依存関係が注入されたNewTransactionViewModel, **When** 開発者がユニットテストを実行する, **Then** ネットワークやAndroidフレームワークなしでテストが実行される
2. **Given** モックリポジトリを持つNewTransactionViewModel, **When** 複数のアカウントに金額が入力される, **Then** 残高がamountヒントで正しく計算される
3. **Given** モックテンプレートリポジトリを持つNewTransactionViewModel, **When** テンプレートが適用される, **Then** アカウント行がテンプレートの値で設定される

---

### User Story 3 - バックグラウンド同期操作のテスト (Priority: P2)

開発者として、実際のネットワークリクエストを実行せずにデータ同期ロジックをテストしたい。現在、`RetrieveTransactionsTask` は Thread を継承し、ネットワークI/Oを直接実行しており、関心事が混在して分離テストが困難である。

**Why this priority**: バックグラウンド同期はデータ整合性に影響する。パースとデータ変換ロジックをネットワークI/Oから分離してテストすることで信頼性が向上する。

**Independent Test**: モックHTTPレスポンスを提供し、パースされたアカウント/取引データを検証することで完全にテスト可能。同期ロジックが様々なサーバーレスポンス形式を正しく処理することを保証する価値を提供。

**Acceptance Scenarios**:

1. **Given** モックHTTPレスポンスを持つ取引パースロジック, **When** JSON取引データがパースされる, **Then** LedgerTransactionオブジェクトが正しい日付、説明、アカウントを含む
2. **Given** モックHTTPレスポンスを持つアカウントパースロジック, **When** JSONアカウントデータがパースされる, **Then** LedgerAccountオブジェクトが正しい名前と残高を含む

---

### User Story 4 - テストカバレッジメトリクスの検証 (Priority: P3)

開発者またはCIシステムとして、クリティカルコンポーネントが適切なカバレッジレベルを維持していることを確認するためにテストカバレッジを測定したい。プロジェクトは主要なパッケージのカバレッジメトリクスを報告すべきである。

**Why this priority**: カバレッジメトリクスはテスト進捗に関する客観的なフィードバックを提供し、テストされていないコードパスの特定に役立つ。

**Independent Test**: カバレッジレポートを有効にしてテストスイートを実行し、クリティカルパッケージが最小閾値を満たしていることを確認することで完全に検証可能。テスト進捗の可視性を提供する価値がある。

**Acceptance Scenarios**:

1. **Given** テストスイート, **When** カバレッジレポートが生成される, **Then** 各パッケージのカバレッジ率が表示される
2. **Given** テストを持つクリティカルViewModel, **When** カバレッジが測定される, **Then** 行カバレッジが最小閾値を満たす

---

### Edge Cases

- リポジトリメソッドがViewModel操作中に例外をスローした場合：ViewModelはキャッチしてUiState.errorに発行し、UIがエラーを表示する
- 長時間実行される操作中のコルーチンキャンセル：CancellationExceptionを再スローし、標準コルーチン動作に従う
- リポジトリが期待されるデータに対してnullを返した場合：空リスト/デフォルト値を使用し、nullを回避する

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: システムはMainViewModelをAndroidフレームワーク依存なしでユニットテストでインスタンス化できなければならない
- **FR-002**: システムはNewTransactionViewModelをAndroidフレームワーク依存なしでユニットテストでインスタンス化できなければならない
- **FR-003**: システムはAppシングルトンのプリファレンス（残高ゼロアカウント設定）のためのテストでモック可能な抽象化を提供しなければならない
- **FR-004**: システムは取引送信のためのテストでモック可能な抽象化を提供しなければならない
- **FR-005**: システムはリファクタリング後も既存の機能を維持しなければならない（エンドユーザーへの動作変更なし）
- **FR-006**: システムはプロファイル選択、アカウント読み込み、取引読み込み、ゼロ残高アカウント表示切替、データリフレッシュ、タブ選択、アカウント検索デバウンス、エラーハンドリングをカバーするMainViewModelのユニットテストを含まなければならない
- **FR-007**: システムは初期化（デフォルト通貨設定）、金額計算、フォームバリデーション、テンプレート適用、取引送信（成功/失敗）、アカウント検索サジェストをカバーするNewTransactionViewModelのユニットテストを含まなければならない
- **FR-008**: システムは接続されたデバイスを必要とせずに `nix run .#test` でテスト実行をサポートしなければならない

### Key Entities *(include if feature involves data)*

- **PreferencesRepository**: アプリケーションレベルのプリファレンスの抽象化（Appシングルトンの直接アクセスを置き換え）
- **TransactionSender**: リモートサーバーへの取引送信の抽象化（直接のThreadインスタンス化を置き換え）。インターフェース: `suspend fun send(profile: Profile, transaction: LedgerTransaction, simulate: Boolean = false): Result<Unit>`（コルーチン + Result型）
- **Fake Repositories**: ProfileRepository、TransactionRepository、AccountRepository等のテストダブル（既存のフェイク実装を拡張）

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: MainViewModelのユニットテストが最低70%の行カバレッジを達成する
- **SC-002**: NewTransactionViewModelのユニットテストが最低70%の行カバレッジを達成する
- **SC-003**: リファクタリング後も既存のすべてのテストがパスし続ける
- **SC-004**: 新しいテストの実行時間が合計30秒以内である
- **SC-005**: ユニットテスト実行時にAndroidフレームワーク依存がゼロである（エミュレータ/デバイスなしでテストが実行される）
- **SC-006**: クリティカルViewModelパッケージの全体的なプロジェクトテストカバレッジが9%から少なくとも30%に向上する

## Assumptions

- 既存のRepositoryパターンがほとんどのデータアクセスに適切な抽象化を提供している。Appシングルトンのプリファレンスとスレッドベースの非同期タスクのみ新しい抽象化が必要
- Hilt依存性注入がすでに導入されているため、新しい注入可能な依存関係の追加は確立されたパターンに従う
- `app/src/test/kotlin/.../fake/` のフェイクリポジトリ実装を拡張して新しいテストシナリオをサポートできる
- テスト実行環境（JUnit 4とkotlinx-coroutines-test）はプロジェクトで既に構成されている
- カバレッジ測定にはJaCoCo（Android/Gradleプロジェクトの標準）を使用する

## Out of Scope

- **UIテスト（Compose/Instrumentation）**: Compose UIコンポーネントのインストルメンテーションテストは本フィーチャーのスコープ外。ユニットテストのみに集中する
- **E2Eテスト**: エンドツーエンドのシナリオテストは将来のフィーチャーとして別途検討
