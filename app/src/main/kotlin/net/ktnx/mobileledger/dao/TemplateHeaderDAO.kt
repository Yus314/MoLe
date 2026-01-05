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

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import net.ktnx.mobileledger.db.DB
import net.ktnx.mobileledger.db.TemplateAccount
import net.ktnx.mobileledger.db.TemplateHeader
import net.ktnx.mobileledger.db.TemplateWithAccounts
import net.ktnx.mobileledger.utils.Misc

@Dao
abstract class TemplateHeaderDAO {
    @Insert
    abstract fun insertSync(item: TemplateHeader): Long

    fun insertAsync(item: TemplateHeader, callback: Runnable?) {
        BaseDAO.runAsync {
            insertSync(item)
            callback?.let { Misc.onMainThread(it) }
        }
    }

    @Update
    abstract fun updateSync(vararg items: TemplateHeader)

    @Delete
    abstract fun deleteSync(item: TemplateHeader)

    fun deleteAsync(item: TemplateHeader, callback: Runnable) {
        BaseDAO.runAsync {
            deleteSync(item)
            Misc.onMainThread(callback)
        }
    }

    @Query("DELETE FROM templates")
    abstract fun deleteAllSync()

    @Query("SELECT * FROM templates ORDER BY is_fallback, UPPER(name)")
    abstract fun getTemplates(): LiveData<List<TemplateHeader>>

    @Query("SELECT * FROM templates WHERE id = :id")
    abstract fun getTemplate(id: Long?): LiveData<TemplateHeader>

    @Query("SELECT * FROM templates WHERE id = :id")
    abstract fun getTemplateSync(id: Long?): TemplateHeader?

    fun getTemplateAsync(id: Long, callback: AsyncResultCallback<TemplateHeader>) {
        val resultReceiver = getTemplate(id)
        resultReceiver.observeForever(object : Observer<TemplateHeader> {
            override fun onChanged(value: TemplateHeader) {
                resultReceiver.removeObserver(this)
                callback.onResult(value)
            }
        })
    }

    @Transaction
    @Query("SELECT * FROM templates WHERE id = :id")
    abstract fun getTemplateWithAccounts(id: Long): LiveData<TemplateWithAccounts>

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
    open fun insertSync(templateWithAccounts: TemplateWithAccounts) {
        val templateId = insertSync(templateWithAccounts.header)
        for (acc in templateWithAccounts.accounts) {
            acc.templateId = templateId
            DB.get().templateAccountDAO.insertSync(acc)
        }
    }

    fun getTemplateWithAccountsAsync(id: Long, callback: AsyncResultCallback<TemplateWithAccounts>) {
        val resultReceiver = getTemplateWithAccounts(id)
        resultReceiver.observeForever(object : Observer<TemplateWithAccounts> {
            override fun onChanged(value: TemplateWithAccounts) {
                resultReceiver.removeObserver(this)
                callback.onResult(value)
            }
        })
    }

    fun insertAsync(item: TemplateWithAccounts, callback: Runnable?) {
        BaseDAO.runAsync {
            insertSync(item)
            callback?.let { Misc.onMainThread(it) }
        }
    }

    fun duplicateTemplateWithAccounts(id: Long, callback: AsyncResultCallback<TemplateWithAccounts>?) {
        BaseDAO.runAsync {
            val src = getTemplateWithAccountsSync(id) ?: return@runAsync
            val dup = src.createDuplicate()
            dup.header.name = dup.header.name
            dup.header.id = insertSync(dup.header)
            val accDao = DB.get().templateAccountDAO
            for (dupAcc in dup.accounts) {
                dupAcc.templateId = dup.header.id
                dupAcc.id = accDao.insertSync(dupAcc)
            }
            callback?.let { Misc.onMainThread { it.onResult(dup) } }
        }
    }
}
