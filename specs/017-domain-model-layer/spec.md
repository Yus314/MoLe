# Feature Specification: Domain Model Layer Introduction

**Feature Branch**: `017-domain-model-layer`
**Created**: 2026-01-16
**Status**: Draft
**Input**: User description: "ドメインモデル層の導入によるレイヤー分離 - 画面表示コードとデータベース内部構造の分離"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Transaction Display Without Database Knowledge (Priority: P1)

画面開発者として、取引データを表示する際にデータベースの内部構造（テーブル構造、カラム名、リレーション）を意識せずに開発したい。これにより、画面のコードがシンプルになり、データベース変更時の影響を受けなくなる。

**Why this priority**: 取引表示はアプリの最も頻繁に使用される機能であり、現在最も密結合している領域。取引ドメインモデルを最初に導入することで、最大の効果と学習効果が得られる。

**Independent Test**: 取引一覧画面のViewModelをテストする際、Fake Repository経由でドメインモデルのみを扱い、データベースエンティティのモックが不要になることを検証できる。

**Acceptance Scenarios**:

1. **Given** ViewModelがTransactionRepositoryを使用している、**When** 取引一覧を取得する、**Then** ドメインモデル（`Transaction`ドメインオブジェクト）のリストが返され、データベースエンティティへの参照がない
2. **Given** 画面のコード、**When** 取引データを表示する、**Then** `net.ktnx.mobileledger.db`パッケージへのimportが存在しない
3. **Given** 単体テスト環境、**When** ViewModelをテストする、**Then** ドメインモデルのFakeデータのみでテストが完結し、データベース関連のモック設定が不要

---

### User Story 2 - Transaction Creation Without Database Coupling (Priority: P2)

画面開発者として、新規取引を作成・編集する際にデータベースへの保存方法を意識せずに開発したい。ビジネスロジック（金額バランスチェック、日付検証など）に集中できる。

**Why this priority**: 取引登録は複雑なビジネスロジックを含む重要機能。P1完了後、同じ取引ドメインの変換処理を追加することで一貫性を保つ。

**Independent Test**: NewTransactionViewModelのバリデーションロジックをテストする際、データベース保存の詳細なしでドメインモデルのみを使用してテスト可能。

**Acceptance Scenarios**:

1. **Given** 新規取引入力画面、**When** ユーザーが取引情報を入力する、**Then** ViewModelはドメインモデルとしてデータを保持し、データベースエンティティを直接操作しない
2. **Given** ViewModelが取引ドメインモデルを保持している、**When** 保存ボタンが押される、**Then** Repositoryがドメインモデルを受け取り、データベースへの変換と保存を担当する
3. **Given** 金額バランスが不正な取引、**When** バリデーションを実行する、**Then** ドメインモデルのメソッドでバリデーション結果を取得でき、データベース操作なしで検証可能

---

### User Story 3 - Profile Management Without Database Details (Priority: P3)

画面開発者として、プロファイル（接続先設定）を管理する際にデータベースの詳細を意識せずに開発したい。サーバー接続、認証設定などのビジネス概念に集中できる。

**Why this priority**: プロファイル管理は複数画面で使用されるが、取引ほど頻繁には変更されない。取引ドメインモデル完了後に対応。

**Independent Test**: ProfileDetailViewModelをテストする際、プロファイルドメインモデルのみでテスト可能。

**Acceptance Scenarios**:

1. **Given** プロファイル編集画面、**When** プロファイル情報を表示する、**Then** ドメインモデル経由でデータを取得し、データベースカラム名への参照がない
2. **Given** 単体テスト環境、**When** プロファイル保存ロジックをテストする、**Then** Fakeドメインモデルのみでテストが完結する

---

### User Story 4 - Account Hierarchy Display (Priority: P4)

画面開発者として、勘定科目階層と残高を表示する際に、データベースのテーブル結合やリレーション構造を意識せずに開発したい。

**Why this priority**: 勘定科目表示はP1の取引表示と密接に関連。取引・プロファイル完了後に対応。

**Independent Test**: AccountSummaryViewModelをテストする際、勘定科目ドメインモデルのみでテスト可能。

**Acceptance Scenarios**:

1. **Given** アカウント一覧画面、**When** 残高付きアカウントリストを取得する、**Then** ドメインモデルのリストが返され、`AccountWithAmounts`データベースエンティティへの直接参照がない

---

### User Story 5 - Template and Currency Management (Priority: P5)

画面開発者として、テンプレートと通貨設定を管理する際にデータベース詳細を意識せずに開発したい。

**Why this priority**: テンプレート・通貨は使用頻度が低く、影響範囲も限定的。最後に対応。

**Independent Test**: テンプレート・通貨関連ViewModelをテストする際、ドメインモデルのみでテスト可能。

**Acceptance Scenarios**:

1. **Given** テンプレート一覧画面、**When** テンプレートを取得する、**Then** ドメインモデルとして返され、`TemplateWithAccounts`データベースエンティティへの参照がない

---

### Edge Cases

