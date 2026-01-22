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
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.Locale
import java.util.regex.Pattern
import logcat.logcat
import net.ktnx.mobileledger.dao.AccountDAO
import net.ktnx.mobileledger.dao.AccountValueDAO
import net.ktnx.mobileledger.dao.CurrencyDAO
import net.ktnx.mobileledger.dao.OptionDAO
import net.ktnx.mobileledger.dao.ProfileDAO
import net.ktnx.mobileledger.dao.TemplateAccountDAO
import net.ktnx.mobileledger.dao.TemplateHeaderDAO
import net.ktnx.mobileledger.dao.TransactionAccountDAO
import net.ktnx.mobileledger.dao.TransactionDAO

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
        const val REVISION = 69
        const val DB_NAME = "MoLe.db"

        /**
         * Database instance for legacy code access.
         * @deprecated Use Hilt dependency injection instead
         */
        @Volatile
        private var instance: DB? = null

        /**
         * Initialize the database instance (called by DatabaseModule).
         * @param db The database instance created by Hilt DI
         */
        @JvmStatic
        fun setInstance(db: DB) {
            instance = db
        }

        /**
         * Get the database instance for legacy code access.
         * @deprecated Use Hilt @Inject instead. This remains only for
         * backward compatibility with DAO classes that need cross-DAO access.
         */
        @Deprecated("Use Hilt @Inject instead")
        @JvmStatic
        fun get(): DB = instance
            ?: throw IllegalStateException(
                "DB not initialized. Ensure DatabaseModule is loaded before accessing DB.get()"
            )

        /**
         * Apply a SQL revision file from raw resources.
         *
         * @param db The SQLite database to apply the revision to
         * @param resources The Resources instance for loading raw files
         * @param packageName The package name for resource identification
         * @param fileName The name of the raw resource file (without extension)
         */
        @JvmStatic
        fun applyRevisionFile(db: SupportSQLiteDatabase, resources: Resources, packageName: String, fileName: String) {
            val resId = resources.getIdentifier(fileName, "raw", packageName)
            if (resId == 0) {
                throw SQLException(String.format(Locale.US, "No resource for %s", fileName))
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
    }
}
