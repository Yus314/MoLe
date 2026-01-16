# Research: Domain Model Layer Introduction

**Feature**: 017-domain-model-layer
**Date**: 2026-01-16
**Status**: Complete

## Overview

ドメインモデル層導入にあたり、以下の調査を実施した。

## 1. 現在のモデル構造

### 1.1 既存の model パッケージ

**場所**: `/app/src/main/kotlin/net/ktnx/mobileledger/model/`

| クラス | 用途 | db依存 | 移行対象 |
|--------|------|--------|----------|
| LedgerTransaction | 取引の表現 | ✅ toDBO()/fromDBO() | ✅ Yes |
| LedgerTransactionAccount | 取引行の表現 | ✅ toDBO()/fromDBO() | ✅ Yes |
| LedgerAccount | 勘定科目の表現 | ✅ fromDBO() | ✅ Yes |
| LedgerAmount | 金額と通貨 | ❌ | ✅ Yes |
| Currency | 通貨設定 | ❌ | ✅ Yes |
| TransactionListItem | UI用リスト項目 | ❌ | ❌ No (UI専用) |
| AccountListItem | UI用リスト項目 | ❌ | ❌ No (UI専用) |
| MatchedTemplate | テンプレートマッチ結果 | ❌ | ❌ No (UseCase専用) |

**問題点**:
- `LedgerTransaction` が `db.TransactionWithAccounts` への変換メソッド（`toDBO()`）を持つ
- コンストラクタで `db.TransactionWithAccounts` を受け取る
- モデルクラス内で `App.profileRepository()` を直接呼び出している

### 1.2 既存の db パッケージ（Room Entity）

**場所**: `/app/src/main/kotlin/net/ktnx/mobileledger/db/`

| Entity | 用途 | 関連Entity |
|--------|------|------------|
| Profile | サーバー接続設定 | - |
| Transaction | 取引ヘッダー | Profile |
| TransactionAccount | 取引行 | Transaction |
| TransactionWithAccounts | Transaction + TransactionAccount リレーション | - |
| Account | 勘定科目 | Profile |
| AccountValue | 勘定科目の残高 | Account |
| AccountWithAmounts | Account + AccountValue リレーション | - |
| TemplateHeader | テンプレートヘッダー | Profile |
| TemplateAccount | テンプレート行 | TemplateHeader |
| TemplateWithAccounts | TemplateHeader + TemplateAccount リレーション | - |
| Currency | 通貨設定 | Profile |
| Option | プロファイル別オプション | Profile |

### 1.3 既存の domain/model パッケージ

**場所**: `/app/src/main/kotlin/net/ktnx/mobileledger/domain/model/`

既存のドメインモデルは**状態管理用**のみ:
- `SyncState` (sealed class) - 同期処理の状態
- `SyncProgress` - 同期進捗
- `SyncResult` - 同期結果
- `SyncError` (sealed class) - エラー種別
- `SendState` (sealed class) - 送信状態
- `BackupState` (sealed class) - バックアップ状態
- `TaskState` (sealed class) - タスク状態
- `Progress` - 進捗情報

**パターン**: sealed class + data class の組み合わせで型安全な状態表現

## 2. Repository インターフェース分析

### 2.1 現在の戻り値型

| Repository | メソッド | 現在の戻り値 | 目標の戻り値 |
|------------|----------|-------------|-------------|
| TransactionRepository | getAllTransactions() | `Flow<List<TransactionWithAccounts>>` | `Flow<List<Transaction>>` |
| TransactionRepository | getTransactionById() | `Flow<TransactionWithAccounts?>` | `Flow<Transaction?>` |
| ProfileRepository | getAllProfiles() | `Flow<List<Profile>>` | `Flow<List<Profile>>` (domain) |
| ProfileRepository | currentProfile | `StateFlow<Profile?>` | `StateFlow<Profile?>` (domain) |
| AccountRepository | getAllWithAmounts() | `Flow<List<AccountWithAmounts>>` | `Flow<List<Account>>` |
| TemplateRepository | getAllTemplates() | `Flow<List<TemplateWithAccounts>>` | `Flow<List<Template>>` |
| CurrencyRepository | getAllCurrencies() | `Flow<List<Currency>>` | `Flow<List<Currency>>` (domain) |

