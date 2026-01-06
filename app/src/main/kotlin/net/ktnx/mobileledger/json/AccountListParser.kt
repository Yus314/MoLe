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

import com.fasterxml.jackson.databind.MappingIterator
import net.ktnx.mobileledger.async.RetrieveTransactionsTask
import net.ktnx.mobileledger.model.LedgerAccount
import net.ktnx.mobileledger.utils.Logger.debug
import java.io.IOException
import java.io.InputStream

abstract class AccountListParser {
    protected lateinit var iterator: MappingIterator<ParsedLedgerAccount>

    abstract val apiVersion: API

    open fun nextAccount(
        task: RetrieveTransactionsTask,
        map: HashMap<String, LedgerAccount>
    ): LedgerAccount? {
        if (!iterator.hasNext()) return null

        val next = iterator.next().toLedgerAccount(task, map)

        if (next.name.equals("root", ignoreCase = true)) {
            return nextAccount(task, map)
        }

        debug(
            "accounts",
            String.format("Got account '%s' [%s]", next.name, apiVersion.description)
        )
        return next
    }

    companion object {
        @JvmStatic
        @Throws(IOException::class)
        fun forApiVersion(version: API, input: InputStream): AccountListParser = when (version) {
            API.v1_14 -> net.ktnx.mobileledger.json.v1_14.AccountListParser(input)
            API.v1_15 -> net.ktnx.mobileledger.json.v1_15.AccountListParser(input)
            API.v1_19_1 -> net.ktnx.mobileledger.json.v1_19_1.AccountListParser(input)
            API.v1_23 -> net.ktnx.mobileledger.json.v1_23.AccountListParser(input)
            API.v1_32 -> net.ktnx.mobileledger.json.v1_32.AccountListParser(input)
            API.v1_40 -> net.ktnx.mobileledger.json.v1_40.AccountListParser(input)
            API.v1_50 -> net.ktnx.mobileledger.json.v1_50.AccountListParser(input)
            else -> throw RuntimeException("Unsupported version $version")
        }
    }
}
