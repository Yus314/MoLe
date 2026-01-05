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

package net.ktnx.mobileledger.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import net.ktnx.mobileledger.db.TemplateAccount

@Dao
interface TemplateAccountDAO {
    @Insert
    fun insertSync(item: TemplateAccount): Long

    @Update
    fun updateSync(vararg items: TemplateAccount)

    @Delete
    fun deleteSync(item: TemplateAccount)

    @Query("DELETE FROM template_accounts")
    fun deleteAllSync()

    @Query("SELECT * FROM template_accounts WHERE template_id=:templateId")
    fun getTemplateAccounts(templateId: Long?): LiveData<List<TemplateAccount>>

    @Query("SELECT * FROM template_accounts WHERE id = :id")
    fun getPatternAccountById(id: Long?): LiveData<TemplateAccount>

    @Query("UPDATE template_accounts set position=-1 WHERE template_id=:templateId")
    fun prepareForSave(templateId: Long)

    @Query("DELETE FROM template_accounts WHERE position=-1 AND template_id=:templateId")
    fun finishSave(templateId: Long)
}
