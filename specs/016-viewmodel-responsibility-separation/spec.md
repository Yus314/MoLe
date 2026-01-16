# Feature Specification: ViewModel 責務分離

**Feature Branch**: `016-viewmodel-responsibility-separation`
**Created**: 2026-01-16
**Status**: Draft
**Input**: MoLe アプリの画面状態管理コードの責務分離 - 肥大化した ViewModel を責務ごとに分割し、単一責任原則に基づいた構造への移行

## User Scenarios & Testing *(mandatory)*

### User Story 1 - MainViewModel の責務分離 (Priority: P1)

開発者として、メイン画面の状態管理コードを機能ごとに分割したい。現在830行を超える MainViewModel が複数の責務（プロファイル選択、アカウント表示、取引表示、データ同期、ナビゲーション制御）を持っており、変更影響範囲の予測と単体テストが困難になっている。

**Why this priority**: メイン画面は最も頻繁に使用・変更される画面であり、ここの改善が最大の開発効率向上をもたらす。現在の構造では1つの機能修正が他機能に意図せず影響するリスクがある。

**Independent Test**: MainViewModel のロジックを移行した既存の各 ViewModel（ProfileSelectionViewModel、AccountSummaryViewModel、TransactionListViewModel、MainCoordinatorViewModel）が独立したユニットテストでカバーされ、テスト実行時に相互干渉がないことを確認できる。移行後、MainViewModel は削除または最小化される。

**Acceptance Scenarios**:

1. **Given** 分離後の ProfileSelectionViewModel、**When** プロファイル選択のテストを実行、**Then** アカウント表示やデータ同期のコードに依存せず独立してテストが完了する
2. **Given** 分離後の MainCoordinatorViewModel、**When** データ同期機能を修正、**Then** プロファイル選択やナビゲーション制御のコードに影響を与えない
3. **Given** 分離後の各 ViewModel、**When** 行数をカウント、**Then** 各 ViewModel が300行以下に収まっている

---

### User Story 2 - NewTransactionViewModel の責務分離 (Priority: P2)

開発者として、取引登録画面の状態管理コードを機能ごとに分割したい。現在960行を超える NewTransactionViewModel が複数の責務（フォーム管理、勘定科目行の追加削除、テンプレート適用、通貨選択、取引送信）を持っている。

**Why this priority**: 取引登録は機能追加要望が多い画面であり、分離により新機能の追加と既存機能のバグ修正を安全に行えるようになる。

**Independent Test**: NewTransactionViewModel から分離された各 ViewModel が独立したユニットテストでカバーされ、テスト実行時に相互干渉がないことを確認できる。
- TransactionFormViewModel: フォーム管理（日付、説明等）、取引送信
- AccountRowsViewModel: 勘定科目行の追加削除、通貨選択
- TemplateApplicatorViewModel: テンプレート適用

**Acceptance Scenarios**:

1. **Given** 分離後の AccountRowsViewModel、**When** 勘定科目行の追加削除テストを実行、**Then** テンプレート適用や取引送信のコードに依存せず独立してテストが完了する
2. **Given** 分離後の TemplateApplicatorViewModel、**When** テンプレート適用機能を修正、**Then** フォーム管理や通貨選択のコードに影響を与えない
3. **Given** 分離後の各 ViewModel、**When** 行数をカウント、**Then** 各 ViewModel が300行以下である

---

### User Story 3 - ProfileDetailModel の StateFlow 移行 (Priority: P3)

開発者として、プロファイル詳細画面の状態管理を LiveData から StateFlow に移行し、他画面との一貫性を確保したい。現在 ProfileDetailModel は LiveData を使用しており、Compose UI との統合やテスト容易性で不整合が生じている。

**Why this priority**: アーキテクチャの一貫性確保はコードベース全体の理解容易性に寄与する。P1/P2 の分離作業で確立したパターンを適用できる。

**Independent Test**: ProfileDetailViewModel が StateFlow ベースで実装され、他の ViewModel と同様のテストパターン（MainDispatcherRule + runTest）でテストできることを確認できる。

**Acceptance Scenarios**:

1. **Given** StateFlow 移行後の ProfileDetailViewModel、**When** ユニットテストを実行、**Then** MainDispatcherRule と runTest を使用した標準パターンでテストが完了する
2. **Given** StateFlow 移行後の ProfileDetailViewModel、**When** Compose UI から状態を購読、**Then** collectAsState() で直接購読できる

