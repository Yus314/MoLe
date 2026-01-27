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
import net.ktnx.mobileledger.core.database.entity.TransactionAccount

@Dao
abstract class TransactionAccountDAO : BaseDAO<TransactionAccount>() {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract override fun insertSync(item: TransactionAccount): Long

    @Update
    abstract override fun updateSync(item: TransactionAccount)

    @Delete
    abstract override fun deleteSync(item: TransactionAccount)

    @Delete
    abstract fun deleteSync(items: List<TransactionAccount>)

    @Query("DELETE FROM transaction_accounts")
    abstract fun deleteAllSync()

    @Query("SELECT * FROM transaction_accounts WHERE id = :id")
    abstract fun getById(id: Long): Flow<TransactionAccount>

    @Query("SELECT * FROM transaction_accounts WHERE transaction_id = :transactionId AND order_no = :orderNo")
    abstract fun getByOrderNoSync(transactionId: Long, orderNo: Int): TransactionAccount?
}
