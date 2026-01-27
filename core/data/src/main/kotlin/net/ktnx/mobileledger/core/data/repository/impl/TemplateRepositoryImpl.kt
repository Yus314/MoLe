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

package net.ktnx.mobileledger.core.data.repository.impl

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import net.ktnx.mobileledger.core.common.di.IoDispatcher
import net.ktnx.mobileledger.core.data.exception.CoreExceptionMapper
import net.ktnx.mobileledger.core.data.mapper.TemplateMapper.toDomain
import net.ktnx.mobileledger.core.data.mapper.TemplateMapper.toEntity
import net.ktnx.mobileledger.core.data.repository.safeCall
import net.ktnx.mobileledger.core.database.dao.TemplateAccountDAO
import net.ktnx.mobileledger.core.database.dao.TemplateHeaderDAO
import net.ktnx.mobileledger.core.database.entity.TemplateAccount
import net.ktnx.mobileledger.core.database.entity.TemplateHeader
import net.ktnx.mobileledger.core.domain.model.Template
import net.ktnx.mobileledger.core.domain.repository.CurrencyRepository
import net.ktnx.mobileledger.core.domain.repository.TemplateRepository

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
    private val exceptionMapper: CoreExceptionMapper,
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

    override suspend fun getTemplateAsDomain(id: Long): Result<Template?> = safeCall(exceptionMapper) {
        withContext(ioDispatcher) {
            val entity = templateHeaderDAO.getTemplateWithAccountsSync(id)
                ?: return@withContext null
            val currencyIds = entity.accounts.mapNotNull { it.currency }.toSet()
            val currencyMap = buildCurrencyMap(currencyIds)
            entity.toDomain(currencyMap)
        }
    }

    override suspend fun getAllTemplatesAsDomain(): Result<List<Template>> = safeCall(exceptionMapper) {
        withContext(ioDispatcher) {
            val entities = templateHeaderDAO.getAllTemplatesWithAccountsSync()
            val allCurrencyIds = entities.flatMap { it.accounts.mapNotNull { acc -> acc.currency } }.toSet()
            val currencyMap = buildCurrencyMap(allCurrencyIds)
            entities.map { it.toDomain(currencyMap) }
        }
    }

    override suspend fun getTemplateByUuid(uuid: String): Result<Template?> = safeCall(exceptionMapper) {
        withContext(ioDispatcher) {
            val entity = templateHeaderDAO.getTemplateWithAccountsByUuidSync(uuid)
                ?: return@withContext null
            val currencyIds = entity.accounts.mapNotNull { it.currency }.toSet()
            val currencyMap = buildCurrencyMap(currencyIds)
            entity.toDomain(currencyMap)
        }
    }

    // ========================================
    // Mutation Operations
    // ========================================

    override suspend fun deleteTemplateById(id: Long): Result<Boolean> = safeCall(exceptionMapper) {
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

    override suspend fun duplicateTemplate(id: Long): Result<Template?> = safeCall(exceptionMapper) {
        withContext(ioDispatcher) {
            val src = templateHeaderDAO.getTemplateWithAccountsSync(id)
                ?: return@withContext null
            val dup = src.createDuplicate()
            dup.header.id = templateHeaderDAO.insertSync(dup.header)
            for (dupAcc in dup.accounts) {
                dupAcc.templateId = dup.header.id
                dupAcc.id = templateAccountDAO.insertSync(dupAcc)
            }
            // Convert to domain model and resolve currencies
            val currencyIds = dup.accounts.mapNotNull { it.currency }.toSet()
            val currencyMap = buildCurrencyMap(currencyIds)
            dup.toDomain(currencyMap)
        }
    }

    override suspend fun deleteAllTemplates(): Result<Unit> = safeCall(exceptionMapper) {
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

    override suspend fun saveTemplate(template: Template): Result<Long> = safeCall(exceptionMapper) {
        val entity = template.toEntity()
        saveTemplateWithAccountsInternal(entity.header, entity.accounts)
    }
}
