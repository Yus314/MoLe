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
import logcat.logcat
import net.ktnx.mobileledger.data.repository.CurrencyRepository
import net.ktnx.mobileledger.data.repository.PreferencesRepository
import net.ktnx.mobileledger.data.repository.ProfileRepository
import net.ktnx.mobileledger.data.repository.TemplateRepository
import net.ktnx.mobileledger.data.repository.mapper.CurrencyMapper.toDomain
import net.ktnx.mobileledger.data.repository.mapper.ProfileMapper.toDomain
import net.ktnx.mobileledger.data.repository.mapper.TemplateMapper.toDomain
import net.ktnx.mobileledger.db.Currency
import net.ktnx.mobileledger.db.Profile
import net.ktnx.mobileledger.db.TemplateWithAccounts

/**
 * Reads and restores backup configuration from JSON input.
 *
 * Coordinates the parsing of currencies, profiles, and templates using
 * dedicated parser classes, then restores them to their respective repositories.
 */
class RawConfigReader(inputStream: InputStream) {
    private val reader: JsonReader = JsonReader(BufferedReader(InputStreamReader(inputStream)))
    private val currencyParser = CurrencyBackupParser()
    private val profileParser = ProfileBackupParser()
    private val templateParser = TemplateBackupParser()

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

        reader.beginObject()
        while (reader.hasNext()) {
            val item = reader.nextName()
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull()
                continue
            }
            when (item) {
                BackupKeys.COMMODITIES -> commodities = currencyParser.parse(reader)
                BackupKeys.PROFILES -> profiles = profileParser.parse(reader)
                BackupKeys.TEMPLATES -> templates = templateParser.parse(reader)
                BackupKeys.CURRENT_PROFILE -> currentProfile = reader.nextString()
                else -> throw RuntimeException("unexpected top-level item $item")
            }
        }
        reader.endObject()
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
            val existing = templateRepository.getTemplateWithAccountsByUuid(t.header.uuid).getOrNull()
            if (existing == null) {
                templateRepository.saveTemplate(t.toDomain())
            }
        }
    }

    private suspend fun restoreProfiles(profileRepository: ProfileRepository) {
        val profilesList = profiles ?: return

        for (p in profilesList) {
            coroutineContext.ensureActive()
            val existing = profileRepository.getProfileByUuid(p.uuid).getOrNull()
            if (existing == null) {
                profileRepository.insertProfile(p.toDomain())
            }
        }
    }

    private suspend fun restoreCommodities(currencyRepository: CurrencyRepository) {
        val commoditiesList = commodities ?: return

        for (c in commoditiesList) {
            coroutineContext.ensureActive()
            val existing = currencyRepository.getCurrencyAsDomainByName(c.name).getOrNull()
            if (existing == null) {
                currencyRepository.saveCurrency(c.toDomain())
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
        val p = profileRepository.getProfileByUuid(currentProfileUuid).getOrNull()

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
