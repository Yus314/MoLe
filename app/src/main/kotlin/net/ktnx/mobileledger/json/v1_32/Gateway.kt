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

package net.ktnx.mobileledger.json.v1_32

import kotlinx.serialization.SerializationException
import net.ktnx.mobileledger.core.domain.model.CurrencySettings
import net.ktnx.mobileledger.core.domain.model.Transaction
import net.ktnx.mobileledger.json.Gateway as BaseGateway
import net.ktnx.mobileledger.json.MoLeJson
import net.ktnx.mobileledger.json.config.ApiVersionConfig
import net.ktnx.mobileledger.json.unified.UnifiedParsedLedgerTransaction

class Gateway : BaseGateway() {
    @Throws(SerializationException::class)
    override fun transactionSaveRequest(transaction: Transaction, settings: CurrencySettings): String {
        val jsonTransaction = UnifiedParsedLedgerTransaction.fromDomain(
            transaction,
            ApiVersionConfig.V1_32_40,
            settings
        )
        return MoLeJson.encodeToString(UnifiedParsedLedgerTransaction.serializer(), jsonTransaction)
    }
}
