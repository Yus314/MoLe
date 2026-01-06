/*
 * Copyright © 2021, 2024 Damyan Ivanov.
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

import com.fasterxml.jackson.core.JsonProcessingException
import net.ktnx.mobileledger.model.LedgerTransaction

abstract class Gateway {
    @Throws(JsonProcessingException::class)
    abstract fun transactionSaveRequest(ledgerTransaction: LedgerTransaction): String

    companion object {
        @JvmStatic
        fun forApiVersion(apiVersion: API): Gateway = when (apiVersion) {
            API.v1_14 -> net.ktnx.mobileledger.json.v1_14.Gateway()
            API.v1_15 -> net.ktnx.mobileledger.json.v1_15.Gateway()
            API.v1_19_1 -> net.ktnx.mobileledger.json.v1_19_1.Gateway()
            API.v1_23 -> net.ktnx.mobileledger.json.v1_23.Gateway()
            API.v1_32 -> net.ktnx.mobileledger.json.v1_32.Gateway()
            API.v1_40 -> net.ktnx.mobileledger.json.v1_40.Gateway()
            API.v1_50 -> net.ktnx.mobileledger.json.v1_50.Gateway()
            else -> throw RuntimeException(
                "JSON API version $apiVersion save implementation missing"
            )
        }
    }
}
