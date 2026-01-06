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

package net.ktnx.mobileledger.json

import java.io.IOException
import java.io.InputStream
import java.text.ParseException
import net.ktnx.mobileledger.model.LedgerTransaction

abstract class TransactionListParser {
    @Throws(ParseException::class)
    abstract fun nextTransaction(): LedgerTransaction?

    companion object {
        @JvmStatic
        @Throws(IOException::class)
        fun forApiVersion(apiVersion: API, input: InputStream): TransactionListParser =
            when (apiVersion) {
                API.v1_14 -> net.ktnx.mobileledger.json.v1_14.TransactionListParser(input)
                API.v1_15 -> net.ktnx.mobileledger.json.v1_15.TransactionListParser(input)
                API.v1_19_1 -> net.ktnx.mobileledger.json.v1_19_1.TransactionListParser(input)
                API.v1_23 -> net.ktnx.mobileledger.json.v1_23.TransactionListParser(input)
                API.v1_32 -> net.ktnx.mobileledger.json.v1_32.TransactionListParser(input)
                API.v1_40 -> net.ktnx.mobileledger.json.v1_40.TransactionListParser(input)
                API.v1_50 -> net.ktnx.mobileledger.json.v1_50.TransactionListParser(input)
                else -> throw RuntimeException("Unsupported version $apiVersion")
            }
    }
}
