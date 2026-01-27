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

package net.ktnx.mobileledger.core.network.json.v1_50

import java.io.InputStream
import java.text.ParseException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeToSequence
import net.ktnx.mobileledger.core.domain.model.Transaction
import net.ktnx.mobileledger.core.network.json.MoLeJson
import net.ktnx.mobileledger.core.network.json.TransactionListParser as BaseParser
import net.ktnx.mobileledger.core.network.json.unified.UnifiedParsedLedgerTransaction

@OptIn(ExperimentalSerializationApi::class)
class TransactionListParser(input: InputStream) : BaseParser() {
    private val iterator: Iterator<UnifiedParsedLedgerTransaction>

    init {
        iterator = MoLeJson.decodeToSequence<UnifiedParsedLedgerTransaction>(input).iterator()
    }

    @Throws(ParseException::class)
    override fun nextTransactionDomain(): Transaction? = if (iterator.hasNext()) iterator.next().toDomain() else null
}
