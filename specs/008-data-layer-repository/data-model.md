# Data Model: Data Layer Repository Migration

**Feature**: 008-data-layer-repository
**Date**: 2026-01-10

## Overview

Repositoryパターン導入に伴うデータモデルとコンポーネント構造の定義。既存のRoom Entity/DAOは変更せず、新規Repository層を追加。

---

## Repository Entities

### ProfileRepository

**責務**: プロファイルデータのCRUD + 現在のプロファイル選択状態の管理

**依存するDAO**: ProfileDAO

**公開メソッド**:

| Method | Return Type | Description |
|--------|-------------|-------------|
| `getAllProfiles()` | `Flow<List<Profile>>` | 全プロファイル（orderNo順） |
| `getProfileById(id: Long)` | `suspend Profile?` | ID指定取得 |
| `getProfileByUuid(uuid: String)` | `suspend Profile?` | UUID指定取得 |
| `insertProfile(profile: Profile)` | `suspend Long` | 新規作成 |
| `updateProfile(profile: Profile)` | `suspend Unit` | 更新 |
| `deleteProfile(profile: Profile)` | `suspend Unit` | 削除 |
| `getProfileCount()` | `suspend Int` | プロファイル数 |
| `currentProfile` | `StateFlow<Profile?>` | 現在のプロファイル（読み取り専用） |
| `setCurrentProfile(profile: Profile?)` | `Unit` | 現在のプロファイル設定 |

**状態**:
- `_currentProfile: MutableStateFlow<Profile?>` - 内部状態
- Data.ktから移行: `profile: MutableLiveData<Profile?>`

---

### TransactionRepository

**責務**: 取引データのCRUD + 検索 + 同期操作

**依存するDAO**: TransactionDAO, TransactionAccountDAO

**公開メソッド**:

| Method | Return Type | Description |
|--------|-------------|-------------|
| `getAllTransactions(profileId: Long)` | `Flow<List<TransactionWithAccounts>>` | 全取引（プロファイル指定） |
| `getTransactionsFiltered(profileId: Long, accountName: String?)` | `Flow<List<TransactionWithAccounts>>` | フィルタ済み取引 |
| `getTransactionById(id: Long)` | `Flow<TransactionWithAccounts?>` | ID指定取得 |
| `insertTransaction(transaction: TransactionWithAccounts)` | `suspend Unit` | 新規作成 |
| `updateTransaction(transaction: TransactionWithAccounts)` | `suspend Unit` | 更新 |
| `deleteTransaction(transaction: Transaction)` | `suspend Unit` | 削除 |
| `storeTransactions(list: List<TransactionWithAccounts>, profileId: Long)` | `suspend Unit` | 一括保存（同期用） |
| `searchByDescription(term: String)` | `suspend List<DescriptionContainer>` | 説明検索 |
| `getFirstByDescription(description: String)` | `suspend TransactionWithAccounts?` | 説明でテンプレート取得 |

---

### AccountRepository

**責務**: アカウントデータのCRUD + 残高管理 + 検索

**依存するDAO**: AccountDAO, AccountValueDAO

**公開メソッド**:

| Method | Return Type | Description |
|--------|-------------|-------------|
| `getAllAccounts(profileId: Long, includeZero: Boolean)` | `Flow<List<AccountWithAmounts>>` | 全アカウント |
| `getAccountById(id: Long)` | `suspend Account?` | ID指定取得 |
| `getAccountByName(profileId: Long, name: String)` | `Flow<AccountWithAmounts?>` | 名前指定取得 |
| `searchAccountNames(profileId: Long, term: String)` | `Flow<List<AccountNameContainer>>` | 名前検索 |
| `insertAccount(account: AccountWithAmounts)` | `suspend Long` | 新規作成 |
| `updateAccount(account: Account)` | `suspend Unit` | 更新 |
| `deleteAccount(account: Account)` | `suspend Unit` | 削除 |
| `storeAccounts(accounts: List<AccountWithAmounts>, profileId: Long)` | `suspend Unit` | 一括保存 |
| `getAccountCount(profileId: Long)` | `suspend Int` | アカウント数 |

---

### TemplateRepository

**責務**: 取引テンプレートのCRUD + 複製

**依存するDAO**: TemplateHeaderDAO, TemplateAccountDAO

**公開メソッド**:

| Method | Return Type | Description |
|--------|-------------|-------------|
| `getAllTemplates()` | `Flow<List<TemplateHeader>>` | 全テンプレート |
| `getTemplateById(id: Long)` | `Flow<TemplateWithAccounts?>` | ID指定取得 |
| `getTemplateByUuid(uuid: String)` | `suspend TemplateWithAccounts?` | UUID指定取得 |
| `insertTemplate(template: TemplateWithAccounts)` | `suspend Unit` | 新規作成 |
| `updateTemplate(template: TemplateWithAccounts)` | `suspend Unit` | 更新 |
| `deleteTemplate(header: TemplateHeader)` | `suspend Unit` | 削除 |
| `duplicateTemplate(id: Long)` | `suspend TemplateWithAccounts` | 複製 |

