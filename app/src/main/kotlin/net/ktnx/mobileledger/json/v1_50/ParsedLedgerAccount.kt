/*
 * Copyright © 2020, 2024 Damyan Ivanov.
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

package net.ktnx.mobileledger.json.v1_50

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import net.ktnx.mobileledger.domain.model.AmountStyle
import net.ktnx.mobileledger.json.ParsedLedgerAccount as BaseAccount

/**
 * Parser for hledger-web 1.50+ account JSON structure.
 *
 * In v1.50+, the account structure changed from:
 *   { "aibalance": [...], "aebalance": [...], "anumpostings": N }
 * to:
 *   { "adata": { "pdperiods": [["date", { "bdincludingsubs": [...], "bdexcludingsubs": [...], "bdnumpostings": N }]], "pdpre": {...} } }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class ParsedLedgerAccount : BaseAccount() {
    var adeclarationinfo: ParsedDeclarationInfo? = null // Added in hledger-web v1.32
    var adata: ParsedAccountData? = null // Added in hledger-web v1.50

    /**
     * Get the number of postings for this account from the new structure.
     * In v1.50+, this is stored in adata.pdperiods[0][1].bdnumpostings
     */
    override var anumpostings: Int
        get() {
            return adata?.getFirstPeriodBalance()?.bdnumpostings ?: 0
        }
        set(value) {
            // No-op, as anumpostings is derived from adata in v1.50+
        }

    override fun getSimpleBalance(): List<SimpleBalance> {
        val result = mutableListOf<SimpleBalance>()

        adata?.let { accountData ->
            accountData.getFirstPeriodBalance()?.let { balanceData ->
                balanceData.bdincludingsubs?.forEach { b ->
                    val style = AmountStyle.fromParsedStyle(b.astyle, b.acommodity)
                    result.add(SimpleBalance(b.acommodity, b.aquantity?.asFloat() ?: 0f, style))
                }
            }
        }

        return result
    }
}
