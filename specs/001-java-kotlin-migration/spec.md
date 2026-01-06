# Feature Specification: Java から Kotlin への移行

**Feature Branch**: `001-java-kotlin-migration`
**Created**: 2026-01-05
**Status**: Draft
**Input**: User description: "java から Kotolinへの移行をしたいです。既存のロジックを絶対に変更しないでください。Kotlin独自の利点は積極的に取り入れてください。コードの重複などリファクタリングが行える場合は適宜行ってください。"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - ビルドとテストが正常に動作する (Priority: P1)

開発者として、JavaからKotlinへの移行後もアプリケーションが正常にビルドでき、既存のテストがすべてパスすることを確認したい。これにより、移行によるリグレッションがないことを保証できる。

**Why this priority**: ロジックを変更しないという要件の最も基本的な検証であり、すべての移行作業の前提条件となる

**Independent Test**: ビルドを実行し、既存のテストスイートを実行して全テストがパスすることを確認

**Acceptance Scenarios**:

1. **Given** すべてのJavaファイルがKotlinに変換された状態, **When** アプリケーションをビルド, **Then** コンパイルエラーなくビルドが成功する
2. **Given** ビルドが成功した状態, **When** 既存のユニットテストを実行, **Then** すべてのテストがパスする
3. **Given** ビルドが成功した状態, **When** 既存のインストルメンテーションテストを実行, **Then** すべてのテストがパスする

---

### User Story 2 - Kotlin言語機能を活用した可読性向上 (Priority: P2)

開発者として、Kotlinの言語機能（null安全、data class、拡張関数、スコープ関数など）を活用することで、コードの可読性と保守性を向上させたい。

**Why this priority**: Kotlinへの移行の主要なメリットを実現するストーリーであり、単純な変換以上の価値を提供する

**Independent Test**: 変換後のコードがKotlinのイディオムに従っていることをコードレビューで確認

**Acceptance Scenarios**:

1. **Given** Javaのデータクラス（getter/setter のみ）, **When** Kotlinに変換, **Then** Kotlin data classとして定義される
2. **Given** Javaの null チェックコード, **When** Kotlinに変換, **Then** Kotlinのnull安全機能（?. ?: !!）を適切に使用
3. **Given** Javaのビルダーパターンやメソッドチェーン, **When** Kotlinに変換, **Then** 適切なスコープ関数（apply, let, run など）を使用

---

### User Story 3 - 重複コードの削減 (Priority: P3)

開発者として、移行時に発見された重複コードを統合し、コードベースの保守性を向上させたい。特にJSONパーサーのバージョン別実装で共通化できる部分を特定する。

**Why this priority**: コードの重複削減は保守性向上に直結するが、ロジック変更を伴わない範囲で慎重に行う必要がある

**Independent Test**: 重複削減後も既存の機能がすべて正常に動作することをテストで確認

**Acceptance Scenarios**:

1. **Given** 複数のJSONパーサーバージョンに共通のコード, **When** 委譲パターンで共通処理クラスに抽出, **Then** 各バージョンは共通処理クラスに委譲し、バージョン固有の処理のみを実装
2. **Given** 同一ロジックの重複実装, **When** 共通ユーティリティに抽出, **Then** 既存のテストがすべてパス

---

### User Story 4 - 段階的移行による安定性確保 (Priority: P4)

開発者として、パッケージ単位で段階的に移行を進め、各段階でビルドとテストを実行することで、問題の早期発見と修正を可能にしたい。

**Why this priority**: 285ファイルの一括変換はリスクが高いため、段階的アプローチで安定性を確保する

**Independent Test**: 各パッケージの移行後にビルドとテストを実行して確認

**Acceptance Scenarios**:

1. **Given** 依存関係の少ないパッケージ（utils, err）, **When** 最初に移行, **Then** ビルドが成功し既存テストがパス
2. **Given** 移行済みのパッケージに依存するパッケージ, **When** 順次移行, **Then** 各段階でビルドが成功

---

### Edge Cases

