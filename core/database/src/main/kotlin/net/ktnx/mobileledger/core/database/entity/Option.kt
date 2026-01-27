/*
 * Copyright © 2021 Damyan Ivanov.
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

package net.ktnx.mobileledger.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "options", primaryKeys = ["profile_id", "name"])
class Option(
    @ColumnInfo(name = "profile_id")
    var profileId: Long,
    @ColumnInfo
    var name: String,
    @ColumnInfo
    var value: String?
) {
    companion object {
        const val OPT_LAST_SCRAPE = "last_scrape"
    }

    override fun toString(): String = name
}
