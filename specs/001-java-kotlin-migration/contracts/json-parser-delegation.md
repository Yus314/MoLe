# JSON Parser Delegation Contract

**Branch**: `001-java-kotlin-migration` | **Date**: 2026-01-05
**Purpose**: JSON パーサー重複コード抽出の詳細設計

## Overview

7 バージョン (v1_14〜v1_50) の JSON パーサーから共通コードを委譲パターンで抽出する。

---

## Current State Analysis

### 重複率

| クラス | 7 バージョン合計行数 | 重複行数 | 重複率 |
|--------|---------------------|----------|--------|
| ParsedPosting | ~1,050 | 800+ | 76% |
| ParsedLedgerTransaction | ~1,200 | 1,000+ | 83% |
| ParsedLedgerAccount | ~420 | 200+ | 48% |
| Gateway | ~280 | 210+ | 75% |

### Version Groupings

```
Group A: v1_14, v1_15, v1_19_1, v1_23
  - ptransaction_: Int
  - asLedgerAccount(): Simple version
  - ParsedStyle: setAsdecimalpoint('.')

Group B: v1_32, v1_40, v1_50
  - ptransaction_: String
  - asLedgerAccount(): Enhanced with AmountStyle
  - ParsedStyle: setAsdecimalmark(".") + setAsrounding()

Special: v1_50
  - ParsedLedgerAccount: New adata structure
  - Additional: ParsedAccountData, ParsedBalanceData
```

---

## Delegation Architecture

### 1. Common Field Container

すべてのバージョンで共通のフィールドを持つコンテナ。

```kotlin
// net.ktnx.mobileledger.json.common

/**
 * 共通フィールドコンテナ (ParsedPosting 用)
 */
class PostingFieldDelegate {
    var pbalanceassertion: Void? = null
    var pstatus: String = "Unmarked"
    var paccount: String = ""
    var pamount: List<ParsedAmount> = emptyList()
    var pdate: String? = null
    var pdate2: String? = null
    var ptype: String = "RegularPosting"
    var pcomment: String = ""
    var ptags: List<List<String>> = emptyList()
    var poriginal: String? = null
}

/**
 * 共通フィールドコンテナ (ParsedLedgerTransaction 用)
 */
class TransactionFieldDelegate {
    var tdate: String = ""
    var tdate2: String? = null
    var tdescription: String = ""
    var tcomment: String = ""
    var tprecedingcomment: String = ""
    var tstatus: String = "Unmarked"
    var tpostings: List<ParsedPosting> = emptyList()
    var tindex: Int = 0
    var ttags: List<List<String>> = emptyList()
}
```

### 2. Style Configurer Strategy

バージョン固有のスタイル設定を抽象化。

```kotlin
// net.ktnx.mobileledger.json.common

sealed interface StyleConfigurer {
    fun configureStyle(style: ParsedStyle, precision: Int)

    object V1_14Style : StyleConfigurer {
        override fun configureStyle(style: ParsedStyle, precision: Int) {
            style.asprecision = precision
            style.asdecimalpoint = '.'
        }
    }

    object V1_19_1Style : StyleConfigurer {
        override fun configureStyle(style: ParsedStyle, precision: Int) {
            style.asprecision = ParsedPrecision(precision)
            style.asdecimalpoint = '.'
        }
    }

    object V1_32Style : StyleConfigurer {
        override fun configureStyle(style: ParsedStyle, precision: Int) {
            style.asprecision = precision
            style.asdecimalmark = "."
            style.asrounding = "NoRounding"
        }
    }
}
```

### 3. Transaction Type Strategy

ptransaction_ の型の違いを抽象化。

```kotlin
// net.ktnx.mobileledger.json.common

sealed interface TransactionIdType {
    fun setOnPosting(posting: ParsedPosting, index: Int)
    fun getValue(posting: ParsedPosting): Any

    object IntType : TransactionIdType {
        override fun setOnPosting(posting: ParsedPosting, index: Int) {
            (posting as ParsedPostingIntId).ptransaction_ = index
        }
        override fun getValue(posting: ParsedPosting): Int =
            (posting as ParsedPostingIntId).ptransaction_
    }

    object StringType : TransactionIdType {
        override fun setOnPosting(posting: ParsedPosting, index: Int) {
            (posting as ParsedPostingStringId).ptransaction_ = index.toString()
        }
        override fun getValue(posting: ParsedPosting): String =
            (posting as ParsedPostingStringId).ptransaction_
    }
}
```

### 4. Account Balance Extractor

v1_50 の構造変更に対応。

