# Feature Specification: Kotlin Version Update

**Feature Branch**: `003-kotlin-update`
**Created**: 2026-01-06
**Status**: Draft
**Input**: User description: "Android プロジェクトの Kotlin バージョンを最新の安定版（Kotlin 2.0 系）にアップデート。コンパイラ性能向上、言語機能の恩恵、コードベースの現代化が目的。"

## Clarifications

### Session 2026-01-06

- Q: Target Kotlin version (2.0.x is ambiguous) → A: Kotlin 2.0.21 (latest 2.0.x patch for stability)

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Build with Updated Kotlin (Priority: P1)

開発者として、プロジェクトを最新の Kotlin バージョンでビルドし、既存の機能が正常に動作することを確認したい。

**Why this priority**: ビルドが通らなければ他のすべての作業が無意味になるため、最優先で確認する必要がある。

**Independent Test**: nix flake コマンドでビルドを実行し、エラーなくAPKが生成されることを確認する。

**Acceptance Scenarios**:

1. **Given** Kotlin 2.0.21 にアップデートされた build 設定, **When** `nix run .#build` を実行, **Then** ビルドがエラーなく完了し、APK が生成される
2. **Given** アップデートされたプロジェクト, **When** `nix run .#test` を実行, **Then** 既存のすべてのユニットテストがパスする
3. **Given** アップデートされたプロジェクト, **When** Kotlin コンパイラの警告を確認, **Then** Deprecation warning が 0 件である

---

### User Story 2 - Application Behavior Preservation (Priority: P1)

エンドユーザーとして、アップデート後もアプリの動作が以前と全く同じであることを期待する。

**Why this priority**: ユーザー体験に影響を与えないことが絶対要件であるため、ビルド成功と同等の優先度。

**Independent Test**: `nix run .#verify` でフル検証フローを実行し、主要機能が従来通り動作することを確認する。

**Acceptance Scenarios**:

1. **Given** アップデート後のアプリ, **When** ユーザーがプロファイル作成を行う, **Then** 従来通りプロファイルが作成・保存される
2. **Given** アップデート後のアプリ, **When** ユーザーがデータ更新（同期）を実行, **Then** 従来通りデータが正常に更新される
3. **Given** アップデート後のアプリ, **When** ユーザーが取引を登録, **Then** 従来通り取引が正常に記録される

---

### User Story 3 - Code Modernization (Priority: P2)

開発者として、Kotlin 2.0 の新しい言語機能を活用してコードの可読性と安全性を向上させたい。

**Why this priority**: 機能等価性が確保された後に、コード品質向上のためのリファクタリングを行う。

**Independent Test**: 変更されたファイルをコードレビューし、新しい言語機能が適切に適用されていることを確認する。

**Acceptance Scenarios**:

1. **Given** Kotlin 2.0 の新機能（data object 等）, **When** 適用可能なコードパターンがある, **Then** 可読性向上のため適切に適用される
2. **Given** 非推奨となった API や構文, **When** 代替が存在する, **Then** 推奨される新しい書き方に修正される
3. **Given** 実験的な機能（`@OptIn` が必要なもの）, **When** 適用検討時, **Then** 使用しない

---

### Edge Cases

- 依存ライブラリが Kotlin 2.0 と非互換の場合はどうするか？
  - 対応：互換性のある最新バージョンにライブラリを更新する。更新不可の場合は、そのライブラリの Kotlin 2.0 対応を待つか代替を検討
- Room の kapt から ksp への移行が必要になった場合はどうするか？
  - 対応：このアップデートのスコープ内で移行を完了する（Room 2.4.x は kapt でも動作するが、将来的な ksp 移行は別タスクとする）
- コンパイラの挙動変更により既存コードの結果が変わる可能性はあるか？
  - 対応：すべてのユニットテストでリグレッションをチェック。テストカバレッジ外の挙動変更は手動テストで検証

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: ビルドシステムは Kotlin 2.0.21 でコンパイルできなければならない
- **FR-002**: Gradle Version Catalog（libs.versions.toml）の Kotlin バージョンを更新しなければならない
- **FR-003**: 依存ライブラリのバージョン整合性を確保しなければならない（Coroutines、Lifecycle 等）
- **FR-004**: すべての deprecation warning を修正し、警告ゼロの状態にしなければならない
- **FR-005**: 非推奨 API を推奨される代替に置き換えなければならない
- **FR-006**: Kotlin 2.0 の安定機能のうち、可読性・安全性を向上させるものを適用しなければならない
- **FR-007**: 実験的機能（`@OptIn` アノテーションが必要なもの）は使用してはならない
- **FR-008**: アプリケーションの外部から見える挙動を一切変更してはならない

### Key Entities

- **Version Catalog**: Gradle の依存関係バージョンを一元管理する設定ファイル（libs.versions.toml）
- **Kotlin Source Files**: 移行対象の .kt ファイル群（現在は app/src/main/kotlin 以下）
- **Build Configuration**: Gradle ビルドスクリプト（build.gradle、settings.gradle）

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: `nix run .#build` でデバッグビルドがエラーなく完了する
- **SC-002**: `nix run .#buildRelease` でリリースビルドがエラーなく完了する
- **SC-003**: `nix run .#test` ですべての既存ユニットテストがパスする
- **SC-004**: Kotlin コンパイラの deprecation warning が 0 件
- **SC-005**: `nix run .#verify` で実機検証が成功し、主要機能（プロファイル管理、データ更新、取引登録）が動作する
- **SC-006**: Kotlin 2.0 の新機能が適用されたコードは、元のコードと同一の振る舞いを持つ

## Assumptions

- AGP 8.7.3 は Kotlin 2.0.21 と互換性がある（公式サポート対象）
- 現在使用中のライブラリ（Room 2.4.2、Lifecycle 2.4.1 等）は Kotlin 2.0 と互換性がある
- kapt は Kotlin 2.0 でも動作する（ksp への移行は必須ではない）
- プロジェクトには十分なテストカバレッジがあり、リグレッション検出が可能

## Out of Scope

- Jetpack Compose の導入
- ksp への完全移行（kapt からの移行は将来タスク）
- 新機能の追加
- UI/UX の変更
- パフォーマンス最適化（Kotlin バージョン更新に起因するもの以外）
