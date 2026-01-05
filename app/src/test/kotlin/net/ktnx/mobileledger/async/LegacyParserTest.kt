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

package net.ktnx.mobileledger.async

import net.ktnx.mobileledger.model.LedgerTransactionAccount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LegacyParserTest {

    private fun expectParsedData(
        input: String,
        accountName: String,
        amount: Float,
        currency: String?,
        comment: String?
    ) {
        val lta: LedgerTransactionAccount? = RetrieveTransactionsTask.parseTransactionAccountLine(input)

        assertNotNull(lta)
        assertEquals(accountName, lta!!.accountName)
        assertEquals(amount, lta.amount)
        assertEquals(currency, lta.currency)
        assertEquals(comment, lta.comment)
    }

    private fun expectNotParsed(input: String) {
        assertNull(RetrieveTransactionsTask.parseTransactionAccountLine(input))
    }

    @Test
    fun parseTransactionAccountLine() {
        expectParsedData(" acc:name  -34.56", "acc:name", -34.56f, null, null)
        expectParsedData(" acc:name3  34.56", "acc:name3", 34.56f, null, null)
        expectParsedData(" acc:name  +34.56", "acc:name", 34.56f, null, null)

        expectParsedData(" acc:name  \$-34.56", "acc:name", -34.56f, "\$", null)
        expectParsedData(" acc:name  \$ -34.56", "acc:name", -34.56f, "\$", null)
        expectParsedData(" acc:name  -34.56\$", "acc:name", -34.56f, "\$", null)
        expectParsedData(" acc:name  -34.56 \$", "acc:name", -34.56f, "\$", null)

        expectParsedData(" acc:name  AU\$-34.56", "acc:name", -34.56f, "AU\$", null)
        expectParsedData(" acc:name  AU\$ -34.56", "acc:name", -34.56f, "AU\$", null)
        expectParsedData(" acc:name  -34.56AU\$", "acc:name", -34.56f, "AU\$", null)
        expectParsedData(" acc:name  -34.56 AU\$", "acc:name", -34.56f, "AU\$", null)
    }
}
