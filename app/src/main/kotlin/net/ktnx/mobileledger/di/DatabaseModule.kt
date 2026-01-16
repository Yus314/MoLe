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
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.Locale
import java.util.UUID
import javax.inject.Singleton
import net.ktnx.mobileledger.dao.AccountDAO
import net.ktnx.mobileledger.dao.AccountValueDAO
import net.ktnx.mobileledger.dao.CurrencyDAO
import net.ktnx.mobileledger.dao.OptionDAO
import net.ktnx.mobileledger.dao.ProfileDAO
import net.ktnx.mobileledger.dao.TemplateAccountDAO
import net.ktnx.mobileledger.dao.TemplateHeaderDAO
import net.ktnx.mobileledger.dao.TransactionAccountDAO
import net.ktnx.mobileledger.dao.TransactionDAO
import net.ktnx.mobileledger.data.repository.PreferencesRepository
import net.ktnx.mobileledger.db.DB

/**
 * Hilt module providing database and DAO dependencies.
 *
 * This module builds the Room database directly using @ApplicationContext,
 * eliminating the need for App.instance singleton access.
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
    fun provideDatabase(@ApplicationContext context: Context, preferencesRepository: PreferencesRepository): DB {
        val builder = Room.databaseBuilder(context, DB::class.java, DB.DB_NAME)
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)

        builder.addMigrations(
            *createMigrations(context, preferencesRepository)
        )

        builder.addCallback(object : RoomDatabase.Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                db.execSQL("PRAGMA foreign_keys = ON")
                db.execSQL("pragma case_sensitive_like=ON;")
            }
        })

        val db = builder.build()
        // Set instance for legacy code that uses DB.get()
        DB.setInstance(db)
        return db
    }

    private fun createMigrations(context: Context, preferencesRepository: PreferencesRepository): Array<Migration> {
        val resources = context.resources
        val packageName = context.packageName

        return arrayOf(
            singleVersionMigration(17, resources, packageName, preferencesRepository),
            singleVersionMigration(18, resources, packageName, preferencesRepository),
            singleVersionMigration(19, resources, packageName, preferencesRepository),
            singleVersionMigration(20, resources, packageName, preferencesRepository),
            multiVersionMigration(20, 22, resources, packageName),
            multiVersionMigration(22, 30, resources, packageName),
            multiVersionMigration(30, 32, resources, packageName),
            multiVersionMigration(32, 34, resources, packageName),
            multiVersionMigration(34, 40, resources, packageName),
            singleVersionMigration(41, resources, packageName, preferencesRepository),
            multiVersionMigration(41, 58, resources, packageName),
            singleVersionMigration(59, resources, packageName, preferencesRepository),
            singleVersionMigration(60, resources, packageName, preferencesRepository),
            singleVersionMigration(61, resources, packageName, preferencesRepository),
            singleVersionMigration(62, resources, packageName, preferencesRepository),
            singleVersionMigration(63, resources, packageName, preferencesRepository),
            singleVersionMigration(64, resources, packageName, preferencesRepository),
            object : Migration(64, 65) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    fixTransactionDescriptionUpper(database)
                }
            },
            object : Migration(64, 66) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    fixTransactionDescriptionUpper(database)
                }
            },
            object : Migration(65, 66) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    fixTransactionDescriptionUpper(database)
                }
            },
            object : Migration(66, 67) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL(
                        "ALTER TABLE transaction_accounts ADD COLUMN amount_style TEXT"
                    )
                }
            },
            object : Migration(67, 68) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL(
                        "ALTER TABLE account_values ADD COLUMN amount_style TEXT"
                    )
                }
            }
        )
    }

    private fun singleVersionMigration(
        toVersion: Int,
        resources: android.content.res.Resources,
        packageName: String,
        preferencesRepository: PreferencesRepository
    ): Migration = object : Migration(toVersion - 1, toVersion) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val fileName = String.format(Locale.US, "db_%d", toVersion)
            DB.applyRevisionFile(db, resources, packageName, fileName)

            if (toVersion == 59) {
                db.query(
                    "SELECT p.id, p.theme FROM profiles p WHERE p.id=(SELECT o.value " +
                        "FROM options o WHERE o.profile_id=0 AND o.name=?)",
                    arrayOf("profile_id")
                ).use { c ->
                    if (c.moveToFirst()) {
                        val currentProfileId = c.getLong(0)
                        val currentTheme = c.getInt(1)
                        if (currentProfileId >= 0 && currentTheme >= 0) {
                            preferencesRepository.setStartupProfileId(currentProfileId)
                            preferencesRepository.setStartupTheme(currentTheme)
                        }
                    }
                }
            }

            if (toVersion == 63) {
                db.query("SELECT id FROM templates").use { c ->
                    while (c.moveToNext()) {
                        db.execSQL(
                            "UPDATE templates SET uuid=? WHERE id=?",
                            arrayOf(UUID.randomUUID().toString(), c.getLong(0))
                        )
                    }
                }
            }
        }
    }

    private fun multiVersionMigration(
        fromVersion: Int,
        toVersion: Int,
        resources: android.content.res.Resources,
        packageName: String
    ): Migration = object : Migration(fromVersion, toVersion) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val fileName = String.format(Locale.US, "db_%d_%d", fromVersion, toVersion)
            DB.applyRevisionFile(db, resources, packageName, fileName)
        }
    }

    private fun fixTransactionDescriptionUpper(database: SupportSQLiteDatabase) {
        database.query("SELECT id, description FROM transactions").use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                val description = c.getString(1)
                database.execSQL(
                    "UPDATE transactions SET description_uc=? WHERE id=?",
                    arrayOf(description.uppercase(), id)
                )
            }
        }
    }

    @Provides
    fun provideProfileDAO(db: DB): ProfileDAO = db.getProfileDAO()

    @Provides
    fun provideTransactionDAO(db: DB): TransactionDAO = db.getTransactionDAO()

    @Provides
    fun provideTransactionAccountDAO(db: DB): TransactionAccountDAO = db.getTransactionAccountDAO()

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
