# Data Model: Java から Kotlin への移行

**Branch**: `001-java-kotlin-migration` | **Date**: 2026-01-05
**Purpose**: Phase 1 - エンティティ構造とドメインモデルの定義

## Database Entity Overview

MoLe は Room (SQLite) を使用し、9 つのエンティティで構成される。

```
Profile (1) ──────┬──────→ (N) Account ──────→ (N) AccountValue
                  ├──────→ (N) Transaction ──────→ (N) TransactionAccount
                  └──────→ (N) Option

TemplateHeader (1) ──────→ (N) TemplateAccount

Currency (1) ──────→ (N) TemplateAccount (optional)
```

---

## Entity Definitions

### 1. Profile (`profiles` table)

hledger-web サーバーへの接続プロファイル。

**Kotlin data class 変換:**
```kotlin
@Entity(
    tableName = "profiles",
    indices = [Index(value = ["uuid"], unique = true, name = "profiles_uuid_idx")]
)
data class Profile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val uuid: String,
    val url: String = "",
    @ColumnInfo(name = "use_authentication") val useAuthentication: Boolean = false,
    @ColumnInfo(name = "auth_user") val authUser: String? = null,
    @ColumnInfo(name = "auth_password") val authPassword: String? = null,
    @ColumnInfo(name = "order_no") val orderNo: Int? = null,
    @ColumnInfo(name = "permit_posting") val permitPosting: Boolean = false,
    val theme: Int = -1,
    @ColumnInfo(name = "preferred_accounts_filter") val preferredAccountsFilter: String? = null,
    @ColumnInfo(name = "future_dates") val futureDates: Int = 0,
    @ColumnInfo(name = "api_version") val apiVersion: Int = 0,
    @ColumnInfo(name = "show_commodity_by_default") val showCommodityByDefault: Boolean = false,
    @ColumnInfo(name = "default_commodity") val defaultCommodity: String? = null,
    @ColumnInfo(name = "show_comments_by_default") val showCommentsByDefault: Boolean = true,
    @ColumnInfo(name = "detected_version_pre_1_19") val detectedVersionPre1_19: Boolean = false,
    @ColumnInfo(name = "detected_version_major") val detectedVersionMajor: Int = 0,
    @ColumnInfo(name = "detected_version_minor") val detectedVersionMinor: Int = 0
)
```

**フィールド:**
| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| id | Long | No | Auto-generated PK |
| name | String | No | プロファイル名 |
| uuid | String | No | 一意識別子 |
| url | String | No | hledger サーバー URL |
| useAuthentication | Boolean | No | 認証有効化 |
| authUser | String | Yes | 認証ユーザー名 |
| authPassword | String | Yes | 認証パスワード |
| orderNo | Int | Yes | 表示順序 |
| permitPosting | Boolean | No | 取引投稿許可 |
| theme | Int | No | UI テーマ (-1 = デフォルト) |
| apiVersion | Int | No | hledger API バージョン |
| detectedVersionMajor/Minor | Int | No | 検出されたサーバーバージョン |

**関係:**
- Parent to: Account, Transaction, Option (CASCADE on delete)

---

### 2. Account (`accounts` table)

勘定科目。階層構造を持つ。

**Kotlin data class 変換:**
```kotlin
@Entity(
    tableName = "accounts",
    foreignKeys = [ForeignKey(
        entity = Profile::class,
        parentColumns = ["id"],
        childColumns = ["profile_id"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.RESTRICT
    )],
    indices = [
        Index(value = ["profile_id", "name"], unique = true, name = "un_account_name"),
        Index(value = ["profile_id"], name = "fk_account_profile")
    ]
)
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "profile_id") val profileId: Long,
    val name: String,
    @ColumnInfo(name = "name_upper") val nameUpper: String,
    val level: Int,
    @ColumnInfo(name = "parent_name") val parentName: String? = null,
    val expanded: Boolean = true,
    @ColumnInfo(name = "amounts_expanded") val amountsExpanded: Boolean = false,
    val generation: Long = 0
)
```

