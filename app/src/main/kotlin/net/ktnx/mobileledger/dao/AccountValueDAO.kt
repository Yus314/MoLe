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

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import net.ktnx.mobileledger.db.AccountValue

@Dao
abstract class AccountValueDAO : BaseDAO<AccountValue>() {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract override fun insertSync(item: AccountValue): Long

    @Update
    abstract override fun updateSync(item: AccountValue)

    @Delete
    abstract override fun deleteSync(item: AccountValue)

    @Query("DELETE FROM account_values")
    abstract fun deleteAllSync()

    @Query("SELECT * FROM account_values WHERE account_id=:accountId")
    abstract fun getAll(accountId: Long): Flow<List<AccountValue>>

    @Query("SELECT * FROM account_values WHERE account_id = :accountId AND currency = :currency")
    abstract fun getByCurrencySync(accountId: Long, currency: String): AccountValue?
}
