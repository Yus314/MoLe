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

import android.util.JsonReader
import android.util.JsonToken
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import net.ktnx.mobileledger.App
import net.ktnx.mobileledger.db.Currency
import net.ktnx.mobileledger.db.DB
import net.ktnx.mobileledger.db.Profile
import net.ktnx.mobileledger.db.TemplateAccount
import net.ktnx.mobileledger.db.TemplateHeader
import net.ktnx.mobileledger.db.TemplateWithAccounts
import net.ktnx.mobileledger.model.Data
import net.ktnx.mobileledger.utils.Logger

class RawConfigReader(inputStream: InputStream) {
    private val r: JsonReader = JsonReader(BufferedReader(InputStreamReader(inputStream)))

    var commodities: List<Currency>? = null
        private set
    var profiles: List<Profile>? = null
        private set
    var templates: List<TemplateWithAccounts>? = null
        private set
    var currentProfile: String? = null
        private set

    @Throws(IOException::class)
    fun readConfig() {
        commodities = null
        profiles = null
        templates = null
        currentProfile = null

        r.beginObject()
        while (r.hasNext()) {
            val item = r.nextName()
            if (r.peek() == JsonToken.NULL) {
                r.nextNull()
                continue
            }
            when (item) {
                ConfigIO.Keys.COMMODITIES -> commodities = readCommodities()
                ConfigIO.Keys.PROFILES -> profiles = readProfiles()
                ConfigIO.Keys.TEMPLATES -> templates = readTemplates()
                ConfigIO.Keys.CURRENT_PROFILE -> currentProfile = r.nextString()
                else -> throw RuntimeException("unexpected top-level item $item")
            }
        }
        r.endObject()
    }

    @Throws(IOException::class)
    private fun readTemplateAccount(): TemplateAccount {
        r.beginObject()
        val result = TemplateAccount(0L, 0L, 0L)
        while (r.peek() != JsonToken.END_OBJECT) {
            val item = r.nextName()
            if (r.peek() == JsonToken.NULL) {
                r.nextNull()
                continue
            }
            when (item) {
                ConfigIO.Keys.NAME -> result.accountName = r.nextString()
                ConfigIO.Keys.NAME_GROUP -> result.accountNameMatchGroup = r.nextInt()
                ConfigIO.Keys.COMMENT -> result.accountComment = r.nextString()
                ConfigIO.Keys.COMMENT_GROUP -> result.accountCommentMatchGroup = r.nextInt()
                ConfigIO.Keys.AMOUNT -> result.amount = r.nextDouble().toFloat()
                ConfigIO.Keys.AMOUNT_GROUP -> result.amountMatchGroup = r.nextInt()
                ConfigIO.Keys.NEGATE_AMOUNT -> result.negateAmount = r.nextBoolean()
                ConfigIO.Keys.CURRENCY -> result.currency = r.nextLong()
                ConfigIO.Keys.CURRENCY_GROUP -> result.currencyMatchGroup = r.nextInt()
                else -> throw IllegalStateException("Unexpected template account item: $item")
            }
        }
        r.endObject()
        return result
    }

    @Throws(IOException::class)
    private fun readTemplate(r: JsonReader): TemplateWithAccounts {
        r.beginObject()
        val t = TemplateHeader(0L, "", "")
        val accounts = ArrayList<TemplateAccount>()

        while (r.peek() != JsonToken.END_OBJECT) {
            val item = r.nextName()
            if (r.peek() == JsonToken.NULL) {
                r.nextNull()
                continue
            }
            when (item) {
                ConfigIO.Keys.UUID -> t.uuid = r.nextString()

                ConfigIO.Keys.NAME -> t.name = r.nextString()

                ConfigIO.Keys.REGEX -> t.regularExpression = r.nextString()

                ConfigIO.Keys.TEST_TEXT -> t.testText = r.nextString()

                ConfigIO.Keys.DATE_YEAR -> t.dateYear = r.nextInt()

                ConfigIO.Keys.DATE_YEAR_GROUP -> t.dateYearMatchGroup = r.nextInt()

                ConfigIO.Keys.DATE_MONTH -> t.dateMonth = r.nextInt()

                ConfigIO.Keys.DATE_MONTH_GROUP -> t.dateMonthMatchGroup = r.nextInt()

                ConfigIO.Keys.DATE_DAY -> t.dateDay = r.nextInt()

                ConfigIO.Keys.DATE_DAY_GROUP -> t.dateDayMatchGroup = r.nextInt()

                ConfigIO.Keys.TRANSACTION -> t.transactionDescription = r.nextString()

                ConfigIO.Keys.TRANSACTION_GROUP -> t.transactionDescriptionMatchGroup = r.nextInt()

                ConfigIO.Keys.COMMENT -> t.transactionComment = r.nextString()

                ConfigIO.Keys.COMMENT_GROUP -> t.transactionCommentMatchGroup = r.nextInt()

                ConfigIO.Keys.IS_FALLBACK -> t.isFallback = r.nextBoolean()

                ConfigIO.Keys.ACCOUNTS -> {
                    r.beginArray()
                    while (r.peek() == JsonToken.BEGIN_OBJECT) {
                        accounts.add(readTemplateAccount())
                    }
                    r.endArray()
                }

                else -> throw RuntimeException("Unknown template header item: $item")
            }
        }
        r.endObject()

        return TemplateWithAccounts().apply {
            header = t
            this.accounts = accounts
        }
    }

