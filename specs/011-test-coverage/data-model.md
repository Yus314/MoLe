# Data Model: テスト抽象化設計

**Feature**: 011-test-coverage
**Date**: 2026-01-13
**Status**: Complete

## 概要

テストカバレッジ向上のために導入する抽象化レイヤーの設計。既存の Repository パターンに従い、テスト可能な依存関係注入を実現する。

## 新規インターフェース

### 1. TransactionSender

取引送信の抽象化。SendTransactionTask（Thread サブクラス）の直接インスタンス化を置き換える。

```kotlin
package net.ktnx.mobileledger.domain.usecase

import net.ktnx.mobileledger.db.Profile
import net.ktnx.mobileledger.model.LedgerTransaction

/**
 * Interface for sending transactions to the ledger server.
 * Abstracts the network layer to enable unit testing without network dependencies.
 */
interface TransactionSender {
    /**
     * Send a transaction to the ledger server.
     *
     * @param profile The profile containing server configuration
     * @param transaction The transaction to send
     * @param simulate If true, simulate the send without actually sending
     * @return Result.success(Unit) on success, Result.failure(exception) on error
     */
    suspend fun send(
        profile: Profile,
        transaction: LedgerTransaction,
        simulate: Boolean = false
    ): Result<Unit>
}
```

**実装クラス**:

| クラス | パッケージ | 用途 |
|--------|-----------|------|
| `TransactionSenderImpl` | `domain.usecase` | 本番実装（既存 SendTransactionTask をラップ） |
| `FakeTransactionSender` | `test/.../fake` | テスト用 Fake |

## 既存インターフェース（変更なし）

### PreferencesRepository

既に存在し、必要なメソッドが定義済み。MainViewModel への注入のみ追加。

```kotlin
interface PreferencesRepository {
    fun getShowZeroBalanceAccounts(): Boolean
    fun setShowZeroBalanceAccounts(value: Boolean)
    fun getStartupProfileId(): Long
    fun setStartupProfileId(profileId: Long)
    fun getStartupTheme(): Int
    fun setStartupTheme(theme: Int)
}
```

## Fake 実装設計

### FakePreferencesRepository

```kotlin
package net.ktnx.mobileledger.fake

class FakePreferencesRepository : PreferencesRepository {
    var showZeroBalanceAccounts = false
    var startupProfileId = -1L
    var startupTheme = -1

    override fun getShowZeroBalanceAccounts(): Boolean = showZeroBalanceAccounts

    override fun setShowZeroBalanceAccounts(value: Boolean) {
        showZeroBalanceAccounts = value
    }

    override fun getStartupProfileId(): Long = startupProfileId

    override fun setStartupProfileId(profileId: Long) {
        startupProfileId = profileId
    }

    override fun getStartupTheme(): Int = startupTheme

    override fun setStartupTheme(theme: Int) {
        startupTheme = theme
    }
}
```

### FakeTransactionSender

```kotlin
package net.ktnx.mobileledger.fake

class FakeTransactionSender : TransactionSender {
    var shouldSucceed = true
    var errorMessage = "Simulated failure"
    val sentTransactions = mutableListOf<SentTransaction>()

    data class SentTransaction(
        val profile: Profile,
        val transaction: LedgerTransaction,
        val simulate: Boolean
    )

    override suspend fun send(
        profile: Profile,
        transaction: LedgerTransaction,
        simulate: Boolean
    ): Result<Unit> {
        sentTransactions.add(SentTransaction(profile, transaction, simulate))
        return if (shouldSucceed) {
            Result.success(Unit)
        } else {
            Result.failure(Exception(errorMessage))
        }
    }

    fun reset() {
        shouldSucceed = true
        errorMessage = "Simulated failure"
        sentTransactions.clear()
    }
}
```

### FakeCurrencyFormatter

```kotlin
package net.ktnx.mobileledger.fake

class FakeCurrencyFormatter : CurrencyFormatter {
    var formattedNumber = "0.00"

    override fun formatNumber(number: Float, currencyName: String?): String = formattedNumber

    override fun formatNumber(number: Double, currencyName: String?): String = formattedNumber
}
```

## ViewModel 依存関係マッピング

