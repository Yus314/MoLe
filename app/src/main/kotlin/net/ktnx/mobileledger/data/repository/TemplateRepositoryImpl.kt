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

import androidx.lifecycle.asFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import net.ktnx.mobileledger.dao.TemplateAccountDAO
import net.ktnx.mobileledger.dao.TemplateHeaderDAO
import net.ktnx.mobileledger.db.TemplateAccount
import net.ktnx.mobileledger.db.TemplateHeader
import net.ktnx.mobileledger.db.TemplateWithAccounts

/**
 * Implementation of [TemplateRepository] that wraps the existing DAOs.
 *
 * This implementation:
 * - Converts LiveData to Flow for reactive data access
 * - Uses Dispatchers.IO for database operations
 * - Delegates all operations to the underlying DAOs
 *
 * Thread-safety: All operations are safe to call from any coroutine context.
 */
@Singleton
class TemplateRepositoryImpl @Inject constructor(
    private val templateHeaderDAO: TemplateHeaderDAO,
    private val templateAccountDAO: TemplateAccountDAO
) : TemplateRepository {

    // ========================================
    // Query Operations
    // ========================================

    override fun getAllTemplates(): Flow<List<TemplateHeader>> = templateHeaderDAO.getTemplates().asFlow()

    override fun getTemplateById(id: Long): Flow<TemplateHeader?> = templateHeaderDAO.getTemplate(id).asFlow()

    override suspend fun getTemplateByIdSync(id: Long): TemplateHeader? = withContext(Dispatchers.IO) {
        templateHeaderDAO.getTemplateSync(id)
    }

    override fun getTemplateWithAccounts(id: Long): Flow<TemplateWithAccounts?> =
        templateHeaderDAO.getTemplateWithAccounts(id).asFlow()

    override suspend fun getTemplateWithAccountsSync(id: Long): TemplateWithAccounts? = withContext(Dispatchers.IO) {
        templateHeaderDAO.getTemplateWithAccountsSync(id)
    }

    override suspend fun getTemplateWithAccountsByUuidSync(uuid: String): TemplateWithAccounts? =
        withContext(Dispatchers.IO) {
            templateHeaderDAO.getTemplateWithAccountsByUuidSync(uuid)
        }

    override suspend fun getAllTemplatesWithAccountsSync(): List<TemplateWithAccounts> = withContext(Dispatchers.IO) {
        templateHeaderDAO.getAllTemplatesWithAccountsSync()
    }

    // ========================================
    // Mutation Operations
    // ========================================

    override suspend fun insertTemplate(template: TemplateHeader): Long = withContext(Dispatchers.IO) {
        templateHeaderDAO.insertSync(template)
    }

    override suspend fun insertTemplateWithAccounts(templateWithAccounts: TemplateWithAccounts) {
        withContext(Dispatchers.IO) {
            // Insert header first
            val templateId = templateHeaderDAO.insertSync(templateWithAccounts.header)
            // Then insert each account with the new template ID
            for (acc in templateWithAccounts.accounts) {
                acc.templateId = templateId
                templateAccountDAO.insertSync(acc)
            }
        }
    }

    override suspend fun updateTemplate(template: TemplateHeader) {
        withContext(Dispatchers.IO) {
            templateHeaderDAO.updateSync(template)
        }
    }

    override suspend fun deleteTemplate(template: TemplateHeader) {
        withContext(Dispatchers.IO) {
            templateHeaderDAO.deleteSync(template)
        }
    }

    override suspend fun duplicateTemplate(id: Long): TemplateWithAccounts? {
        return withContext(Dispatchers.IO) {
            val src = templateHeaderDAO.getTemplateWithAccountsSync(id) ?: return@withContext null
            val dup = src.createDuplicate()
            dup.header.id = templateHeaderDAO.insertSync(dup.header)
            for (dupAcc in dup.accounts) {
                dupAcc.templateId = dup.header.id
                dupAcc.id = templateAccountDAO.insertSync(dupAcc)
            }
            dup
        }
    }

    override suspend fun deleteAllTemplates() {
        withContext(Dispatchers.IO) {
            templateHeaderDAO.deleteAllSync()
        }
    }

    override suspend fun saveTemplateWithAccounts(
        header: TemplateHeader,
        accounts: List<TemplateAccount>
    ): Long = withContext(Dispatchers.IO) {
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
}
