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
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.Locale
import java.util.UUID
import java.util.regex.Pattern
import javax.inject.Singleton
import logcat.logcat
import net.ktnx.mobileledger.core.database.MoLeDatabase
import net.ktnx.mobileledger.core.database.dao.AccountDAO
import net.ktnx.mobileledger.core.database.dao.AccountValueDAO
import net.ktnx.mobileledger.core.database.dao.CurrencyDAO
import net.ktnx.mobileledger.core.database.dao.OptionDAO
import net.ktnx.mobileledger.core.database.dao.ProfileDAO
import net.ktnx.mobileledger.core.database.dao.TemplateAccountDAO
import net.ktnx.mobileledger.core.database.dao.TemplateHeaderDAO
import net.ktnx.mobileledger.core.database.dao.TransactionAccountDAO
import net.ktnx.mobileledger.core.database.dao.TransactionDAO
import net.ktnx.mobileledger.core.domain.repository.PreferencesRepository

/**
 * Hilt module providing database and DAO dependencies.
 *
 * This module builds the Room database directly using @ApplicationContext,
 * eliminating the need for App.instance singleton access.
 *
 * ## Provided Dependencies
 *
 * - [MoLeDatabase] - The Room database instance (singleton)
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
    fun provideDatabase(
        @ApplicationContext context: Context,
        preferencesRepository: PreferencesRepository
    ): MoLeDatabase {
        val builder = Room.databaseBuilder(context, MoLeDatabase::class.java, MoLeDatabase.DB_NAME)
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

        return builder.build()
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
            },
            singleVersionMigration(69, resources, packageName, preferencesRepository)
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
            applyRevisionFile(db, resources, packageName, fileName)

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
                            arrayOf<Any?>(UUID.randomUUID().toString(), c.getLong(0))
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
            applyRevisionFile(db, resources, packageName, fileName)
        }
    }

    private fun fixTransactionDescriptionUpper(database: SupportSQLiteDatabase) {
        database.query("SELECT id, description FROM transactions").use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                val description = c.getString(1)
                database.execSQL(
                    "UPDATE transactions SET description_uc=? WHERE id=?",
                    arrayOf<Any?>(description.uppercase(), id)
                )
            }
        }
    }

    /**
     * Apply a SQL revision file from raw resources.
     *
     * @param db The SQLite database to apply the revision to
     * @param resources The Resources instance for loading raw files
     * @param packageName The package name for resource identification
     * @param fileName The name of the raw resource file (without extension)
     */
    private fun applyRevisionFile(
        db: SupportSQLiteDatabase,
        resources: android.content.res.Resources,
        packageName: String,
        fileName: String
    ) {
        val resId = resources.getIdentifier(fileName, "raw", packageName)
        if (resId == 0) {
            throw android.database.SQLException(String.format(Locale.US, "No resource for %s", fileName))
        }

        try {
            resources.openRawResource(resId).use { res ->
                logcat { "Applying $fileName" }
                val reader = BufferedReader(InputStreamReader(res))

                val endOfStatement = Pattern.compile(";\\s*(?:--.*)?$")

                var sqlStatement: String? = null
                var lineNo = 0

                reader.forEachLine { line ->
                    lineNo++
                    if (line.startsWith("--") || line.isEmpty()) {
                        return@forEachLine
                    }

                    sqlStatement = if (sqlStatement == null) {
                        line
                    } else {
                        "$sqlStatement $line"
                    }

                    val m = endOfStatement.matcher(line)
                    if (m.find()) {
                        try {
                            db.execSQL(checkNotNull(sqlStatement) { "SQL statement is null" })
                            sqlStatement = null
                        } catch (e: Exception) {
                            throw RuntimeException(
                                String.format(
                                    "Error applying %s, line %d, statement: %s",
                                    fileName,
                                    lineNo,
                                    sqlStatement
                                ),
                                e
                            )
                        }
                    }
                }

                if (sqlStatement != null) {
                    throw RuntimeException(
                        String.format(
                            "Error applying %s: EOF after continuation. Line %s, " +
                                "Incomplete statement: %s",
                            fileName,
                            lineNo,
                            sqlStatement
                        )
                    )
                }
            }
        } catch (e: IOException) {
            throw RuntimeException(
                String.format("Error opening raw resource for %s", fileName),
                e
            )
        }
    }

    @Provides
    fun provideProfileDAO(db: MoLeDatabase): ProfileDAO = db.getProfileDAO()

    @Provides
    fun provideTransactionDAO(db: MoLeDatabase): TransactionDAO = db.getTransactionDAO()

    @Provides
    fun provideTransactionAccountDAO(db: MoLeDatabase): TransactionAccountDAO = db.getTransactionAccountDAO()

    @Provides
    fun provideAccountDAO(db: MoLeDatabase): AccountDAO = db.getAccountDAO()

    @Provides
    fun provideAccountValueDAO(db: MoLeDatabase): AccountValueDAO = db.getAccountValueDAO()

    @Provides
    fun provideTemplateHeaderDAO(db: MoLeDatabase): TemplateHeaderDAO = db.getTemplateDAO()

    @Provides
    fun provideTemplateAccountDAO(db: MoLeDatabase): TemplateAccountDAO = db.getTemplateAccountDAO()

    @Provides
    fun provideCurrencyDAO(db: MoLeDatabase): CurrencyDAO = db.getCurrencyDAO()

    @Provides
    fun provideOptionDAO(db: MoLeDatabase): OptionDAO = db.getOptionDAO()
}
