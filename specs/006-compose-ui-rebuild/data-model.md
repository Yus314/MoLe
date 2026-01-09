# Data Model: Jetpack Compose UI Rebuild

**Feature**: 006-compose-ui-rebuild
**Date**: 2026-01-06

## Overview

本機能はUI層の再構築であり、既存のRoomデータベースエンティティは変更しない。
このドキュメントでは、Compose UI層で使用するUI State モデルを定義する。

---

## 既存エンティティ（変更なし）

以下のRoomエンティティは既存のまま継続使用する。

| エンティティ | テーブル | 役割 |
|-------------|---------|------|
| Profile | profiles | サーバー接続設定、テーマカラー |
| Transaction | transactions | 取引データ |
| TransactionAccount | transaction_accounts | 取引のアカウント行 |
| Account | accounts | アカウント情報 |
| AccountValue | account_values | アカウント残高 |
| TemplateHeader | templates | テンプレートヘッダー |
| TemplateAccount | template_accounts | テンプレートのアカウント行 |
| Currency | currencies | 通貨情報 |

---

## UI State Models

### ProfileDetailUiState

```kotlin
data class ProfileDetailUiState(
    val profileId: Long? = null,
    val name: String = "",
    val url: String = "",
    val useAuthentication: Boolean = false,
    val authUser: String = "",
    val authPassword: String = "",
    val themeHue: Float = 0f,
    val preferredAccountsFilter: String = "",
    val futureDates: FutureDates = FutureDates.All,
    val apiVersion: Int = 0,

    // UI状態
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isTestingConnection: Boolean = false,
    val connectionTestResult: ConnectionTestResult? = null,
    val hasUnsavedChanges: Boolean = false,
    val validationErrors: Map<String, String> = emptyMap()
)

enum class FutureDates { All, ThisMonth, Today }

sealed class ConnectionTestResult {
    data object Success : ConnectionTestResult()
    data class Error(val message: String) : ConnectionTestResult()
}
```

### TemplateListUiState

```kotlin
data class TemplateListUiState(
    val templates: List<TemplateItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedTemplateId: Long? = null
)

data class TemplateItem(
    val id: Long,
    val name: String,
    val description: String,
    val accountCount: Int,
    val lastUsed: LocalDateTime?
)
```

### TemplateDetailUiState

```kotlin
data class TemplateDetailUiState(
    val templateId: Long? = null,
    val name: String = "",
    val description: String = "",
    val accounts: List<TemplateAccountRow> = listOf(TemplateAccountRow(), TemplateAccountRow()),

    // UI状態
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val validationErrors: Map<String, String> = emptyMap()
)

data class TemplateAccountRow(
    val id: Long = 0,
    val accountName: String = "",
    val amount: String = "",
    val currency: String = "",
    val isAmountNegated: Boolean = false
)
```

### MainUiState

```kotlin
data class MainUiState(
    val currentProfile: Profile? = null,
    val profiles: List<Profile> = emptyList(),
    val selectedTab: MainTab = MainTab.Accounts,
    val isDrawerOpen: Boolean = false,
    val isRefreshing: Boolean = false,
    val lastUpdateDate: LocalDateTime? = null,
    val backgroundTaskProgress: Float = 0f,
    val backgroundTasksRunning: Boolean = false
)

enum class MainTab { Accounts, Transactions }
```

### AccountSummaryUiState

```kotlin
data class AccountSummaryUiState(
    val accounts: List<AccountSummaryItem> = emptyList(),
    val showZeroBalanceAccounts: Boolean = false,
    val accountFilter: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class AccountSummaryItem(
    val id: Long,
    val name: String,
    val shortName: String,  // 最後のセグメント
    val level: Int,         // インデントレベル
    val amounts: List<AccountAmount>,
    val isExpanded: Boolean = true
)

data class AccountAmount(
    val amount: BigDecimal,
    val currency: String
)
```

### TransactionListUiState

```kotlin
data class TransactionListUiState(
    val transactions: List<TransactionListItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val foundTransactionIndex: Int? = null
)

sealed class TransactionListItem {
    data class Header(val date: LocalDate, val formattedDate: String) : TransactionListItem()
    data class Transaction(
        val id: Long,
        val description: String,
        val date: LocalDate,
        val accounts: List<TransactionAccountSummary>
    ) : TransactionListItem()
}

data class TransactionAccountSummary(
    val accountName: String,
    val amount: BigDecimal,
    val currency: String
)
```

### NewTransactionUiState

```kotlin
data class NewTransactionUiState(
    val date: LocalDate = LocalDate.now(),
    val description: String = "",
    val comment: String = "",
    val accounts: List<TransactionAccountRow> = listOf(
        TransactionAccountRow(),
        TransactionAccountRow()
    ),

    // UI状態
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val showDatePicker: Boolean = false,
    val showTemplateSelector: Boolean = false,
    val availableTemplates: List<TemplateItem> = emptyList(),
    val accountSuggestions: List<String> = emptyList(),
    val focusedRowIndex: Int? = null,
    val validationErrors: Map<String, String> = emptyMap()
)

data class TransactionAccountRow(
    val id: Int = 0,  // ローカルID（リスト内での識別用）
    val accountName: String = "",
    val accountNameCursor: Int = 0,
    val amount: String = "",
    val amountHint: String = "",
    val currency: String = "",
    val comment: String = "",
    val isAmountValid: Boolean = true
)
```

---

## State Transitions

### ProfileDetailUiState

```
Initial → Loading → Loaded
                 ↓
Editing → Validating → Saving → Saved
                    ↓
                  Error

TestingConnection → ConnectionSuccess / ConnectionError
```

### MainUiState

```
Initial → Loading → Loaded
                 ↓
             Refreshing → Refreshed / Error
                 ↓
         DrawerOpened / DrawerClosed
                 ↓
           TabChanged
```

### NewTransactionUiState

```
Initial → Editing → AddRow / RemoveRow
              ↓
       ApplyTemplate
              ↓
        Validating → Submitting → Submitted / SubmitError
```

---

## Validation Rules

### Profile Validation

| フィールド | ルール |
|-----------|--------|
| name | 必須、空白不可 |
| url | 必須、有効なURL形式 |
| authUser | useAuthentication=trueの場合は必須 |
| authPassword | useAuthentication=trueの場合は必須 |
| themeHue | 0-360の範囲 |

### Template Validation

| フィールド | ルール |
|-----------|--------|
| name | 必須、空白不可 |
| accounts | 最低2行必須 |
| accountName | 各行で必須 |

### Transaction Validation

| フィールド | ルール |
|-----------|--------|
| date | 必須、有効な日付 |
| description | 必須、空白不可 |
| accounts | 最低2行必須 |
| accountName | 各行で必須 |
| amounts | 合計が0になること（バランスチェック） |

---

## Data Flow

```
Room DB (既存)
    ↓
DAO (既存)
    ↓
ViewModel (StateFlow)
    ↓
Compose UI (collectAsState)
    ↓
User Interaction
    ↓
ViewModel (update state, call DAO)
    ↓
Room DB (既存)
```
