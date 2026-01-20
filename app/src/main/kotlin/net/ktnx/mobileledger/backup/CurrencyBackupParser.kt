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
import net.ktnx.mobileledger.db.Currency

/**
 * Parser for currency/commodity data from backup JSON.
 *
 * Handles the parsing of currency entries from the backup file format.
 */
class CurrencyBackupParser {

    @Throws(IOException::class)
    fun parse(reader: JsonReader): List<Currency> {
        val list = ArrayList<Currency>()
        reader.beginArray()
        while (reader.peek() == JsonToken.BEGIN_OBJECT) {
            list.add(parseCurrency(reader))
        }
        reader.endArray()
        return list
    }

    @Throws(IOException::class)
    private fun parseCurrency(reader: JsonReader): Currency {
        val currency = Currency()
        reader.beginObject()
        while (reader.peek() != JsonToken.END_OBJECT) {
            val item = reader.nextName()
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull()
                continue
            }
            when (item) {
                BackupKeys.NAME -> currency.name = reader.nextString()
                BackupKeys.POSITION -> currency.position = reader.nextString()
                BackupKeys.HAS_GAP -> currency.hasGap = reader.nextBoolean()
                else -> throw RuntimeException("Unknown commodity key: $item")
            }
        }
        reader.endObject()

        if (currency.name.isEmpty()) {
            throw RuntimeException("Missing commodity name")
        }
        return currency
    }
}
