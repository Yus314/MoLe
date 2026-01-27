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

import androidx.room.Embedded
import androidx.room.Relation

class TemplateWithAccounts {
    @Embedded
    lateinit var header: TemplateHeader

    @Relation(parentColumn = "id", entityColumn = "template_id")
    lateinit var accounts: List<TemplateAccount>

    val id: Long
        get() = header.id

    fun createDuplicate(): TemplateWithAccounts {
        val result = TemplateWithAccounts()
        result.header = header.createDuplicate()
        result.accounts = accounts.map { it.createDuplicate(result.header) }
        return result
    }

    companion object {
        @JvmStatic
        fun from(o: TemplateWithAccounts): TemplateWithAccounts {
            val result = TemplateWithAccounts()
            result.header = TemplateHeader(o.header)
            result.accounts = o.accounts.map { TemplateAccount(it) }
            return result
        }
    }
}
