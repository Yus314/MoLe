# Quickstart: Domain Model Layer Implementation

**Feature**: 017-domain-model-layer
**Date**: 2026-01-16

## Prerequisites

- MoLe プロジェクトがビルド可能な状態であること
- Kotlin 2.0.21 + Coroutines 1.9.0 環境
- Hilt DI が設定済み

## Phase 1: Transaction ドメインモデル（P1）

### Step 1: ドメインモデル作成

**ファイル作成**: `app/src/main/kotlin/net/ktnx/mobileledger/domain/model/Transaction.kt`

```kotlin
package net.ktnx.mobileledger.domain.model

import net.ktnx.mobileledger.utils.SimpleDate

data class Transaction(
    val id: Long? = null,
    val ledgerId: Long = 0,
    val date: SimpleDate,
    val description: String,
    val comment: String? = null,
    val lines: List<TransactionLine> = emptyList()
) {
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()
        if (description.isBlank()) errors.add("説明は必須です")
        if (lines.isEmpty()) errors.add("少なくとも1つの取引行が必要です")
        return if (errors.isEmpty()) ValidationResult.Success else ValidationResult.Error(errors)
    }
}

data class TransactionLine(
    val id: Long? = null,
    val accountName: String,
    val amount: Float? = null,
    val currency: String = "",
    val comment: String? = null
)
```

### Step 2: ValidationResult 作成

**ファイル作成**: `app/src/main/kotlin/net/ktnx/mobileledger/domain/model/ValidationResult.kt`

```kotlin
package net.ktnx.mobileledger.domain.model

sealed class ValidationResult {
    data object Success : ValidationResult()
    data class Error(val reasons: List<String>) : ValidationResult() {
        constructor(reason: String) : this(listOf(reason))
    }
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
}
```

### Step 3: Mapper 作成

**ファイル作成**: `app/src/main/kotlin/net/ktnx/mobileledger/data/repository/mapper/TransactionMapper.kt`

```kotlin
package net.ktnx.mobileledger.data.repository.mapper

import net.ktnx.mobileledger.db.TransactionAccount
import net.ktnx.mobileledger.db.TransactionWithAccounts
import net.ktnx.mobileledger.domain.model.Transaction
import net.ktnx.mobileledger.domain.model.TransactionLine
import net.ktnx.mobileledger.utils.SimpleDate

object TransactionMapper {

    fun TransactionWithAccounts.toDomain(): Transaction = Transaction(
        id = transaction.id,
        ledgerId = transaction.ledgerId,
        date = SimpleDate(transaction.year, transaction.month, transaction.day),
        description = transaction.description,
        comment = transaction.comment,
        lines = accounts?.map { it.toDomain() } ?: emptyList()
    )

    fun TransactionAccount.toDomain(): TransactionLine = TransactionLine(
        id = id,
        accountName = accountName,
        amount = amount,
        currency = currency ?: "",
        comment = comment
    )

    // toEntity は Phase 2 で実装
}
```

### Step 4: Repository インターフェース更新

**ファイル変更**: `app/src/main/kotlin/net/ktnx/mobileledger/data/repository/TransactionRepository.kt`

```kotlin
// Import を追加
import net.ktnx.mobileledger.domain.model.Transaction

// メソッドの戻り値を変更
interface TransactionRepository {
    // Before: fun getAllTransactions(profileId: Long): Flow<List<TransactionWithAccounts>>
    fun getAllTransactions(profileId: Long): Flow<List<Transaction>>

    // 他のメソッドも同様に更新
}
```

### Step 5: Repository 実装更新

**ファイル変更**: `app/src/main/kotlin/net/ktnx/mobileledger/data/repository/TransactionRepositoryImpl.kt`

```kotlin
import net.ktnx.mobileledger.data.repository.mapper.TransactionMapper.toDomain

override fun getAllTransactions(profileId: Long): Flow<List<Transaction>> =
    transactionDAO.getAllWithAccounts(profileId)
        .asFlow()
        .map { entities -> entities.map { it.toDomain() } }
```

