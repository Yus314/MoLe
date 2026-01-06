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

package net.ktnx.mobileledger.db

import android.content.res.Resources
import android.database.SQLException
import androidx.lifecycle.MutableLiveData
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.Locale
import java.util.UUID
import java.util.regex.Pattern
import net.ktnx.mobileledger.App
import net.ktnx.mobileledger.dao.AccountDAO
import net.ktnx.mobileledger.dao.AccountValueDAO
import net.ktnx.mobileledger.dao.CurrencyDAO
import net.ktnx.mobileledger.dao.OptionDAO
import net.ktnx.mobileledger.dao.ProfileDAO
import net.ktnx.mobileledger.dao.TemplateAccountDAO
import net.ktnx.mobileledger.dao.TemplateHeaderDAO
import net.ktnx.mobileledger.dao.TransactionAccountDAO
import net.ktnx.mobileledger.dao.TransactionDAO
import net.ktnx.mobileledger.utils.Logger

@Database(
    version = DB.REVISION,
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
abstract class DB : RoomDatabase() {

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
        const val REVISION = 68
        const val DB_NAME = "MoLe.db"

        @JvmField
        val initComplete = MutableLiveData(false)

        @Volatile
        private var instance: DB? = null

        @JvmStatic
        fun get(): DB {
            return instance ?: synchronized(DB::class.java) {
                instance ?: buildDatabase().also { instance = it }
            }
        }

        private fun buildDatabase(): DB {
            val builder = Room.databaseBuilder(App.instance, DB::class.java, DB_NAME)

            builder.addMigrations(
                singleVersionMigration(17),
                singleVersionMigration(18),
                singleVersionMigration(19),
                singleVersionMigration(20),
                multiVersionMigration(20, 22),
                multiVersionMigration(22, 30),
                multiVersionMigration(30, 32),
                multiVersionMigration(32, 34),
                multiVersionMigration(34, 40),
                singleVersionMigration(41),
                multiVersionMigration(41, 58),
                singleVersionMigration(59),
                singleVersionMigration(60),
                singleVersionMigration(61),
                singleVersionMigration(62),
                singleVersionMigration(63),
                singleVersionMigration(64),
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

            builder.addCallback(object : Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    db.execSQL("PRAGMA foreign_keys = ON")
                    db.execSQL("pragma case_sensitive_like=ON;")
                }
            })

            return builder.build()
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

        private fun singleVersionMigration(toVersion: Int): Migration {
            return object : Migration(toVersion - 1, toVersion) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    val fileName = String.format(Locale.US, "db_%d", toVersion)
                    applyRevisionFile(db, fileName)

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
                                    App.storeStartupProfileAndTheme(currentProfileId, currentTheme)
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
        }

        private fun multiVersionMigration(fromVersion: Int, toVersion: Int): Migration {
            return object : Migration(fromVersion, toVersion) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    val fileName = String.format(Locale.US, "db_%d_%d", fromVersion, toVersion)
                    applyRevisionFile(db, fileName)
                }
            }
        }

        @JvmStatic
        fun applyRevisionFile(db: SupportSQLiteDatabase, fileName: String) {
            val rm: Resources = App.instance.resources
            val resId = rm.getIdentifier(fileName, "raw", App.instance.packageName)
            if (resId == 0) {
                throw SQLException(String.format(Locale.US, "No resource for %s", fileName))
            }

            try {
                rm.openRawResource(resId).use { res ->
                    Logger.debug("db", "Applying $fileName")
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
    }
}
