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

package net.ktnx.mobileledger.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import net.ktnx.mobileledger.core.database.entity.Currency

@Dao
abstract class CurrencyDAO : BaseDAO<Currency>() {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract override fun insertSync(item: Currency): Long

    @Update
    abstract override fun updateSync(item: Currency)

    @Delete
    abstract override fun deleteSync(item: Currency)

    @Query("DELETE FROM currencies")
    abstract fun deleteAllSync()

    @Query("SELECT * FROM currencies")
    abstract fun getAll(): Flow<List<Currency>>

    @Query("SELECT * FROM currencies")
    abstract fun getAllSync(): List<Currency>

    @Query("SELECT * FROM currencies WHERE id = :id")
    abstract fun getById(id: Long): Flow<Currency>

    @Query("SELECT * FROM currencies WHERE id = :id")
    abstract fun getByIdSync(id: Long): Currency?

    @Query("SELECT * FROM currencies WHERE name = :name")
    abstract fun getByName(name: String?): Flow<Currency>

    @Query("SELECT * FROM currencies WHERE name = :name")
    abstract fun getByNameSync(name: String?): Currency?
}
