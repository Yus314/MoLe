/*
 * Copyright © 2024 Damyan Ivanov.
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
import net.ktnx.mobileledger.db.AccountWithAmounts
import java.util.regex.Pattern

class LedgerAccount(
    name: String,
    private val parent: LedgerAccount?
) {
    private var dbId: Long = 0
    private var profileId: Long = 0
    var name: String = name
        set(value) {
            field = value
            stripName()
        }
    var shortName: String = ""
        private set
    var level: Int = 0
        private set
    var isExpanded: Boolean = false
    private var amounts: MutableList<LedgerAmount>? = null
    var hasSubAccounts: Boolean = false
    var amountsExpanded: Boolean = false

    // Java compatibility methods
    fun hasSubAccounts(): Boolean = hasSubAccounts
    fun amountsExpanded(): Boolean = amountsExpanded

    init {
        if (parent != null && !name.startsWith(parent.name + ":")) {
            throw IllegalStateException(
                String.format("Account name '%s' doesn't match parent account '%s'",
                    name, parent.name)
            )
        }
        this.name = name
    }

    val id: Long
        get() = dbId

    val parentName: String?
        get() = parent?.name

    // an account is visible if:
    //  - it has an expanded visible parent or is a top account
    val isVisible: Boolean
        get() = parent == null || (parent.isExpanded && parent.isVisible)

    val amountCount: Int
        get() = amounts?.size ?: 0

    private fun stripName() {
        val split = name.split(":")
        shortName = split.last()
        level = split.size - 1
    }

    fun isParentOf(potentialChild: LedgerAccount): Boolean {
        return potentialChild.name.startsWith("$name:")
    }

    @JvmOverloads
    fun addAmount(amount: Float, currency: String = "", amountStyle: AmountStyle? = null) {
        val amountList = amounts ?: ArrayList<LedgerAmount>().also { amounts = it }
        amountList.add(LedgerAmount(amount, currency, amountStyle))
    }

    fun getAmountsString(): String {
        val amts = amounts
        if (amts.isNullOrEmpty()) return ""

        return amts.joinToString("\n") { it.toString() }
    }

    fun getAmountsString(limit: Int): String {
        val amts = amounts
        if (amts.isNullOrEmpty()) return ""

        return amts.take(limit).joinToString("\n") { it.toString() }
    }

    fun toggleExpanded() {
        isExpanded = !isExpanded
    }

    fun removeAmounts() {
        amounts?.clear()
    }

    fun toggleAmountsExpanded() {
        amountsExpanded = !amountsExpanded
    }

    fun propagateAmountsTo(acc: LedgerAccount) {
        amounts?.forEach { it.propagateToAccount(acc) }
    }

    fun allAmountsAreZero(): Boolean {
        return amounts?.all { it.amount == 0f } ?: true
    }

    fun getAmounts(): List<LedgerAmount>? = amounts

    fun toDBO(): Account {
        val dbo = Account()
        dbo.name = name
        dbo.nameUpper = name.uppercase()
        dbo.parentName = extractParentName(name)
        dbo.level = level
        dbo.id = dbId
        dbo.profileId = profileId
        dbo.expanded = isExpanded
        dbo.amountsExpanded = amountsExpanded
        return dbo
    }

    fun toDBOWithAmounts(): AccountWithAmounts {
        val dbo = AccountWithAmounts()
        dbo.account = toDBO()

        val amountsList = ArrayList<AccountValue>()
        amounts?.forEach { amt ->
            val accountValue = AccountValue()
            accountValue.currency = amt.currency ?: ""
            accountValue.value = amt.amount
            // Save amount style if present
            amt.amountStyle?.let {
                accountValue.amountStyle = it.serialize()
            }
            amountsList.add(accountValue)
        }
        dbo.amounts = amountsList

        return dbo
    }

    override fun hashCode(): Int = name.hashCode()

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is LedgerAccount) return false

        if (name != other.name) return false
        if (getAmountsString() != other.getAmountsString()) return false

        return isExpanded == other.isExpanded && amountsExpanded == other.amountsExpanded
    }

    companion object {
        private const val ACCOUNT_DELIMITER = ':'
        private val reHigherAccount: Pattern = Pattern.compile("^[^:]+:")

        @JvmStatic
        fun extractParentName(accName: String): String? {
            val colonPos = accName.lastIndexOf(ACCOUNT_DELIMITER)
            return if (colonPos < 0) null else accName.substring(0, colonPos)
        }

        @JvmStatic
        fun isParentOf(possibleParent: String, accountName: String): Boolean {
            return accountName.startsWith("$possibleParent:")
        }

        @JvmStatic
        fun fromDBO(input: AccountWithAmounts, parent: LedgerAccount?): LedgerAccount {
            val res = LedgerAccount(input.account.name, parent)
            res.dbId = input.account.id
            res.profileId = input.account.profileId
            res.name = input.account.name
            res.isExpanded = input.account.expanded
            res.amountsExpanded = input.account.amountsExpanded

            val amountsList = ArrayList<LedgerAmount>()
            for (accountValue in input.amounts) {
                amountsList.add(LedgerAmount.fromDBO(accountValue))
            }
            res.amounts = amountsList

            return res
        }

        @JvmStatic
        fun determineLevel(accName: String): Int {
            var level = 0
            var delimiterPosition = accName.indexOf(ACCOUNT_DELIMITER)
            while (delimiterPosition >= 0) {
                level++
                delimiterPosition = accName.indexOf(ACCOUNT_DELIMITER, delimiterPosition + 1)
            }
            return level
        }
    }
}
