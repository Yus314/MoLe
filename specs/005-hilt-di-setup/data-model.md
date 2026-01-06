# データモデル: Hilt 依存性注入セットアップ

**作成日**: 2026-01-06
**ブランチ**: `005-hilt-di-setup`

## DIコンポーネント階層

```
┌─────────────────────────────────────────────────────────────┐
│                    SingletonComponent                        │
│  (アプリケーションライフサイクル)                              │
│                                                              │
│  ┌─────────────────┐  ┌─────────────────┐                   │
│  │ DatabaseModule  │  │   DataModule    │                   │
│  │                 │  │                 │                   │
│  │ - DB            │  │ - Data          │                   │
│  │ - ProfileDAO    │  │                 │                   │
│  │ - TransactionDAO│  │                 │                   │
│  │ - AccountDAO    │  │                 │                   │
│  │ - 他DAO...      │  │                 │                   │
│  └─────────────────┘  └─────────────────┘                   │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   ActivityComponent                          │
│  (Activityライフサイクル)                                     │
│                                                              │
│  @AndroidEntryPoint Activities:                              │
│  - MainActivity                                              │
│  - NewTransactionActivity                                    │
│  - ProfileDetailActivity                                     │
│  - TemplateListActivity                                      │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   ViewModelComponent                         │
│  (ViewModel SavedStateライフサイクル)                         │
│                                                              │
│  @HiltViewModel ViewModels:                                  │
│  - MainModel (パイロット)                                     │
│  - NewTransactionModel (後続)                                 │
│  - ProfileDetailModel (後続)                                  │
│  - TemplateDetailsViewModel (後続)                            │
│  - CurrencySelectorModel (後続)                               │
└─────────────────────────────────────────────────────────────┘
```

## エンティティ定義

### 1. DatabaseModule

**目的**: Room Databaseと全DAOを提供
**スコープ**: `@InstallIn(SingletonComponent::class)`
**ファイル**: `app/src/main/kotlin/net/ktnx/mobileledger/di/DatabaseModule.kt`

| 提供物 | 型 | スコープ | 説明 |
|--------|-----|---------|------|
| provideDatabase | DB | @Singleton | 既存DB.get()をラップ |
| provideProfileDAO | ProfileDAO | - | プロファイルアクセス |
| provideTransactionDAO | TransactionDAO | - | 取引アクセス |
| provideAccountDAO | AccountDAO | - | 勘定科目アクセス |
| provideAccountValueDAO | AccountValueDAO | - | 勘定科目残高アクセス |
| provideTemplateHeaderDAO | TemplateHeaderDAO | - | テンプレートヘッダーアクセス |
| provideTemplateAccountDAO | TemplateAccountDAO | - | テンプレート勘定科目アクセス |
| provideCurrencyDAO | CurrencyDAO | - | 通貨アクセス |
| provideOptionDAO | OptionDAO | - | オプションアクセス |

### 2. DataModule

**目的**: 既存Dataシングルトンを提供
**スコープ**: `@InstallIn(SingletonComponent::class)`
**ファイル**: `app/src/main/kotlin/net/ktnx/mobileledger/di/DataModule.kt`

| 提供物 | 型 | スコープ | 説明 |
|--------|-----|---------|------|
| provideData | Data | @Singleton | グローバル状態（profiles, profile, etc.） |

### 3. MainModel (パイロットViewModel)

**目的**: メイン画面のUI状態管理
**アノテーション**: `@HiltViewModel`
**ファイル**: `app/src/main/kotlin/net/ktnx/mobileledger/ui/MainModel.kt`

**注入される依存関係**:

| 依存関係 | 型 | 用途 |
|----------|-----|------|
| profileDAO | ProfileDAO | プロファイル読み込み |
| transactionDAO | TransactionDAO | 取引リスト取得 |
| accountDAO | AccountDAO | 勘定科目フィルタリング |
| data | Data | グローバル状態観測 |

**現在の直接依存（移行対象）**:
- `DB.get().getProfileDAO()`
- `DB.get().getTransactionDAO()`
- `DB.get().getAccountDAO()`
- `Data.profiles`
- `Data.profile`

## 状態遷移

### ViewModel移行状態

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   未移行      │ ──► │   移行中      │ ──► │   完了        │
│              │     │              │     │              │
│ グローバル    │     │ @HiltViewModel │     │ テスト可能    │
│ シングルトン  │     │ 追加          │     │ モック注入可   │
│ 直接アクセス  │     │ 依存関係注入   │     │              │
└──────────────┘     └──────────────┘     └──────────────┘
```

### 移行順序

| 順序 | ViewModel | ステータス | 複雑度 |
|------|-----------|-----------|-------|
| 1 | MainModel | パイロット | 高（多数の依存関係） |
| 2 | CurrencySelectorModel | 後続 | 低 |
| 3 | ProfileDetailModel | 後続 | 中 |
| 4 | NewTransactionModel | 後続 | 中 |
| 5 | TemplateDetailsViewModel | 後続 | 中 |

## バリデーションルール

### コンパイル時検証（Hilt自動）

- すべての`@Inject`依存関係にバインディングが存在すること
- 循環依存がないこと
- スコープの整合性（ViewModelがSingletonの依存関係のみを持つこと）

### 実行時検証

- アプリ起動時にすべてのコンポーネントが正常に初期化されること
- `@AndroidEntryPoint`マークされたActivityが依存関係を受け取ること

## テストコンポーネント

### ユニットテスト構造

```
┌─────────────────────────────────────────────────────────────┐
│                    テストクラス                               │
│                                                              │
│  ┌─────────────────┐                                        │
│  │   Mock DAOs     │  MockK/Mockitoで作成                   │
│  │                 │                                        │
│  │ - mockProfileDAO│                                        │
│  │ - mockTxnDAO    │                                        │
│  └────────┬────────┘                                        │
│           │                                                 │
│           ▼                                                 │
│  ┌─────────────────┐                                        │
│  │   ViewModel     │  直接コンストラクタ呼び出し              │
│  │                 │                                        │
│  │ MainModel(      │                                        │
│  │   mockProfileDAO│                                        │
│  │   mockTxnDAO    │                                        │
│  │ )               │                                        │
│  └─────────────────┘                                        │
└─────────────────────────────────────────────────────────────┘
```

### インストルメンテーションテスト構造

```
┌─────────────────────────────────────────────────────────────┐
│              @HiltAndroidTest テストクラス                    │
│                                                              │
│  ┌─────────────────┐                                        │
│  │ TestDatabaseModule │  @TestInstallIn で本番モジュール置換  │
│  │                    │                                     │
│  │ - inMemoryDatabase │                                     │
│  │ - testDAOs         │                                     │
│  └────────────────────┘                                     │
│                                                              │
│  HiltAndroidRule → Activity起動 → ViewModel自動注入          │
└─────────────────────────────────────────────────────────────┘
```