---

### Edge Cases

- ViewModel 分離後に画面間での状態共有が必要な場合はどうなるか？→ 共有が必要な状態は AppStateService を経由する（既存パターン）
- 分離した ViewModel 間で循環依存が発生した場合はどうなるか？→ Activity が全 ViewModel を保持し Activity レベルで調整することで回避（ViewModel 間の直接依存なし）
- 既存のテストが分離により影響を受けた場合はどうなるか？→ 分離後の新しい ViewModel 構造に合わせてテストを移行する

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: MainViewModel のロジックを既存の ProfileSelectionViewModel、AccountSummaryViewModel、TransactionListViewModel、MainCoordinatorViewModel に移行し、MainViewModel を削除または最小化すること。各 ViewModel は単一の責務のみを持つこと
- **FR-002**: NewTransactionViewModel を TransactionFormViewModel（フォーム管理+取引送信）、AccountRowsViewModel（勘定科目行+通貨選択）、TemplateApplicatorViewModel（テンプレート適用）に分離し、各 ViewModel は単一の責務のみを持つこと
- **FR-003**: 分離後の各 ViewModel は目安として300行以下に収まること（超過時は理由を文書化、400行超で再分割検討）
- **FR-004**: ProfileDetailModel を ProfileDetailViewModel として StateFlow ベースに移行すること
- **FR-005**: 分離後の各 ViewModel は Hilt による依存性注入を使用すること
- **FR-006**: 分離により画面の動作や UI に変更が生じないこと（リファクタリングのみ）
- **FR-007**: 分離後の各 ViewModel は独立してユニットテスト可能であること
- **FR-008**: 既存の Fake 実装パターン（FakeRepository 等）を継続使用すること

### Key Entities

- **ViewModel**: 画面の状態管理とビジネスロジックを担当するコンポーネント。分離後は単一責務を持つ
- **UiState**: 画面の表示状態を表すデータクラス。各 ViewModel が自身の UiState を管理する
- **Effect**: ViewModel から UI への単発イベント（ナビゲーション、スナックバー表示等）

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: MainViewModel（830行）から分離された各 ViewModel が目安300行以下である（超過時は理由文書化）
- **SC-002**: NewTransactionViewModel（960行）から分離された各 ViewModel が目安300行以下である（超過時は理由文書化）
- **SC-003**: 分離後の各 ViewModel に対するユニットテストカバレッジが個別に70%以上である（全体の集計ではなく、各 ViewModel 単位で計測）
- **SC-004**: 新規開発者が1つの ViewModel の責務を理解するのに必要なコード量が300行以下に削減されている
- **SC-005**: 分離後、1つの機能を修正する際に変更が必要なファイル数が平均2ファイル以下である
- **SC-006**: 全画面でアプリが正常に動作し、既存機能に退行がないこと

## Clarifications

### Session 2026-01-16

- Q: SyncViewModel は新規コンポーネントか、既存の MainCoordinatorViewModel に統合すべきか？ → A: 同期責務を既存の MainCoordinatorViewModel に統合する（SyncViewModel は作成しない）
- Q: 分離後の ViewModel 間の通信パターンは？ → A: Activity が全 ViewModel を保持し、Activity レベルで調整（既存パターン継続）
- Q: NewTransactionViewModel の通貨選択と取引送信はどの ViewModel に含めるか？ → A: 通貨選択→AccountRowsViewModel、取引送信→TransactionFormViewModel に統合
- Q: 300行制限は厳格か目安か？ → A: 目安として扱い、超過時は理由を文書化。400行超で再分割検討
- Q: 既存の専門化 ViewModel への移行か新規作成か？ → A: 既存の専門化 ViewModel にロジック移行、MainViewModel を削除または最小化

## Assumptions

- 分離の粒度は責務の明確性を優先し、過度な細分化は避ける
- 各専門化 ViewModel 間の通信は Activity レベルでの調整を使用する（ViewModel 間の直接依存なし）
- プロファイル切り替え等のグローバル状態変更は AppStateService 経由で伝播する（既存パターン継続）
- Hilt の @HiltViewModel アノテーションと @Inject constructor パターンを継続使用する
