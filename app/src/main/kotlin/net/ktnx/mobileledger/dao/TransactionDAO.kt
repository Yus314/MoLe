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
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import net.ktnx.mobileledger.db.Account
import net.ktnx.mobileledger.db.AccountValue
import net.ktnx.mobileledger.db.DB
import net.ktnx.mobileledger.db.Transaction
import net.ktnx.mobileledger.db.TransactionWithAccounts
import net.ktnx.mobileledger.model.LedgerAccount
import net.ktnx.mobileledger.utils.Logger
import net.ktnx.mobileledger.utils.Misc
import java.util.Locale

@Dao
abstract class TransactionDAO : BaseDAO<Transaction>() {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract override fun insertSync(item: Transaction): Long

    @Update
    abstract override fun updateSync(item: Transaction)

    @Delete
    abstract override fun deleteSync(item: Transaction)

    @Delete
    abstract fun deleteSync(vararg items: Transaction)

    @Delete
    abstract fun deleteSync(items: List<Transaction>)

    @Query("DELETE FROM transactions")
    abstract fun deleteAllSync()

    @Query("SELECT * FROM transactions WHERE id = :id")
    abstract fun getById(id: Long): LiveData<Transaction>

    @androidx.room.Transaction
    @Query("SELECT * FROM transactions WHERE id = :transactionId")
    abstract fun getByIdWithAccounts(transactionId: Long): LiveData<TransactionWithAccounts>

    @androidx.room.Transaction
    @Query("SELECT * FROM transactions WHERE id = :transactionId")
    abstract fun getByIdWithAccountsSync(transactionId: Long): TransactionWithAccounts?

    @Query("SELECT DISTINCT description, CASE WHEN description_uc LIKE :term||'%' THEN 1 " +
           "               WHEN description_uc LIKE '%:'||:term||'%' THEN 2 " +
           "               WHEN description_uc LIKE '% '||:term||'%' THEN 3 " +
           "               ELSE 9 END AS ordering FROM transactions " +
           "WHERE description_uc LIKE '%'||:term||'%' ORDER BY ordering, description_uc, rowid ")
    abstract fun lookupDescriptionSync(term: String): List<DescriptionContainer>

    @androidx.room.Transaction
    @Query("SELECT * from transactions WHERE description = :description ORDER BY year desc, month" +
           " desc, day desc LIMIT 1")
    abstract fun getFirstByDescriptionSync(description: String): TransactionWithAccounts?

    @androidx.room.Transaction
    @Query("SELECT tr.id, tr.profile_id, tr.ledger_id, tr.description, tr.description_uc, tr" +
           ".data_hash, tr.comment, tr.year, tr.month, tr.day, tr.generation from transactions tr" +
           " JOIN transaction_accounts t_a ON t_a.transaction_id = tr.id WHERE tr.description = " +
           ":description AND t_a.account_name LIKE '%'||:accountTerm||'%' ORDER BY year desc, " +
           "month desc, day desc, tr.ledger_id desc LIMIT 1")
    abstract fun getFirstByDescriptionHavingAccountSync(description: String, accountTerm: String): TransactionWithAccounts?

    @Query("SELECT * from transactions WHERE profile_id = :profileId")
    abstract fun getAllForProfileUnorderedSync(profileId: Long): List<Transaction>

    @Query("SELECT generation FROM transactions WHERE profile_id = :profileId LIMIT 1")
    protected abstract fun getGenerationPOJOSync(profileId: Long): TransactionGenerationContainer?

    @androidx.room.Transaction
    @Query("SELECT * FROM transactions WHERE profile_id = :profileId ORDER BY year " +
           " asc, month asc, day asc, ledger_id asc")
    abstract fun getAllWithAccounts(profileId: Long): LiveData<List<TransactionWithAccounts>>

    @androidx.room.Transaction
    @Query("SELECT distinct(tr.id), tr.ledger_id, tr.profile_id, tr.data_hash, tr.year, tr.month," +
           " tr.day, tr.description, tr.description_uc, tr.comment, tr.generation FROM " +
           "transactions tr JOIN transaction_accounts ta ON ta.transaction_id=tr.id WHERE ta" +
           ".account_name LIKE :accountName||'%' AND ta.amount <> 0 AND tr.profile_id = " +
           ":profileId ORDER BY tr.year asc, tr.month asc, tr.day asc, tr.ledger_id asc")
    abstract fun getAllWithAccountsFiltered(profileId: Long, accountName: String?): LiveData<List<TransactionWithAccounts>>

    @Query("DELETE FROM transactions WHERE profile_id = :profileId AND generation <> :currentGeneration")
    abstract fun purgeOldTransactionsSync(profileId: Long, currentGeneration: Long): Int

    @Query("DELETE FROM transaction_accounts WHERE EXISTS (SELECT 1 FROM transactions tr WHERE tr" +
           ".id=transaction_accounts.transaction_id AND tr.profile_id=:profileId) AND generation " +
           "<> :currentGeneration")
    abstract fun purgeOldTransactionAccountsSync(profileId: Long, currentGeneration: Long): Int

    @Query("DELETE FROM transactions WHERE profile_id = :profileId")
    abstract fun deleteAllSync(profileId: Long): Int

    @Query("SELECT * FROM transactions where profile_id = :profileId AND ledger_id = :ledgerId")
    abstract fun getByLedgerId(profileId: Long, ledgerId: Long): Transaction?

