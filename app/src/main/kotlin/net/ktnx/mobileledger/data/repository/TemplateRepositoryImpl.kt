/*
 * Copyright © 2026 Damyan Ivanov.
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

package net.ktnx.mobileledger.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import net.ktnx.mobileledger.dao.TemplateAccountDAO
import net.ktnx.mobileledger.dao.TemplateHeaderDAO
import net.ktnx.mobileledger.data.repository.mapper.TemplateMapper.toDomain
import net.ktnx.mobileledger.data.repository.mapper.TemplateMapper.toEntity
import net.ktnx.mobileledger.db.TemplateAccount
import net.ktnx.mobileledger.db.TemplateHeader
import net.ktnx.mobileledger.db.TemplateWithAccounts
import net.ktnx.mobileledger.di.IoDispatcher
import net.ktnx.mobileledger.domain.model.Template
import net.ktnx.mobileledger.domain.usecase.AppExceptionMapper

/**
 * Implementation of [TemplateRepository] that wraps the existing DAOs.
 *
 * This implementation:
 * - Converts LiveData to Flow for reactive data access
 * - Uses ioDispatcher for database operations
 * - Delegates all operations to the underlying DAOs
 * - Returns Result<T> for all suspend operations with error handling
 *
 * Thread-safety: All operations are safe to call from any coroutine context.
 */
@Singleton
class TemplateRepositoryImpl @Inject constructor(
    private val templateHeaderDAO: TemplateHeaderDAO,
    private val templateAccountDAO: TemplateAccountDAO,
    private val currencyRepository: CurrencyRepository,
    private val appExceptionMapper: AppExceptionMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : TemplateRepository {

    /**
     * 通貨IDから通貨名へのマップを構築
     */
    private suspend fun buildCurrencyMap(currencyIds: Set<Long>): Map<Long, String> {
        if (currencyIds.isEmpty()) return emptyMap()
        return currencyIds.mapNotNull { id ->
            currencyRepository.getCurrencyAsDomain(id).getOrNull()?.let { id to it.name }
        }.toMap()
    }

    // ========================================
    // Domain Model Query Operations
    // ========================================

    override fun observeAllTemplatesAsDomain(): Flow<List<Template>> =
        templateHeaderDAO.getTemplatesWithAccounts().map { list ->
            val allCurrencyIds = list.flatMap { it.accounts.mapNotNull { acc -> acc.currency } }.toSet()
            val currencyMap = buildCurrencyMap(allCurrencyIds)
            list.map { it.toDomain(currencyMap) }
        }

    override fun observeTemplateAsDomain(id: Long): Flow<Template?> =
        templateHeaderDAO.getTemplateWithAccounts(id).map { entity ->
            entity?.let {
                val currencyIds = it.accounts.mapNotNull { acc -> acc.currency }.toSet()
                val currencyMap = buildCurrencyMap(currencyIds)
                it.toDomain(currencyMap)
            }
        }

    override suspend fun getTemplateAsDomain(id: Long): Result<Template?> = safeCall(appExceptionMapper) {
        withContext(ioDispatcher) {
            val entity = templateHeaderDAO.getTemplateWithAccountsSync(id)
                ?: return@withContext null
            val currencyIds = entity.accounts.mapNotNull { it.currency }.toSet()
            val currencyMap = buildCurrencyMap(currencyIds)
            entity.toDomain(currencyMap)
        }
    }

    override suspend fun getAllTemplatesAsDomain(): Result<List<Template>> = safeCall(appExceptionMapper) {
        withContext(ioDispatcher) {
            val entities = templateHeaderDAO.getAllTemplatesWithAccountsSync()
            val allCurrencyIds = entities.flatMap { it.accounts.mapNotNull { acc -> acc.currency } }.toSet()
            val currencyMap = buildCurrencyMap(allCurrencyIds)
            entities.map { it.toDomain(currencyMap) }
        }
    }

    // ========================================
    // Database Entity Query Operations (for internal use)
    // ========================================

    @Deprecated("Internal use for backup/restore only")
    override suspend fun getTemplateWithAccountsByUuid(uuid: String): Result<TemplateWithAccounts?> =
        safeCall(appExceptionMapper) {
            withContext(ioDispatcher) {
                templateHeaderDAO.getTemplateWithAccountsByUuidSync(uuid)
            }
        }

    @Deprecated("Use getAllTemplatesAsDomain() instead. Internal use for backup only.")
    override suspend fun getAllTemplatesWithAccounts(): Result<List<TemplateWithAccounts>> =
        safeCall(appExceptionMapper) {
            withContext(ioDispatcher) {
                templateHeaderDAO.getAllTemplatesWithAccountsSync()
            }
        }

    // ========================================
    // Mutation Operations
    // ========================================

    @Deprecated("Use saveTemplate() instead. Internal use for backup/restore only.")
    override suspend fun insertTemplateWithAccounts(templateWithAccounts: TemplateWithAccounts): Result<Unit> =
        safeCall(appExceptionMapper) {
            withContext(ioDispatcher) {
                // Insert header first
                val templateId = templateHeaderDAO.insertSync(templateWithAccounts.header)
                // Then insert each account with the new template ID
                for (acc in templateWithAccounts.accounts) {
                    acc.templateId = templateId
                    templateAccountDAO.insertSync(acc)
                }
            }
        }

    override suspend fun deleteTemplateById(id: Long): Result<Boolean> = safeCall(appExceptionMapper) {
        withContext(ioDispatcher) {
            val template = templateHeaderDAO.getTemplateSync(id)
            if (template != null) {
                templateHeaderDAO.deleteSync(template)
                true
            } else {
                false
            }
        }
    }

    @Deprecated("Returns DB entity. Consider using domain model alternative in future.")
    override suspend fun duplicateTemplate(id: Long): Result<TemplateWithAccounts?> = safeCall(appExceptionMapper) {
        withContext(ioDispatcher) {
            val src = templateHeaderDAO.getTemplateWithAccountsSync(id)
                ?: return@withContext null
            val dup = src.createDuplicate()
            dup.header.id = templateHeaderDAO.insertSync(dup.header)
            for (dupAcc in dup.accounts) {
                dupAcc.templateId = dup.header.id
                dupAcc.id = templateAccountDAO.insertSync(dupAcc)
            }
            dup
        }
    }

    override suspend fun deleteAllTemplates(): Result<Unit> = safeCall(appExceptionMapper) {
        withContext(ioDispatcher) {
            templateHeaderDAO.deleteAllSync()
        }
    }

    private suspend fun saveTemplateWithAccountsInternal(
        header: TemplateHeader,
        accounts: List<TemplateAccount>
    ): Long = withContext(ioDispatcher) {
        val isNew = header.id == 0L
        val savedId = if (isNew) {
            templateHeaderDAO.insertSync(header)
        } else {
            templateHeaderDAO.updateSync(header)
            header.id
        }

        // Save accounts using the existing DAO pattern
        templateAccountDAO.prepareForSave(savedId)

        for (account in accounts) {
            if (account.id <= 0) {
                account.id = 0
                account.templateId = savedId
                templateAccountDAO.insertSync(account)
            } else {
                account.templateId = savedId
                templateAccountDAO.updateSync(account)
            }
        }

        templateAccountDAO.finishSave(savedId)
        savedId
    }

    override suspend fun saveTemplate(template: Template): Result<Long> = safeCall(appExceptionMapper) {
        val entity = template.toEntity()
        saveTemplateWithAccountsInternal(entity.header, entity.accounts)
    }
}
