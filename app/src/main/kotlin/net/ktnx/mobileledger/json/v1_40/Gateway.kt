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

package net.ktnx.mobileledger.json.v1_40

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import net.ktnx.mobileledger.domain.model.Transaction
import net.ktnx.mobileledger.json.Gateway as BaseGateway

class Gateway : BaseGateway() {
    @Throws(JsonProcessingException::class)
    override fun transactionSaveRequest(transaction: Transaction): String {
        val jsonTransaction = ParsedLedgerTransaction.fromDomain(transaction)
        val mapper = ObjectMapper()
        val writer = mapper.writerFor(ParsedLedgerTransaction::class.java)
        return writer.writeValueAsString(jsonTransaction)
    }
}