### Step 6: ViewModel 更新

**ファイル変更**: `app/src/main/kotlin/net/ktnx/mobileledger/ui/main/TransactionListViewModel.kt`

```kotlin
// Before
import net.ktnx.mobileledger.db.TransactionWithAccounts

// After
import net.ktnx.mobileledger.domain.model.Transaction
```

### Step 7: テスト作成

**ファイル作成**: `app/src/test/kotlin/net/ktnx/mobileledger/domain/model/TransactionTest.kt`

```kotlin
package net.ktnx.mobileledger.domain.model

import org.junit.Assert.*
import org.junit.Test
import net.ktnx.mobileledger.utils.SimpleDate

class TransactionTest {

    @Test
    fun `validate returns error when description is blank`() {
        val transaction = Transaction(
            date = SimpleDate(2026, 1, 16),
            description = "",
            lines = listOf(TransactionLine(accountName = "Assets:Cash"))
        )

        val result = transaction.validate()

        assertTrue(result is ValidationResult.Error)
        assertTrue((result as ValidationResult.Error).reasons.contains("説明は必須です"))
    }

    @Test
    fun `validate returns success for valid transaction`() {
        val transaction = Transaction(
            date = SimpleDate(2026, 1, 16),
            description = "Test",
            lines = listOf(
                TransactionLine(accountName = "Assets:Cash", amount = 100f),
                TransactionLine(accountName = "Expenses:Food", amount = -100f)
            )
        )

        val result = transaction.validate()

        assertTrue(result is ValidationResult.Success)
    }
}
```

## Verification

```bash
# テスト実行
nix run .#test

# ビルド確認
nix run .#build

# フルワークフロー
nix run .#verify
```

## Common Issues

### Issue 1: Import 競合

**問題**: `Profile` が `db.Profile` と `domain.model.Profile` で競合

**解決**: 型エイリアスまたは完全修飾名を使用

```kotlin
import net.ktnx.mobileledger.domain.model.Profile as DomainProfile
import net.ktnx.mobileledger.db.Profile as DbProfile
```

### Issue 2: Flow 変換

**問題**: LiveData から Flow への変換時にエラー

**解決**: `asFlow()` 拡張関数を使用

```kotlin
import androidx.lifecycle.asFlow
```

### Issue 3: Mapper が見つからない

**問題**: `toDomain()` がコンパイルエラー

**解決**: Mapper の import を確認

```kotlin
import net.ktnx.mobileledger.data.repository.mapper.TransactionMapper.toDomain
```

## Next Steps

1. P1 完了後、P2 (Profile) に進む
2. 各フェーズで `@Deprecated` アノテーションを追加
3. 全フェーズ完了後、既存 `model` パッケージのクラスを削除

## File Checklist

### Phase 1 (Transaction)

- [ ] `domain/model/Transaction.kt`
- [ ] `domain/model/TransactionLine.kt`
- [ ] `domain/model/ValidationResult.kt`
- [ ] `data/repository/mapper/TransactionMapper.kt`
- [ ] `data/repository/TransactionRepository.kt` (更新)
- [ ] `data/repository/TransactionRepositoryImpl.kt` (更新)
- [ ] `ui/main/TransactionListViewModel.kt` (更新)
- [ ] `test/domain/model/TransactionTest.kt`
- [ ] `test/data/repository/mapper/TransactionMapperTest.kt`

### Phase 2 (Profile)

- [ ] `domain/model/Profile.kt`
- [ ] `domain/model/ProfileAuthentication.kt`
- [ ] `domain/model/ServerVersion.kt`
- [ ] `data/repository/mapper/ProfileMapper.kt`
- [ ] `data/repository/ProfileRepository.kt` (更新)
- [ ] `data/repository/ProfileRepositoryImpl.kt` (更新)
- [ ] UI ファイル群 (更新)
