# Data Model: Domain Model Layer

**Feature**: 017-domain-model-layer
**Date**: 2026-01-16

## Overview

このドキュメントでは、ドメインモデル層で定義するエンティティの設計を記述する。

## Domain Models

### 1. Transaction（取引）

**Package**: `net.ktnx.mobileledger.domain.model`

```kotlin
/**
 * 取引のドメインモデル
 *
 * データベースの内部構造（年・月・日の分離、dataHash等）を隠蔽し、
 * 画面開発者がビジネス概念のみを扱えるようにする。
 */
data class Transaction(
    /** データベースID。新規取引の場合はnull */
    val id: Long? = null,

    /** サーバー上の取引ID（ledger ID） */
    val ledgerId: Long = 0,

    /** 取引日 */
    val date: SimpleDate,

    /** 取引の説明（摘要） */
    val description: String,

    /** 取引のコメント（オプション） */
    val comment: String? = null,

    /** 取引行のリスト */
    val lines: List<TransactionLine> = emptyList()
) {
    /**
     * 取引の金額バランスを検証する
     *
     * @return バリデーション結果
     */
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()

        if (description.isBlank()) {
            errors.add("説明は必須です")
        }

        if (lines.isEmpty()) {
            errors.add("少なくとも1つの取引行が必要です")
        }

        // 通貨ごとの金額バランスをチェック
        val balanceByCurrency = lines
            .filter { it.amount != null }
            .groupBy { it.currency }
            .mapValues { (_, lines) -> lines.sumOf { it.amount?.toDouble() ?: 0.0 } }

        for ((currency, balance) in balanceByCurrency) {
            if (kotlin.math.abs(balance) > 0.0001) {
                val currencyLabel = currency.ifEmpty { "デフォルト通貨" }
                errors.add("$currencyLabel の金額が不均衡です（差額: $balance）")
            }
        }

        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }

    /**
     * 指定した勘定科目名を含むかどうか
     */
    fun hasAccountNamed(name: String): Boolean {
        val upperName = name.uppercase()
        return lines.any { it.accountName.uppercase().contains(upperName) }
    }

    /**
     * 取引行を追加した新しいTransactionを返す
     */
    fun withLine(line: TransactionLine): Transaction =
        copy(lines = lines + line)

    /**
     * 取引行を更新した新しいTransactionを返す
     */
    fun withUpdatedLine(index: Int, line: TransactionLine): Transaction =
        copy(lines = lines.toMutableList().apply { set(index, line) })
}
```

### 2. TransactionLine（取引行）

```kotlin
/**
 * 取引行のドメインモデル
 *
 * 取引内の1行（勘定科目、金額、通貨、コメント）を表す。
 * データベースのorderNoや外部キーを隠蔽する。
 */
data class TransactionLine(
    /** データベースID。新規行の場合はnull */
    val id: Long? = null,

    /** 勘定科目名 */
    val accountName: String,

    /** 金額。自動計算行の場合はnull */
    val amount: Float? = null,

    /** 通貨。空文字はデフォルト通貨 */
    val currency: String = "",

    /** 行のコメント（オプション） */
    val comment: String? = null
) {
    /**
     * 金額が設定されているかどうか
     */
    val hasAmount: Boolean get() = amount != null

    /**
     * 金額を設定した新しいTransactionLineを返す
     */
    fun withAmount(amount: Float): TransactionLine = copy(amount = amount)

    /**
     * 金額をクリアした新しいTransactionLineを返す
     */
    fun withoutAmount(): TransactionLine = copy(amount = null)
}
```

### 3. Profile（プロファイル）

```kotlin
/**
 * プロファイルのドメインモデル
 *
 * サーバー接続設定のビジネス表現。
 * データベースの検出バージョンフラグ等の実装詳細を隠蔽する。
 */
data class Profile(
    /** データベースID。新規プロファイルの場合はnull */
    val id: Long? = null,

    /** プロファイル名 */
    val name: String,

    /** UUID（同期用識別子） */
    val uuid: String,

    /** サーバーURL */
    val url: String,

    /** 認証設定 */
    val authentication: ProfileAuthentication? = null,

    /** 表示順序 */
    val orderNo: Int = 0,

    /** 投稿許可 */
    val permitPosting: Boolean = false,

    /** テーマID（-1はデフォルト） */
    val theme: Int = -1,

    /** 推奨アカウントフィルタ */
    val preferredAccountsFilter: String? = null,

    /** 将来日付設定 */
    val futureDates: FutureDates = FutureDates.None,

    /** APIバージョン */
    val apiVersion: Int = 0,

    /** デフォルトで通貨を表示 */
    val showCommodityByDefault: Boolean = false,

    /** デフォルト通貨 */
    val defaultCommodity: String? = null,

    /** デフォルトでコメントを表示 */
    val showCommentsByDefault: Boolean = true,

    /** 検出されたサーバーバージョン */
    val serverVersion: ServerVersion? = null
) {
    /**
     * 認証が有効かどうか
     */
    val isAuthEnabled: Boolean get() = authentication != null

    /**
     * 投稿可能かどうか
     */
    val canPost: Boolean get() = permitPosting

    /**
     * デフォルト通貨（空文字の場合は空文字を返す）
     */
    val defaultCommodityOrEmpty: String get() = defaultCommodity ?: ""
}

/**
 * プロファイルの認証設定
 */
data class ProfileAuthentication(
    val user: String,
    val password: String
)

/**
 * サーバーバージョン情報
 */
data class ServerVersion(
    val major: Int,
    val minor: Int,
    val isPre_1_19: Boolean = false
) {
    val displayString: String get() = "$major.$minor"
}
```

