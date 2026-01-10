# Feature Specification: Data Layer Repository Migration

**Feature Branch**: `008-data-layer-repository`
**Created**: 2026-01-10
**Status**: Draft
**Input**: User description: "Googleが推奨しているデータレイヤに移行します。Data.ktのシングルトンからRepositoryパターンへの移行"

## Clarifications

### Session 2026-01-10

- Q: Data.ktに残る機能（通貨フォーマット、ロケール設定、バックグラウンドタスク状態）をどう扱うか？ → A: Data.ktを`AppStateManager`としてリネームし、UI/App状態専用として残す
- Q: Repositoryはインターフェース+実装クラスの分離パターンを使用するか？ → A: インターフェース + 実装クラス分離（例: `TransactionRepository` interface + `TransactionRepositoryImpl` class）
- Q: 移行中、ViewModelでRepository導入時に既存のData.kt/DAO直接呼び出しとの並行運用期間を設けるか？ → A: ViewModel単位で完全移行（1つのViewModelは全てRepository経由、または全て旧方式）

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Modular Transaction Management (Priority: P1)

開発者として、取引データに関する操作（取得、追加、更新）を独立したコンポーネントで管理したい。これにより、取引機能の修正やテスト時にアプリ全体のデータ状態を考慮する必要がなくなる。

**Why this priority**: 取引管理はアプリの中核機能であり、最も頻繁に修正・拡張される領域。TransactionRepositoryを最初に作成することで、最大の効果が得られる。

**Independent Test**: TransactionRepositoryのみを使用したユニットテストで、取引の取得・追加・更新操作が正常に動作することを検証できる。モックDAOを注入して、データベースなしでテスト可能。

**Acceptance Scenarios**:

1. **Given** TransactionRepositoryがDIで注入されている、**When** 取引一覧を取得する、**Then** 指定したプロファイルの取引リストがFlowで返される
2. **Given** TransactionRepositoryがDIで注入されている、**When** 新規取引を追加する、**Then** データベースに保存され、Flowが更新される
3. **Given** 単体テスト環境、**When** モックDAOを注入してTransactionRepositoryをテストする、**Then** 実データベースなしで全操作をテスト可能

---

### User Story 2 - Profile Repository Separation (Priority: P2)

開発者として、プロファイル（ユーザー設定・ledger設定）の管理を独立したリポジトリで行いたい。現在のプロファイル選択と一覧管理がData.ktから分離されることで、責務が明確になる。

**Why this priority**: プロファイル管理は複数画面で使用されるが、取引ほど頻繁には変更されない。P1完了後に対応。

**Independent Test**: ProfileRepositoryのみをテストし、プロファイルのCRUD操作と現在のプロファイル選択が正常動作することを検証できる。

**Acceptance Scenarios**:

1. **Given** ProfileRepositoryがDIで注入されている、**When** 全プロファイル一覧を取得する、**Then** 並び順通りのプロファイルリストがFlowで返される
2. **Given** ProfileRepositoryがDIで注入されている、**When** 現在のプロファイルを設定する、**Then** アプリ全体で現在のプロファイルが更新される
3. **Given** 単体テスト環境、**When** モックDAOでProfileRepositoryをテストする、**Then** プロファイル切り替えロジックを独立してテスト可能

---

### User Story 3 - ViewModel DI Refactoring (Priority: P3)

開発者として、各ViewModelが必要なRepositoryのみをHilt経由で注入されるようにしたい。これにより、ViewModelとデータ層の結合度が下がり、テスタビリティが向上する。

**Why this priority**: P1, P2のRepository作成後、ViewModelを順次移行する。既存機能への影響を最小化するため、段階的に実施。

**Independent Test**: 各ViewModelを個別にテストし、注入されたRepositoryのモックを使用して、ビジネスロジックの正確性を検証できる。

**Acceptance Scenarios**:

1. **Given** MainViewModelがProfileRepositoryとTransactionRepositoryを注入されている、**When** メイン画面を表示する、**Then** プロファイルと取引データが正常に表示される
2. **Given** NewTransactionViewModelがTransactionRepositoryを注入されている、**When** 取引を保存する、**Then** Repository経由でデータが保存される
3. **Given** 単体テスト環境、**When** ViewModelにモックRepositoryを注入する、**Then** Data.ktなしでViewModelのロジックをテスト可能

