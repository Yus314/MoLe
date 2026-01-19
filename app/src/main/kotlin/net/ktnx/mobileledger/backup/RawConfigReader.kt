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
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.ensureActive
import logcat.LogPriority
import logcat.logcat
import net.ktnx.mobileledger.data.repository.CurrencyRepository
import net.ktnx.mobileledger.data.repository.PreferencesRepository
import net.ktnx.mobileledger.data.repository.ProfileRepository
import net.ktnx.mobileledger.data.repository.TemplateRepository
import net.ktnx.mobileledger.data.repository.mapper.ProfileMapper.toDomain
import net.ktnx.mobileledger.data.repository.mapper.ProfileMapper.toEntity
import net.ktnx.mobileledger.db.Currency
import net.ktnx.mobileledger.db.Profile
import net.ktnx.mobileledger.db.TemplateAccount
import net.ktnx.mobileledger.db.TemplateHeader
import net.ktnx.mobileledger.db.TemplateWithAccounts

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
                BackupKeys.COMMODITIES -> commodities = readCommodities()
                BackupKeys.PROFILES -> profiles = readProfiles()
                BackupKeys.TEMPLATES -> templates = readTemplates()
                BackupKeys.CURRENT_PROFILE -> currentProfile = r.nextString()
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
                BackupKeys.NAME -> result.accountName = r.nextString()
                BackupKeys.NAME_GROUP -> result.accountNameMatchGroup = r.nextInt()
                BackupKeys.COMMENT -> result.accountComment = r.nextString()
                BackupKeys.COMMENT_GROUP -> result.accountCommentMatchGroup = r.nextInt()
                BackupKeys.AMOUNT -> result.amount = r.nextDouble().toFloat()
                BackupKeys.AMOUNT_GROUP -> result.amountMatchGroup = r.nextInt()
                BackupKeys.NEGATE_AMOUNT -> result.negateAmount = r.nextBoolean()
                BackupKeys.CURRENCY -> result.currency = r.nextLong()
                BackupKeys.CURRENCY_GROUP -> result.currencyMatchGroup = r.nextInt()
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
                BackupKeys.UUID -> t.uuid = r.nextString()

                BackupKeys.NAME -> t.name = r.nextString()

                BackupKeys.REGEX -> t.regularExpression = r.nextString()

                BackupKeys.TEST_TEXT -> t.testText = r.nextString()

                BackupKeys.DATE_YEAR -> t.dateYear = r.nextInt()

                BackupKeys.DATE_YEAR_GROUP -> t.dateYearMatchGroup = r.nextInt()

                BackupKeys.DATE_MONTH -> t.dateMonth = r.nextInt()

                BackupKeys.DATE_MONTH_GROUP -> t.dateMonthMatchGroup = r.nextInt()

                BackupKeys.DATE_DAY -> t.dateDay = r.nextInt()

                BackupKeys.DATE_DAY_GROUP -> t.dateDayMatchGroup = r.nextInt()

                BackupKeys.TRANSACTION -> t.transactionDescription = r.nextString()

                BackupKeys.TRANSACTION_GROUP -> t.transactionDescriptionMatchGroup = r.nextInt()

                BackupKeys.COMMENT -> t.transactionComment = r.nextString()

                BackupKeys.COMMENT_GROUP -> t.transactionCommentMatchGroup = r.nextInt()

                BackupKeys.IS_FALLBACK -> t.isFallback = r.nextBoolean()

                BackupKeys.ACCOUNTS -> {
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
                    BackupKeys.NAME -> c.name = r.nextString()
                    BackupKeys.POSITION -> c.position = r.nextString()
                    BackupKeys.HAS_GAP -> c.hasGap = r.nextBoolean()
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
                    BackupKeys.UUID -> p.uuid = r.nextString()
                    BackupKeys.NAME -> p.name = r.nextString()
                    BackupKeys.URL -> p.url = r.nextString()
                    BackupKeys.USE_AUTH -> p.useAuthentication = r.nextBoolean()
                    BackupKeys.AUTH_USER -> p.authUser = r.nextString()
                    BackupKeys.AUTH_PASS -> p.authPassword = r.nextString()
                    BackupKeys.API_VER -> p.apiVersion = r.nextInt()
                    BackupKeys.CAN_POST -> p.permitPosting = r.nextBoolean()
                    BackupKeys.DEFAULT_COMMODITY -> p.setDefaultCommodity(r.nextString())
                    BackupKeys.SHOW_COMMODITY -> p.showCommodityByDefault = r.nextBoolean()
                    BackupKeys.SHOW_COMMENTS -> p.showCommentsByDefault = r.nextBoolean()
                    BackupKeys.FUTURE_DATES -> p.futureDates = r.nextInt()
                    BackupKeys.PREF_ACCOUNT -> p.preferredAccountsFilter = r.nextString()
                    BackupKeys.COLOUR -> p.theme = r.nextInt()
                    else -> throw IllegalStateException("Unexpected profile item: $item")
                }
            }
            r.endObject()
            list.add(p)
        }
        r.endArray()
        return list
    }

    suspend fun restoreAll(
        profileRepository: ProfileRepository,
        templateRepository: TemplateRepository,
        currencyRepository: CurrencyRepository,
        preferencesRepository: PreferencesRepository
    ) {
        coroutineContext.ensureActive()
        restoreCommodities(currencyRepository)
        coroutineContext.ensureActive()
        restoreProfiles(profileRepository)
        coroutineContext.ensureActive()
        restoreTemplates(templateRepository)
        coroutineContext.ensureActive()
        restoreCurrentProfile(profileRepository, preferencesRepository)
    }

    private suspend fun restoreTemplates(templateRepository: TemplateRepository) {
        val templatesList = templates ?: return

        for (t in templatesList) {
            coroutineContext.ensureActive()
            if (templateRepository.getTemplateWithAccountsByUuidSync(t.header.uuid) == null) {
                templateRepository.insertTemplateWithAccounts(t)
            }
        }
    }

    private suspend fun restoreProfiles(profileRepository: ProfileRepository) {
        val profilesList = profiles ?: return

        for (p in profilesList) {
            coroutineContext.ensureActive()
            if (profileRepository.getProfileByUuidSync(p.uuid) == null) {
                // Convert db.Profile to domain.Profile for repository
                profileRepository.insertProfile(p.toDomain())
            }
        }
    }

    private suspend fun restoreCommodities(currencyRepository: CurrencyRepository) {
        val commoditiesList = commodities ?: return

        for (c in commoditiesList) {
            coroutineContext.ensureActive()
            if (currencyRepository.getCurrencyByName(c.name) == null) {
                currencyRepository.insertCurrency(c)
            }
        }
    }

    private suspend fun restoreCurrentProfile(
        profileRepository: ProfileRepository,
        preferencesRepository: PreferencesRepository
    ) {
        if (currentProfile == null) {
            logcat { "Not restoring current profile (not present in backup)" }
            return
        }

        val currentProfileUuid = currentProfile ?: return
        val p = profileRepository.getProfileByUuidSync(currentProfileUuid)

        if (p != null) {
            logcat { "Restoring current profile ${p.name}" }
            profileRepository.setCurrentProfile(p)
            preferencesRepository.setStartupProfileId(p.id ?: 0)
            preferencesRepository.setStartupTheme(p.theme)
        } else {
            logcat { "Not restoring profile $currentProfile: not found in DB" }
        }
    }
}
