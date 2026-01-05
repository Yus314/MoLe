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

package net.ktnx.mobileledger.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "accounts",
    indices = [
        Index(name = "un_account_name", unique = true, value = ["profile_id", "name"]),
        Index(name = "fk_account_profile", value = ["profile_id"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Profile::class,
            parentColumns = ["id"],
            childColumns = ["profile_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.RESTRICT
        )
    ]
)
class Account {
    @ColumnInfo
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    @ColumnInfo(name = "profile_id")
    var profileId: Long = 0

    @ColumnInfo
    var level: Int = 0

    @ColumnInfo
    var name: String = ""

    @ColumnInfo(name = "name_upper")
    var nameUpper: String = ""

    @ColumnInfo(name = "parent_name")
    var parentName: String? = null

    @ColumnInfo(defaultValue = "1")
    var expanded: Boolean = true

    @ColumnInfo(name = "amounts_expanded", defaultValue = "0")
    var amountsExpanded: Boolean = false

    @ColumnInfo(defaultValue = "0")
    var generation: Long = 0

    @Ignore
    fun isExpanded(): Boolean = expanded

    @Ignore
    fun isAmountsExpanded(): Boolean = amountsExpanded

    override fun toString(): String = name
}
