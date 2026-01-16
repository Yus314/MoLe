# Mapper Contracts

**Feature**: 017-domain-model-layer
**Date**: 2026-01-16

## Overview

Mapperは Repository 内で使用され、データベースエンティティとドメインモデル間の変換を担当する。

## 1. TransactionMapper

**Location**: `data/repository/mapper/TransactionMapper.kt`

### Contract

```kotlin
/**
 * Transaction ドメインモデルとデータベースエンティティ間の変換を担当
 */
object TransactionMapper {

    /**
     * データベースエンティティからドメインモデルへ変換
     *
     * @receiver TransactionWithAccounts (Room Relation)
     * @return Transaction ドメインモデル
     *
     * Mapping rules:
     * - id: transaction.id
     * - ledgerId: transaction.ledgerId
     * - date: SimpleDate(transaction.year, transaction.month, transaction.day)
     * - description: transaction.description
     * - comment: transaction.comment
     * - lines: accounts.map { it.toDomain() }
     *
     * Hidden fields (not exposed to domain):
     * - profileId
     * - dataHash
     * - descriptionUpper
     * - generation
     */
    fun TransactionWithAccounts.toDomain(): Transaction

    /**
     * ドメインモデルからデータベースエンティティへ変換
     *
     * @receiver Transaction ドメインモデル
     * @param profileId 所属プロファイルID
     * @return TransactionWithAccounts (Room Relation)
     *
     * Mapping rules:
     * - transaction.id: this.id ?: 0
     * - transaction.ledgerId: this.ledgerId
     * - transaction.profileId: profileId (parameter)
     * - transaction.year: this.date.year
     * - transaction.month: this.date.month
     * - transaction.day: this.date.day
     * - transaction.description: this.description
     * - transaction.comment: this.comment
     * - transaction.dataHash: calculated from content
     * - accounts: this.lines.mapIndexed { index, line -> line.toEntity(index + 1) }
     */
    fun Transaction.toEntity(profileId: Long): TransactionWithAccounts

    /**
     * TransactionAccount のドメインモデルへの変換
     */
    fun TransactionAccount.toDomain(): TransactionLine

    /**
     * TransactionLine のエンティティへの変換
     */
    fun TransactionLine.toEntity(orderNo: Int): TransactionAccount
}
```

### Test Cases

| Test | Input | Expected Output |
|------|-------|-----------------|
| toDomain_basicTransaction | TransactionWithAccounts with 2 accounts | Transaction with 2 lines |
| toDomain_nullComment | TransactionWithAccounts with null comment | Transaction with null comment |
| toDomain_emptyAccounts | TransactionWithAccounts with empty accounts | Transaction with empty lines |
| toEntity_newTransaction | Transaction with null id | TransactionWithAccounts with id=0 |
| toEntity_existingTransaction | Transaction with id=123 | TransactionWithAccounts with id=123 |
| roundTrip_preservesData | Transaction → toEntity → toDomain | Original Transaction |

---

## 2. ProfileMapper

**Location**: `data/repository/mapper/ProfileMapper.kt`

### Contract

