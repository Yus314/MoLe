/*
 * Copyright © 2022 Damyan Ivanov.
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
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import kotlinx.coroutines.runBlocking
import net.ktnx.mobileledger.data.repository.CurrencyRepository
import net.ktnx.mobileledger.data.repository.ProfileRepository
import net.ktnx.mobileledger.data.repository.TemplateRepository
import net.ktnx.mobileledger.di.BackupEntryPoint
import net.ktnx.mobileledger.utils.Misc

class ConfigReader
@Throws(FileNotFoundException::class)
constructor(
    context: Context,
    uri: Uri,
    onErrorListener: OnErrorListener?,
    private val onDoneListener: OnDoneListener?
) : ConfigIO(context, uri, onErrorListener) {

    private lateinit var r: RawConfigReader
    private val profileRepository: ProfileRepository
    private val templateRepository: TemplateRepository
    private val currencyRepository: CurrencyRepository

    init {
        val entryPoint = BackupEntryPoint.get(context)
        profileRepository = entryPoint.profileRepository()
        templateRepository = entryPoint.templateRepository()
        currencyRepository = entryPoint.currencyRepository()
    }

    override fun getStreamMode(): String = "r"

    override fun initStream() {
        val fd = pfd?.fileDescriptor ?: throw IllegalStateException("File descriptor not available")
        r = RawConfigReader(FileInputStream(fd))
    }

    @Throws(IOException::class)
    override fun processStream() {
        r.readConfig()
        r.restoreAll(profileRepository, templateRepository, currencyRepository)
        val currentProfile = r.currentProfile

        if (profileRepository.currentProfile.value == null) {
            var p = runBlocking {
                if (currentProfile != null) profileRepository.getProfileByUuidSync(currentProfile) else null
            }

            if (p == null) {
                p = runBlocking { profileRepository.getAnyProfile() }
            }

            p?.let { profileRepository.setCurrentProfile(it) }
        }

        onDoneListener?.let { Misc.onMainThread { it.done() } }
    }

    abstract class OnDoneListener {
        abstract fun done()
    }
}
