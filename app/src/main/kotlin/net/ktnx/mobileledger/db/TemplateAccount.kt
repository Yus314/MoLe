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
    tableName = "template_accounts",
    indices = [
        Index(name = "fk_template_accounts_template", value = ["template_id"]),
        Index(name = "fk_template_accounts_currency", value = ["currency"])
    ],
    foreignKeys = [
        ForeignKey(
            childColumns = ["template_id"],
            parentColumns = ["id"],
            entity = TemplateHeader::class,
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.RESTRICT
        ),
        ForeignKey(
            childColumns = ["currency"],
            parentColumns = ["id"],
            entity = Currency::class,
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.RESTRICT
        )
    ]
)
class TemplateAccount : TemplateBase {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    @ColumnInfo(name = "template_id")
    var templateId: Long = 0

    @ColumnInfo(name = "acc")
    var accountName: String? = null

    @ColumnInfo(name = "position")
    var position: Long = 0

    @ColumnInfo(name = "acc_match_group")
    var accountNameMatchGroup: Int? = null

    @ColumnInfo
    var currency: Long? = null

    @ColumnInfo(name = "currency_match_group")
    var currencyMatchGroup: Int? = null

    @ColumnInfo(name = "amount")
    var amount: Float? = null

    @ColumnInfo(name = "amount_match_group")
    var amountMatchGroup: Int? = null

    @ColumnInfo(name = "comment")
    var accountComment: String? = null

    @ColumnInfo(name = "comment_match_group")
    var accountCommentMatchGroup: Int? = null

    @ColumnInfo(name = "negate_amount")
    var negateAmount: Boolean? = null

    constructor(id: Long, templateId: Long, position: Long) : super() {
        this.id = id
        this.templateId = templateId
        this.position = position
    }

    constructor(o: TemplateAccount) : super() {
        id = o.id
        templateId = o.templateId
        accountName = o.accountName
        position = o.position
        accountNameMatchGroup = o.accountNameMatchGroup
        currency = o.currency
        currencyMatchGroup = o.currencyMatchGroup
        amount = o.amount
        amountMatchGroup = o.amountMatchGroup
        accountComment = o.accountComment
        accountCommentMatchGroup = o.accountCommentMatchGroup
        negateAmount = o.negateAmount
    }

    fun setPosition(position: Int) {
        this.position = position.toLong()
    }

    fun createDuplicate(header: TemplateHeader): TemplateAccount {
        val dup = TemplateAccount(this)
        dup.id = 0
        dup.templateId = header.id
        return dup
    }
}
