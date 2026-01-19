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

import android.util.JsonWriter
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStream
import java.io.OutputStreamWriter
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.ensureActive
import net.ktnx.mobileledger.data.repository.CurrencyRepository
import net.ktnx.mobileledger.data.repository.ProfileRepository
import net.ktnx.mobileledger.data.repository.TemplateRepository
import net.ktnx.mobileledger.json.API

class RawConfigWriter(
    outputStream: OutputStream,
    private val profileRepository: ProfileRepository,
    private val templateRepository: TemplateRepository,
    private val currencyRepository: CurrencyRepository
) {
    private val w: JsonWriter = JsonWriter(BufferedWriter(OutputStreamWriter(outputStream))).apply {
        setIndent("  ")
    }

    @Throws(IOException::class)
    suspend fun writeConfig() {
        coroutineContext.ensureActive()
        w.beginObject()
        writeCommodities()
        coroutineContext.ensureActive()
        writeProfiles()
        coroutineContext.ensureActive()
        writeCurrentProfile()
        coroutineContext.ensureActive()
        writeConfigTemplates()
        w.endObject()
        w.flush()
    }

    @Throws(IOException::class)
    private fun writeKey(key: String, value: String?) {
        value?.let { w.name(key).value(it) }
    }

    @Throws(IOException::class)
    private fun writeKey(key: String, value: Int?) {
        value?.let { w.name(key).value(it.toLong()) }
    }

    @Throws(IOException::class)
    private fun writeKey(key: String, value: Long?) {
        value?.let { w.name(key).value(it) }
    }

    @Throws(IOException::class)
    private fun writeKey(key: String, value: Float?) {
        value?.let { w.name(key).value(it.toDouble()) }
    }

    @Throws(IOException::class)
    private fun writeKey(key: String, value: Boolean?) {
        value?.let { w.name(key).value(it) }
    }

    @Throws(IOException::class)
    private suspend fun writeConfigTemplates() {
        val templates = templateRepository.getAllTemplatesWithAccounts()

        if (templates.isEmpty()) return

        w.name("templates").beginArray()
        for (t in templates) {
            w.beginObject()

            w.name(BackupKeys.UUID).value(t.header.uuid)
            w.name(BackupKeys.NAME).value(t.header.name)
            w.name(BackupKeys.REGEX).value(t.header.regularExpression)
            writeKey(BackupKeys.TEST_TEXT, t.header.testText)
            writeKey(BackupKeys.DATE_YEAR, t.header.dateYear)
            writeKey(BackupKeys.DATE_YEAR_GROUP, t.header.dateYearMatchGroup)
            writeKey(BackupKeys.DATE_MONTH, t.header.dateMonth)
            writeKey(BackupKeys.DATE_MONTH_GROUP, t.header.dateMonthMatchGroup)
            writeKey(BackupKeys.DATE_DAY, t.header.dateDay)
            writeKey(BackupKeys.DATE_DAY_GROUP, t.header.dateDayMatchGroup)
            writeKey(BackupKeys.TRANSACTION, t.header.transactionDescription)
            writeKey(BackupKeys.TRANSACTION_GROUP, t.header.transactionDescriptionMatchGroup)
            writeKey(BackupKeys.COMMENT, t.header.transactionComment)
            writeKey(BackupKeys.COMMENT_GROUP, t.header.transactionCommentMatchGroup)
            w.name(BackupKeys.IS_FALLBACK).value(t.header.isFallback)

            if (t.accounts.isNotEmpty()) {
                w.name(BackupKeys.ACCOUNTS).beginArray()
                for (a in t.accounts) {
                    w.beginObject()
                    writeKey(BackupKeys.NAME, a.accountName)
                    writeKey(BackupKeys.NAME_GROUP, a.accountNameMatchGroup)
                    writeKey(BackupKeys.COMMENT, a.accountComment)
                    writeKey(BackupKeys.COMMENT_GROUP, a.accountCommentMatchGroup)
                    writeKey(BackupKeys.AMOUNT, a.amount)
                    writeKey(BackupKeys.AMOUNT_GROUP, a.amountMatchGroup)
                    writeKey(BackupKeys.NEGATE_AMOUNT, a.negateAmount)
                    writeKey(BackupKeys.CURRENCY, a.currency)
                    writeKey(BackupKeys.CURRENCY_GROUP, a.currencyMatchGroup)
                    w.endObject()
                }
                w.endArray()
            }
            w.endObject()
        }
        w.endArray()
    }

    @Throws(IOException::class)
    private suspend fun writeCommodities() {
        val list = currencyRepository.getAllCurrencies()
        if (list.isEmpty()) return

        w.name(BackupKeys.COMMODITIES).beginArray()
        for (c in list) {
            w.beginObject()
            writeKey(BackupKeys.NAME, c.name)
            writeKey(BackupKeys.POSITION, c.position)
            writeKey(BackupKeys.HAS_GAP, c.hasGap)
            w.endObject()
        }
        w.endArray()
    }

    @Throws(IOException::class)
    private suspend fun writeProfiles() {
        val profiles = profileRepository.getAllProfiles()

        if (profiles.isEmpty()) return

        w.name(BackupKeys.PROFILES).beginArray()
        for (p in profiles) {
            w.beginObject()

            w.name(BackupKeys.NAME).value(p.name)
            w.name(BackupKeys.UUID).value(p.uuid)
            w.name(BackupKeys.URL).value(p.url)
            w.name(BackupKeys.USE_AUTH).value(p.isAuthEnabled)
            if (p.isAuthEnabled) {
                w.name(BackupKeys.AUTH_USER).value(p.authentication?.user)
                w.name(BackupKeys.AUTH_PASS).value(p.authentication?.password)
            }
            if (p.apiVersion != API.auto.toInt()) {
                w.name(BackupKeys.API_VER).value(p.apiVersion.toLong())
            }
            w.name(BackupKeys.CAN_POST).value(p.canPost)
            if (p.canPost) {
                val defaultCommodity = p.defaultCommodityOrEmpty
                if (defaultCommodity.isNotEmpty()) {
                    w.name(BackupKeys.DEFAULT_COMMODITY).value(defaultCommodity)
                }
                w.name(BackupKeys.SHOW_COMMODITY).value(p.showCommodityByDefault)
                w.name(BackupKeys.SHOW_COMMENTS).value(p.showCommentsByDefault)
                w.name(BackupKeys.FUTURE_DATES).value(p.futureDates.toInt().toLong())
                w.name(BackupKeys.PREF_ACCOUNT).value(p.preferredAccountsFilter)
            }
            w.name(BackupKeys.COLOUR).value(p.theme)

            w.endObject()
        }
        w.endArray()
    }

    @Throws(IOException::class)
    private fun writeCurrentProfile() {
        val currentProfile = profileRepository.currentProfile.value ?: return
        w.name(BackupKeys.CURRENT_PROFILE).value(currentProfile.uuid)
    }
}
