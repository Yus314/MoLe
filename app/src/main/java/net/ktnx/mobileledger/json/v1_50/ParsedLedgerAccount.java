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

package net.ktnx.mobileledger.json.v1_50;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import net.ktnx.mobileledger.model.AmountStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * Parser for hledger-web 1.50+ account JSON structure.
 *
 * In v1.50+, the account structure changed from:
 *   { "aibalance": [...], "aebalance": [...], "anumpostings": N }
 * to:
 *   { "adata": { "pdperiods": [["date", { "bdincludingsubs": [...], "bdexcludingsubs": [...], "bdnumpostings": N }]], "pdpre": {...} } }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParsedLedgerAccount extends net.ktnx.mobileledger.json.ParsedLedgerAccount {
    private ParsedDeclarationInfo adeclarationinfo;  // Added in hledger-web v1.32
    private ParsedAccountData adata;  // Added in hledger-web v1.50

    public ParsedLedgerAccount() {
    }

    public ParsedDeclarationInfo getAdeclarationinfo() {
        return adeclarationinfo;
    }

    public void setAdeclarationinfo(ParsedDeclarationInfo adeclarationinfo) {
        this.adeclarationinfo = adeclarationinfo;
    }

    public ParsedAccountData getAdata() {
        return adata;
    }

    public void setAdata(ParsedAccountData adata) {
        this.adata = adata;
    }

    /**
     * Get the number of postings for this account from the new structure.
     * In v1.50+, this is stored in adata.pdperiods[0][1].bdnumpostings
     */
    @Override
    public int getAnumpostings() {
        if (adata != null) {
            ParsedBalanceData balanceData = adata.getFirstPeriodBalance();
            if (balanceData != null) {
                return balanceData.getBdnumpostings();
            }
        }
        return 0;
    }

    @Override
    public List<SimpleBalance> getSimpleBalance() {
        List<SimpleBalance> result = new ArrayList<SimpleBalance>();

        if (adata != null) {
            ParsedBalanceData balanceData = adata.getFirstPeriodBalance();
            if (balanceData != null) {
                List<ParsedBalance> balances = balanceData.getBdincludingsubs();
                if (balances != null) {
                    for (ParsedBalance b : balances) {
                        AmountStyle style = AmountStyle.fromParsedStyle(b.getAstyle(), b.getAcommodity());
                        result.add(new SimpleBalance(b.getAcommodity(), b.getAquantity()
                                                                         .asFloat(), style));
                    }
                }
            }
        }

        return result;
    }
}
