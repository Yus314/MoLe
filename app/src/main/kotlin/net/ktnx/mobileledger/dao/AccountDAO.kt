/*
 * Copyright © 2024 Damyan Ivanov.
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
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import net.ktnx.mobileledger.db.Account
import net.ktnx.mobileledger.db.AccountWithAmounts
import net.ktnx.mobileledger.db.DB

@Dao
abstract class AccountDAO : BaseDAO<Account>() {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract override fun insertSync(item: Account): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertSync(items: List<Account>)

    @Transaction
    open fun insertSync(accountWithAmounts: AccountWithAmounts) {
        val valueDAO = DB.get().getAccountValueDAO()
        val account = accountWithAmounts.account
        account.id = insertSync(account)
        for (value in accountWithAmounts.amounts) {
            value.accountId = account.id
            value.generation = account.generation
            value.id = valueDAO.insertSync(value)
        }
    }

    @Update
    abstract override fun updateSync(item: Account)

    @Delete
    abstract override fun deleteSync(item: Account)

    @Delete
    abstract fun deleteSync(items: List<Account>)

    @Query("DELETE FROM accounts")
    abstract fun deleteAllSync()

    @Query(
        "SELECT * FROM accounts WHERE profile_id=:profileId AND IIF(:includeZeroBalances=1, 1," +
           " (EXISTS(SELECT 1 FROM account_values av WHERE av.account_id=accounts.id AND av.value" +
           " <> 0) OR EXISTS(SELECT 1 FROM accounts a WHERE a.parent_name = accounts.name))) " +
           "ORDER BY name"
    )
    abstract fun getAll(profileId: Long, includeZeroBalances: Boolean): LiveData<List<Account>>

    @Transaction
    @Query(
        "SELECT * FROM accounts WHERE profile_id = :profileId AND IIF(:includeZeroBalances=1, " +
           "1, (EXISTS(SELECT 1 FROM account_values av WHERE av.account_id=accounts.id AND av" +
           ".value <> 0) OR EXISTS(SELECT 1 FROM accounts a WHERE a.parent_name = accounts.name))" +
           ") ORDER BY name"
    )
    abstract fun getAllWithAmounts(profileId: Long, includeZeroBalances: Boolean): LiveData<List<AccountWithAmounts>>

    @Query("SELECT * FROM accounts WHERE id=:id")
    abstract fun getByIdSync(id: Long): Account?

    @Query("SELECT * FROM accounts WHERE profile_id = :profileId AND name = :accountName")
    abstract fun getByName(profileId: Long, accountName: String): LiveData<Account>

    @Query("SELECT * FROM accounts WHERE profile_id = :profileId AND name = :accountName")
    abstract fun getByNameSync(profileId: Long, accountName: String): Account?

    @Transaction
    @Query("SELECT * FROM accounts WHERE profile_id = :profileId AND name = :accountName")
    abstract fun getByNameWithAmounts(profileId: Long, accountName: String): LiveData<AccountWithAmounts>

    @Query(
        "SELECT name, CASE WHEN name_upper LIKE :term||'%' THEN 1 " +
           "               WHEN name_upper LIKE '%:'||:term||'%' THEN 2 " +
           "               WHEN name_upper LIKE '% '||:term||'%' THEN 3 " +
           "               ELSE 9 END AS ordering " + "FROM accounts " +
           "WHERE profile_id=:profileId AND name_upper LIKE '%'||:term||'%' " +
           "ORDER BY ordering, name_upper, rowid "
    )
    abstract fun lookupNamesInProfileByName(profileId: Long, term: String): LiveData<List<AccountNameContainer>>

    @Query(
        "SELECT name, CASE WHEN name_upper LIKE :term||'%' THEN 1 " +
           "               WHEN name_upper LIKE '%:'||:term||'%' THEN 2 " +
           "               WHEN name_upper LIKE '% '||:term||'%' THEN 3 " +
           "               ELSE 9 END AS ordering " + "FROM accounts " +
           "WHERE profile_id=:profileId AND name_upper LIKE '%'||:term||'%' " +
           "ORDER BY ordering, name_upper, rowid "
    )
    abstract fun lookupNamesInProfileByNameSync(profileId: Long, term: String): List<AccountNameContainer>

    @Transaction
    @Query(
        "SELECT * FROM accounts " +
           "WHERE profile_id=:profileId AND name_upper LIKE '%'||:term||'%' " +
           "ORDER BY  CASE WHEN name_upper LIKE :term||'%' THEN 1 " +
           "               WHEN name_upper LIKE '%:'||:term||'%' THEN 2 " +
           "               WHEN name_upper LIKE '% '||:term||'%' THEN 3 " +
           "               ELSE 9 END, name_upper, rowid "
    )
    abstract fun lookupWithAmountsInProfileByNameSync(profileId: Long, term: String): List<AccountWithAmounts>

    @Query(
        "SELECT DISTINCT name, CASE WHEN name_upper LIKE :term||'%' THEN 1 " +
           "               WHEN name_upper LIKE '%:'||:term||'%' THEN 2 " +
           "               WHEN name_upper LIKE '% '||:term||'%' THEN 3 " +
           "               ELSE 9 END AS ordering " + "FROM accounts " +
           "WHERE name_upper LIKE '%'||:term||'%' " + "ORDER BY ordering, name_upper, rowid "
    )
    abstract fun lookupNamesByName(term: String): LiveData<List<AccountNameContainer>>

    @Query(
        "SELECT DISTINCT name, CASE WHEN name_upper LIKE :term||'%' THEN 1 " +
           "               WHEN name_upper LIKE '%:'||:term||'%' THEN 2 " +
           "               WHEN name_upper LIKE '% '||:term||'%' THEN 3 " +
           "               ELSE 9 END AS ordering " + "FROM accounts " +
           "WHERE name_upper LIKE '%'||:term||'%' " + "ORDER BY ordering, name_upper, rowid "
    )
    abstract fun lookupNamesByNameSync(term: String): List<AccountNameContainer>

    @Query("SELECT * FROM accounts WHERE profile_id = :profileId")
    abstract fun allForProfileSync(profileId: Long): List<Account>

    @Query("SELECT generation FROM accounts WHERE profile_id = :profileId LIMIT 1")
    protected abstract fun getGenerationPOJOSync(profileId: Long): AccountGenerationContainer?

    fun getGenerationSync(profileId: Long): Long {
        val result = getGenerationPOJOSync(profileId) ?: return 0
        return result.generation
    }

    @Query("DELETE FROM accounts WHERE profile_id = :profileId AND generation <> :currentGeneration")
    abstract fun purgeOldAccountsSync(profileId: Long, currentGeneration: Long)

    @Query(
        "DELETE FROM account_values WHERE EXISTS (SELECT 1 FROM accounts a WHERE a" +
           ".id=account_values.account_id AND a.profile_id=:profileId) AND generation <> " +
           ":currentGeneration"
    )
    abstract fun purgeOldAccountValuesSync(profileId: Long, currentGeneration: Long)

    @Transaction
    open fun storeAccountsSync(accounts: List<AccountWithAmounts>, profileId: Long) {
        val generation = getGenerationSync(profileId) + 1

        for (rec in accounts) {
            rec.account.generation = generation
            rec.account.profileId = profileId
            insertSync(rec)
        }
        purgeOldAccountsSync(profileId, generation)
        purgeOldAccountValuesSync(profileId, generation)
    }

    class AccountNameContainer {
        @ColumnInfo
        @JvmField
        var name: String? = null

        @ColumnInfo
        @JvmField
        var ordering: Int = 0
    }

    class AccountGenerationContainer(@ColumnInfo var generation: Long)

    companion object {
        @JvmStatic
        fun unbox(list: List<AccountNameContainer>): List<String> = list.mapNotNull { it.name }
    }
}
