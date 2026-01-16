# Implementation Plan: Domain Model Layer Introduction

**Branch**: `017-domain-model-layer` | **Date**: 2026-01-16 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/017-domain-model-layer/spec.md`

## Summary

画面コード（ViewModel/Composable）とデータベース内部構造（Room Entity/DAO）を分離するため、新しいドメインモデル層（`domain/model`パッケージ）を導入する。Repositoryがデータベースエンティティとドメインモデル間の変換を担当し、ViewModelにはドメインモデルのみを公開することで、データベース変更時のUI層への影響を排除する。

**技術アプローチ**:
- 新しい`domain/model`パッケージにイミュータブルなドメインモデル（data class）を作成
- Repository内にMapperを実装し、Flow返却時にドメインモデルへ変換
- 既存`model`パッケージのクラスを`@Deprecated`でマークし段階的に移行
- ViewModelから`net.ktnx.mobileledger.db`パッケージへのimportを排除

## Technical Context

**Language/Version**: Kotlin 2.0.21 / JVM target 1.8
**Primary Dependencies**: Hilt 2.51.1, Jetpack Compose (composeBom 2024.12.01), Coroutines 1.9.0, Room 2.4.2
**Storage**: Room Database (SQLite) - 既存、変更なし
**Testing**: JUnit 4 + kotlinx-coroutines-test (runTest) + Fake implementations
**Target Platform**: Android (minSdk 23+)
**Project Type**: Mobile (Android)
**Performance Goals**: Eager conversion（数千件程度の変換で遅延評価不要）
**Constraints**: 既存機能のリグレッションなし、UIとDBの構造変更はスコープ外
**Scale/Scope**: 6エンティティ（Transaction, Profile, Account, Template, Currency, Option）

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| 原則 | 状態 | 確認内容 |
|------|------|----------|
| I. コードの可読性とメンテナンス性 | ✅ PASS | ドメインモデル導入により関心の分離が改善される |
| II. テスト駆動開発 (TDD) | ✅ PASS | 各ドメインモデルにテストを先行して作成 |
| III. 最小構築・段階的開発 | ✅ PASS | P1→P2→P3→P4→P5の順で段階的に移行 |
| IV. パフォーマンス最適化 | ✅ PASS | Eager conversionで数千件は問題なし |
| V. アクセシビリティ | N/A | UIレイヤの変更なし |
| VI. Kotlinコード標準 | ✅ PASS | data class、sealed class、val優先 |
| VII. Nix開発環境 | ✅ PASS | 既存環境で対応可能 |
| VIII. 依存性注入 (Hilt) | ✅ PASS | Repositoryはすでに@Singleton、Mapper追加のみ |
| IX. 静的解析とリント | ✅ PASS | 新規コードはktlint/detektを通過 |
| X. 階層型アーキテクチャ | ✅ PASS | Domain Layer強化、Single Source of Truth維持 |

**Gate Result**: ✅ ALL PASS - Phase 0に進行可能

### Post-Design Re-check (Phase 1完了後)

| 原則 | 状態 | 確認内容 |
|------|------|----------|
| I. コードの可読性 | ✅ PASS | data classとsealed classでコード明確化 |
| II. TDD | ✅ PASS | テスト先行でドメインモデルとMapperを実装 |
| III. 段階的開発 | ✅ PASS | P1-P5の5フェーズで段階移行 |
| IV. パフォーマンス | ✅ PASS | Eager conversion採用、数千件で問題なし |
| V. アクセシビリティ | N/A | UI変更なし |
| VI. Kotlin標準 | ✅ PASS | data class、sealed class、val、拡張関数 |
| VII. Nix環境 | ✅ PASS | 既存環境で対応可能 |
| VIII. Hilt DI | ✅ PASS | Mapper追加のみ、Repositoryは既存 |
| IX. 静的解析 | ✅ PASS | 新規コードはktlint/detekt通過予定 |
| X. 階層型アーキテクチャ | ✅ PASS | Domain Layer強化、Single Source of Truth維持 |

**Post-Design Gate Result**: ✅ ALL PASS - 実装フェーズに進行可能

## Project Structure

### Documentation (this feature)

```text
specs/017-domain-model-layer/
├── spec.md              # 機能仕様（完了）
├── plan.md              # このファイル
├── research.md          # Phase 0 output - 調査結果
├── data-model.md        # Phase 1 output - ドメインモデル設計
├── quickstart.md        # Phase 1 output - 実装クイックスタート
├── contracts/           # Phase 1 output - Mapper契約
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
app/src/main/kotlin/net/ktnx/mobileledger/
├── domain/                         # Domain Layer（既存 + 拡張）
│   ├── model/                      # ドメインモデル（NEW）
│   │   ├── Transaction.kt          # 取引ドメインモデル
│   │   ├── TransactionLine.kt      # 取引行ドメインモデル
│   │   ├── Profile.kt              # プロファイルドメインモデル
│   │   ├── Account.kt              # 勘定科目ドメインモデル
│   │   ├── Template.kt             # テンプレートドメインモデル
│   │   ├── Currency.kt             # 通貨ドメインモデル
│   │   └── ValidationResult.kt     # バリデーション結果（sealed class）
│   ├── usecase/                    # UseCase（既存）
│   └── exception/                  # 例外（既存）
│
├── data/                           # Data Layer
│   ├── repository/                 # Repository（既存、Mapper追加）
│   │   ├── TransactionRepository.kt      # インターフェース変更（ドメインモデル公開）
│   │   ├── TransactionRepositoryImpl.kt  # Mapper実装追加
│   │   ├── mapper/                       # Mapper専用ディレクトリ（NEW）
│   │   │   ├── TransactionMapper.kt      # Transaction変換
│   │   │   ├── ProfileMapper.kt          # Profile変換
│   │   │   ├── AccountMapper.kt          # Account変換
│   │   │   ├── TemplateMapper.kt         # Template変換
│   │   │   └── CurrencyMapper.kt         # Currency変換
│   │   └── ...
│   └── local/                      # ローカルデータソース（既存）
│
├── model/                          # 既存モデル（@Deprecated対象）
│   ├── LedgerTransaction.kt        # @Deprecated → domain/model/Transaction
│   ├── LedgerAccount.kt            # @Deprecated → domain/model/Account
│   └── ...
│
├── ui/                             # UI Layer（dbパッケージimport排除）
│   ├── main/
│   ├── transaction/
│   ├── profiles/
│   └── templates/
│
├── db/                             # Database Layer（変更なし）
│   ├── Profile.kt
│   ├── Transaction.kt
│   ├── Account.kt
│   └── ...
│
└── dao/                            # DAO Layer（変更なし）