- 古いデータベーススキーマからの移行時、ドメインモデルへの変換が正しく動作するか
- 大量データ（数千件の取引）変換時のパフォーマンス影響
- null/空値を含むデータベースレコードのドメインモデルへの変換時のデフォルト値処理
- 複数通貨を持つ取引のドメインモデル表現

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: システムはドメインモデル層（`domain`パッケージ）を提供し、画面コードが使用するビジネスオブジェクトを定義しなければならない
- **FR-002**: ドメインモデルはデータベースエンティティ（`db`パッケージ）への依存を持ってはならない
- **FR-003**: Repositoryはデータベースエンティティとドメインモデル間の変換責務を持ち、ViewModelにはドメインモデルのみを公開しなければならない
- **FR-004**: 画面コード（ViewModel、Composable）は`net.ktnx.mobileledger.db`パッケージをimportしてはならない
- **FR-005**: 各ドメインモデルはビジネスロジック（バリデーション、計算）をメソッドとして持つことができ、バリデーション結果は`sealed class`（Success/Error）で型安全に表現しなければならない
- **FR-006**: ドメインモデルは不変（immutable）として設計し、データ変更時は新しいインスタンスを作成しなければならない
- **FR-007**: Mapper（変換ロジック）はRepository内またはMapper専用クラスとして実装し、ドメインモデルやデータベースエンティティには含めない
- **FR-008**: 既存の`model`パッケージ（`LedgerTransaction`等）は`@Deprecated`アノテーションでマークし、全参照をドメインモデルに移行後、削除しなければならない
- **FR-009**: 移行中は既存機能（取引登録、データ同期、バックアップ）が正常に動作し続けなければならない

### Key Entities

- **Transaction（ドメインモデル）**: 取引のビジネス表現。日付、説明、コメント、取引行リスト（勘定科目、金額、通貨）を持つ。データベースの年・月・日分離やdataHash等の実装詳細を隠蔽
- **TransactionLine（ドメインモデル）**: 取引内の1行（勘定科目、金額、通貨、コメント）。データベースのorderNoや外部キーを隠蔽
- **Profile（ドメインモデル）**: 接続先プロファイルのビジネス表現。サーバーURL、認証設定、テーマ設定を持つ。データベースの検出バージョンフラグ等を隠蔽
- **Account（ドメインモデル）**: 勘定科目のビジネス表現。名前、階層レベル、残高リストを持つ
- **Template（ドメインモデル）**: 取引テンプレートのビジネス表現。パターン、テンプレート行を持つ
- **Currency（ドメインモデル）**: 通貨のビジネス表現。名前、表示位置、小数桁数を持つ
- **Mapper**: データベースエンティティとドメインモデル間の変換を担当するコンポーネント
- **ValidationResult（sealed class）**: バリデーション結果を型安全に表現。`Success`と`Error(reasons: List<String>)`のサブクラスを持つ

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 全てのViewModel（Main、NewTransaction、ProfileDetail等）が`net.ktnx.mobileledger.db`パッケージへの直接importを持たない
- **SC-002**: ViewModelの単体テストがデータベースエンティティのモック設定なしで実行可能（Fakeドメインモデルのみ使用）
- **SC-003**: 新規ViewModel作成時に、開発者がデータベーススキーマを参照せずドメインモデルのみで実装可能
- **SC-004**: 既存の全機能（プロファイル管理、取引登録、データ同期、バックアップ・リストア）がリグレッションなく動作する
- **SC-005**: ドメインモデル（`domain/model/`パッケージ）およびMapper（`data/repository/mapper/`パッケージ）のテストカバレッジが80%以上（バリデーションロジック等のビジネスルールを含む）
- **SC-006**: データベーススキーマ変更時に、画面コード（ViewModel/Composable）への修正が不要であることをコードレビューで確認可能

## Clarifications

### Session 2026-01-16

- Q: ドメインモデルがデータベースIDを保持する方法は？ → A: `Long?` nullable - 新規エンティティはnull、保存後にIDが設定される
- Q: 既存`model`パッケージの移行戦略は？ → A: Deprecate then delete - 既存modelクラスを`@Deprecated`でマークし、全参照移行後に削除
- Q: ドメインモデルでエラーをどう表現するか？ → A: Sealed class Result - `sealed class ValidationResult { Success, Error(reasons) }` を使用
- Q: 大量データ変換時のパフォーマンス戦略は？ → A: Eager conversion - Repository が Flow で返す時点で全件ドメインモデルに変換
- Q: スコープ外の境界は？ → A: UI/DB changes excluded - UI層（Compose）やDB層（Room Entity）の構造変更はスコープ外

## Out of Scope

- **UI層（Compose）の構造変更**: Composable関数やUIコンポーネントのリファクタリングは含まない
- **DB層（Room Entity）の構造変更**: データベーススキーマ、Room Entity、DAOの変更は含まない
- **Mapper追加とRepository変更のみ**: 本機能はドメインモデル定義、Mapper実装、Repository のドメインモデル公開に限定する

## Assumptions

- ドメインモデルのID属性は`Long?`型とし、新規作成時はnull、データベース保存後にIDが設定されるパターンを採用する
- 現在の`model`パッケージ（`LedgerTransaction`、`LedgerAccount`等）はデータベースエンティティに密結合しているため、新しい`domain`パッケージを作成して段階的に移行する
- Repository層は既に008-data-layer-repositoryで導入済みのため、Mapper追加とドメインモデル公開への変更が主な作業となる
- 大量データ取得時もEager conversion（Flow返却時点で全件変換）を採用し、数千件程度では遅延評価は不要とする
- 移行は段階的に行い、各エンティティごとに完全移行してから次に進む
