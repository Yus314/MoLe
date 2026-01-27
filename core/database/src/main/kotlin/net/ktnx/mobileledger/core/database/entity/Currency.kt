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
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "currencies",
    indices = [Index(name = "currency_name_idx", unique = true, value = ["name"])]
)
class Currency {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    var name: String = ""

    var position: String = "after"

    @ColumnInfo(name = "has_gap")
    var hasGap: Boolean = true

    @Ignore
    constructor() {
        id = 0
        name = ""
        position = "after"
        hasGap = true
    }

    constructor(id: Long, name: String, position: String, hasGap: Boolean) {
        this.id = id
        this.name = name
        this.position = position
        this.hasGap = hasGap
    }
}