    @Query("UPDATE transactions SET generation = :newGeneration WHERE id = :transactionId")
    abstract fun updateGeneration(transactionId: Long, newGeneration: Long): Int

    @Query("UPDATE transaction_accounts SET generation = :newGeneration WHERE transaction_id = :transactionId")
    abstract fun updateAccountsGeneration(transactionId: Long, newGeneration: Long): Int

    @Query("SELECT max(ledger_id) as ledger_id FROM transactions WHERE profile_id = :profileId")
    abstract fun getMaxLedgerIdPOJOSync(profileId: Long): LedgerIdContainer?

    @androidx.room.Transaction
    open fun updateGenerationWithAccounts(transactionId: Long, newGeneration: Long) {
        updateGeneration(transactionId, newGeneration)
        updateAccountsGeneration(transactionId, newGeneration)
    }

    fun getGenerationSync(profileId: Long): Long {
        val result = getGenerationPOJOSync(profileId) ?: return 0
        return result.generation
    }

    fun getMaxLedgerIdSync(profileId: Long): Long {
        val result = getMaxLedgerIdPOJOSync(profileId) ?: return 0
        return result.ledgerId
    }

    @androidx.room.Transaction
    open fun storeTransactionsSync(list: List<TransactionWithAccounts>, profileId: Long) {
        val generation = getGenerationSync(profileId) + 1

        for (tr in list) {
            tr.transaction.generation = generation
            tr.transaction.profileId = profileId
            storeSync(tr)
        }

        Logger.debug("Transaction", "Purging old transactions")
        var removed = purgeOldTransactionsSync(profileId, generation)
        Logger.debug("Transaction", String.format(Locale.ROOT, "Purged %d transactions", removed))

        removed = purgeOldTransactionAccountsSync(profileId, generation)
        Logger.debug("Transaction", String.format(Locale.ROOT, "Purged %d transaction accounts", removed))
    }

    @androidx.room.Transaction
    open fun storeSync(rec: TransactionWithAccounts) {
        val trAccDao = DB.get().getTransactionAccountDAO()

        var transaction = rec.transaction
        val existing = getByLedgerId(transaction.profileId, transaction.ledgerId)
        if (existing != null) {
            if (Misc.equalStrings(transaction.dataHash, existing.dataHash)) {
                updateGenerationWithAccounts(existing.id, rec.transaction.generation)
                return
            }

            existing.copyDataFrom(transaction)
            updateSync(existing)

            transaction = existing
        } else {
            transaction.id = insertSync(transaction)
        }

        for (trAcc in rec.accounts ?: emptyList()) {
            trAcc.transactionId = transaction.id
            trAcc.generation = transaction.generation
            val existingAcc = trAccDao.getByOrderNoSync(trAcc.transactionId, trAcc.orderNo)
            if (existingAcc != null) {
                existingAcc.copyDataFrom(trAcc)
                trAccDao.updateSync(existingAcc)
            } else {
                trAcc.id = trAccDao.insertSync(trAcc)
            }
        }
    }

    fun storeLast(rec: TransactionWithAccounts) {
        runAsync { appendSync(rec) }
    }

    @androidx.room.Transaction
    open fun appendSync(rec: TransactionWithAccounts) {
        val trAccDao = DB.get().getTransactionAccountDAO()
        val accDao = DB.get().getAccountDAO()
        val accValDao = DB.get().getAccountValueDAO()

        val transaction = rec.transaction
        val profileId = transaction.profileId
        transaction.generation = getGenerationSync(profileId)
        transaction.ledgerId = getMaxLedgerIdSync(profileId) + 1
        transaction.id = insertSync(transaction)

        for (trAcc in rec.accounts ?: emptyList()) {
            trAcc.transactionId = transaction.id
            trAcc.generation = transaction.generation
            trAcc.id = trAccDao.insertSync(trAcc)

            var accName: String? = trAcc.accountName
            while (accName != null) {
                var acc = accDao.getByNameSync(profileId, accName)
                if (acc == null) {
                    acc = Account()
                    acc.profileId = profileId
                    acc.name = accName
                    acc.nameUpper = accName.uppercase()
                    acc.parentName = LedgerAccount.extractParentName(accName)
                    acc.level = LedgerAccount.determineLevel(acc.name)
                    acc.generation = trAcc.generation

                    acc.id = accDao.insertSync(acc)
                }

                var accVal = accValDao.getByCurrencySync(acc.id, trAcc.currency)
                if (accVal == null) {
                    accVal = AccountValue()
                    accVal.accountId = acc.id
                    accVal.generation = trAcc.generation
                    accVal.currency = trAcc.currency
                    accVal.value = trAcc.amount
                    accVal.id = accValDao.insertSync(accVal)
                } else {
                    accVal.value = accVal.value + trAcc.amount
                    accValDao.updateSync(accVal)
                }

                accName = LedgerAccount.extractParentName(accName)
            }
        }
    }

    class TransactionGenerationContainer(@ColumnInfo var generation: Long)

    class LedgerIdContainer(@ColumnInfo(name = "ledger_id") var ledgerId: Long)

    class DescriptionContainer {
        @ColumnInfo
        @JvmField
        var description: String? = null

        @ColumnInfo
        @JvmField
        var ordering: Int = 0
    }

    companion object {
        @JvmStatic
        fun unbox(list: List<DescriptionContainer>): List<String> {
            return list.mapNotNull { it.description }
        }
    }
}
