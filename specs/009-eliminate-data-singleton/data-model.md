# Data Model: Data.kt シングルトンの廃止

**Date**: 2026-01-12
**Feature**: 009-eliminate-data-singleton

## 1. Overview

本ドキュメントでは、AppStateManager シングルトンを置換する新規サービスのデータモデルを定義する。

## 2. Domain Entities

### 2.1 既存エンティティ (変更なし)

以下のエンティティは Room Database で既に定義済みであり、本リファクタリングでは変更しない。

| Entity | Location | Description |
|--------|----------|-------------|
| Profile | `model/Profile.kt` | プロファイル情報 |
| LedgerTransaction | `model/LedgerTransaction.kt` | 取引情報 |
| LedgerAccount | `model/LedgerAccount.kt` | 勘定科目情報 |
| Currency | `model/Currency.kt` | 通貨情報 |
| TemplateHeader | `model/TemplateHeader.kt` | テンプレートヘッダー |

### 2.2 新規データクラス

#### TaskProgress

バックグラウンドタスクの進捗状態を表現する。

```kotlin
package net.ktnx.mobileledger.service

/**
 * バックグラウンドタスクの進捗情報
 *
 * @property taskId タスクの一意識別子
 * @property state タスクの状態
 * @property message ユーザー表示用メッセージ
 * @property current 現在の進捗 (オプション)
 * @property total 合計 (オプション)
 */
data class TaskProgress(
    val taskId: String,
    val state: TaskState,
    val message: String,
    val current: Int = 0,
    val total: Int = 0
) {
    /**
     * 進捗率を 0.0〜1.0 で返す
     * total が 0 の場合は 0.0 を返す
     */
    val progressFraction: Float
        get() = if (total > 0) current.toFloat() / total else 0f

    /**
     * 進捗率を百分率で返す (0〜100)
     */
    val progressPercent: Int
        get() = (progressFraction * 100).toInt()
}
```

#### TaskState

タスクの状態を表す列挙型。

```kotlin
package net.ktnox.mobileledger.service

/**
 * バックグラウンドタスクの状態
 */
enum class TaskState {
    /** タスク開始準備中 */
    STARTING,

    /** タスク実行中 */
    RUNNING,

    /** タスク正常完了 */
    FINISHED,

    /** タスクエラー終了 */
    ERROR
}
```

#### SyncInfo

同期結果の情報を保持する。

```kotlin
package net.ktnox.mobileledger.service

import java.util.Date

/**
 * データ同期の結果情報
 *
 * @property date 同期完了日時
 * @property transactionCount 取得した取引数
 * @property accountCount 表示対象のアカウント数
 * @property totalAccountCount 全アカウント数
 */
data class SyncInfo(
    val date: Date?,
    val transactionCount: Int,
    val accountCount: Int,
    val totalAccountCount: Int
) {
    companion object {
        /**
         * 同期未実行時の初期値
         */
        val EMPTY = SyncInfo(
            date = null,
            transactionCount = 0,
            accountCount = 0,
            totalAccountCount = 0
        )
    }

    /**
     * 同期が実行されたことがあるか
     */
    val hasSynced: Boolean
        get() = date != null
}
```

#### CurrencyFormatConfig

通貨フォーマット設定を保持する。

```kotlin
package net.ktnox.mobileledger.service

import net.ktnx.mobileledger.model.Currency
import java.util.Locale

/**
 * 通貨フォーマット設定
 *
 * @property locale 現在のロケール
 * @property symbolPosition 通貨記号の位置
 * @property hasGap 記号と数値の間にスペースを入れるか
 * @property decimalSeparator 小数点区切り文字
 * @property groupingSeparator 桁区切り文字
 */
data class CurrencyFormatConfig(
    val locale: Locale,
    val symbolPosition: Currency.Position,
    val hasGap: Boolean,
    val decimalSeparator: Char,
    val groupingSeparator: Char
) {
    companion object {
        /**
         * ロケールからデフォルト設定を生成
         */
        fun fromLocale(locale: Locale): CurrencyFormatConfig {
            val symbols = java.text.DecimalFormatSymbols.getInstance(locale)
            return CurrencyFormatConfig(
                locale = locale,
                symbolPosition = Currency.Position.before, // デフォルト
                hasGap = true,
                decimalSeparator = symbols.decimalSeparator,
                groupingSeparator = symbols.groupingSeparator
            )
        }
    }
}
```

## 3. State Relationships