### 2.2 変更が必要なRepositoryメソッド

**TransactionRepository**:
- `getAllTransactions()` - Mapper追加
- `getTransactionsFiltered()` - Mapper追加
- `getTransactionById()` - Mapper追加
- `getTransactionByIdSync()` - Mapper追加
- `getFirstByDescription()` - Mapper追加
- `insertTransaction()` - 逆方向Mapper追加
- `storeTransaction()` - 逆方向Mapper追加

**ProfileRepository**:
- `getAllProfiles()` - Mapper追加
- `currentProfile` - Mapper追加
- `getProfileById()` - Mapper追加
- `insertProfile()` - 逆方向Mapper追加
- `updateProfile()` - 逆方向Mapper追加

**AccountRepository**:
- `getAllWithAmounts()` - Mapper追加
- `searchAccountNames()` - 変更不要（String返却）

**TemplateRepository**:
- `getAllTemplates()` - Mapper追加
- `getTemplateWithAccounts()` - Mapper追加

**CurrencyRepository**:
- `getAllCurrencies()` - Mapper追加
- `getCurrencyByName()` - Mapper追加

## 3. ViewModelの db import 状況

### 3.1 現在の import 状況

| ViewModel | db import | 用途 |
|-----------|-----------|------|
| TransactionListViewModel | TransactionWithAccounts | 取引一覧表示 |
| ProfileSelectionViewModel | Profile | プロファイル選択 |
| ProfileDetailViewModel | Profile | プロファイル編集 |
| ProfileDetailUiState | Profile | 状態保持 |
| MainCoordinatorViewModel | Profile | UI調整 |
| TemplateDetailViewModelCompose | TemplateAccount, TemplateHeader | テンプレート編集 |
| TemplateListViewModelCompose | TemplateHeader | テンプレート一覧 |
| TemplateApplicatorViewModel | TemplateAccount, TemplateWithAccounts | テンプレート適用 |

### 3.2 Activity の db import 状況

| Activity | db import | 用途 |
|----------|-----------|------|
| MainActivityCompose | Profile, Option | プロファイル表示、オプション参照 |
| NewTransactionActivityCompose | Profile | プロファイル参照 |
| ProfileDetailActivity | Profile | プロファイル編集 |
| ProfileThemedActivity | Profile | テーマ取得 |

## 4. Mapper 設計パターン

### 4.1 採用パターン: 拡張関数 + Mapper クラス

**Decision**: Mapper専用クラスを作成し、拡張関数として変換メソッドを提供

**Rationale**:
- Repository内でのみ使用されるため、スコープを限定できる
- 単体テストが容易
- 複雑な変換ロジックをカプセル化できる

**Alternatives considered**:
- モデル内にtoEntity/fromEntity - ドメインモデルがdb依存になるため却下
- 拡張関数のみ - 複雑な変換では可読性が下がるため却下

### 4.2 Mapper 配置

```text
data/repository/mapper/
├── TransactionMapper.kt   # Transaction ↔ TransactionWithAccounts
├── ProfileMapper.kt       # Profile ↔ Profile (db)
├── AccountMapper.kt       # Account ↔ AccountWithAmounts
├── TemplateMapper.kt      # Template ↔ TemplateWithAccounts
└── CurrencyMapper.kt      # Currency ↔ Currency (db)
```

### 4.3 Mapper の責務

```kotlin
// Mapper クラスの責務
object TransactionMapper {
    // DB Entity → Domain Model
    fun TransactionWithAccounts.toDomain(): Transaction

    // Domain Model → DB Entity
    fun Transaction.toEntity(profileId: Long): TransactionWithAccounts
}
```

