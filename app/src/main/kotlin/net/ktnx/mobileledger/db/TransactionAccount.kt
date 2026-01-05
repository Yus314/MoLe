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
import net.ktnx.mobileledger.utils.Misc

@Entity(
    tableName = "transaction_accounts",
    foreignKeys = [
        ForeignKey(
            entity = Transaction::class,
            parentColumns = ["id"],
            childColumns = ["transaction_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(name = "fk_trans_acc_trans", value = ["transaction_id"]),
        Index(name = "un_transaction_accounts", unique = true, value = ["transaction_id", "order_no"])
    ]
)
class TransactionAccount {
    @ColumnInfo
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    @ColumnInfo(name = "transaction_id")
    var transactionId: Long = 0

    @ColumnInfo(name = "order_no")
    var orderNo: Int = 0

    @ColumnInfo(name = "account_name")
    var accountName: String = ""

    @ColumnInfo(defaultValue = "")
    var currency: String = ""

    @ColumnInfo
    var amount: Float = 0f

    @ColumnInfo
    var comment: String? = null

    @ColumnInfo(name = "amount_style")
    var amountStyle: String? = null

    @ColumnInfo(defaultValue = "0")
    var generation: Long = 0

    fun copyDataFrom(o: TransactionAccount) {
        transactionId = o.transactionId
        orderNo = o.orderNo
        accountName = o.accountName
        currency = Misc.nullIsEmpty(o.currency)
        amount = o.amount
        comment = o.comment
        amountStyle = o.amountStyle
        generation = o.generation
    }
}
