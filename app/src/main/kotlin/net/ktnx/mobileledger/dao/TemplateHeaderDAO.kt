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

package net.ktnx.mobileledger.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import net.ktnx.mobileledger.db.TemplateHeader
import net.ktnx.mobileledger.db.TemplateWithAccounts

@Dao
abstract class TemplateHeaderDAO {
    @Insert
    abstract fun insertSync(item: TemplateHeader): Long

    @Update
    abstract fun updateSync(vararg items: TemplateHeader)

    @Delete
    abstract fun deleteSync(item: TemplateHeader)

    @Query("DELETE FROM templates")
    abstract fun deleteAllSync()

    @Query("SELECT * FROM templates ORDER BY is_fallback, UPPER(name)")
    abstract fun getTemplates(): Flow<List<TemplateHeader>>

    @Query("SELECT * FROM templates WHERE id = :id")
    abstract fun getTemplate(id: Long?): Flow<TemplateHeader>

    @Query("SELECT * FROM templates WHERE id = :id")
    abstract fun getTemplateSync(id: Long?): TemplateHeader?

    @Transaction
    @Query("SELECT * FROM templates WHERE id = :id")
    abstract fun getTemplateWithAccounts(id: Long): Flow<TemplateWithAccounts>

    @Transaction
    @Query("SELECT * FROM templates WHERE id = :id")
    abstract fun getTemplateWithAccountsSync(id: Long): TemplateWithAccounts?

    @Transaction
    @Query("SELECT * FROM templates WHERE uuid = :uuid")
    abstract fun getTemplateWithAccountsByUuidSync(uuid: String?): TemplateWithAccounts?

    @Transaction
    @Query("SELECT * FROM templates")
    abstract fun getAllTemplatesWithAccountsSync(): List<TemplateWithAccounts>

    @Transaction
    @Query("SELECT * FROM templates ORDER BY is_fallback, UPPER(name)")
    abstract fun getTemplatesWithAccounts(): Flow<List<TemplateWithAccounts>>
}
