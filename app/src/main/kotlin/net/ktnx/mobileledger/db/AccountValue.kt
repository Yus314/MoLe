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
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "account_values",
    indices = [
        Index(name = "un_account_values", unique = true, value = ["account_id", "currency"]),
        Index(name = "fk_account_value_acc", value = ["account_id"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.RESTRICT
        )
    ]
)
class AccountValue {
    @ColumnInfo
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    @ColumnInfo(name = "account_id")
    var accountId: Long = 0

    @ColumnInfo(defaultValue = "")
    var currency: String = ""

    @ColumnInfo
    var value: Float = 0f

    @ColumnInfo(defaultValue = "0")
    var generation: Long = 0

    @ColumnInfo(name = "amount_style")
    var amountStyle: String? = null
}
