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

/**
 * Account declaration information (hledger-web v1.32+)
 * Indicates where in the journal file the account was declared
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParsedDeclarationInfo {
    private String file;
    private int line;

    public ParsedDeclarationInfo() {
    }

    public ParsedDeclarationInfo(String file, int line) {
        this.file = file;
        this.line = line;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    @Override
    public String toString() {
        return String.format("%s:%d", file != null ? file : "unknown", line);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ParsedDeclarationInfo that = (ParsedDeclarationInfo) o;

        if (line != that.line) return false;
        return file != null ? file.equals(that.file) : that.file == null;
    }

    @Override
    public int hashCode() {
        int result = file != null ? file.hashCode() : 0;
        result = 31 * result + line;
        return result;
    }
}
