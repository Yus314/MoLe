/*
 * Copyright © 2026 Damyan Ivanov.
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

package net.ktnx.mobileledger.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import net.ktnx.mobileledger.dao.AccountDAO
import net.ktnx.mobileledger.dao.AccountValueDAO
import net.ktnx.mobileledger.dao.CurrencyDAO
import net.ktnx.mobileledger.dao.OptionDAO
import net.ktnx.mobileledger.dao.ProfileDAO
import net.ktnx.mobileledger.dao.TemplateAccountDAO
import net.ktnx.mobileledger.dao.TemplateHeaderDAO
import net.ktnx.mobileledger.dao.TransactionDAO
import net.ktnx.mobileledger.db.DB

/**
 * Hilt module providing database and DAO dependencies.
 *
 * This module wraps the existing [DB] singleton to enable dependency injection
 * while maintaining backward compatibility with existing code.
 *
 * ## Provided Dependencies
 *
 * - [DB] - The Room database instance (singleton)
 * - [ProfileDAO] - Data access for user profiles/ledgers
 * - [TransactionDAO] - Data access for ledger transactions
 * - [AccountDAO] - Data access for accounts
 * - [AccountValueDAO] - Data access for account balance values
 * - [TemplateHeaderDAO] - Data access for transaction templates
 * - [TemplateAccountDAO] - Data access for template account entries
 * - [CurrencyDAO] - Data access for currencies
 * - [OptionDAO] - Data access for application options
 *
 * ## Testing
 *
 * For instrumentation tests, this module is replaced by [TestDatabaseModule]
 * which provides an in-memory database for test isolation.
 *
 * @see TestDatabaseModule
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(): DB = DB.get()

    @Provides
    fun provideProfileDAO(db: DB): ProfileDAO = db.getProfileDAO()

    @Provides
    fun provideTransactionDAO(db: DB): TransactionDAO = db.getTransactionDAO()

    @Provides
    fun provideAccountDAO(db: DB): AccountDAO = db.getAccountDAO()

    @Provides
    fun provideAccountValueDAO(db: DB): AccountValueDAO = db.getAccountValueDAO()

    @Provides
    fun provideTemplateHeaderDAO(db: DB): TemplateHeaderDAO = db.getTemplateDAO()

    @Provides
    fun provideTemplateAccountDAO(db: DB): TemplateAccountDAO = db.getTemplateAccountDAO()

    @Provides
    fun provideCurrencyDAO(db: DB): CurrencyDAO = db.getCurrencyDAO()

    @Provides
    fun provideOptionDAO(db: DB): OptionDAO = db.getOptionDAO()
}
