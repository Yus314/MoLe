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
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Profile::class,
            parentColumns = ["id"],
            childColumns = ["profile_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(name = "un_transactions_ledger_id", unique = true, value = ["profile_id", "ledger_id"]),
        Index(name = "idx_transaction_description", value = ["description"]),
        Index(name = "fk_transaction_profile", value = ["profile_id"])
    ]
)
class Transaction {
    @ColumnInfo
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    @ColumnInfo(name = "ledger_id")
    var ledgerId: Long = 0

    @ColumnInfo(name = "profile_id")
    var profileId: Long = 0

    @ColumnInfo(name = "data_hash")
    var dataHash: String = ""

    @ColumnInfo
    var year: Int = 0

    @ColumnInfo
    var month: Int = 0

    @ColumnInfo
    var day: Int = 0

    @ColumnInfo(name = "description", collate = ColumnInfo.NOCASE)
    var description: String = ""
        set(value) {
            field = value
            descriptionUpper = value.uppercase()
        }

    @ColumnInfo(name = "description_uc")
    var descriptionUpper: String = ""

    @ColumnInfo
    var comment: String? = null

    @ColumnInfo
    var generation: Long = 0

    fun copyDataFrom(o: Transaction) {
        ledgerId = o.ledgerId
        profileId = o.profileId
        dataHash = o.dataHash
        year = o.year
        month = o.month
        day = o.day
        description = o.description
        descriptionUpper = o.description.uppercase()
        comment = o.comment
        generation = o.generation
    }
}
