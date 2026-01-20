/*
 * Copyright © 2024 Damyan Ivanov.
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

package net.ktnx.mobileledger.backup

import android.util.JsonReader
import android.util.JsonToken
import java.io.IOException
import net.ktnx.mobileledger.db.TemplateAccount
import net.ktnx.mobileledger.db.TemplateHeader
import net.ktnx.mobileledger.db.TemplateWithAccounts

/**
 * Parser for template data from backup JSON.
 *
 * Handles the parsing of template entries (header + accounts) from the backup file format.
 */
class TemplateBackupParser {

    @Throws(IOException::class)
    fun parse(reader: JsonReader): List<TemplateWithAccounts> {
        val list = ArrayList<TemplateWithAccounts>()
        reader.beginArray()
        while (reader.peek() == JsonToken.BEGIN_OBJECT) {
            list.add(parseTemplate(reader))
        }
        reader.endArray()
        return list
    }

    @Throws(IOException::class)
    private fun parseTemplate(reader: JsonReader): TemplateWithAccounts {
        reader.beginObject()
        val header = TemplateHeader(0L, "", "")
        val accounts = ArrayList<TemplateAccount>()

        while (reader.peek() != JsonToken.END_OBJECT) {
            val item = reader.nextName()
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull()
                continue
            }
            when (item) {
                BackupKeys.UUID -> header.uuid = reader.nextString()

                BackupKeys.NAME -> header.name = reader.nextString()

                BackupKeys.REGEX -> header.regularExpression = reader.nextString()

                BackupKeys.TEST_TEXT -> header.testText = reader.nextString()

                BackupKeys.DATE_YEAR -> header.dateYear = reader.nextInt()

                BackupKeys.DATE_YEAR_GROUP -> header.dateYearMatchGroup = reader.nextInt()

                BackupKeys.DATE_MONTH -> header.dateMonth = reader.nextInt()

                BackupKeys.DATE_MONTH_GROUP -> header.dateMonthMatchGroup = reader.nextInt()

                BackupKeys.DATE_DAY -> header.dateDay = reader.nextInt()

                BackupKeys.DATE_DAY_GROUP -> header.dateDayMatchGroup = reader.nextInt()

                BackupKeys.TRANSACTION -> header.transactionDescription = reader.nextString()

                BackupKeys.TRANSACTION_GROUP -> header.transactionDescriptionMatchGroup = reader.nextInt()

                BackupKeys.COMMENT -> header.transactionComment = reader.nextString()

                BackupKeys.COMMENT_GROUP -> header.transactionCommentMatchGroup = reader.nextInt()

                BackupKeys.IS_FALLBACK -> header.isFallback = reader.nextBoolean()

                BackupKeys.ACCOUNTS -> {
                    reader.beginArray()
                    while (reader.peek() == JsonToken.BEGIN_OBJECT) {
                        accounts.add(parseTemplateAccount(reader))
                    }
                    reader.endArray()
                }

                else -> throw RuntimeException("Unknown template header item: $item")
            }
        }
        reader.endObject()

        return TemplateWithAccounts().apply {
            this.header = header
            this.accounts = accounts
        }
    }

    @Throws(IOException::class)
    private fun parseTemplateAccount(reader: JsonReader): TemplateAccount {
        reader.beginObject()
        val account = TemplateAccount(0L, 0L, 0L)
        while (reader.peek() != JsonToken.END_OBJECT) {
            val item = reader.nextName()
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull()
                continue
            }
            when (item) {
                BackupKeys.NAME -> account.accountName = reader.nextString()
                BackupKeys.NAME_GROUP -> account.accountNameMatchGroup = reader.nextInt()
                BackupKeys.COMMENT -> account.accountComment = reader.nextString()
                BackupKeys.COMMENT_GROUP -> account.accountCommentMatchGroup = reader.nextInt()
                BackupKeys.AMOUNT -> account.amount = reader.nextDouble().toFloat()
                BackupKeys.AMOUNT_GROUP -> account.amountMatchGroup = reader.nextInt()
                BackupKeys.NEGATE_AMOUNT -> account.negateAmount = reader.nextBoolean()
                BackupKeys.CURRENCY -> account.currency = reader.nextLong()
                BackupKeys.CURRENCY_GROUP -> account.currencyMatchGroup = reader.nextInt()
                else -> throw IllegalStateException("Unexpected template account item: $item")
            }
        }
        reader.endObject()
        return account
    }
}