    @Throws(IOException::class)
    private fun readTemplates(): List<TemplateWithAccounts> {
        val list = ArrayList<TemplateWithAccounts>()
        r.beginArray()
        while (r.peek() == JsonToken.BEGIN_OBJECT) {
            list.add(readTemplate(r))
        }
        r.endArray()
        return list
    }

    @Throws(IOException::class)
    private fun readCommodities(): List<Currency> {
        val list = ArrayList<Currency>()
        r.beginArray()
        while (r.peek() == JsonToken.BEGIN_OBJECT) {
            val c = Currency()
            r.beginObject()
            while (r.peek() != JsonToken.END_OBJECT) {
                val item = r.nextName()
                if (r.peek() == JsonToken.NULL) {
                    r.nextNull()
                    continue
                }
                when (item) {
                    ConfigIO.Keys.NAME -> c.name = r.nextString()
                    ConfigIO.Keys.POSITION -> c.position = r.nextString()
                    ConfigIO.Keys.HAS_GAP -> c.hasGap = r.nextBoolean()
                    else -> throw RuntimeException("Unknown commodity key: $item")
                }
            }
            r.endObject()

            if (c.name.isEmpty()) {
                throw RuntimeException("Missing commodity name")
            }
            list.add(c)
        }
        r.endArray()
        return list
    }

    @Throws(IOException::class)
    private fun readProfiles(): List<Profile> {
        val list = ArrayList<Profile>()
        r.beginArray()
        while (r.peek() == JsonToken.BEGIN_OBJECT) {
            val p = Profile()
            r.beginObject()
            while (r.peek() != JsonToken.END_OBJECT) {
                val item = r.nextName()
                if (r.peek() == JsonToken.NULL) {
                    r.nextNull()
                    continue
                }
                when (item) {
                    ConfigIO.Keys.UUID -> p.uuid = r.nextString()
                    ConfigIO.Keys.NAME -> p.name = r.nextString()
                    ConfigIO.Keys.URL -> p.url = r.nextString()
                    ConfigIO.Keys.USE_AUTH -> p.useAuthentication = r.nextBoolean()
                    ConfigIO.Keys.AUTH_USER -> p.authUser = r.nextString()
                    ConfigIO.Keys.AUTH_PASS -> p.authPassword = r.nextString()
                    ConfigIO.Keys.API_VER -> p.apiVersion = r.nextInt()
                    ConfigIO.Keys.CAN_POST -> p.permitPosting = r.nextBoolean()
                    ConfigIO.Keys.DEFAULT_COMMODITY -> p.setDefaultCommodity(r.nextString())
                    ConfigIO.Keys.SHOW_COMMODITY -> p.showCommodityByDefault = r.nextBoolean()
                    ConfigIO.Keys.SHOW_COMMENTS -> p.showCommentsByDefault = r.nextBoolean()
                    ConfigIO.Keys.FUTURE_DATES -> p.futureDates = r.nextInt()
                    ConfigIO.Keys.PREF_ACCOUNT -> p.preferredAccountsFilter = r.nextString()
                    ConfigIO.Keys.COLOUR -> p.theme = r.nextInt()
                    else -> throw IllegalStateException("Unexpected profile item: $item")
                }
            }
            r.endObject()
            list.add(p)
        }
        r.endArray()
        return list
    }

    fun restoreAll() {
        restoreCommodities()
        restoreProfiles()
        restoreTemplates()
        restoreCurrentProfile()
    }

    private fun restoreTemplates() {
        val templatesList = templates ?: return

        val dao = DB.get().getTemplateDAO()
        for (t in templatesList) {
            if (dao.getTemplateWithAccountsByUuidSync(t.header.uuid) == null) {
                dao.insertSync(t)
            }
        }
    }

    private fun restoreProfiles() {
        val profilesList = profiles ?: return

        val dao = DB.get().getProfileDAO()
        for (p in profilesList) {
            if (dao.getByUuidSync(p.uuid) == null) {
                dao.insert(p)
            }
        }
    }

    private fun restoreCommodities() {
        val commoditiesList = commodities ?: return

        val dao = DB.get().getCurrencyDAO()
        for (c in commoditiesList) {
            if (dao.getByNameSync(c.name) == null) {
                dao.insert(c)
            }
        }
    }

    private fun restoreCurrentProfile() {
        if (currentProfile == null) {
            Logger.debug("backup", "Not restoring current profile (not present in backup)")
            return
        }

        val currentProfileUuid = currentProfile ?: return
        val dao = DB.get().getProfileDAO()
        val p = dao.getByUuidSync(currentProfileUuid)

        if (p != null) {
            Logger.debug("backup", "Restoring current profile ${p.name}")
            Data.postCurrentProfile(p)
            App.storeStartupProfileAndTheme(p.id, p.theme)
        } else {
            Logger.debug("backup", "Not restoring profile $currentProfile: not found in DB")
        }
    }
}