### 4. Account（勘定科目）

```kotlin
/**
 * 勘定科目のドメインモデル
 *
 * 勘定科目と残高のビジネス表現。
 * データベースのテーブル結合やリレーション構造を隠蔽する。
 */
data class Account(
    /** データベースID。新規勘定科目の場合はnull */
    val id: Long? = null,

    /** 勘定科目名（フルパス） */
    val name: String,

    /** 階層レベル（0-based） */
    val level: Int = 0,

    /** 展開状態（UI用） */
    val isExpanded: Boolean = false,

    /** 可視状態（UI用） */
    val isVisible: Boolean = true,

    /** 勘定科目ごとの残高リスト */
    val amounts: List<AccountAmount> = emptyList()
) {
    /**
     * 親勘定科目名を取得
     */
    val parentName: String?
        get() = name.lastIndexOf(':').let { idx ->
            if (idx > 0) name.substring(0, idx) else null
        }

    /**
     * 短い名前（最後のセグメント）を取得
     */
    val shortName: String
        get() = name.substringAfterLast(':')

    /**
     * 残高を持っているかどうか
     */
    val hasAmounts: Boolean get() = amounts.isNotEmpty()
}

/**
 * 勘定科目の残高
 */
data class AccountAmount(
    /** 通貨 */
    val currency: String,

    /** 金額 */
    val amount: Float
)
```

### 5. Template（テンプレート）

```kotlin
/**
 * テンプレートのドメインモデル
 *
 * 取引テンプレートのビジネス表現。
 * データベースのヘッダー/行分離を隠蔽する。
 */
data class Template(
    /** データベースID。新規テンプレートの場合はnull */
    val id: Long? = null,

    /** テンプレート名 */
    val name: String,

    /** マッチングパターン（正規表現） */
    val pattern: String? = null,

    /** テスト用テキスト */
    val testText: String? = null,

    /** 取引説明のテンプレート */
    val transactionDescription: String? = null,

    /** 取引コメントのテンプレート */
    val transactionComment: String? = null,

    /** 日付の年グループ番号 */
    val dateYearGroup: Int? = null,

    /** 日付の月グループ番号 */
    val dateMonthGroup: Int? = null,

    /** 日付の日グループ番号 */
    val dateDayGroup: Int? = null,

    /** テンプレート行のリスト */
    val lines: List<TemplateLine> = emptyList(),

    /** フォールバックフラグ */
    val isFallback: Boolean = false
)

/**
 * テンプレート行
 */
data class TemplateLine(
    /** データベースID */
    val id: Long? = null,

    /** 勘定科目名（リテラルまたはグループ参照） */
    val accountName: String? = null,

    /** 勘定科目名グループ番号 */
    val accountNameGroup: Int? = null,

    /** 金額（リテラルまたはグループ参照） */
    val amount: Float? = null,

    /** 金額グループ番号 */
    val amountGroup: Int? = null,

    /** 通貨 */
    val currency: String? = null,

    /** 通貨グループ番号 */
    val currencyGroup: Int? = null,

    /** コメント */
    val comment: String? = null,

    /** コメントグループ番号 */
    val commentGroup: Int? = null,

    /** 金額の符号を反転 */
    val negateAmount: Boolean = false
)
```

### 6. Currency（通貨）

```kotlin
/**
 * 通貨のドメインモデル
 *
 * 通貨設定のビジネス表現。
 */
data class Currency(
    /** データベースID。新規通貨の場合はnull */
    val id: Long? = null,

    /** 通貨名/シンボル */
    val name: String,

    /** 表示位置 */
    val position: CurrencyPosition = CurrencyPosition.AFTER,

    /** 金額との間にスペースを入れる */
    val hasGap: Boolean = true,

    /** 小数桁数 */
    val decimalPoints: Int = 2
)

/**
 * 通貨の表示位置
 */
enum class CurrencyPosition {
    BEFORE,  // $100
    AFTER    // 100円
}
```

### 7. ValidationResult（バリデーション結果）

```kotlin
/**
 * バリデーション結果を型安全に表現するsealed class
 */
sealed class ValidationResult {
    /**
     * バリデーション成功
     */
    data object Success : ValidationResult()

    /**
     * バリデーション失敗
     */
    data class Error(
        val reasons: List<String>
    ) : ValidationResult() {
        constructor(reason: String) : this(listOf(reason))
    }

    /**
     * 成功かどうか
     */
    val isSuccess: Boolean get() = this is Success

    /**
     * 失敗かどうか
     */
    val isError: Boolean get() = this is Error
}
```

