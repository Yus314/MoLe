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

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
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
 * Test module that replaces DatabaseModule for instrumentation tests.
 *
 * Provides an in-memory database instead of the production file-based database.
 * This enables:
 * - Faster test execution (no disk I/O)
 * - Clean database state for each test
 * - No side effects between tests
 * - No need to clean up test data
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DatabaseModule::class]
)
object TestDatabaseModule {

    @Provides
    @Singleton
    fun provideInMemoryDatabase(@ApplicationContext context: Context): DB = Room.inMemoryDatabaseBuilder(context, DB::class.java)
            .allowMainThreadQueries() // Allow queries on main thread for simpler tests
            .build()

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
