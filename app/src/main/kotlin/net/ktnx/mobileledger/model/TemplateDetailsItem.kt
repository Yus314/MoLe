/*
 * Copyright © 2021 Damyan Ivanov.
 * This file is part of MoLe.
 * MoLe is free software: you can distribute it and/or modify it
 * under the term of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your opinion), any later version.
 *
 * MoLe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License terms for details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MoLe. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ktnx.mobileledger.model

import android.content.res.Resources
import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import java.util.Locale
import java.util.Objects
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import net.ktnx.mobileledger.R
import net.ktnx.mobileledger.db.TemplateAccount
import net.ktnx.mobileledger.db.TemplateBase
import net.ktnx.mobileledger.db.TemplateHeader
import net.ktnx.mobileledger.utils.Logger
import net.ktnx.mobileledger.utils.Misc

abstract class TemplateDetailsItem protected constructor(val type: Type) {
    var id: Long? = null
    var position: Long = 0

    fun asHeaderItem(): Header {
        ensureType(Type.HEADER)
        return this as Header
    }

    fun asAccountRowItem(): AccountRow {
        ensureType(Type.ACCOUNT_ITEM)
        return this as AccountRow
    }

    private fun ensureType(type: Type) {
        if (this.type != type) {
            throw IllegalStateException(
                String.format("Type is %s, but %s is required", this.type, type)
            )
        }
    }

    protected fun ensureTrue(flag: Boolean) {
        if (!flag) {
            throw IllegalStateException(
                "Literal value requested, but it is matched via a pattern group"
            )
        }
    }

    protected fun ensureFalse(flag: Boolean) {
        if (flag) {
            throw IllegalStateException("Matching group requested, but the value is a literal")
        }
    }

    abstract fun getProblem(r: Resources, patternGroupCount: Int): String?

    enum class Type(val index: Int) {
        HEADER(TYPE.header),
        ACCOUNT_ITEM(TYPE.accountItem);

        fun toInt(): Int = index
    }

    class PossiblyMatchedValue<T> {
        private var literalValue: Boolean = true
        private var value: T? = null
        private var matchGroup: Int = 0

        constructor()

        constructor(origin: PossiblyMatchedValue<T>) {
            literalValue = origin.literalValue
            value = origin.value
            matchGroup = origin.matchGroup
        }

        fun copyFrom(origin: PossiblyMatchedValue<T>) {
            literalValue = origin.literalValue
            value = origin.value
            matchGroup = origin.matchGroup
        }

        fun getValue(): T? {
            if (!literalValue) {
                throw IllegalStateException("Value is not literal")
            }
            return value
        }

        fun setValue(newValue: T?) {
            value = newValue
            literalValue = true
        }

        fun hasLiteralValue(): Boolean = literalValue

        fun getMatchGroup(): Int {
            if (literalValue) {
                throw IllegalStateException("Value is literal")
            }
            return matchGroup
        }

        fun setMatchGroup(group: Int) {
            this.matchGroup = group
            literalValue = false
        }

        override fun equals(other: Any?): Boolean {
            if (other !is PossiblyMatchedValue<*>) return false
            if (other.literalValue != literalValue) return false
            return if (literalValue) {
                if (value == null) other.value == null else value == other.value
            } else {
                matchGroup == other.matchGroup
            }
        }

        override fun hashCode(): Int {
            var result = literalValue.hashCode()
            result = 31 * result + (value?.hashCode() ?: 0)
            result = 31 * result + matchGroup
            return result
        }

        fun switchToLiteral() {
            literalValue = true
        }

        override fun toString(): String = when {
            literalValue -> value?.toString() ?: "<null>"
            matchGroup > 0 -> "grp:$matchGroup"
            else -> "<null>"
        }

        fun isEmpty(): Boolean = if (literalValue) {
            value == null || Misc.emptyIsNull(value.toString()) == null
        } else {
            matchGroup > 0
        }

        companion object {
            @JvmStatic
            fun withLiteralInt(initialValue: Int?): PossiblyMatchedValue<Int> =
                PossiblyMatchedValue<Int>().apply { setValue(initialValue) }

            @JvmStatic
            fun withLiteralFloat(initialValue: Float?): PossiblyMatchedValue<Float> =
                PossiblyMatchedValue<Float>().apply { setValue(initialValue) }

            @JvmStatic
            fun withLiteralShort(initialValue: Short?): PossiblyMatchedValue<Short> =
                PossiblyMatchedValue<Short>().apply { setValue(initialValue) }

            @JvmStatic
            fun withLiteralString(initialValue: String?): PossiblyMatchedValue<String> =
                PossiblyMatchedValue<String>().apply { setValue(initialValue) }
        }
    }

    object TYPE {
        const val header = 0
        const val accountItem = 1
    }

    class AccountRow : TemplateDetailsItem {
        private val accountName = PossiblyMatchedValue.withLiteralString("")
        private val accountComment = PossiblyMatchedValue.withLiteralString("")
        private val amount = PossiblyMatchedValue.withLiteralFloat(null)
        private val currency = PossiblyMatchedValue<net.ktnx.mobileledger.db.Currency>()
        var isNegateAmount: Boolean = false

        constructor() : super(Type.ACCOUNT_ITEM)

        constructor(origin: AccountRow) : super(Type.ACCOUNT_ITEM) {
            id = origin.id
            position = origin.position
            accountName.copyFrom(origin.accountName)
            accountComment.copyFrom(origin.accountComment)
            amount.copyFrom(origin.amount)
            currency.copyFrom(origin.currency)
            isNegateAmount = origin.isNegateAmount
        }

        fun getAccountCommentMatchGroup(): Int = accountComment.getMatchGroup()
        fun setAccountCommentMatchGroup(group: Int) = accountComment.setMatchGroup(group)
        fun getAccountComment(): String? = accountComment.getValue()
        fun setAccountComment(comment: String?) = accountComment.setValue(comment)

        fun getCurrencyMatchGroup(): Int = currency.getMatchGroup()
        fun setCurrencyMatchGroup(group: Int) = currency.setMatchGroup(group)
        fun getCurrency(): net.ktnx.mobileledger.db.Currency? = currency.getValue()
        fun setCurrency(currency: net.ktnx.mobileledger.db.Currency?) = this.currency.setValue(currency)

        fun getAccountNameMatchGroup(): Int = accountName.getMatchGroup()
        fun setAccountNameMatchGroup(group: Int) = accountName.setMatchGroup(group)
        fun getAccountName(): String? = accountName.getValue()
        fun setAccountName(accountName: String?) = this.accountName.setValue(accountName)

        fun hasLiteralAccountName(): Boolean = accountName.hasLiteralValue()
        fun hasLiteralAmount(): Boolean = amount.hasLiteralValue()

        fun getAmountMatchGroup(): Int = amount.getMatchGroup()
        fun setAmountMatchGroup(group: Int) = amount.setMatchGroup(group)
        fun getAmount(): Float? = amount.getValue()
        fun setAmount(amount: Float?) = this.amount.setValue(amount)

        override fun getProblem(r: Resources, patternGroupCount: Int): String? {
            if (Misc.emptyIsNull(accountName.getValue()) == null) {
                return r.getString(R.string.account_name_is_empty)
            }
            if (!amount.hasLiteralValue() &&
                (amount.getMatchGroup() < 1 || amount.getMatchGroup() > patternGroupCount)
            ) {
                return r.getString(R.string.invalid_matching_group_number)
            }
            return null
        }

        fun hasLiteralAccountComment(): Boolean = accountComment.hasLiteralValue()
        fun hasLiteralCurrency(): Boolean = currency.hasLiteralValue()

        fun equalContents(o: AccountRow): Boolean {
            if (position != o.position) {
                Logger.debug(
                    "cmpAcc",
                    String.format(
                        Locale.US,
                        "[%d] != [%d]: pos %d != pos %d",
                        id,
                        o.id,
                        position,
                        o.position
                    )
                )
                return false
            }
            return amount == o.amount && accountName == o.accountName &&
                position == o.position && accountComment == o.accountComment &&
                isNegateAmount == o.isNegateAmount
        }

        fun switchToLiteralAmount() = amount.switchToLiteral()
        fun switchToLiteralCurrency() = currency.switchToLiteral()
        fun switchToLiteralAccountName() = accountName.switchToLiteral()
        fun switchToLiteralAccountComment() = accountComment.switchToLiteral()

        fun toDBO(patternId: Long): TemplateAccount {
            val result = TemplateAccount(id ?: 0, patternId, position)

            if (accountName.hasLiteralValue()) {
                result.accountName = accountName.getValue()
            } else {
                result.accountNameMatchGroup = accountName.getMatchGroup()
            }

            if (accountComment.hasLiteralValue()) {
                result.accountComment = accountComment.getValue()
            } else {
                result.accountCommentMatchGroup = accountComment.getMatchGroup()
            }

            if (amount.hasLiteralValue()) {
                result.amount = amount.getValue()
                result.negateAmount = null
            } else {
                result.amountMatchGroup = amount.getMatchGroup()
                result.negateAmount = if (isNegateAmount) true else null
            }

            if (currency.hasLiteralValue()) {
                val c = currency.getValue()
                result.currency = c?.id
            } else {
                result.currencyMatchGroup = currency.getMatchGroup()
            }

            return result
        }

        fun isEmpty(): Boolean = accountName.isEmpty() && accountComment.isEmpty() && amount.isEmpty()
    }

    class Header : TemplateDetailsItem {
        var pattern: String = ""
            set(value) {
                field = value
                try {
                    compiledPattern = Pattern.compile(value)
                    checkPatternMatch()
                } catch (ex: PatternSyntaxException) {
                    patternError = ex.description
                    compiledPattern = null
                    testMatch = SpannableString(testText).also { ss ->
                        if (testText.isNotEmpty()) {
                            ss.setSpan(
                                notMatchedSpan(),
                                0,
                                testText.length - 1,
                                Spanned.SPAN_INCLUSIVE_INCLUSIVE
                            )
                        }
                    }
                }
            }

        var testText: String = ""
            set(value) {
                field = value
                checkPatternMatch()
            }

        var name: String = ""
        var compiledPattern: Pattern? = null
            private set
        var patternError: String? = null
            private set

        private val transactionDescription = PossiblyMatchedValue.withLiteralString("")
        private val transactionComment = PossiblyMatchedValue.withLiteralString("")
        private val dateYear = PossiblyMatchedValue.withLiteralInt(null)
        private val dateMonth = PossiblyMatchedValue.withLiteralInt(null)
        private val dateDay = PossiblyMatchedValue.withLiteralInt(null)
        var testMatch: SpannableString? = null
            private set
        var isFallback: Boolean = false

        // Java compatibility method
        fun testMatch(): SpannableString? = testMatch

        private constructor() : super(Type.HEADER)

        constructor(origin: Header) : this() {
            id = origin.id
            name = origin.name
            testText = origin.testText
            testMatch = origin.testMatch
            pattern = origin.pattern

            transactionDescription.copyFrom(origin.transactionDescription)
            transactionComment.copyFrom(origin.transactionComment)

            dateYear.copyFrom(origin.dateYear)
            dateMonth.copyFrom(origin.dateMonth)
            dateDay.copyFrom(origin.dateDay)

            isFallback = origin.isFallback
        }

        fun getTransactionDescription(): String? = transactionDescription.getValue()
        fun setTransactionDescription(value: String?) = transactionDescription.setValue(value)
        fun getTransactionComment(): String? = transactionComment.getValue()
        fun setTransactionComment(value: String?) = transactionComment.setValue(value)

        fun getDateYear(): Int? = dateYear.getValue()
        fun setDateYear(value: Int?) = dateYear.setValue(value)
        fun getDateMonth(): Int? = dateMonth.getValue()
        fun setDateMonth(value: Int?) = dateMonth.setValue(value)
        fun getDateDay(): Int? = dateDay.getValue()
        fun setDateDay(value: Int?) = dateDay.setValue(value)

        fun getDateYearMatchGroup(): Int = dateYear.getMatchGroup()
        fun setDateYearMatchGroup(group: Int) = dateYear.setMatchGroup(group)
        fun getDateMonthMatchGroup(): Int = dateMonth.getMatchGroup()
        fun setDateMonthMatchGroup(group: Int) = dateMonth.setMatchGroup(group)
        fun getDateDayMatchGroup(): Int = dateDay.getMatchGroup()
        fun setDateDayMatchGroup(group: Int) = dateDay.setMatchGroup(group)

        fun hasLiteralDateYear(): Boolean = dateYear.hasLiteralValue()
        fun hasLiteralDateMonth(): Boolean = dateMonth.hasLiteralValue()
        fun hasLiteralDateDay(): Boolean = dateDay.hasLiteralValue()
        fun hasLiteralTransactionDescription(): Boolean = transactionDescription.hasLiteralValue()
        fun hasLiteralTransactionComment(): Boolean = transactionComment.hasLiteralValue()

        override fun getProblem(r: Resources, patternGroupCount: Int): String? {
            if (patternError != null) {
                return r.getString(R.string.pattern_has_errors) + ": " + patternError
            }
            if (Misc.emptyIsNull(pattern) == null) {
                return r.getString(R.string.pattern_is_empty)
            }

            if (!dateYear.hasLiteralValue() && compiledPattern != null &&
                (dateDay.getMatchGroup() < 1 || dateDay.getMatchGroup() > patternGroupCount)
            ) {
                return r.getString(R.string.invalid_matching_group_number)
            }

            if (!dateMonth.hasLiteralValue() && compiledPattern != null &&
                (dateMonth.getMatchGroup() < 1 || dateMonth.getMatchGroup() > patternGroupCount)
            ) {
                return r.getString(R.string.invalid_matching_group_number)
            }

            if (!dateDay.hasLiteralValue() && compiledPattern != null &&
                (dateDay.getMatchGroup() < 1 || dateDay.getMatchGroup() > patternGroupCount)
            ) {
                return r.getString(R.string.invalid_matching_group_number)
            }

            return null
        }

        fun equalContents(o: Header): Boolean {
            if (dateDay != o.dateDay) return false
            if (dateMonth != o.dateMonth) return false
            if (dateYear != o.dateYear) return false
            if (transactionDescription != o.transactionDescription) return false
            if (transactionComment != o.transactionComment) return true

            return Misc.equalStrings(name, o.name) && Misc.equalStrings(pattern, o.pattern) &&
                Misc.equalStrings(testText, o.testText) &&
                Misc.equalStrings(patternError, o.patternError) &&
                Objects.equals(testMatch, o.testMatch) && isFallback == o.isFallback
        }

        fun getMatchGroupText(group: Int): String {
            val pattern = compiledPattern
            if (pattern != null && testText.isNotEmpty()) {
                val m = pattern.matcher(testText)
                if (m.matches()) {
                    return m.group(group) ?: "ø"
                }
            }
            return "ø"
        }

        fun switchToLiteralTransactionDescription() = transactionDescription.switchToLiteral()
        fun switchToLiteralTransactionComment() = transactionComment.switchToLiteral()

        fun getTransactionDescriptionMatchGroup(): Int = transactionDescription.getMatchGroup()
        fun setTransactionDescriptionMatchGroup(group: Int) = transactionDescription.setMatchGroup(group)
        fun getTransactionCommentMatchGroup(): Int = transactionComment.getMatchGroup()
        fun setTransactionCommentMatchGroup(group: Int) = transactionComment.setMatchGroup(group)

        fun switchToLiteralDateYear() = dateYear.switchToLiteral()
        fun switchToLiteralDateMonth() = dateMonth.switchToLiteral()
        fun switchToLiteralDateDay() = dateDay.switchToLiteral()

        fun toDBO(): TemplateHeader {
            val result = TemplateHeader(id ?: 0, name, pattern)

            if (Misc.emptyIsNull(testText) != null) {
                result.testText = testText
            }

            if (transactionDescription.hasLiteralValue()) {
                result.transactionDescription = transactionDescription.getValue()
            } else {
                result.transactionDescriptionMatchGroup = transactionDescription.getMatchGroup()
            }

            if (transactionComment.hasLiteralValue()) {
                result.transactionComment = transactionComment.getValue()
            } else {
                result.transactionCommentMatchGroup = transactionComment.getMatchGroup()
            }

            if (dateYear.hasLiteralValue()) {
                result.dateYear = dateYear.getValue()
            } else {
                result.dateYearMatchGroup = dateYear.getMatchGroup()
            }

            if (dateMonth.hasLiteralValue()) {
                result.dateMonth = dateMonth.getValue()
            } else {
                result.dateMonthMatchGroup = dateMonth.getMatchGroup()
            }

            if (dateDay.hasLiteralValue()) {
                result.dateDay = dateDay.getValue()
            } else {
                result.dateDayMatchGroup = dateDay.getMatchGroup()
            }

            result.isFallback = isFallback

            return result
        }

        fun checkPatternMatch() {
            patternError = null
            testMatch = null

            if (pattern.isNotEmpty()) {
                try {
                    val compiledPat = compiledPattern ?: return
                    if (Misc.emptyIsNull(testText) != null) {
                        val ss = SpannableString(testText)
                        val m = compiledPat.matcher(testText)
                        if (m.find()) {
                            if (m.start() > 0) {
                                ss.setSpan(
                                    notMatchedSpan(),
                                    0,
                                    m.start(),
                                    Spanned.SPAN_INCLUSIVE_INCLUSIVE
                                )
                            }
                            if (m.end() < testText.length - 1) {
                                ss.setSpan(
                                    notMatchedSpan(),
                                    m.end(),
                                    testText.length,
                                    Spanned.SPAN_INCLUSIVE_INCLUSIVE
                                )
                            }

                            ss.setSpan(
                                matchedSpan(),
                                m.start(0),
                                m.end(0),
                                Spanned.SPAN_INCLUSIVE_INCLUSIVE
                            )

                            if (m.groupCount() > 0) {
                                for (g in 1..m.groupCount()) {
                                    ss.setSpan(
                                        capturedSpan(),
                                        m.start(g),
                                        m.end(g),
                                        Spanned.SPAN_INCLUSIVE_INCLUSIVE
                                    )
                                }
                            }
                        } else {
                            patternError = "Pattern does not match"
                            ss.setSpan(
                                ForegroundColorSpan(Color.GRAY),
                                0,
                                testText.length - 1,
                                Spanned.SPAN_INCLUSIVE_INCLUSIVE
                            )
                        }

                        testMatch = ss
                    }
                } catch (e: PatternSyntaxException) {
                    compiledPattern = null
                    patternError = e.message
                }
            } else {
                patternError = "Missing pattern"
            }
        }

        override fun toString(): String = super.toString() +
            String.format(
                " name[%s] pat[%s] test[%s] tran[%s] com[%s]",
                name,
                pattern,
                testText,
                transactionDescription,
                transactionComment
            )

        companion object {
            private fun capturedSpan() = StyleSpan(Typeface.BOLD)
            private fun matchedSpan() = UnderlineSpan()
            private fun notMatchedSpan() = ForegroundColorSpan(Color.GRAY)

            @JvmStatic
            fun createHeader(): Header = Header()

            @JvmStatic
            fun createHeader(origin: Header): Header = Header(origin)
        }
    }

    companion object {
        @JvmStatic
        fun createHeader(): Header = Header.createHeader()

        @JvmStatic
        fun createHeader(origin: Header): Header = Header.createHeader(origin)

        @JvmStatic
        fun createAccountRow(): AccountRow = AccountRow()

        @JvmStatic
        fun fromRoomObject(p: TemplateBase): TemplateDetailsItem = when (p) {
            is TemplateHeader -> {
                val header = createHeader()
                header.id = p.id
                header.name = p.name
                header.pattern = p.regularExpression
                header.testText = p.testText ?: ""

                p.transactionDescriptionMatchGroup?.let { group ->
                    header.setTransactionDescriptionMatchGroup(group)
                } ?: header.setTransactionDescription(p.transactionDescription)

                p.transactionCommentMatchGroup?.let { group ->
                    header.setTransactionCommentMatchGroup(group)
                } ?: header.setTransactionComment(p.transactionComment)

                p.dateDayMatchGroup?.let { group ->
                    header.setDateDayMatchGroup(group)
                } ?: header.setDateDay(p.dateDay)

                p.dateMonthMatchGroup?.let { group ->
                    header.setDateMonthMatchGroup(group)
                } ?: header.setDateMonth(p.dateMonth)

                p.dateYearMatchGroup?.let { group ->
                    header.setDateYearMatchGroup(group)
                } ?: header.setDateYear(p.dateYear)

                header.isFallback = p.isFallback

                header
            }

            is TemplateAccount -> {
                val acc = createAccountRow()
                acc.id = p.id
                acc.position = p.position

                p.accountNameMatchGroup?.let { group ->
                    acc.setAccountNameMatchGroup(group)
                } ?: acc.setAccountName(Misc.nullIsEmpty(p.accountName))

                p.accountCommentMatchGroup?.let { group ->
                    acc.setAccountCommentMatchGroup(group)
                } ?: acc.setAccountComment(Misc.nullIsEmpty(p.accountComment))

                p.currencyMatchGroup?.let { group ->
                    acc.setCurrencyMatchGroup(group)
                } ?: acc.setCurrency(p.getCurrencyObject())

                val amountMatchGroup = p.amountMatchGroup
                if (amountMatchGroup != null && amountMatchGroup > 0) {
                    acc.setAmountMatchGroup(amountMatchGroup)
                    val negateAmount = p.negateAmount
                    acc.isNegateAmount = negateAmount != null && negateAmount
                } else {
                    acc.setAmount(p.amount)
                }

                acc
            }

            else -> throw IllegalStateException("Unexpected item class ${p.javaClass}")
        }
    }
}