---

### User Story 4 - Legacy Data.kt Deprecation (Priority: P4)

開発者として、Data.ktシングルトンの直接使用を段階的に廃止し、最終的にData.ktを削除または最小化したい。

**Why this priority**: 移行完了後の最終フェーズ。全ViewModelがRepository経由になった後に実施。

**Independent Test**: Data.ktへの直接参照がコードベースから削除されていることを静的解析で確認できる。

**Acceptance Scenarios**:

1. **Given** 全ViewModelがRepository経由でデータアクセスしている、**When** Data.ktの使用箇所を検索する、**Then** ViewModel内での直接参照がゼロ件
2. **Given** 移行完了後、**When** アプリを実行する、**Then** 既存の全機能が正常動作する（リグレッションなし）

---

### Edge Cases

- 複数のViewModelが同時に同じRepositoryにアクセスした場合のスレッドセーフティ
- プロファイル切り替え中にトランザクション操作が発生した場合の整合性
- データベースマイグレーション中のRepository動作
- メモリ不足時のFlow/LiveData観察者の挙動

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: システムは`TransactionRepository`を提供し、取引データの取得・追加・更新・削除操作を一元管理しなければならない
- **FR-002**: システムは`ProfileRepository`を提供し、プロファイルの一覧取得・作成・更新・削除・選択操作を一元管理しなければならない
- **FR-003**: 各Repositoryは対応するDAOをコンストラクタ注入で受け取り、Hiltで管理されなければならない
- **FR-004**: ViewModelは必要なRepositoryのみをコンストラクタ注入で受け取らなければならない（Data.ktの直接使用を禁止）
- **FR-005**: Repositoryはリアクティブなデータストリーム（Kotlin Flow）を公開し、データ変更を自動的に通知しなければならない
- **FR-006**: 現在のプロファイル選択状態はアプリ全体で共有され、ProfileRepository経由でアクセス可能でなければならない
- **FR-007**: 移行はViewModel単位で完全移行し、同一ViewModel内でRepository + 旧方式の混在を許容しない（各ViewModelは全てRepository経由、または全て旧方式）
- **FR-008**: 各Repositoryはインターフェース+実装クラス分離パターンを採用し、テスト時にFake実装への差し替えを可能にしなければならない（例: `TransactionRepository` interface + `TransactionRepositoryImpl` class）
- **FR-009**: Data.ktは`AppStateManager`としてリネームし、UI/アプリ状態（バックグラウンドタスク、通貨設定、ドロワー状態）専用として維持しなければならない

### Key Entities

- **TransactionRepository**: 取引データへの単一アクセスポイント。TransactionDAOをラップし、ビジネスロジック（バリデーション、変換）を含む
- **ProfileRepository**: プロファイルデータと現在のプロファイル選択状態を管理。ProfileDAOをラップし、プロファイル切り替えロジックを含む
- **AccountRepository**: アカウント階層とアカウント残高の管理。AccountDAO、AccountValueDAOをラップ
- **TemplateRepository**: 取引テンプレートの管理。TemplateHeaderDAO、TemplateAccountDAOをラップ
- **AppStateManager**: UI/アプリ状態の管理（Data.ktからリネーム）。バックグラウンドタスク状態、通貨/ロケール設定、ドロワー状態を保持。データアクセス層とは別責務

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 全てのViewModelがData.ktへの直接参照を持たず、Repository経由でデータアクセスする
- **SC-002**: TransactionRepositoryの単体テストが、実データベースなしで実行可能
- **SC-003**: ProfileRepositoryの単体テストが、実データベースなしで実行可能
- **SC-004**: 新規ViewModel作成時に、必要なRepositoryを注入するだけでデータアクセスが可能
- **SC-005**: 既存の全機能（プロファイル管理、取引登録、データ同期）がリグレッションなく動作する
- **SC-006**: コードレビュー時に、データアクセスの責務がどのRepositoryにあるか明確に特定可能