**フィールド:**
| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| id | Long | No | Auto-generated PK |
| profileId | Long | No | FK to Profile |
| name | String | No | 完全勘定科目名 (e.g., "Assets:Bank:Checking") |
| nameUpper | String | No | 大文字版（検索用） |
| level | Int | No | 階層深度 (0 = top-level) |
| parentName | String | Yes | 親勘定科目名 |
| expanded | Boolean | No | UI 展開状態 |
| amountsExpanded | Boolean | No | 金額リスト展開状態 |
| generation | Long | No | データ世代追跡 |

**関係:**
- Child of: Profile (N:1)
- Parent to: AccountValue (1:N, CASCADE on delete)

---

### 3. AccountValue (`account_values` table)

勘定科目の残高（通貨別）。

**Kotlin data class 変換:**
```kotlin
@Entity(
    tableName = "account_values",
    foreignKeys = [ForeignKey(
        entity = Account::class,
        parentColumns = ["id"],
        childColumns = ["account_id"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.RESTRICT
    )],
    indices = [
        Index(value = ["account_id", "currency"], unique = true, name = "un_account_values"),
        Index(value = ["account_id"], name = "fk_account_value_acc")
    ]
)
data class AccountValue(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "account_id") val accountId: Long,
    val currency: String = "",
    val value: Float,
    @ColumnInfo(name = "amount_style") val amountStyle: String? = null,
    val generation: Long = 0
)
```

**フィールド:**
| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| id | Long | No | Auto-generated PK |
| accountId | Long | No | FK to Account |
| currency | String | No | 通貨/商品コード |
| value | Float | No | 残高 |
| amountStyle | String | Yes | 金額フォーマットスタイル (JSON) |
| generation | Long | No | データ世代追跡 |

---

### 4. Currency (`currencies` table)

通貨/商品の定義。

**Kotlin data class 変換:**
```kotlin
@Entity(
    tableName = "currencies",
    indices = [Index(value = ["name"], unique = true, name = "currency_name_idx")]
)
data class Currency(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val position: String = "after",
    @ColumnInfo(name = "has_gap") val hasGap: Boolean = true
)
```

**フィールド:**
| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| id | Long | No | Auto-generated PK |
| name | String | No | 通貨コード (e.g., "USD", "EUR") |
| position | String | No | 位置: "before" or "after" |
| hasGap | Boolean | No | 金額と通貨間のスペース |

---

### 5. Transaction (`transactions` table)

仕訳取引。

**Kotlin data class 変換:**
```kotlin
@Entity(
    tableName = "transactions",
    foreignKeys = [ForeignKey(
        entity = Profile::class,
        parentColumns = ["id"],
        childColumns = ["profile_id"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.RESTRICT
    )],
    indices = [
        Index(value = ["profile_id", "ledger_id"], unique = true, name = "un_transactions_ledger_id"),
        Index(value = ["description"], name = "idx_transaction_description"),
        Index(value = ["profile_id"], name = "fk_transaction_profile")
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "ledger_id") val ledgerId: Long,
    @ColumnInfo(name = "profile_id") val profileId: Long,
    @ColumnInfo(name = "data_hash") val dataHash: String,
    val year: Int,
    val month: Int,
    val day: Int,
    val description: String,
    @ColumnInfo(name = "description_uc") val descriptionUc: String,
    val comment: String? = null,
    val generation: Long = 0
)
```

**フィールド:**
| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| id | Long | No | Database PK |
| ledgerId | Long | No | サーバー提供の取引ID |
| profileId | Long | No | FK to Profile |
| dataHash | String | No | SHA-256 ハッシュ（重複検出） |
| year, month, day | Int | No | 取引日付 |
| description | String | No | 取引説明 |
| descriptionUc | String | No | 大文字版（検索用） |
| comment | String | Yes | コメント |
| generation | Long | No | データ世代追跡 |

