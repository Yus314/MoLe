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

package net.ktnx.mobileledger.backup

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import net.ktnx.mobileledger.utils.Misc
import java.io.FileNotFoundException
import java.io.IOException

abstract class ConfigIO @Throws(FileNotFoundException::class) constructor(
    context: Context,
    uri: Uri,
    protected val onErrorListener: OnErrorListener?
) : Thread() {

    protected var pfd: ParcelFileDescriptor? = context.contentResolver.openFileDescriptor(uri, getStreamMode())

    init {
        initStream()
    }

    protected abstract fun getStreamMode(): String

    protected abstract fun initStream()

    @Throws(IOException::class)
    protected abstract fun processStream()

    override fun run() {
        try {
            processStream()
        } catch (e: Exception) {
            Log.e("cfg-json", "Error processing settings as JSON", e)
            onErrorListener?.let { listener ->
                Misc.onMainThread { listener.error(e) }
            }
        } finally {
            try {
                pfd?.close()
            } catch (e: Exception) {
                Log.e("cfg-json", "Error closing file descriptor", e)
            }
        }
    }

    object Keys {
        const val ACCOUNTS = "accounts"
        const val AMOUNT = "amount"
        const val AMOUNT_GROUP = "amountGroup"
        const val API_VER = "apiVersion"
        const val AUTH_PASS = "authPass"
        const val AUTH_USER = "authUser"
        const val CAN_POST = "permitPosting"
        const val COLOUR = "colour"
        const val COMMENT = "comment"
        const val COMMENT_GROUP = "commentMatchGroup"
        const val COMMODITIES = "commodities"
        const val CURRENCY = "commodity"
        const val CURRENCY_GROUP = "commodityGroup"
        const val CURRENT_PROFILE = "currentProfile"
        const val DATE_DAY = "dateDay"
        const val DATE_DAY_GROUP = "dateDayMatchGroup"
        const val DATE_MONTH = "dateMonth"
        const val DATE_MONTH_GROUP = "dateMonthMatchGroup"
        const val DATE_YEAR = "dateYear"
        const val DATE_YEAR_GROUP = "dateYearMatchGroup"
        const val DEFAULT_COMMODITY = "defaultCommodity"
        const val FUTURE_DATES = "futureDates"
        const val HAS_GAP = "hasGap"
        const val IS_FALLBACK = "isFallback"
        const val NAME = "name"
        const val NAME_GROUP = "nameMatchGroup"
        const val NEGATE_AMOUNT = "negateAmount"
        const val POSITION = "position"
        const val PREF_ACCOUNT = "preferredAccountsFilter"
        const val PROFILES = "profiles"
        const val REGEX = "regex"
        const val SHOW_COMMENTS = "showCommentsByDefault"
        const val SHOW_COMMODITY = "showCommodityByDefault"
        const val TEMPLATES = "templates"
        const val TEST_TEXT = "testText"
        const val TRANSACTION = "description"
        const val TRANSACTION_GROUP = "descriptionMatchGroup"
        const val URL = "url"
        const val USE_AUTH = "useAuth"
        const val UUID = "uuid"
    }

    abstract class OnErrorListener {
        abstract fun error(e: Exception)
    }
}
