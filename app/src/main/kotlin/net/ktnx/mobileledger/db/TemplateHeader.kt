/*
 * Copyright © 2022 Damyan Ivanov.
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
import androidx.room.Index
import androidx.room.PrimaryKey
import net.ktnx.mobileledger.utils.Misc
import java.util.UUID

@Entity(
    tableName = "templates",
    indices = [Index(name = "templates_uuid_idx", unique = true, value = ["uuid"])]
)
class TemplateHeader : TemplateBase {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    @ColumnInfo(name = "name")
    var name: String = ""

    @ColumnInfo
    var uuid: String = UUID.randomUUID().toString()

    @ColumnInfo(name = "regular_expression")
    var regularExpression: String = ""

    @ColumnInfo(name = "test_text")
    var testText: String? = null

    @ColumnInfo(name = "transaction_description")
    var transactionDescription: String? = null

    @ColumnInfo(name = "transaction_description_match_group")
    var transactionDescriptionMatchGroup: Int? = null

    @ColumnInfo(name = "transaction_comment")
    var transactionComment: String? = null

    @ColumnInfo(name = "transaction_comment_match_group")
    var transactionCommentMatchGroup: Int? = null

    @ColumnInfo(name = "date_year")
    var dateYear: Int? = null

    @ColumnInfo(name = "date_year_match_group")
    var dateYearMatchGroup: Int? = null

    @ColumnInfo(name = "date_month")
    var dateMonth: Int? = null

    @ColumnInfo(name = "date_month_match_group")
    var dateMonthMatchGroup: Int? = null

    @ColumnInfo(name = "date_day")
    var dateDay: Int? = null

    @ColumnInfo(name = "date_day_match_group")
    var dateDayMatchGroup: Int? = null

    @ColumnInfo(name = "is_fallback")
    var isFallback: Boolean = false

    constructor(id: Long, name: String, regularExpression: String) : super() {
        this.id = id
        this.name = name
        this.regularExpression = regularExpression
        this.uuid = UUID.randomUUID().toString()
    }

    constructor(origin: TemplateHeader) : super() {
        id = origin.id
        name = origin.name
        uuid = origin.uuid
        regularExpression = origin.regularExpression
        testText = origin.testText
        transactionDescription = origin.transactionDescription
        transactionDescriptionMatchGroup = origin.transactionDescriptionMatchGroup
        transactionComment = origin.transactionComment
        transactionCommentMatchGroup = origin.transactionCommentMatchGroup
        dateYear = origin.dateYear
        dateYearMatchGroup = origin.dateYearMatchGroup
        dateMonth = origin.dateMonth
        dateMonthMatchGroup = origin.dateMonthMatchGroup
        dateDay = origin.dateDay
        dateDayMatchGroup = origin.dateDayMatchGroup
        isFallback = origin.isFallback
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is TemplateHeader) return false

        return Misc.equalLongs(id, other.id) &&
                Misc.equalStrings(name, other.name) &&
                Misc.equalStrings(regularExpression, other.regularExpression) &&
                Misc.equalStrings(transactionDescription, other.transactionDescription) &&
                Misc.equalStrings(transactionComment, other.transactionComment) &&
                Misc.equalIntegers(transactionDescriptionMatchGroup, other.transactionDescriptionMatchGroup) &&
                Misc.equalIntegers(transactionCommentMatchGroup, other.transactionCommentMatchGroup) &&
                Misc.equalIntegers(dateDay, other.dateDay) &&
                Misc.equalIntegers(dateDayMatchGroup, other.dateDayMatchGroup) &&
                Misc.equalIntegers(dateMonth, other.dateMonth) &&
                Misc.equalIntegers(dateMonthMatchGroup, other.dateMonthMatchGroup) &&
                Misc.equalIntegers(dateYear, other.dateYear) &&
                Misc.equalIntegers(dateYearMatchGroup, other.dateYearMatchGroup)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + uuid.hashCode()
        return result
    }

    fun createDuplicate(): TemplateHeader {
        val dup = TemplateHeader(this)
        dup.id = 0
        dup.uuid = UUID.randomUUID().toString()
        return dup
    }
}