---

### 6. TransactionAccount (`transaction_accounts` table)

取引の勘定科目行（借方/貸方）。

**Kotlin data class 変換:**
```kotlin
@Entity(
    tableName = "transaction_accounts",
    foreignKeys = [ForeignKey(
        entity = Transaction::class,
        parentColumns = ["id"],
        childColumns = ["transaction_id"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.RESTRICT
    )],
    indices = [
        Index(value = ["transaction_id"], name = "fk_trans_acc_trans"),
        Index(value = ["transaction_id", "order_no"], unique = true, name = "un_transaction_accounts")
    ]
)
data class TransactionAccount(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "transaction_id") val transactionId: Long,
    @ColumnInfo(name = "order_no") val orderNo: Int,
    @ColumnInfo(name = "account_name") val accountName: String,
    val currency: String = "",
    val amount: Float,
    val comment: String? = null,
    @ColumnInfo(name = "amount_style") val amountStyle: String? = null,
    val generation: Long = 0
)
```

---

### 7. TemplateHeader (`templates` table)

取引テンプレートのヘッダー。正規表現でテキストからマッチング。

**Kotlin data class 変換:**
```kotlin
@Entity(
    tableName = "templates",
    indices = [Index(value = ["uuid"], unique = true, name = "templates_uuid_idx")]
)
data class TemplateHeader(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val uuid: String,
    @ColumnInfo(name = "regular_expression") val regularExpression: String,
    @ColumnInfo(name = "test_text") val testText: String? = null,
    @ColumnInfo(name = "transaction_description") val transactionDescription: String? = null,
    @ColumnInfo(name = "transaction_description_match_group") val transactionDescriptionMatchGroup: Int? = null,
    @ColumnInfo(name = "transaction_comment") val transactionComment: String? = null,
    @ColumnInfo(name = "transaction_comment_match_group") val transactionCommentMatchGroup: Int? = null,
    @ColumnInfo(name = "date_year") val dateYear: Int? = null,
    @ColumnInfo(name = "date_year_match_group") val dateYearMatchGroup: Int? = null,
    @ColumnInfo(name = "date_month") val dateMonth: Int? = null,
    @ColumnInfo(name = "date_month_match_group") val dateMonthMatchGroup: Int? = null,
    @ColumnInfo(name = "date_day") val dateDay: Int? = null,
    @ColumnInfo(name = "date_day_match_group") val dateDayMatchGroup: Int? = null,
    @ColumnInfo(name = "is_fallback") val isFallback: Boolean = false
)
```

---

### 8. TemplateAccount (`template_accounts` table)

テンプレートの勘定科目行定義。

**Kotlin data class 変換:**
```kotlin
@Entity(
    tableName = "template_accounts",
    foreignKeys = [
        ForeignKey(
            entity = TemplateHeader::class,
            parentColumns = ["id"],
            childColumns = ["template_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Currency::class,
            parentColumns = ["id"],
            childColumns = ["currency"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["template_id"], name = "fk_template_accounts_template"),
        Index(value = ["currency"], name = "fk_template_accounts_currency")
    ]
)
data class TemplateAccount(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "template_id") val templateId: Long,
    val position: Long,
    val acc: String? = null,
    @ColumnInfo(name = "acc_match_group") val accMatchGroup: Int? = null,
    val currency: Long? = null,
    @ColumnInfo(name = "currency_match_group") val currencyMatchGroup: Int? = null,
    val amount: Float? = null,
    @ColumnInfo(name = "amount_match_group") val amountMatchGroup: Int? = null,
    val comment: String? = null,
    @ColumnInfo(name = "comment_match_group") val commentMatchGroup: Int? = null,
    @ColumnInfo(name = "negate_amount") val negateAmount: Boolean? = null
)
```

---

### 9. Option (`options` table)

