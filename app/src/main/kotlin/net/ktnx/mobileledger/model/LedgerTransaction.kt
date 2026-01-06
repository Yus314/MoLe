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

import java.nio.charset.StandardCharsets
import java.text.ParseException
import net.ktnx.mobileledger.db.Profile
import net.ktnx.mobileledger.db.Transaction
import net.ktnx.mobileledger.db.TransactionAccount
import net.ktnx.mobileledger.db.TransactionWithAccounts
import net.ktnx.mobileledger.utils.Digest
import net.ktnx.mobileledger.utils.Globals
import net.ktnx.mobileledger.utils.SimpleDate

class LedgerTransaction {
    val comparator: Comparator<LedgerTransactionAccount> = Comparator { o1, o2 ->
        var res = o1.accountName.compareTo(o2.accountName)
        if (res != 0) return@Comparator res
        res = (o1.currency ?: "").compareTo(o2.currency ?: "")
        if (res != 0) return@Comparator res
        res = (o1.comment ?: "").compareTo(o2.comment ?: "")
        if (res != 0) return@Comparator res
        o1.amount.compareTo(o2.amount)
    }

    private val profile: Long
    val ledgerId: Long
    val accounts: MutableList<LedgerTransactionAccount>
    private var dbId: Long = 0

    @get:JvmName("getDateNullable")
    var date: SimpleDate? = null
        set(value) {
            field = value
            dataHash = null
        }
    var description: String? = null
        set(value) {
            field = value
            dataHash = null
        }
    var comment: String? = null
    private var dataHash: String? = null
    private var dataLoaded: Boolean = false

    @Throws(ParseException::class)
    constructor(ledgerId: Long, dateString: String, description: String?) :
            this(ledgerId, Globals.parseLedgerDate(dateString), description)

    constructor(dbo: TransactionWithAccounts) : this(dbo.transaction.ledgerId, dbo.transaction.profileId) {
        dbId = dbo.transaction.id
        date = SimpleDate(dbo.transaction.year, dbo.transaction.month, dbo.transaction.day)
        description = dbo.transaction.description
        comment = dbo.transaction.comment
        dataHash = dbo.transaction.dataHash
        dbo.accounts?.forEach { acc ->
            accounts.add(LedgerTransactionAccount(acc))
        }
        dataLoaded = true
    }

    constructor(ledgerId: Long, date: SimpleDate?, description: String?, profile: Profile) {
        this.profile = profile.id
        this.ledgerId = ledgerId
        this.date = date
        this.description = description
        this.accounts = ArrayList()
        this.dataHash = null
        dataLoaded = false
    }

    constructor(ledgerId: Long, date: SimpleDate?, description: String?) :
            this(ledgerId, date, description, requireNotNull(Data.getProfile()) { "No profile selected" })

    constructor(date: SimpleDate?, description: String?) :
            this(0, date, description)

    constructor(ledgerId: Int) :
            this(ledgerId.toLong(), null as SimpleDate?, null)

    constructor(ledgerId: Long, profileId: Long) {
        this.profile = profileId
        this.ledgerId = ledgerId
        this.date = null
        this.description = null
        this.accounts = ArrayList()
        this.dataHash = null
        this.dataLoaded = false
    }

    fun getDateIfAny(): SimpleDate? = date

    @JvmName("getDate")
    fun requireDate(): SimpleDate = date ?: throw IllegalStateException("Transaction has no date")

    fun addAccount(item: LedgerTransactionAccount) {
        accounts.add(item)
        dataHash = null
    }

    fun toDBO(): TransactionWithAccounts {
        val d = requireNotNull(date) { "Transaction date must be set before converting to DBO" }
        val o = TransactionWithAccounts()
        o.transaction = Transaction()
        o.transaction.id = dbId
        o.transaction.profileId = profile
        o.transaction.ledgerId = ledgerId
        o.transaction.year = d.year
        o.transaction.month = d.month
        o.transaction.day = d.day
        o.transaction.description = description ?: ""
        o.transaction.comment = comment
        fillDataHash()
        o.transaction.dataHash = dataHash ?: ""

        val accountsList = ArrayList<TransactionAccount>()
        var orderNo = 1
        for (acc in accounts) {
            val a: TransactionAccount = acc.toDBO()
            a.orderNo = orderNo++
            a.transactionId = dbId
            accountsList.add(a)
        }
        o.accounts = accountsList
        return o
    }

    private fun fillDataHash() {
        if (dataHash != null) return

        try {
            val sha = Digest(DIGEST_TYPE)
            val data = StringBuilder()
            data.append("ver1")
            data.append(profile)
            data.append(ledgerId)
            data.append('\u0000')
            data.append(description)
            data.append('\u0000')
            data.append(comment)
            data.append('\u0000')
            data.append(Globals.formatLedgerDate(requireDate()))
            data.append('\u0000')
            for (item in accounts) {
                data.append(item.accountName)
                data.append('\u0000')
                data.append(item.currency)
                data.append('\u0000')
                data.append(item.amount)
                data.append('\u0000')
                data.append(item.comment)
            }
            sha.update(data.toString().toByteArray(StandardCharsets.UTF_8))
            dataHash = sha.digestToHexString()
        } catch (e: Exception) {
            throw RuntimeException(
                String.format("Unable to get instance of %s digest", DIGEST_TYPE),
                e
            )
        }
    }

    fun getDataHash(): String {
        fillDataHash()
        return checkNotNull(dataHash) { "Data hash computation failed" }
    }

    fun finishLoading() {
        dataLoaded = true
    }

    fun markDataAsLoaded() {
        dataLoaded = true
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other.javaClass != this.javaClass) return false
        return (other as LedgerTransaction).getDataHash() == getDataHash()
    }

    override fun hashCode(): Int = getDataHash().hashCode()

    fun hasAccountNamedLike(name: String): Boolean {
        val upperName = name.uppercase()
        return accounts.any { it.accountName.uppercase().contains(upperName) }
    }

    companion object {
        private const val DIGEST_TYPE = "SHA-256"
    }
}