### MainViewModel

| 依存関係 | インターフェース | 本番実装 | テスト実装 |
|----------|-----------------|----------|------------|
| profileRepository | ProfileRepository | ProfileRepositoryImpl | FakeProfileRepositoryForViewModel |
| accountRepository | AccountRepository | AccountRepositoryImpl | FakeAccountRepositoryForViewModel |
| transactionRepository | TransactionRepository | TransactionRepositoryImpl | FakeTransactionRepositoryForViewModel |
| optionRepository | OptionRepository | OptionRepositoryImpl | FakeOptionRepositoryForViewModel |
| backgroundTaskManager | BackgroundTaskManager | BackgroundTaskManagerImpl | FakeBackgroundTaskManagerForViewModel |
| appStateService | AppStateService | AppStateServiceImpl | FakeAppStateServiceForViewModel |
| **preferencesRepository** | PreferencesRepository | PreferencesRepositoryImpl | **FakePreferencesRepository** (新規) |

### NewTransactionViewModel

| 依存関係 | インターフェース | 本番実装 | テスト実装 |
|----------|-----------------|----------|------------|
| profileRepository | ProfileRepository | ProfileRepositoryImpl | FakeProfileRepositoryForViewModel |
| transactionRepository | TransactionRepository | TransactionRepositoryImpl | FakeTransactionRepositoryForViewModel |
| accountRepository | AccountRepository | AccountRepositoryImpl | FakeAccountRepositoryForViewModel |
| templateRepository | TemplateRepository | TemplateRepositoryImpl | FakeTemplateRepository (新規) |
| currencyRepository | CurrencyRepository | CurrencyRepositoryImpl | FakeCurrencyRepository (新規) |
| currencyFormatter | CurrencyFormatter | CurrencyFormatterImpl | FakeCurrencyFormatter (新規) |
| appStateService | AppStateService | AppStateServiceImpl | FakeAppStateServiceForViewModel |
| **transactionSender** | TransactionSender | TransactionSenderImpl | **FakeTransactionSender** (新規) |

## DI モジュール変更

### UseCaseModule（新規）

```kotlin
package net.ktnx.mobileledger.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import net.ktnx.mobileledger.domain.usecase.TransactionSender
import net.ktnx.mobileledger.domain.usecase.TransactionSenderImpl

@Module
@InstallIn(SingletonComponent::class)
abstract class UseCaseModule {

    @Binds
    @Singleton
    abstract fun bindTransactionSender(
        impl: TransactionSenderImpl
    ): TransactionSender
}
```

## 状態遷移

### TransactionSender 送信フロー

```text
           ┌─────────────────────────────────────────────────────────┐
           │                NewTransactionViewModel                   │
           └─────────────────────────────────────────────────────────┘
                                    │
                                    │ submitTransaction()
                                    ▼
           ┌─────────────────────────────────────────────────────────┐
           │              TransactionSender.send()                    │
           │                                                          │
           │  ┌──────────────────────┐  ┌──────────────────────┐    │
           │  │    Result.success    │  │    Result.failure    │    │
           │  └──────────────────────┘  └──────────────────────┘    │
           └─────────────────────────────────────────────────────────┘
                         │                        │
                         ▼                        ▼
           ┌──────────────────────┐  ┌──────────────────────────────┐
           │ 1. Save to local DB  │  │ Update UiState with error    │
           │ 2. Clear form        │  │                              │
           │ 3. Navigate back     │  │                              │
           └──────────────────────┘  └──────────────────────────────┘
```

## バリデーションルール

### LedgerTransaction バリデーション（既存）

| フィールド | ルール | エラー時の動作 |
|-----------|--------|---------------|
| date | null 不可 | フォーム送信ブロック |
| description | 空文字列不可 | フォーム送信ブロック |
| accounts | 最低2件 | フォーム送信ブロック |
| 金額合計 | 0 であること（バランス） | フォーム送信ブロック |

### Profile バリデーション（既存）

| フィールド | ルール | エラー時の動作 |
|-----------|--------|---------------|
| url | 有効な URL | 接続エラー |
| apiVersion | サポート対象バージョン | ApiNotSupportedException |
