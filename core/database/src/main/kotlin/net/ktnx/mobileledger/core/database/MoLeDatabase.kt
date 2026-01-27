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

package net.ktnx.mobileledger.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import net.ktnx.mobileledger.core.database.dao.AccountDAO
import net.ktnx.mobileledger.core.database.dao.AccountValueDAO
import net.ktnx.mobileledger.core.database.dao.CurrencyDAO
import net.ktnx.mobileledger.core.database.dao.OptionDAO
import net.ktnx.mobileledger.core.database.dao.ProfileDAO
import net.ktnx.mobileledger.core.database.dao.TemplateAccountDAO
import net.ktnx.mobileledger.core.database.dao.TemplateHeaderDAO
import net.ktnx.mobileledger.core.database.dao.TransactionAccountDAO
import net.ktnx.mobileledger.core.database.dao.TransactionDAO
import net.ktnx.mobileledger.core.database.entity.Account
import net.ktnx.mobileledger.core.database.entity.AccountValue
import net.ktnx.mobileledger.core.database.entity.Currency
import net.ktnx.mobileledger.core.database.entity.Option
import net.ktnx.mobileledger.core.database.entity.Profile
import net.ktnx.mobileledger.core.database.entity.TemplateAccount
import net.ktnx.mobileledger.core.database.entity.TemplateHeader
import net.ktnx.mobileledger.core.database.entity.Transaction
import net.ktnx.mobileledger.core.database.entity.TransactionAccount

@Database(
    version = MoLeDatabase.REVISION,
    entities = [
        TemplateHeader::class,
        TemplateAccount::class,
        Currency::class,
        Account::class,
        Profile::class,
        Option::class,
        AccountValue::class,
        Transaction::class,
        TransactionAccount::class
    ]
)
abstract class MoLeDatabase : RoomDatabase() {

    abstract fun getTemplateDAO(): TemplateHeaderDAO
    abstract fun getTemplateAccountDAO(): TemplateAccountDAO
    abstract fun getCurrencyDAO(): CurrencyDAO
    abstract fun getAccountDAO(): AccountDAO
    abstract fun getAccountValueDAO(): AccountValueDAO
    abstract fun getTransactionDAO(): TransactionDAO
    abstract fun getTransactionAccountDAO(): TransactionAccountDAO
    abstract fun getOptionDAO(): OptionDAO
    abstract fun getProfileDAO(): ProfileDAO

    @androidx.room.Transaction
    open fun deleteAllSync() {
        getTransactionAccountDAO().deleteAllSync()
        getTransactionDAO().deleteAllSync()
        getAccountValueDAO().deleteAllSync()
        getAccountDAO().deleteAllSync()
        getTemplateAccountDAO().deleteAllSync()
        getTemplateDAO().deleteAllSync()
        getCurrencyDAO().deleteAllSync()
        getOptionDAO().deleteAllSync()
        getProfileDAO().deleteAllSync()
    }

    companion object {
        const val REVISION = 69
        const val DB_NAME = "MoLe.db"
    }
}
