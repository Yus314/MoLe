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

import java.io.IOException
import java.io.OutputStream
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.ktnx.mobileledger.core.database.entity.TemplateAccount
import net.ktnx.mobileledger.core.database.entity.TemplateWithAccounts
import net.ktnx.mobileledger.core.domain.model.Currency
import net.ktnx.mobileledger.core.domain.model.Profile
import net.ktnx.mobileledger.core.domain.repository.CurrencyRepository
import net.ktnx.mobileledger.core.domain.repository.ProfileRepository
import net.ktnx.mobileledger.domain.repository.TemplateRepository
import net.ktnx.mobileledger.json.API

/**
 * Writes MoLe configuration to a backup file using kotlinx-serialization.
 *
 * Uses explicit JSON construction to match the original backup format with
 * conditional field writing.
 */
class RawConfigWriter(
    private val outputStream: OutputStream,
    private val profileRepository: ProfileRepository,
    private val templateRepository: TemplateRepository,
    private val currencyRepository: CurrencyRepository
) {
    private val prettyJson = Json { prettyPrint = true }

    @Throws(IOException::class)
    suspend fun writeConfig() {
        coroutineContext.ensureActive()

        val jsonObject = buildJsonObject {
            // Write commodities
            val commodities = getCommodities()
            if (commodities.isNotEmpty()) {
                put(
                    BackupKeys.COMMODITIES,
                    buildJsonArray {
                        commodities.forEach { add(buildCommodityJson(it)) }
                    }
                )
            }

            coroutineContext.ensureActive()

            // Write profiles
            val profiles = getProfiles()
            if (profiles.isNotEmpty()) {
                put(
                    BackupKeys.PROFILES,
                    buildJsonArray {
                        profiles.forEach { add(buildProfileJson(it)) }
                    }
                )
            }

            coroutineContext.ensureActive()

            // Write current profile
            val currentProfile = profileRepository.currentProfile.value
            if (currentProfile != null) {
                put(BackupKeys.CURRENT_PROFILE, currentProfile.uuid)
            }

            coroutineContext.ensureActive()

            // Write templates
            val templates = getTemplates()
            if (templates.isNotEmpty()) {
                put(
                    BackupKeys.TEMPLATES,
                    buildJsonArray {
                        templates.forEach { add(buildTemplateJson(it)) }
                    }
                )
            }
        }

        val jsonString = prettyJson.encodeToString(JsonObject.serializer(), jsonObject)
        val writer = outputStream.bufferedWriter()
        writer.write(jsonString)
        writer.flush()
    }

    private fun buildCommodityJson(c: Currency): JsonObject = buildJsonObject {
        put(BackupKeys.NAME, c.name)
        put(BackupKeys.POSITION, c.position.toDbString())
        put(BackupKeys.HAS_GAP, c.hasGap)
    }

    private fun buildProfileJson(p: Profile): JsonObject = buildJsonObject {
        put(BackupKeys.NAME, p.name)
        put(BackupKeys.UUID, p.uuid)
        put(BackupKeys.URL, p.url)
        put(BackupKeys.USE_AUTH, p.isAuthEnabled)

        if (p.isAuthEnabled) {
            p.authentication?.user?.let { put(BackupKeys.AUTH_USER, it) }
            p.authentication?.password?.let { put(BackupKeys.AUTH_PASS, it) }
        }

        if (p.apiVersion != API.auto.toInt()) {
            put(BackupKeys.API_VER, p.apiVersion)
        }

        put(BackupKeys.CAN_POST, p.canPost)

        if (p.canPost) {
            val defaultCommodity = p.defaultCommodityOrEmpty
            if (defaultCommodity.isNotEmpty()) {
                put(BackupKeys.DEFAULT_COMMODITY, defaultCommodity)
            }
            put(BackupKeys.SHOW_COMMODITY, p.showCommodityByDefault)
            put(BackupKeys.SHOW_COMMENTS, p.showCommentsByDefault)
            put(BackupKeys.FUTURE_DATES, p.futureDates.toInt())
            put(BackupKeys.PREF_ACCOUNT, p.preferredAccountsFilter)
        }

        put(BackupKeys.COLOUR, p.theme)
    }

    private fun buildTemplateJson(t: TemplateWithAccounts): JsonObject = buildJsonObject {
        put(BackupKeys.UUID, t.header.uuid)
        put(BackupKeys.NAME, t.header.name)
        put(BackupKeys.REGEX, t.header.regularExpression)

        t.header.testText?.let { put(BackupKeys.TEST_TEXT, it) }
        t.header.dateYear?.let { put(BackupKeys.DATE_YEAR, it) }
        t.header.dateYearMatchGroup?.let { put(BackupKeys.DATE_YEAR_GROUP, it) }
        t.header.dateMonth?.let { put(BackupKeys.DATE_MONTH, it) }
        t.header.dateMonthMatchGroup?.let { put(BackupKeys.DATE_MONTH_GROUP, it) }
        t.header.dateDay?.let { put(BackupKeys.DATE_DAY, it) }
        t.header.dateDayMatchGroup?.let { put(BackupKeys.DATE_DAY_GROUP, it) }
        t.header.transactionDescription?.let { put(BackupKeys.TRANSACTION, it) }
        t.header.transactionDescriptionMatchGroup?.let { put(BackupKeys.TRANSACTION_GROUP, it) }
        t.header.transactionComment?.let { put(BackupKeys.COMMENT, it) }
        t.header.transactionCommentMatchGroup?.let { put(BackupKeys.COMMENT_GROUP, it) }

        put(BackupKeys.IS_FALLBACK, t.header.isFallback)

        if (t.accounts.isNotEmpty()) {
            put(
                BackupKeys.ACCOUNTS,
                buildJsonArray {
                    t.accounts.forEach { add(buildTemplateAccountJson(it)) }
                }
            )
        }
    }

    private fun buildTemplateAccountJson(a: TemplateAccount): JsonObject = buildJsonObject {
        a.accountName?.let { put(BackupKeys.NAME, it) }
        a.accountNameMatchGroup?.let { put(BackupKeys.NAME_GROUP, it) }
        a.accountComment?.let { put(BackupKeys.COMMENT, it) }
        a.accountCommentMatchGroup?.let { put(BackupKeys.COMMENT_GROUP, it) }
        a.amount?.let { put(BackupKeys.AMOUNT, it.toDouble()) }
        a.amountMatchGroup?.let { put(BackupKeys.AMOUNT_GROUP, it) }
        a.negateAmount?.let { put(BackupKeys.NEGATE_AMOUNT, it) }
        a.currency?.let { put(BackupKeys.CURRENCY, it) }
        a.currencyMatchGroup?.let { put(BackupKeys.CURRENCY_GROUP, it) }
    }

    private suspend fun getCommodities(): List<Currency> =
        currencyRepository.getAllCurrenciesAsDomain().getOrElse { emptyList() }

    private suspend fun getProfiles(): List<Profile> = profileRepository.getAllProfiles().getOrElse { emptyList() }

    @Suppress("DEPRECATION")
    private suspend fun getTemplates(): List<TemplateWithAccounts> =
        templateRepository.getAllTemplatesWithAccounts().getOrElse { emptyList() }
}
