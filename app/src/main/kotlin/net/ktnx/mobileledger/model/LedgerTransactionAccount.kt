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

import java.util.Locale
import kotlin.math.abs
import kotlin.math.round
import net.ktnx.mobileledger.db.TransactionAccount
import net.ktnx.mobileledger.utils.Misc

class LedgerTransactionAccount {
    var accountName: String = ""
        set(value) {
            field = value
            shortAccountName = value.replace(Regex("(?<=^|:)(.)[^:]+(?=:)"), "$1")
        }

    var shortAccountName: String = ""
        private set

    private var _amount: Float = 0f
    var isAmountSet: Boolean = false
        private set
    var isAmountValid: Boolean = true
        private set

    var currency: String?
        get() = _currency
        set(value) {
            _currency = Misc.emptyIsNull(value)
        }
    private var _currency: String? = null

    var comment: String? = null

    private var dbId: Long = 0

    var amountStyle: AmountStyle? = null

    val amount: Float
        get() {
            if (!isAmountSet)
                throw IllegalStateException("Account amount is not set")
            return _amount
        }

    val effectiveStyle: AmountStyle
        get() = amountStyle ?: AmountStyle.getDefault(currency)

    constructor(accountName: String, amount: Float, currency: String?, comment: String?) :
            this(accountName, amount, currency, comment, null)

    constructor(
        accountName: String,
        amount: Float,
        currency: String?,
        comment: String?,
        amountStyle: AmountStyle?
    ) {
        this.accountName = accountName
        this._amount = amount
        this.isAmountSet = true
        this.isAmountValid = true
        this._currency = Misc.emptyIsNull(currency)
        this.comment = Misc.emptyIsNull(comment)
        this.amountStyle = amountStyle
    }

    constructor(accountName: String) {
        this.accountName = accountName
    }

    constructor(accountName: String, currency: String?) {
        this.accountName = accountName
        this._currency = Misc.emptyIsNull(currency)
    }

    // Copy constructor
    constructor(origin: LedgerTransactionAccount) {
        accountName = origin.accountName
        comment = origin.comment
        if (origin.isAmountSet) {
            setAmount(origin.amount)
        }
        isAmountValid = origin.isAmountValid
        _currency = origin.currency
        amountStyle = origin.amountStyle
    }

    constructor(dbo: TransactionAccount) : this(
        dbo.accountName,
        dbo.amount,
        Misc.emptyIsNull(dbo.currency),
        Misc.emptyIsNull(dbo.comment),
        dbo.amountStyle?.let { AmountStyle.deserialize(it) }
    ) {
        isAmountSet = true
        isAmountValid = true
        dbId = dbo.id
    }

    fun setAmount(accountAmount: Float) {
        this._amount = accountAmount
        this.isAmountSet = true
        this.isAmountValid = true
    }

    fun resetAmount() {
        this.isAmountSet = false
        this.isAmountValid = true
    }

    fun invalidateAmount() {
        this.isAmountValid = false
    }

    override fun toString(): String {
        if (!isAmountSet) return ""

        val style = effectiveStyle
        val sb = StringBuilder()

        // Currency before amount
        if (currency != null && style.commodityPosition == AmountStyle.Position.BEFORE) {
            sb.append(currency)
            if (style.isCommoditySpaced) {
                sb.append(' ')
            }
        }

        // Format the amount
        sb.append(formatAmount(_amount, style))

        // Currency after amount
        if (currency != null && style.commodityPosition == AmountStyle.Position.AFTER) {
            if (style.isCommoditySpaced) {
                sb.append(' ')
            }
            sb.append(currency)
        }

        return sb.toString()
    }

    /**
     * Formats the amount value according to the given style
     */
    private fun formatAmount(amount: Float, style: AmountStyle): String {
        val precision = style.precision
        val decimalMark = style.decimalMark

        // Check if amount is effectively an integer
        val isInteger = abs(amount - round(amount)) < 0.001f

        // For zero precision and integer amounts, format as integer
        if (precision == 0 && isInteger) {
            return String.format(Locale.US, "%,d", round(amount).toLong())
        }

        // Format with specified precision
        val pattern = "%,.${precision}f"
        var formatted = String.format(Locale.US, pattern, amount)

        // Replace decimal mark if needed
        if (decimalMark != ".") {
            // When decimal mark is comma, we need to be careful not to replace thousand separators
            if (decimalMark == ",") {
                formatted = formatted.replace(".", "DECIMAL_PLACEHOLDER")
                formatted = formatted.replace("DECIMAL_PLACEHOLDER", decimalMark)
            } else {
                formatted = formatted.replace(".", decimalMark)
            }
        }

        return formatted
    }

    fun toDBO(): TransactionAccount {
        val dbo = TransactionAccount()
        dbo.accountName = accountName
        if (isAmountSet) {
            dbo.amount = _amount
        }
        dbo.comment = comment
        dbo.currency = Misc.nullIsEmpty(currency)
        dbo.id = dbId

        // Save amount style if present
        amountStyle?.let {
            dbo.amountStyle = it.serialize()
        }

        return dbo
    }
}