```kotlin
/**
 * Profile ドメインモデルとデータベースエンティティ間の変換を担当
 */
object ProfileMapper {

    /**
     * データベースエンティティからドメインモデルへ変換
     *
     * @receiver Profile (db.Profile Room Entity)
     * @return Profile (domain.model.Profile)
     *
     * Mapping rules:
     * - id: this.id
     * - name: this.name
     * - uuid: this.uuid
     * - url: this.url
     * - authentication: if (useAuthentication) ProfileAuthentication(authUser, authPassword) else null
     * - orderNo: this.orderNo
     * - permitPosting: this.permitPosting
     * - theme: this.theme
     * - preferredAccountsFilter: this.preferredAccountsFilter
     * - futureDates: FutureDates.fromInt(this.futureDates)
     * - apiVersion: this.apiVersion
     * - showCommodityByDefault: this.showCommodityByDefault
     * - defaultCommodity: this.defaultCommodity
     * - showCommentsByDefault: this.showCommentsByDefault
     * - serverVersion: ServerVersion(detectedVersionMajor, detectedVersionMinor, detectedVersionPre_1_19)
     */
    fun DbProfile.toDomain(): DomainProfile

    /**
     * ドメインモデルからデータベースエンティティへ変換
     *
     * @receiver Profile (domain.model.Profile)
     * @return Profile (db.Profile Room Entity)
     *
     * Mapping rules:
     * - id: this.id ?: 0
     * - name: this.name
     * - uuid: this.uuid
     * - url: this.url
     * - useAuthentication: this.authentication != null
     * - authUser: this.authentication?.user
     * - authPassword: this.authentication?.password
     * - orderNo: this.orderNo
     * - permitPosting: this.permitPosting
     * - theme: this.theme
     * - preferredAccountsFilter: this.preferredAccountsFilter
     * - futureDates: this.futureDates.toInt()
     * - apiVersion: this.apiVersion
     * - showCommodityByDefault: this.showCommodityByDefault
     * - defaultCommodity: this.defaultCommodity
     * - showCommentsByDefault: this.showCommentsByDefault
     * - detectedVersionPre_1_19: this.serverVersion?.isPre_1_19 ?: false
     * - detectedVersionMajor: this.serverVersion?.major ?: 0
     * - detectedVersionMinor: this.serverVersion?.minor ?: 0
     */
    fun DomainProfile.toEntity(): DbProfile
}
```

### Test Cases

| Test | Input | Expected Output |
|------|-------|-----------------|
| toDomain_withAuthentication | DbProfile with useAuthentication=true | DomainProfile with authentication != null |
| toDomain_withoutAuthentication | DbProfile with useAuthentication=false | DomainProfile with authentication = null |
| toDomain_serverVersion | DbProfile with version info | DomainProfile with serverVersion |
| toEntity_newProfile | DomainProfile with null id | DbProfile with id=0 |
| toEntity_authenticationFields | DomainProfile with authentication | DbProfile with useAuthentication=true |
| roundTrip_preservesData | Profile → toEntity → toDomain | Original Profile |

---

## 3. AccountMapper

**Location**: `data/repository/mapper/AccountMapper.kt`

### Contract

```kotlin
/**
 * Account ドメインモデルとデータベースエンティティ間の変換を担当
 */
object AccountMapper {

    /**
     * データベースエンティティからドメインモデルへ変換
     *
     * @receiver AccountWithAmounts (Room Relation)
     * @return Account ドメインモデル
     *
     * Mapping rules:
     * - id: account.id
     * - name: account.name
     * - level: account.level
     * - isExpanded: account.expanded
     * - isVisible: true (default, UI state)
     * - amounts: amounts.map { AccountAmount(it.currency, it.amount) }
     *
     * Hidden fields:
     * - profileId
     * - amountsExpanded
     * - expandedLevel
     * - lastUsed
     * - generation
     */
    fun AccountWithAmounts.toDomain(): Account

    /**
     * ドメインモデルからデータベースエンティティへ変換
     *
     * @receiver Account ドメインモデル
     * @param profileId 所属プロファイルID
     * @return AccountWithAmounts (Room Relation)
     */
    fun Account.toEntity(profileId: Long): AccountWithAmounts
}
```

### Test Cases

| Test | Input | Expected Output |
|------|-------|-----------------|
| toDomain_withAmounts | AccountWithAmounts with 2 amounts | Account with 2 amounts |
| toDomain_hierarchyLevel | AccountWithAmounts with level=2 | Account with level=2 |
| toDomain_expandedState | AccountWithAmounts with expanded=true | Account with isExpanded=true |
| toEntity_preservesId | Account with id | AccountWithAmounts with same id |

---

## 4. TemplateMapper

**Location**: `data/repository/mapper/TemplateMapper.kt`

### Contract

