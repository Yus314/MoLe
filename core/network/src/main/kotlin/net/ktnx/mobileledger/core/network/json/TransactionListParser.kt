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

package net.ktnx.mobileledger.core.network.json

import java.io.IOException
import java.io.InputStream
import java.text.ParseException
import net.ktnx.mobileledger.core.domain.model.API
import net.ktnx.mobileledger.core.domain.model.Transaction

abstract class TransactionListParser {
    @Throws(ParseException::class)
    abstract fun nextTransactionDomain(): Transaction?

    companion object {
        @JvmStatic
        @Throws(IOException::class)
        fun forApiVersion(apiVersion: API, input: InputStream): TransactionListParser = when (apiVersion) {
            API.v1_32 -> net.ktnx.mobileledger.core.network.json.v1_32.TransactionListParser(input)

            API.v1_40 -> net.ktnx.mobileledger.core.network.json.v1_40.TransactionListParser(input)

            API.v1_50 -> net.ktnx.mobileledger.core.network.json.v1_50.TransactionListParser(input)

            API.auto -> throw RuntimeException(
                "Cannot create TransactionListParser for auto API version - resolve to specific version first"
            )
        }
    }
}
