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

package net.ktnx.mobileledger.core.network.json.v1_40

import java.io.InputStream
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeToSequence
import net.ktnx.mobileledger.core.domain.model.API
import net.ktnx.mobileledger.core.domain.model.Account
import net.ktnx.mobileledger.core.network.json.AccountListParser as BaseParser
import net.ktnx.mobileledger.core.network.json.MoLeJson
import net.ktnx.mobileledger.core.network.json.unified.UnifiedParsedLedgerAccount

@OptIn(ExperimentalSerializationApi::class)
class AccountListParser(input: InputStream) : BaseParser() {
    override val apiVersion: API = API.v1_40

    private val iterator: Iterator<UnifiedParsedLedgerAccount>

    init {
        iterator = MoLeJson.decodeToSequence<UnifiedParsedLedgerAccount>(input).iterator()
    }

    override fun nextAccountDomain(): Account? {
        if (!iterator.hasNext()) return null

        val parsed = iterator.next()

        if (parsed.aname.equals("root", ignoreCase = true)) {
            return nextAccountDomain()
        }

        val account = parsed.toDomain()
        logAccount(account)
        return account
    }
}
