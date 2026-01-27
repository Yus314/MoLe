/*
 * Copyright © 2026 Damyan Ivanov.
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

package net.ktnx.mobileledger.fake

import net.ktnx.mobileledger.core.domain.model.Profile
import net.ktnx.mobileledger.core.domain.model.Transaction
import net.ktnx.mobileledger.domain.usecase.TransactionSender

/**
 * Fake implementation of [TransactionSender] for testing.
 *
 * This fake allows tests to control the success/failure of transaction sends
 * and verify that the correct transactions were sent.
 */
class FakeTransactionSender : TransactionSender {

    var shouldSucceed = true
    var errorMessage = "Simulated failure"

    val sentTransactions = mutableListOf<SentTransaction>()

    /**
     * Record of a sent transaction.
     */
    data class SentTransaction(
        val profile: Profile,
        val transaction: Transaction,
        val simulate: Boolean
    )

    override suspend fun send(profile: Profile, transaction: Transaction, simulate: Boolean): Result<Unit> {
        sentTransactions.add(SentTransaction(profile, transaction, simulate))
        return if (shouldSucceed) {
            Result.success(Unit)
        } else {
            Result.failure(Exception(errorMessage))
        }
    }

    /**
     * Reset the fake to its initial state.
     */
    fun reset() {
        shouldSucceed = true
        errorMessage = "Simulated failure"
        sentTransactions.clear()
    }
}