プロファイル別の設定オプション。複合主キー。

**Kotlin data class 変換:**
```kotlin
@Entity(
    tableName = "options",
    primaryKeys = ["profile_id", "name"]
)
data class Option(
    @ColumnInfo(name = "profile_id") val profileId: Long,
    val name: String,
    val value: String? = null
)
```

---

## Domain Models

DB エンティティとは別に、ビジネスロジック層で使用するドメインモデル。

### LedgerAccount

```kotlin
data class LedgerAccount(
    val name: String,
    val shortName: String,
    val level: Int,
    val parentName: String?,
    val amounts: List<LedgerAmount>,
    var expanded: Boolean = true,
    var amountsExpanded: Boolean = false
) {
    companion object {
        fun fromDBO(dbo: AccountWithAmounts): LedgerAccount
    }

    fun toDBO(profileId: Long): AccountWithAmounts
}
```

### LedgerTransaction

```kotlin
data class LedgerTransaction(
    val ledgerId: Long,
    val date: SimpleDate,
    val description: String,
    val comment: String?,
    val accounts: List<LedgerTransactionAccount>,
    val dataHash: String
) {
    companion object {
        fun fromDBO(dbo: TransactionWithAccounts): LedgerTransaction
    }

    fun toDBO(profileId: Long): TransactionWithAccounts
}
```

### LedgerTransactionAccount

```kotlin
data class LedgerTransactionAccount(
    val accountName: String,
    val amount: Float?,
    val currency: String,
    val comment: String?,
    val amountStyle: AmountStyle?
) {
    val isAmountValid: Boolean
        get() = amount != null
}
```

### LedgerAmount

```kotlin
data class LedgerAmount(
    val currency: String,
    val amount: Float,
    val amountStyle: AmountStyle?
)
```

### AmountStyle

金額の表示フォーマット。JSON シリアライズされて DB に保存。

```kotlin
data class AmountStyle(
    val position: Position,
    val hasGap: Boolean,
    val precision: Int,
    val decimalMark: Char
) {
    enum class Position { BEFORE, AFTER, NONE }

    fun toJson(): String

    companion object {
        fun fromJson(json: String): AmountStyle?
        fun fromParsedStyle(style: ParsedStyle, commodity: String): AmountStyle
    }
}
```

---

## Validation Rules

### Account
- `name` は空文字不可
- `name` はプロファイル内で一意
- `level` は 0 以上

### Transaction
- `ledgerId` はプロファイル内で一意
- `dataHash` は SHA-256 形式
- 日付フィールドは有効な日付

### TransactionAccount
- `accountName` は空文字不可
- `orderNo` は取引内で一意

### Profile
- `uuid` はグローバルに一意
- `url` は有効な URL 形式

---

## State Transitions

### Profile データ同期

```
[Initial] → [Syncing] → [Synced]
                ↓
            [Error]
```

1. **Initial**: プロファイル作成直後
2. **Syncing**: サーバーからデータ取得中
3. **Synced**: 同期完了 (`generation` 更新)
4. **Error**: 同期失敗

### Transaction 作成フロー

```
[Draft] → [Validating] → [Submitting] → [Submitted]
              ↓               ↓
          [Invalid]       [Failed]
```

1. **Draft**: ユーザー入力中
2. **Validating**: バランスチェック
3. **Submitting**: サーバーへ送信中
4. **Submitted**: 送信完了（`ledgerId` 取得）

---

## Migration Notes

### data class 変換時の注意

1. **Primary Key**: `@PrimaryKey(autoGenerate = true)` はコンストラクタ引数に
2. **Default 値**: Room は data class のデフォルト値を尊重
3. **Nullability**: Java の `@Nullable` → Kotlin の `?`
4. **ForeignKey**: `@Entity` アノテーション内で定義

### 互換性

- スキーマ変更なし
- 既存の `DB.java` マイグレーションは維持
- TypeConverter は変更不要