```kotlin
/**
 * Template ドメインモデルとデータベースエンティティ間の変換を担当
 */
object TemplateMapper {

    /**
     * データベースエンティティからドメインモデルへ変換
     *
     * @receiver TemplateWithAccounts (Room Relation)
     * @return Template ドメインモデル
     *
     * Mapping rules:
     * - id: header.id
     * - name: header.name
     * - pattern: header.pattern
     * - testText: header.testText
     * - transactionDescription: header.transactionDescription
     * - transactionComment: header.transactionComment
     * - dateYearGroup: header.dateYear
     * - dateMonthGroup: header.dateMonth
     * - dateDayGroup: header.dateDay
     * - lines: accounts.sortedBy { it.position }.map { it.toDomain() }
     * - isFallback: header.isFallback
     *
     * Hidden fields:
     * - profileId
     */
    fun TemplateWithAccounts.toDomain(): Template

    /**
     * ドメインモデルからデータベースエンティティへ変換
     */
    fun Template.toEntity(profileId: Long): TemplateWithAccounts

    /**
     * TemplateAccount のドメインモデルへの変換
     */
    fun TemplateAccount.toDomain(): TemplateLine

    /**
     * TemplateLine のエンティティへの変換
     */
    fun TemplateLine.toEntity(templateId: Long, position: Int): TemplateAccount
}
```

---

## 5. CurrencyMapper

**Location**: `data/repository/mapper/CurrencyMapper.kt`

### Contract

```kotlin
/**
 * Currency ドメインモデルとデータベースエンティティ間の変換を担当
 */
object CurrencyMapper {

    /**
     * データベースエンティティからドメインモデルへ変換
     *
     * @receiver Currency (db.Currency Room Entity)
     * @return Currency (domain.model.Currency)
     *
     * Mapping rules:
     * - id: this.id
     * - name: this.name
     * - position: CurrencyPosition.fromInt(this.position)
     * - hasGap: this.hasGap
     * - decimalPoints: this.decimalPoints
     *
     * Hidden fields:
     * - profileId
     */
    fun DbCurrency.toDomain(): DomainCurrency

    /**
     * ドメインモデルからデータベースエンティティへ変換
     */
    fun DomainCurrency.toEntity(profileId: Long): DbCurrency
}
```

---

## Repository Integration

### Example: TransactionRepository

```kotlin
interface TransactionRepository {
    // Before: Flow<List<TransactionWithAccounts>>
    // After:
    fun getAllTransactions(profileId: Long): Flow<List<Transaction>>
}

class TransactionRepositoryImpl @Inject constructor(
    private val transactionDAO: TransactionDAO
) : TransactionRepository {

    override fun getAllTransactions(profileId: Long): Flow<List<Transaction>> =
        transactionDAO.getAllWithAccounts(profileId)
            .asFlow()
            .map { entities ->
                entities.map { it.toDomain() }  // Mapper使用
            }

    override suspend fun storeTransaction(transaction: Transaction, profileId: Long) {
        val entity = transaction.toEntity(profileId)  // Mapper使用
        transactionDAO.insert(entity.transaction)
        // ...
    }
}
```

## Type Aliases (for clarity)

```kotlin
// To avoid confusion between db.Profile and domain.model.Profile
typealias DbProfile = net.ktnx.mobileledger.db.Profile
typealias DomainProfile = net.ktnx.mobileledger.domain.model.Profile

typealias DbCurrency = net.ktnx.mobileledger.db.Currency
typealias DomainCurrency = net.ktnx.mobileledger.domain.model.Currency
```

## Error Handling

Mapper変換中のエラーは以下のように処理する:

1. **Null handling**: `?:` でデフォルト値を設定
2. **Invalid enum**: デフォルト値にフォールバック
3. **Missing relations**: 空リストを返す

```kotlin
fun DbProfile.toDomain(): DomainProfile = DomainProfile(
    id = this.id,
    name = this.name,
    // Invalid futureDates value defaults to None
    futureDates = FutureDates.entries.getOrNull(this.futureDates) ?: FutureDates.None,
    // ...
)
```