```kotlin
// net.ktnx.mobileledger.json.common

interface BalanceExtractor {
    fun extractBalances(account: ParsedLedgerAccount): List<ParsedBalance>

    object LegacyExtractor : BalanceExtractor {
        // v1_14 ~ v1_40
        override fun extractBalances(account: ParsedLedgerAccount): List<ParsedBalance> =
            account.aibalance ?: emptyList()
    }

    object V1_50Extractor : BalanceExtractor {
        // v1_50: adata.getFirstPeriodBalance().getBdincludingsubs()
        override fun extractBalances(account: ParsedLedgerAccount): List<ParsedBalance> =
            account.adata?.getFirstPeriodBalance()?.bdincludingsubs ?: emptyList()
    }
}
```

---

## Refactored Class Hierarchy

### ParsedPosting

```kotlin
// Base interface
interface ParsedPostingBase {
    var paccount: String
    var pamount: List<ParsedAmount>
    var pcomment: String
    // ... other common fields
}

// Common implementation via delegation
abstract class AbstractParsedPosting(
    private val fields: PostingFieldDelegate = PostingFieldDelegate()
) : ParsedPostingBase by fields {

    abstract val styleConfigurer: StyleConfigurer
    abstract val transactionIdType: TransactionIdType

    // Common implementation
    fun fromLedgerAccountCommon(acc: LedgerTransactionAccount): AbstractParsedPosting {
        paccount = acc.accountName
        pcomment = acc.comment ?: ""

        val amounts = mutableListOf<ParsedAmount>()
        val amt = ParsedAmount().apply {
            acommodity = acc.currency ?: ""
            aismultiplier = false
            aquantity = ParsedQuantity().apply {
                decimalPlaces = 2
                decimalMantissa = (acc.amount * 100).roundToLong()
            }
            astyle = ParsedStyle().apply {
                ascommodityside = getCommoditySide()
                ascommodityspaced = getCommoditySpaced()
                styleConfigurer.configureStyle(this, 2)
            }
        }
        amounts.add(amt)
        pamount = amounts

        return this
    }

    companion object {
        @JvmStatic fun getCommoditySide(): String = "R"
        @JvmStatic fun getCommoditySpaced(): Boolean = true
    }
}

// v1_14 implementation
class ParsedPosting_v1_14 : AbstractParsedPosting() {
    var ptransaction_: Int = 0

    override val styleConfigurer = StyleConfigurer.V1_14Style
    override val transactionIdType = TransactionIdType.IntType
}

// v1_32 implementation
class ParsedPosting_v1_32 : AbstractParsedPosting() {
    var ptransaction_: String = "1"

    override val styleConfigurer = StyleConfigurer.V1_32Style
    override val transactionIdType = TransactionIdType.StringType

    // Enhanced asLedgerAccount with AmountStyle
    override fun asLedgerAccount(): LedgerTransactionAccount {
        val amt = pamount.firstOrNull() ?: return LedgerTransactionAccount(paccount, 0f, "", pcomment, null)
        val style = amt.astyle?.let { AmountStyle.fromParsedStyle(it, amt.acommodity) }
        return LedgerTransactionAccount(
            paccount,
            amt.aquantity.asFloat(),
            amt.acommodity,
            pcomment,
            style
        )
    }
}
```

### ParsedLedgerTransaction

```kotlin
abstract class AbstractParsedLedgerTransaction(
    private val fields: TransactionFieldDelegate = TransactionFieldDelegate()
) {
    // Delegate common fields
    var tdate by fields::tdate
    var tdescription by fields::tdescription
    var tcomment by fields::tcomment
    var tpostings by fields::tpostings
    var tindex by fields::tindex

    abstract val transactionIdType: TransactionIdType
    abstract val includesMarkDataAsLoaded: Boolean

    // Common implementation - identical across all versions
    fun asLedgerTransactionCommon(): LedgerTransaction {
        val date = Globals.parseIsoDate(tdate)
        val tr = LedgerTransaction(tindex, date, tdescription).apply {
            comment = Misc.trim(Misc.emptyIsNull(tcomment))
        }
        tpostings.forEach { p ->
            tr.addAccount(p.asLedgerAccount())
        }
        if (includesMarkDataAsLoaded) {
            tr.markDataAsLoaded()
        }
        return tr
    }

    // Common static factory - identical across all versions
    companion object {
        @JvmStatic
        fun <T : AbstractParsedLedgerTransaction> fromLedgerTransactionCommon(
            tr: LedgerTransaction,
            factory: () -> T
        ): T = factory().apply {
            tcomment = Misc.nullIsEmpty(tr.comment)
            tprecedingcomment = ""

            val postings = tr.accounts
                .filter { it.accountName.isNotEmpty() }
                .map { ParsedPosting.fromLedgerAccount(it) }

            tpostings = postings.toList()

            val transactionDate = tr.dateIfAny ?: SimpleDate.today()
            tdate = Globals.formatIsoDate(transactionDate)
            tdate2 = null
            tindex = 1
            tdescription = tr.description
        }
    }
}
```

### Gateway