---

## AppStateManager (renamed from Data.kt)

**責務**: UI/アプリ状態の管理（データアクセス層とは別責務）

**保持する状態**:

| Field | Type | Description |
|-------|------|-------------|
| `backgroundTasksRunning` | `MutableLiveData<Boolean>` | バックグラウンドタスク実行中 |
| `backgroundTaskProgress` | `MutableLiveData<Progress>` | タスク進捗 |
| `drawerOpen` | `MutableLiveData<Boolean>` | ドロワー開閉状態 |
| `currencySymbolPosition` | `MutableLiveData<Currency.Position>` | 通貨記号位置 |
| `currencyGap` | `MutableLiveData<Boolean>` | 通貨記号と数値の間隔 |
| `locale` | `MutableLiveData<Locale>` | ロケール |
| `lastUpdateDate` | `MutableLiveData<Date?>` | 最終更新日時 |
| `lastUpdateTransactionCount` | `MutableLiveData<Int>` | 最終更新取引数 |
| `lastUpdateAccountCount` | `MutableLiveData<Int>` | 最終更新アカウント数 |
| `lastTransactionsUpdateText` | `MutableLiveData<String>` | 更新テキスト |
| `lastAccountsUpdateText` | `MutableLiveData<String>` | 更新テキスト |

**削除される状態** (ProfileRepositoryに移行):
- `profiles: LiveData<List<Profile>>` → `ProfileRepository.getAllProfiles()`
- `profile: MutableLiveData<Profile?>` → `ProfileRepository.currentProfile`

---

## Hilt DI Bindings

### RepositoryModule

```text
┌─────────────────────────────────────────────────────────────────┐
│                       RepositoryModule                           │
│ @Module @InstallIn(SingletonComponent::class)                   │
├─────────────────────────────────────────────────────────────────┤
│ @Binds @Singleton                                               │
│ ProfileRepository ← ProfileRepositoryImpl                       │
│                                                                 │
│ @Binds @Singleton                                               │
│ TransactionRepository ← TransactionRepositoryImpl               │
│                                                                 │
│ @Binds @Singleton                                               │
│ AccountRepository ← AccountRepositoryImpl                       │
│                                                                 │
│ @Binds @Singleton                                               │
│ TemplateRepository ← TemplateRepositoryImpl                     │
└─────────────────────────────────────────────────────────────────┘
```

### Dependency Graph

```text
ViewModel
    │
    ├── ProfileRepository (interface)
    │       │
    │       └── ProfileRepositoryImpl
    │               │
    │               └── ProfileDAO
    │
    ├── TransactionRepository (interface)
    │       │
    │       └── TransactionRepositoryImpl
    │               │
    │               ├── TransactionDAO
    │               └── TransactionAccountDAO
    │
    ├── AccountRepository (interface)
    │       │
    │       └── AccountRepositoryImpl
    │               │
    │               ├── AccountDAO
    │               └── AccountValueDAO
    │
    ├── TemplateRepository (interface)
    │       │
    │       └── TemplateRepositoryImpl
    │               │
    │               ├── TemplateHeaderDAO
    │               └── TemplateAccountDAO
    │
    └── AppStateManager (object, renamed from Data)
            │
            └── (no DAO dependency - UI state only)
```

---

## ViewModel Dependencies After Migration

### MainViewModel

**Before**:
```kotlin
@HiltViewModel
class MainViewModel @Inject constructor(
    private val profileDAO: ProfileDAO,
    private val accountDAO: AccountDAO,
    private val transactionDAO: TransactionDAO,
    private val data: Data
) : ViewModel()
```

**After**:
```kotlin
@HiltViewModel
class MainViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val appStateManager: AppStateManager
) : ViewModel()
```

### NewTransactionViewModel

**Before**:
```kotlin
@HiltViewModel
class NewTransactionViewModel @Inject constructor(
    private val accountDAO: AccountDAO,
    private val transactionDAO: TransactionDAO,
    private val templateHeaderDAO: TemplateHeaderDAO,
    private val currencyDAO: CurrencyDAO
) : ViewModel()
```

**After**:
```kotlin
@HiltViewModel
class NewTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val templateRepository: TemplateRepository,
    private val appStateManager: AppStateManager  // for locale/number formatting
) : ViewModel()
```

### ProfileDetailViewModel

**Before**:
```kotlin
@HiltViewModel
class ProfileDetailViewModel @Inject constructor(
    private val profileDAO: ProfileDAO,
    savedStateHandle: SavedStateHandle
) : ViewModel()
```

**After**:
```kotlin
@HiltViewModel
class ProfileDetailViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel()
```

### BackupsViewModel

**Before**:
```kotlin
@HiltViewModel
class BackupsViewModel @Inject constructor() : ViewModel()
// Uses Data.getProfile() directly
```

**After**:
```kotlin
@HiltViewModel
class BackupsViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel()
```
