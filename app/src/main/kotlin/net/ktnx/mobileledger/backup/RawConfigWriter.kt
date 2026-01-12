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
import kotlinx.coroutines.runBlocking
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
    fun writeConfig() {
        w.beginObject()
        writeCommodities()
        writeProfiles()
        writeCurrentProfile()
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
    private fun writeConfigTemplates() {
        val templates = runBlocking { templateRepository.getAllTemplatesWithAccountsSync() }

        if (templates.isEmpty()) return

        w.name("templates").beginArray()
        for (t in templates) {
            w.beginObject()

            w.name(ConfigIO.Keys.UUID).value(t.header.uuid)
            w.name(ConfigIO.Keys.NAME).value(t.header.name)
            w.name(ConfigIO.Keys.REGEX).value(t.header.regularExpression)
            writeKey(ConfigIO.Keys.TEST_TEXT, t.header.testText)
            writeKey(ConfigIO.Keys.DATE_YEAR, t.header.dateYear)
            writeKey(ConfigIO.Keys.DATE_YEAR_GROUP, t.header.dateYearMatchGroup)
            writeKey(ConfigIO.Keys.DATE_MONTH, t.header.dateMonth)
            writeKey(ConfigIO.Keys.DATE_MONTH_GROUP, t.header.dateMonthMatchGroup)
            writeKey(ConfigIO.Keys.DATE_DAY, t.header.dateDay)
            writeKey(ConfigIO.Keys.DATE_DAY_GROUP, t.header.dateDayMatchGroup)
            writeKey(ConfigIO.Keys.TRANSACTION, t.header.transactionDescription)
            writeKey(ConfigIO.Keys.TRANSACTION_GROUP, t.header.transactionDescriptionMatchGroup)
            writeKey(ConfigIO.Keys.COMMENT, t.header.transactionComment)
            writeKey(ConfigIO.Keys.COMMENT_GROUP, t.header.transactionCommentMatchGroup)
            w.name(ConfigIO.Keys.IS_FALLBACK).value(t.header.isFallback)

            if (t.accounts.isNotEmpty()) {
                w.name(ConfigIO.Keys.ACCOUNTS).beginArray()
                for (a in t.accounts) {
                    w.beginObject()
                    writeKey(ConfigIO.Keys.NAME, a.accountName)
                    writeKey(ConfigIO.Keys.NAME_GROUP, a.accountNameMatchGroup)
                    writeKey(ConfigIO.Keys.COMMENT, a.accountComment)
                    writeKey(ConfigIO.Keys.COMMENT_GROUP, a.accountCommentMatchGroup)
                    writeKey(ConfigIO.Keys.AMOUNT, a.amount)
                    writeKey(ConfigIO.Keys.AMOUNT_GROUP, a.amountMatchGroup)
                    writeKey(ConfigIO.Keys.NEGATE_AMOUNT, a.negateAmount)
                    writeKey(ConfigIO.Keys.CURRENCY, a.currency)
                    writeKey(ConfigIO.Keys.CURRENCY_GROUP, a.currencyMatchGroup)
                    w.endObject()
                }
                w.endArray()
            }
            w.endObject()
        }
        w.endArray()
    }

    @Throws(IOException::class)
    private fun writeCommodities() {
        val list = runBlocking { currencyRepository.getAllCurrenciesSync() }
        if (list.isEmpty()) return

        w.name(ConfigIO.Keys.COMMODITIES).beginArray()
        for (c in list) {
            w.beginObject()
            writeKey(ConfigIO.Keys.NAME, c.name)
            writeKey(ConfigIO.Keys.POSITION, c.position)
            writeKey(ConfigIO.Keys.HAS_GAP, c.hasGap)
            w.endObject()
        }
        w.endArray()
    }

    @Throws(IOException::class)
    private fun writeProfiles() {
        val profiles = runBlocking { profileRepository.getAllProfilesSync() }

        if (profiles.isEmpty()) return

        w.name(ConfigIO.Keys.PROFILES).beginArray()
        for (p in profiles) {
            w.beginObject()

            w.name(ConfigIO.Keys.NAME).value(p.name)
            w.name(ConfigIO.Keys.UUID).value(p.uuid)
            w.name(ConfigIO.Keys.URL).value(p.url)
            w.name(ConfigIO.Keys.USE_AUTH).value(p.isAuthEnabled())
            if (p.isAuthEnabled()) {
                w.name(ConfigIO.Keys.AUTH_USER).value(p.authUser)
                w.name(ConfigIO.Keys.AUTH_PASS).value(p.authPassword)
            }
            if (p.apiVersion != API.auto.toInt()) {
                w.name(ConfigIO.Keys.API_VER).value(p.apiVersion.toLong())
            }
            w.name(ConfigIO.Keys.CAN_POST).value(p.canPost())
            if (p.canPost()) {
                val defaultCommodity = p.getDefaultCommodityOrEmpty()
                if (defaultCommodity.isNotEmpty()) {
                    w.name(ConfigIO.Keys.DEFAULT_COMMODITY).value(defaultCommodity)
                }
                w.name(ConfigIO.Keys.SHOW_COMMODITY).value(p.showCommodityByDefault)
                w.name(ConfigIO.Keys.SHOW_COMMENTS).value(p.showCommentsByDefault)
                w.name(ConfigIO.Keys.FUTURE_DATES).value(p.futureDates)
                w.name(ConfigIO.Keys.PREF_ACCOUNT).value(p.preferredAccountsFilter)
            }
            w.name(ConfigIO.Keys.COLOUR).value(p.theme)

            w.endObject()
        }
        w.endArray()
    }

    @Throws(IOException::class)
    private fun writeCurrentProfile() {
        val currentProfile = profileRepository.currentProfile.value ?: return
        w.name(ConfigIO.Keys.CURRENT_PROFILE).value(currentProfile.uuid)
    }
}
