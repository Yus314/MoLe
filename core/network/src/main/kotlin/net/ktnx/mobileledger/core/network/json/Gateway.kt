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

package net.ktnx.mobileledger.core.network.json

import kotlinx.serialization.SerializationException
import net.ktnx.mobileledger.core.domain.model.CurrencySettings
import net.ktnx.mobileledger.core.domain.model.Transaction

abstract class Gateway {
    /**
     * Create JSON save request from domain Transaction model.
     * Subclasses should override this to provide version-specific serialization.
     *
     * @param transaction Transaction to serialize
     * @param settings Currency formatting settings (optional, defaults to [CurrencySettings.DEFAULT])
     * @return JSON string for the API request
     */
    @Throws(SerializationException::class)
    abstract fun transactionSaveRequest(
        transaction: Transaction,
        settings: CurrencySettings = CurrencySettings.DEFAULT
    ): String

    companion object {
        @JvmStatic
        fun forApiVersion(apiVersion: API): Gateway = when (apiVersion) {
            API.v1_32 -> net.ktnx.mobileledger.core.network.json.v1_32.Gateway()

            API.v1_40 -> net.ktnx.mobileledger.core.network.json.v1_40.Gateway()

            API.v1_50 -> net.ktnx.mobileledger.core.network.json.v1_50.Gateway()

            API.auto -> throw RuntimeException(
                "Cannot create Gateway for auto API version - resolve to specific version first"
            )
        }
    }
}
