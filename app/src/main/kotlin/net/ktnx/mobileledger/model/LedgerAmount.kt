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

import net.ktnx.mobileledger.db.Account
import net.ktnx.mobileledger.db.AccountValue
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

class LedgerAmount @JvmOverloads constructor(
    val amount: Float,
    val currency: String? = null,
    var amountStyle: AmountStyle? = null
) {
    private var dbId: Long = 0

    val effectiveStyle: AmountStyle
        get() = amountStyle ?: AmountStyle.getDefault(currency)

    override fun toString(): String {
        val style = effectiveStyle
        val sb = StringBuilder()

        if (currency == null) {
            // No currency, just format the amount
            sb.append(formatAmount(amount, style))
        } else {
            // Currency before amount
            if (style.commodityPosition == AmountStyle.Position.BEFORE) {
                sb.append(currency)
                if (style.isCommoditySpaced) {
                    sb.append(' ')
                }
            }

            // Format the amount
            sb.append(formatAmount(amount, style))

            // Currency after amount
            if (style.commodityPosition == AmountStyle.Position.AFTER) {
                if (style.isCommoditySpaced) {
                    sb.append(' ')
                }
                sb.append(currency)
            }
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
            if (decimalMark == ",") {
                formatted = formatted.replace(".", "DECIMAL_PLACEHOLDER")
                formatted = formatted.replace("DECIMAL_PLACEHOLDER", decimalMark)
            } else {
                formatted = formatted.replace(".", decimalMark)
            }
        }

        return formatted
    }

    fun propagateToAccount(acc: LedgerAccount) {
        if (currency != null) {
            acc.addAmount(amount, currency)
        } else {
            acc.addAmount(amount)
        }
    }

    fun toDBO(account: Account): AccountValue {
        val obj = AccountValue()
        obj.id = dbId
        obj.accountId = account.id
        obj.currency = currency ?: ""
        obj.value = amount

        // Save amount style if present
        amountStyle?.let {
            obj.amountStyle = it.serialize()
        }

        return obj
    }

    companion object {
        @JvmStatic
        fun fromDBO(dbo: AccountValue): LedgerAmount {
            val style = dbo.amountStyle?.let { AmountStyle.deserialize(it) }
            return LedgerAmount(dbo.value, dbo.currency, style).apply {
                dbId = dbo.id
            }
        }
    }
}