- 既存のJavaアノテーション（Room, Jetpackなど）がKotlinでも正しく動作するか
- Javaの静的メソッドをKotlinのcompanion objectに変換する際の互換性
- Null許容型と非Null型の適切な判断: **変換前にJavaコードに `@NonNull`/`@Nullable` アノテーションを追加してから移行する**
- JavaとKotlinの相互運用中に発生する可能性のある問題

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: システムは既存の285のJavaファイルをKotlinに変換しなければならない
- **FR-002**: 変換後のコードは既存のビジネスロジックを完全に保持しなければならない
- **FR-003**: 変換後のコードはKotlinのnull安全機能を適切に活用しなければならない
- **FR-004**: データ保持専用のクラスはKotlin data classに変換しなければならない
- **FR-005**: 適切な箇所でKotlinスコープ関数（let, apply, run, also, with）を使用しなければならない
- **FR-006**: 重複コードは委譲パターン（共通処理クラスへの委譲）を使用して抽出しなければならない
- **FR-007**: Room DAOアノテーションはKotlin互換の形式で維持しなければならない
- **FR-008**: 既存のテストは変換後もすべてパスしなければならない
- **FR-009**: 段階的移行をサポートするため、Java-Kotlin間の相互運用性を確保しなければならない
- **FR-010**: JSONパーサーのバージョン別実装（v1_14, v1_15, v1_19_1, v1_23, v1_32, v1_40, v1_50）間の共通コードを抽出しなければならない

### Key Entities

- **パッケージ構造**:
  - async (7ファイル): 非同期処理関連
  - backup (6ファイル): バックアップ機能
  - dao (11ファイル): Room DAOインターフェース
  - db (17ファイル): データベースエンティティ
  - json (102ファイル): JSONパーサー（7バージョン）
  - model (15ファイル): ドメインモデル
  - ui (56ファイル): UIコンポーネント
  - utils (13ファイル): ユーティリティ
  - err (1ファイル): 例外クラス
  - その他のルートレベル（App, BackupsActivityなど）

- **移行優先順位**:
  1. utils, err - 依存関係が最も少ない
  2. model - ドメインモデル（data class化）
  3. db - データベースエンティティ（data class化）
  4. dao - Room DAOインターフェース
  5. json - JSONパーサー（重複コード削減の主要対象）
  6. async, backup - 非同期・バックアップ処理
  7. ui - UIコンポーネント（最も依存関係が多い）

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: すべてのJavaファイル（285ファイル）がKotlinに変換される
- **SC-002**: 既存のユニットテストが100%パスする
- **SC-003**: 既存のインストルメンテーションテストが100%パスする
- **SC-004**: アプリケーションがエラーなくビルドできる
- **SC-005**: JSONパーサーの共通コードが抽出され、バージョン間の重複が削減される
- **SC-006**: データクラス候補のクラスがすべてKotlin data classに変換される
- **SC-007**: nullチェックコードがKotlinのnull安全機能に置き換えられる
- **SC-008**: 各UIフローがユーザー視点で正常に動作する（手動テストで確認）
- **SC-009**: 全変換ファイルがコードレビューで変換前後のロジック同一性を確認される

## Clarifications

### Session 2026-01-05

- Q: What Kotlin language version should be used for the migration? → A: Kotlin 2.x (latest stable 2.1.x)
- Q: JavaとKotlinが混在する期間中、nullabilityアノテーションのないJavaコードをどう扱うか？ → A: 変換前にJavaコードに `@NonNull`/`@Nullable` アノテーションを追加する（最も安全）
- Q: JSONパーサー7バージョン間の共通コード抽出のアーキテクチャパターンは？ → A: 委譲パターン（Composition over Inheritance）
- Q: 既存ビジネスロジック不変の検証方法は？ → A: コードレビューで変換前後のロジックを目視確認
- Q: アノテーションプロセッサの処理方式は？ → A: KSP（Kotlin Symbol Processing）を採用

## Assumptions

- Kotlin 2.x（最新安定版 2.1.x）を使用する
- Gradleビルド設定にKotlinプラグインを追加する必要がある
- アノテーション処理にはKSP（Kotlin Symbol Processing）を使用する
- Room、Jetpack等の既存のアノテーションプロセッサはKSP経由でKotlinと互換性がある
- 既存のJavaアノテーション（@Nullable, @NonNull）を参考にnull安全性を判断する
- 移行中はJavaとKotlinが混在する期間があり、相互運用性を維持する
- テストコードも移行対象とする（instrumentedTestを含む）