## 5. ドメインモデル設計方針

### 5.1 イミュータビリティ

**Decision**: すべてのドメインモデルは `data class` + `val` プロパティで不変とする

**Rationale**:
- 状態変更時は`copy()`で新インスタンス生成
- スレッドセーフ
- Compose UIとの親和性が高い

### 5.2 ID の扱い

**Decision**: `id: Long?` (nullable) - 新規エンティティは`null`、保存後に値が設定される

**Rationale**:
- 新規作成と既存エンティティを型で区別可能
- Room のautoGenerate IDと整合

### 5.3 日付の扱い

**Decision**: `SimpleDate` を継続使用（既存ユーティリティクラス）

**Rationale**:
- 既存コードとの互換性維持
- 年・月・日を明示的に保持する設計はhledgerとの親和性が高い

### 5.4 ValidationResult パターン

**Decision**: sealed class で型安全なバリデーション結果を表現

```kotlin
sealed class ValidationResult {
    data object Success : ValidationResult()
    data class Error(val reasons: List<String>) : ValidationResult()
}
```

**Rationale**:
- 既存の SyncState 等と同じパターン
- コンパイル時に全ケースの処理を強制できる

## 6. 移行戦略

### 6.1 段階的移行

| Phase | エンティティ | 優先度 | 影響ViewModel数 |
|-------|-------------|--------|----------------|
| P1 | Transaction | 最高 | 2 (TransactionList, TransactionForm) |
| P2 | Profile | 高 | 4 (ProfileSelection, ProfileDetail, MainCoordinator, Main) |
| P3 | Account | 中 | 1 (AccountSummary) |
| P4 | Template | 低 | 2 (TemplateList, TemplateDetail) |
| P5 | Currency | 最低 | 0 (Repository経由のみ) |

### 6.2 既存モデルの非推奨化

**Decision**: `@Deprecated` アノテーションでマークし、全参照移行後に削除

```kotlin
@Deprecated(
    message = "Use net.ktnx.mobileledger.domain.model.Transaction instead",
    replaceWith = ReplaceWith("Transaction", "net.ktnx.mobileledger.domain.model.Transaction")
)
class LedgerTransaction { ... }
```

### 6.3 共存期間中のルール

- 新規コードはドメインモデルを使用
- 既存コードは段階的に移行
- UseCase層はドメインモデルを受け取る（TransactionSender等）

## 7. テスト戦略

### 7.1 ドメインモデルのテスト

- バリデーションロジックのテスト
- イミュータビリティの確認
- equals/hashCode の確認

### 7.2 Mapper のテスト

- 双方向変換の整合性
- null/空値のハンドリング
- 複雑なリレーション（Transaction + TransactionLine）の変換

### 7.3 Fake Repository の更新

- ドメインモデルを返すFake実装に更新
- 既存のFakeDAOは継続使用可能（Repository内部）

## 8. パフォーマンス考慮事項

### 8.1 Eager Conversion

**Decision**: Repository が Flow を返す時点で全件ドメインモデルに変換

**Rationale**:
- 数千件程度では遅延評価の利点が少ない
- コードの複雑性を抑えられる
- メモリ使用量は許容範囲内

### 8.2 大量データ対策（将来）

- 必要に応じてPaging3との統合を検討
- 現時点ではスコープ外

## Conclusion

調査の結果、ドメインモデル層の導入は以下の方針で進める:

1. **Mapperパターン**: Repository内にMapperクラスを配置し、変換を担当
2. **イミュータブルdata class**: Kotlin標準のdata classで不変なドメインモデルを定義
3. **段階的移行**: Transaction → Profile → Account → Template → Currency の順で移行
4. **既存モデルの非推奨化**: @Deprecated でマークし、移行完了後に削除
5. **Eager conversion**: Flow返却時に全件変換（遅延評価は不要）

すべての NEEDS CLARIFICATION が解決され、Phase 1 に進行可能。
