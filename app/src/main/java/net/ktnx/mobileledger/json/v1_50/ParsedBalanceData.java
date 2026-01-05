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

package net.ktnx.mobileledger.json.v1_50;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Represents balance data structure in hledger-web 1.50+.
 * Used for both pdperiods entries and pdpre field.
 *
 * JSON structure:
 * {
 *   "bdincludingsubs": [...],
 *   "bdexcludingsubs": [...],
 *   "bdnumpostings": 1
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParsedBalanceData {
    private List<ParsedBalance> bdincludingsubs;
    private List<ParsedBalance> bdexcludingsubs;
    private int bdnumpostings;

    public ParsedBalanceData() {
    }

    public List<ParsedBalance> getBdincludingsubs() {
        return bdincludingsubs;
    }

    public void setBdincludingsubs(List<ParsedBalance> bdincludingsubs) {
        this.bdincludingsubs = bdincludingsubs;
    }

    public List<ParsedBalance> getBdexcludingsubs() {
        return bdexcludingsubs;
    }

    public void setBdexcludingsubs(List<ParsedBalance> bdexcludingsubs) {
        this.bdexcludingsubs = bdexcludingsubs;
    }

    public int getBdnumpostings() {
        return bdnumpostings;
    }

    public void setBdnumpostings(int bdnumpostings) {
        this.bdnumpostings = bdnumpostings;
    }
}