```kotlin
abstract class Gateway {
    abstract val apiVersion: API
    abstract fun createPosting(): ParsedPosting
    abstract fun createTransaction(): ParsedLedgerTransaction
    abstract fun createAccountListParser(input: InputStream): AccountListParser
    abstract fun createTransactionListParser(input: InputStream): TransactionListParser

    // Common implementation - identical across all versions
    open fun transactionSaveRequest(ledgerTransaction: LedgerTransaction): String {
        val jsonTransaction = createTransaction().apply {
            AbstractParsedLedgerTransaction.fromLedgerTransactionCommon(
                ledgerTransaction,
                ::createTransaction
            )
        }
        return objectMapper.writeValueAsString(jsonTransaction)
    }

    companion object {
        private val objectMapper = ObjectMapper().apply {
            registerModule(KotlinModule.Builder().build())
        }

        @JvmStatic
        fun forVersion(version: API): Gateway = when (version) {
            API.v1_14 -> Gateway_v1_14()
            API.v1_15 -> Gateway_v1_15()
            API.v1_19_1 -> Gateway_v1_19_1()
            API.v1_23 -> Gateway_v1_23()
            API.v1_32 -> Gateway_v1_32()
            API.v1_40 -> Gateway_v1_40()
            API.v1_50 -> Gateway_v1_50()
            else -> throw IllegalArgumentException("Unknown API version: $version")
        }
    }
}
```

---

## File Organization

### Before (Current)

```
json/
├── API.java
├── AccountListParser.java
├── Gateway.java
├── TransactionListParser.java
├── v1_14/
│   ├── Gateway.java (Gateway_v1_14)
│   ├── ParsedLedgerAccount.java
│   ├── ParsedLedgerTransaction.java
│   ├── ParsedPosting.java
│   └── ... (12 files total)
├── v1_15/ (12 files)
├── v1_19_1/ (13 files)
├── v1_23/ (12 files)
├── v1_32/ (13 files)
├── v1_40/ (13 files)
└── v1_50/ (15 files)
```

### After (Refactored)

```
json/
├── API.kt
├── common/
│   ├── PostingFieldDelegate.kt
│   ├── TransactionFieldDelegate.kt
│   ├── StyleConfigurer.kt
│   ├── TransactionIdType.kt
│   ├── BalanceExtractor.kt
│   ├── AbstractParsedPosting.kt
│   ├── AbstractParsedLedgerTransaction.kt
│   ├── AbstractParsedLedgerAccount.kt
│   └── ObjectMapperProvider.kt
├── parsers/
│   ├── AccountListParser.kt (abstract)
│   └── TransactionListParser.kt (abstract)
├── Gateway.kt (abstract with factory)
├── v1_14/
│   ├── Gateway_v1_14.kt
│   ├── ParsedLedgerAccount_v1_14.kt
│   ├── ParsedPosting_v1_14.kt
│   └── ParsedLedgerTransaction_v1_14.kt
│   └── ... (version-specific only)
├── ... (similar for v1_15 ~ v1_50)
└── v1_50/
    ├── Gateway_v1_50.kt
    ├── ParsedAccountData.kt (v1_50 specific)
    ├── ParsedBalanceData.kt (v1_50 specific)
    └── ... (version-specific)
```

---

## Expected Code Reduction

| Area | Before | After | Reduction |
|------|--------|-------|-----------|
| Getter/Setter boilerplate | ~800 lines | ~0 lines | 100% |
| fromLedgerAccount() | ~300 lines | ~50 lines | 83% |
| asLedgerTransaction() | ~100 lines | ~20 lines | 80% |
| Gateway.transactionSaveRequest() | ~210 lines | ~30 lines | 86% |
| **Total** | ~3,000 lines | ~1,800 lines | **40%** |

---

## Migration Steps

### Step 1: Create Common Package
- `PostingFieldDelegate.kt`
- `TransactionFieldDelegate.kt`

### Step 2: Extract Strategies
- `StyleConfigurer.kt`
- `TransactionIdType.kt`
- `BalanceExtractor.kt`

### Step 3: Create Abstract Classes
- `AbstractParsedPosting.kt`
- `AbstractParsedLedgerTransaction.kt`

### Step 4: Migrate Version by Version
- v1_14 first (simplest)
- Verify tests pass
- Proceed to v1_15, etc.

### Step 5: Cleanup
- Remove duplicated code
- Consolidate common imports
- Update documentation

---

## Testing Strategy

各 Step で以下を確認:

1. **Unit Test**: `ParsedQuantityTest` が pass
2. **Parser Test**: `LegacyParserTest` が pass
3. **Integration**: 実際の hledger-web API からのパース成功
4. **Round-trip**: `fromLedgerTransaction` → `asLedgerTransaction` の往復

```kotlin
@Test
fun testTransactionRoundTrip() {
    val original = LedgerTransaction(...)
    val parsed = ParsedLedgerTransaction.fromLedgerTransaction(original)
    val restored = parsed.asLedgerTransaction()

    assertEquals(original.description, restored.description)
    assertEquals(original.accounts.size, restored.accounts.size)
    // ...
}
```
