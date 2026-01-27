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
import java.io.InputStream
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.json.Json
import logcat.logcat
import net.ktnx.mobileledger.backup.model.BackupMapper.toDbEntity
import net.ktnx.mobileledger.backup.model.BackupModel
import net.ktnx.mobileledger.core.data.mapper.CurrencyMapper.toDomain
import net.ktnx.mobileledger.core.data.mapper.ProfileMapper.toDomain
import net.ktnx.mobileledger.core.data.mapper.TemplateMapper.toDomain
import net.ktnx.mobileledger.core.database.entity.Currency
import net.ktnx.mobileledger.core.database.entity.Profile
import net.ktnx.mobileledger.core.database.entity.TemplateWithAccounts
import net.ktnx.mobileledger.core.domain.repository.CurrencyRepository
import net.ktnx.mobileledger.core.domain.repository.PreferencesRepository
import net.ktnx.mobileledger.core.domain.repository.ProfileRepository
import net.ktnx.mobileledger.core.domain.repository.TemplateRepository

/**
 * Reads and restores backup configuration from JSON input using kotlinx-serialization.
 *
 * Coordinates the parsing of currencies, profiles, and templates,
 * then restores them to their respective repositories.
 */
class RawConfigReader(private val inputStream: InputStream) {
    private val backupJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

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

        val json = inputStream.bufferedReader().use { it.readText() }
        val backup = backupJson.decodeFromString(BackupModel.serializer(), json)

        commodities = backup.commodities?.map { it.toDbEntity() }
        profiles = backup.profiles?.map { it.toDbEntity() }
        templates = backup.templates?.map { it.toDbEntity() }
        currentProfile = backup.currentProfile
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
            val existing = templateRepository.getTemplateByUuid(t.header.uuid).getOrNull()
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
