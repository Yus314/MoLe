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

package net.ktnx.mobileledger.json.common

import java.text.ParseException
import net.ktnx.mobileledger.model.LedgerTransaction
import net.ktnx.mobileledger.utils.Globals
import net.ktnx.mobileledger.utils.Misc
import net.ktnx.mobileledger.utils.SimpleDate

/**
 * Utility object providing common functionality for ParsedLedgerTransaction implementations.
 *
 * This object extracts the common logic from the various version-specific
 * ParsedLedgerTransaction classes to reduce code duplication.
 */
object TransactionHelper {
    /**
     * Common conversion from ParsedLedgerTransaction to LedgerTransaction.
     * Handles date parsing, description, and comment.
     *
     * @param tindex Transaction index
     * @param tdate Date string in ISO format
     * @param tdescription Transaction description
     * @param tcomment Transaction comment
     * @return A partially constructed LedgerTransaction
     * @throws ParseException if date parsing fails
     */
    @JvmStatic
    @Throws(ParseException::class)
    fun createBaseLedgerTransaction(
        tindex: Int,
        tdate: String?,
        tdescription: String?,
        tcomment: String?
    ): LedgerTransaction {
        val date = tdate?.let { Globals.parseIsoDate(it) }
        val tr = LedgerTransaction(tindex.toLong(), date, tdescription)
        tr.comment = Misc.trim(Misc.emptyIsNull(tcomment))
        return tr
    }

    /**
     * Populate a transaction object from a LedgerTransaction.
     * Sets the common fields that are identical across all API versions.
     *
     * @param tr Source LedgerTransaction
     * @param setComment Function to set comment
     * @param setPrecedingComment Function to set preceding comment
     * @param setDate Function to set date
     * @param setDate2 Function to set secondary date
     * @param setIndex Function to set index
     * @param setDescription Function to set description
     */
    @JvmStatic
    fun populateFromLedgerTransaction(
        tr: LedgerTransaction,
        setComment: (String) -> Unit,
        setPrecedingComment: (String) -> Unit,
        setDate: (String) -> Unit,
        setDate2: (String?) -> Unit,
        setIndex: (Int) -> Unit,
        setDescription: (String?) -> Unit
    ) {
        setComment(Misc.nullIsEmpty(tr.comment))
        setPrecedingComment("")
        setDate(Globals.formatIsoDate(tr.getDateIfAny() ?: SimpleDate.today()))
        setDate2(null)
        setIndex(1)
        setDescription(tr.description)
    }
}
