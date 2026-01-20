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
import net.ktnx.mobileledger.db.Profile

/**
 * Parser for profile data from backup JSON.
 *
 * Handles the parsing of profile entries from the backup file format.
 */
class ProfileBackupParser {

    @Throws(IOException::class)
    fun parse(reader: JsonReader): List<Profile> {
        val list = ArrayList<Profile>()
        reader.beginArray()
        while (reader.peek() == JsonToken.BEGIN_OBJECT) {
            list.add(parseProfile(reader))
        }
        reader.endArray()
        return list
    }

    @Throws(IOException::class)
    private fun parseProfile(reader: JsonReader): Profile {
        val profile = Profile()
        reader.beginObject()
        while (reader.peek() != JsonToken.END_OBJECT) {
            val item = reader.nextName()
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull()
                continue
            }
            when (item) {
                BackupKeys.UUID -> profile.uuid = reader.nextString()
                BackupKeys.NAME -> profile.name = reader.nextString()
                BackupKeys.URL -> profile.url = reader.nextString()
                BackupKeys.USE_AUTH -> profile.useAuthentication = reader.nextBoolean()
                BackupKeys.AUTH_USER -> profile.authUser = reader.nextString()
                BackupKeys.AUTH_PASS -> profile.authPassword = reader.nextString()
                BackupKeys.API_VER -> profile.apiVersion = reader.nextInt()
                BackupKeys.CAN_POST -> profile.permitPosting = reader.nextBoolean()
                BackupKeys.DEFAULT_COMMODITY -> profile.setDefaultCommodity(reader.nextString())
                BackupKeys.SHOW_COMMODITY -> profile.showCommodityByDefault = reader.nextBoolean()
                BackupKeys.SHOW_COMMENTS -> profile.showCommentsByDefault = reader.nextBoolean()
                BackupKeys.FUTURE_DATES -> profile.futureDates = reader.nextInt()
                BackupKeys.PREF_ACCOUNT -> profile.preferredAccountsFilter = reader.nextString()
                BackupKeys.COLOUR -> profile.theme = reader.nextInt()
                else -> throw IllegalStateException("Unexpected profile item: $item")
            }
        }
        reader.endObject()
        return profile
    }
}
