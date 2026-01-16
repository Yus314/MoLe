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
import java.io.IOException
import java.io.InputStream
import logcat.logcat
import net.ktnx.mobileledger.domain.model.Account
import net.ktnx.mobileledger.model.LedgerAccount

abstract class AccountListParser {
    protected lateinit var iterator: MappingIterator<ParsedLedgerAccount>

    abstract val apiVersion: API

    /**
     * Parse the next account from the JSON stream.
     *
     * @param map Map of account names to LedgerAccount instances for parent lookup
     * @return The next LedgerAccount or null if no more accounts
     */
    @Deprecated(
        "Use nextAccountDomain() instead",
        ReplaceWith("nextAccountDomain()")
    )
    open fun nextAccount(map: HashMap<String, LedgerAccount>): LedgerAccount? {
        if (!iterator.hasNext()) return null

        val next = iterator.next().toLedgerAccount(map)

        if (next.name.equals("root", ignoreCase = true)) {
            return nextAccount(map)
        }

        logcat { "Got account '${next.name}' [${apiVersion.description}]" }
        return next
    }

    /**
     * Parse the next account as a domain model from the JSON stream.
     *
     * Note: This does NOT create parent accounts. Parent account creation
     * should be handled by the caller (e.g., TransactionSyncerImpl).
     *
     * @return The next Account or null if no more accounts
     */
    open fun nextAccountDomain(): Account? {
        if (!iterator.hasNext()) return null

        val parsed = iterator.next()

        if (parsed.aname.equals("root", ignoreCase = true)) {
            return nextAccountDomain()
        }

        val account = parsed.toDomain()
        logcat { "Got account '${account.name}' [${apiVersion.description}]" }
        return account
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