```
┌─────────────────────────────────────────────────────────────────┐
│                         Service Layer                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────────────────┐    ┌─────────────────────┐            │
│  │ BackgroundTaskManager│    │   CurrencyFormatter │            │
│  │                     │    │                     │            │
│  │ - isRunning: Flow   │    │ - config: Flow      │            │
│  │ - progress: Flow    │    │ - locale: Flow      │            │
│  │                     │    │                     │            │
│  │ Holds: TaskProgress │    │ Holds:              │            │
│  └─────────────────────┘    │   CurrencyFormatConfig           │
│                             └─────────────────────┘            │
│                                                                  │
│  ┌─────────────────────┐    ┌─────────────────────┐            │
│  │   AppStateService   │    │  ProfileRepository  │            │
│  │                     │    │     (既存)          │            │
│  │ - lastSyncInfo: Flow│    │                     │            │
│  │ - drawerOpen: Flow  │    │ - currentProfile:   │            │
│  │                     │    │     StateFlow       │            │
│  │ Holds: SyncInfo     │    │ - profiles: Flow    │            │
│  └─────────────────────┘    └─────────────────────┘            │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Inject via Hilt
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                         ViewModel Layer                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────────────────────────────────────────────────┐        │
│  │                    MainViewModel                     │        │
│  │                                                     │        │
│  │  Depends on:                                        │        │
│  │  - ProfileRepository                                │        │
│  │  - BackgroundTaskManager                            │        │
│  │  - CurrencyFormatter                                │        │
│  │  - AppStateService                                  │        │
│  │                                                     │        │
│  │  Exposes to UI:                                     │        │
│  │  - MainUiState (combined)                           │        │
│  │  - effects (Channel)                                │        │
│  └─────────────────────────────────────────────────────┘        │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## 4. State Transitions

### 4.1 TaskProgress State Machine

```
                    taskStarted()
    ┌───────────────────────────────────────┐
    │                                       │
    ▼                                       │
┌───────────┐     updateProgress()    ┌─────────────┐
│ STARTING  │ ───────────────────────►│   RUNNING   │
└───────────┘                         └─────────────┘
                                           │
                    ┌──────────────────────┴──────────────────────┐
                    │                                              │
                    ▼ taskFinished()                               ▼ onError()
              ┌───────────┐                                  ┌───────────┐
              │ FINISHED  │                                  │   ERROR   │
              └───────────┘                                  └───────────┘
```

### 4.2 Profile Selection Flow

```
User selects profile
         │
         ▼
┌─────────────────────────────┐
│ ProfileRepository           │
│ .setCurrentProfile(profile) │
└─────────────────────────────┘
         │
         │ updates StateFlow
         ▼
┌─────────────────────────────┐
│ ProfileRepository           │
│ .currentProfile emits       │
└─────────────────────────────┘
         │
         │ collected by
         ▼
┌─────────────────────────────┐
│ MainViewModel               │
│ - updates uiState           │
│ - triggers data reload      │
└─────────────────────────────┘
         │
         │ recomposition
         ▼
┌─────────────────────────────┐
│ MainScreen (Compose)        │
│ - displays new profile      │
└─────────────────────────────┘
```

## 5. Validation Rules

### 5.1 TaskProgress Validation

| Field | Rule | Error Handling |
|-------|------|---------------|
| taskId | 非空文字列 | require() で検証 |
| current | >= 0 | 負の場合は 0 にクランプ |
| total | >= 0 | 負の場合は 0 にクランプ |
| current <= total | 進捗は合計を超えない | current を total にクランプ |

### 5.2 SyncInfo Validation

| Field | Rule | Error Handling |
|-------|------|---------------|
| transactionCount | >= 0 | 負の場合は 0 |
| accountCount | >= 0 | 負の場合は 0 |
| totalAccountCount | >= accountCount | totalAccountCount を下限として accountCount を使用 |

## 6. Migration Mapping

### 6.1 AppStateManager → New Services

| AppStateManager Property | New Location | Type Change |
|-------------------------|--------------|-------------|
| `profiles` | ProfileRepository.getAllProfiles() | LiveData → Flow |
| `profile` | ProfileRepository.currentProfile | LiveData → StateFlow |
| `backgroundTasksRunning` | BackgroundTaskManager.isRunning | LiveData → StateFlow |
| `backgroundTaskProgress` | BackgroundTaskManager.progress | LiveData → StateFlow |
| `locale` | CurrencyFormatter.locale | LiveData → StateFlow |
| `currencySymbolPosition` | CurrencyFormatter.config | LiveData → StateFlow |
| `currencyGap` | CurrencyFormatter.config | LiveData → StateFlow |
| `drawerOpen` | AppStateService.drawerOpen | LiveData → StateFlow |
| `lastUpdateDate` | AppStateService.lastSyncInfo | LiveData → StateFlow |
| `lastUpdateTransactionCount` | AppStateService.lastSyncInfo | LiveData → StateFlow |
| `lastUpdateAccountCount` | AppStateService.lastSyncInfo | LiveData → StateFlow |

### 6.2 AppStateManager Methods → New Services

| AppStateManager Method | New Location |
|-----------------------|--------------|
| `getProfile()` | ProfileRepository.currentProfile.value |
| `setCurrentProfile()` | ProfileRepository.setCurrentProfile() |
| `backgroundTaskStarted()` | BackgroundTaskManager.taskStarted() |
| `backgroundTaskFinished()` | BackgroundTaskManager.taskFinished() |
| `formatCurrency()` | CurrencyFormatter.formatCurrency() |
| `formatNumber()` | CurrencyFormatter.formatNumber() |
| `parseNumber()` | CurrencyFormatter.parseNumber() |
| `getDecimalSeparator()` | CurrencyFormatter.getDecimalSeparator() |
| `refreshCurrencyData()` | CurrencyFormatter.refresh() |

## 7. Database Schema

**変更なし** - 本リファクタリングではデータベーススキーマの変更は行わない。

既存の Room エンティティとその関連は維持される:
- Profile テーブル
- Transaction テーブル
- Account テーブル
- Currency テーブル
- Template 関連テーブル
- Option テーブル
