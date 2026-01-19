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
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import net.ktnx.mobileledger.db.Profile

@Dao
abstract class ProfileDAO : BaseDAO<Profile>() {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract override fun insertSync(item: Profile): Long

    @Transaction
    open fun insertLastSync(item: Profile): Long {
        val count = getProfileCountSync()
        item.orderNo = count + 1
        return insertSync(item)
    }

    @Update
    abstract override fun updateSync(item: Profile)

    @Delete
    abstract override fun deleteSync(item: Profile)

    @Query("DELETE FROM profiles")
    abstract fun deleteAllSync()

    @Query("select * from profiles where id = :profileId")
    abstract fun getByIdSync(profileId: Long): Profile?

    @Query("SELECT * FROM profiles WHERE id=:profileId")
    abstract fun getById(profileId: Long): Flow<Profile>

    @Query("SELECT * FROM profiles ORDER BY order_no")
    abstract fun getAllOrderedSync(): List<Profile>

    @Query("SELECT * FROM profiles ORDER BY order_no")
    abstract fun getAllOrdered(): Flow<List<Profile>>

    @Query("SELECT * FROM profiles LIMIT 1")
    abstract fun getAnySync(): Profile?

    @Query("SELECT * FROM profiles WHERE uuid=:uuid")
    abstract fun getByUuid(uuid: String?): Flow<Profile>

    @Query("SELECT * FROM profiles WHERE uuid=:uuid")
    abstract fun getByUuidSync(uuid: String?): Profile?

    @Query("SELECT MAX(order_no) FROM profiles")
    abstract fun getProfileCountSync(): Int

    fun updateOrderSync(list: List<Profile>?) {
        val profileList = list ?: getAllOrderedSync()
        var order = 1
        for (p in profileList) {
            p.orderNo = order++
            updateSync(p)
        }
    }
}
