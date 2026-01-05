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

package net.ktnx.mobileledger.json.v1_40

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import net.ktnx.mobileledger.json.ParsedLedgerTransaction as IParsedLedgerTransaction
import net.ktnx.mobileledger.model.LedgerTransaction
import net.ktnx.mobileledger.utils.Globals
import net.ktnx.mobileledger.utils.Misc
import net.ktnx.mobileledger.utils.SimpleDate
import java.text.ParseException

@JsonIgnoreProperties(ignoreUnknown = true)
class ParsedLedgerTransaction : IParsedLedgerTransaction {
    var tdate: String? = null
    var tdate2: String? = null
    var tdescription: String? = null
    var tcomment: String? = null
    var tcode: String = ""
    var tstatus: String = "Unmarked"
    var tprecedingcomment: String = ""
    var ttags: MutableList<List<String>> = mutableListOf()
    var tsourcepos: MutableList<ParsedSourcePos> = mutableListOf()
    var tpostings: MutableList<ParsedPosting>? = null

    private var _tindex: Int = 0
    var tindex: Int
        get() = _tindex
        set(value) {
            _tindex = value
            tpostings?.forEach { it.ptransaction_ = value.toString() }
        }

    init {
        val startPos = ParsedSourcePos()
        val endPos = ParsedSourcePos()
        endPos.sourceLine = 2
        tsourcepos.add(startPos)
        tsourcepos.add(endPos)
    }

    fun addPosting(posting: ParsedPosting) {
        posting.ptransaction_ = tindex.toString()
        tpostings?.add(posting)
    }

    @Throws(ParseException::class)
    override fun asLedgerTransaction(): LedgerTransaction {
        val date = tdate?.let { Globals.parseIsoDate(it) }
        val tr = LedgerTransaction(tindex.toLong(), date, tdescription)
        tr.comment = Misc.trim(Misc.emptyIsNull(tcomment))

        tpostings?.forEach { p ->
            tr.addAccount(p.asLedgerAccount())
        }

        tr.markDataAsLoaded()
        return tr
    }

    companion object {
        @JvmStatic
        fun fromLedgerTransaction(tr: LedgerTransaction): ParsedLedgerTransaction {
            val result = ParsedLedgerTransaction()
            result.tcomment = Misc.nullIsEmpty(tr.comment)
            result.tprecedingcomment = ""

            val postings = mutableListOf<ParsedPosting>()
            for (acc in tr.accounts) {
                if (acc.accountName.isNotEmpty()) {
                    postings.add(ParsedPosting.fromLedgerAccount(acc))
                }
            }

            result.tpostings = postings
            var transactionDate = tr.getDateIfAny()
            if (transactionDate == null) {
                transactionDate = SimpleDate.today()
            }
            result.tdate = Globals.formatIsoDate(transactionDate)
            result.tdate2 = null
            result.tindex = 1
            result.tdescription = tr.description
            return result
        }
    }
}