app/src/test/kotlin/net/ktnx/mobileledger/
├── domain/
│   └── model/                      # ドメインモデルテスト（NEW）
│       ├── TransactionTest.kt
│       ├── ProfileTest.kt
│       └── ValidationResultTest.kt
├── data/
│   └── repository/
│       └── mapper/                 # Mapperテスト（NEW）
│           ├── TransactionMapperTest.kt
│           └── ProfileMapperTest.kt
└── fake/                           # Fake Repository（ドメインモデル版追加）
    ├── FakeTransactionRepository.kt  # ドメインモデルを返す版
    └── FakeProfileRepository.kt
```

**Structure Decision**: 既存の階層型アーキテクチャ（domain/data/ui）を維持しつつ、`domain/model`にドメインモデル、`data/repository/mapper`にMapperを追加。DBレイヤ（`db`/`dao`）は変更せず、Repositoryの戻り値型のみを変更する。

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| N/A | N/A | N/A |

## Current State Analysis

### 現在のデータフロー

```text
現在:
ViewModel → Repository → DAO → Room Entity
     ↑                           ↓
     └───── TransactionWithAccounts (db パッケージ) ─────┘

問題点:
- ViewModelが net.ktnx.mobileledger.db をimport
- データベース構造変更がUI層に影響
- テスト時にRoom Entityのモックが必要
```

### 目標のデータフロー

```text
目標:
ViewModel → Repository → DAO → Room Entity
     ↑           │
     │           └── Mapper (変換)
     │                 ↓
     └──── Transaction (domain/model パッケージ) ──┘

利点:
- ViewModelは domain/model のみをimport
- データベース構造変更はRepository/Mapperで吸収
- テスト時はFakeドメインモデルのみで完結
```

### 影響範囲分析

| コンポーネント | 現在のdb import | 対応 |
|---------------|----------------|------|
| TransactionListViewModel | TransactionWithAccounts | ドメインモデルに変更 |
| TransactionFormViewModel | なし（LedgerTransaction経由） | LedgerTransaction→ドメインモデル |
| ProfileSelectionViewModel | Profile | ドメインモデルに変更 |
| ProfileDetailViewModel | Profile | ドメインモデルに変更 |
| AccountSummaryViewModel | AccountWithAmounts (間接) | ドメインモデルに変更 |
| TemplateDetailViewModelCompose | TemplateAccount, TemplateHeader | ドメインモデルに変更 |
| MainCoordinatorViewModel | Profile | ドメインモデルに変更 |
| MainActivityCompose | Profile, Option | ドメインモデルに変更 |