## Relationships

```text
┌────────────────────────────────────────────────────────────────┐
│                        Domain Models                            │
├────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────────┐     1:N     ┌─────────────────────┐           │
│  │ Transaction │────────────►│  TransactionLine    │           │
│  └─────────────┘             └─────────────────────┘           │
│         │                                                        │
│         │ N:1                                                    │
│         ▼                                                        │
│  ┌─────────────┐                                                │
│  │   Profile   │◄──────────────────────────────────┐           │
│  └─────────────┘                                   │            │
│         │                                           │            │
│         │ 1:N                                       │            │
│         ▼                                           │            │
│  ┌─────────────┐     1:N     ┌─────────────────┐  │            │
│  │   Account   │────────────►│  AccountAmount  │  │            │
│  └─────────────┘             └─────────────────┘  │            │
│                                                    │            │
│  ┌─────────────┐     1:N     ┌─────────────────┐  │            │
│  │  Template   │────────────►│  TemplateLine   │──┘           │
│  └─────────────┘             └─────────────────┘               │
│                                                                  │
│  ┌─────────────┐                                                │
│  │  Currency   │─────────────────────────────────────►Profile  │
│  └─────────────┘                                                │
│                                                                  │
└────────────────────────────────────────────────────────────────┘
```

## State Transitions

### Transaction State

```text
┌──────────┐       ┌──────────┐       ┌──────────┐
│  Draft   │──────►│ Validated│──────►│  Saved   │
│ (id=null)│       │(id=null) │       │(id!=null)│
└──────────┘       └──────────┘       └──────────┘
     │                  │                   │
     │  validate()      │  save()           │
     └──────────────────┴───────────────────┘
                  │
                  ▼ (on failure)
            ┌──────────┐
            │  Error   │
            │(reasons) │
            └──────────┘
```

### Profile State

```text
┌──────────┐       ┌──────────┐       ┌──────────┐
│   New    │──────►│  Saved   │──────►│ Selected │
│(id=null) │       │(id!=null)│       │(current) │
└──────────┘       └──────────┘       └──────────┘
```

## Validation Rules

### Transaction

| Field | Rule | Error Message |
|-------|------|---------------|
| description | Required, non-blank | "説明は必須です" |
| lines | At least 1 line | "少なくとも1つの取引行が必要です" |
| lines | Balance by currency | "{currency} の金額が不均衡です" |

### TransactionLine

| Field | Rule | Error Message |
|-------|------|---------------|
| accountName | Required, non-blank | "勘定科目名は必須です" |

### Profile

| Field | Rule | Error Message |
|-------|------|---------------|
| name | Required, non-blank | "プロファイル名は必須です" |
| url | Required, valid URL | "有効なURLを入力してください" |
| authentication.user | Required if auth enabled | "ユーザー名は必須です" |
| authentication.password | Required if auth enabled | "パスワードは必須です" |

## Comparison: Domain Model vs DB Entity

### Transaction

| Domain Model Property | DB Entity Property | Notes |
|-----------------------|-------------------|-------|
| id | id | Same |
| ledgerId | ledgerId | Same |
| date: SimpleDate | year, month, day | Combined |
| description | description | Same |
| comment | comment | Same |
| lines: List | @Relation accounts | Nested |
| - | profileId | Hidden |
| - | dataHash | Hidden |
| - | descriptionUpper | Hidden |
| - | generation | Hidden |

### Profile

| Domain Model Property | DB Entity Property | Notes |
|-----------------------|-------------------|-------|
| id | id | Same |
| name | name | Same |
| uuid | uuid | Same |
| url | url | Same |
| authentication | useAuthentication, authUser, authPassword | Combined |
| orderNo | orderNo | Same |
| permitPosting | permitPosting | Same |
| theme | theme | Same |
| preferredAccountsFilter | preferredAccountsFilter | Same |
| futureDates: FutureDates | futureDates: Int | Type changed |
| apiVersion | apiVersion | Same |
| showCommodityByDefault | showCommodityByDefault | Same |
| defaultCommodity | defaultCommodity | Same |
| showCommentsByDefault | showCommentsByDefault | Same |
| serverVersion | detectedVersionPre_1_19, detectedVersionMajor, detectedVersionMinor | Combined |

## Package Structure

```text
net.ktnx.mobileledger.domain.model/
├── Transaction.kt
├── TransactionLine.kt
├── Profile.kt
├── ProfileAuthentication.kt
├── ServerVersion.kt
├── Account.kt
├── AccountAmount.kt
├── Template.kt
├── TemplateLine.kt
├── Currency.kt
├── CurrencyPosition.kt
└── ValidationResult.kt
```

## Notes

- すべてのドメインモデルは `data class` + `val` プロパティで不変
- ID は `Long?` 型で、新規エンティティは `null`
- ビジネスロジック（バリデーション等）はドメインモデルのメソッドとして実装
- UI専用の状態（展開状態等）はドメインモデルに含めるが、永続化はしない
- 既存の `model` パッケージのクラスとは別に新規作成し、段階的に移行
