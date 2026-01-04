/*
 * Copyright © 2020 Damyan Ivanov.
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

package net.ktnx.mobileledger.json.v1_40;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ParsedStyle extends net.ktnx.mobileledger.json.ParsedStyle {
    private int asprecision;
    private String asdecimalmark = ".";
    private String asrounding = "NoRounding";
    public ParsedStyle() {
    }
    public int getAsprecision() {
        return asprecision;
    }
    public void setAsprecision(int asprecision) {
        this.asprecision = asprecision;
    }
    public String getAsdecimalmark() {
        return asdecimalmark;
    }
    public void setAsdecimalmark(String asdecimalmark) {
        this.asdecimalmark = asdecimalmark;
    }
    public String getAsrounding() {
        return asrounding;
    }
    public void setAsrounding(String asrounding) {
        this.asrounding = asrounding;
    }
}
