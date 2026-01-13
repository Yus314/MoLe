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

package net.ktnx.mobileledger.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import net.ktnx.mobileledger.data.repository.TemplateRepository
import net.ktnx.mobileledger.db.TemplateAccount
import net.ktnx.mobileledger.db.TemplateHeader
import net.ktnx.mobileledger.db.TemplateWithAccounts

/**
 * Fake implementation of [TemplateRepository] for testing.
 */
class FakeTemplateRepository : TemplateRepository {

    private val templates = mutableMapOf<Long, TemplateWithAccounts>()
    private var nextId = 1L

    override fun getAllTemplates(): Flow<List<TemplateHeader>> =
        MutableStateFlow(templates.values.map { it.header }.sortedBy { it.name })

    override fun getTemplateById(id: Long): Flow<TemplateHeader?> = MutableStateFlow(templates[id]?.header)

    override suspend fun getTemplateByIdSync(id: Long): TemplateHeader? = templates[id]?.header

    override fun getTemplateWithAccounts(id: Long): Flow<TemplateWithAccounts?> = MutableStateFlow(templates[id])

    override suspend fun getTemplateWithAccountsSync(id: Long): TemplateWithAccounts? = templates[id]

    override suspend fun getTemplateWithAccountsByUuidSync(uuid: String): TemplateWithAccounts? =
        templates.values.find { it.header.uuid == uuid }

    override suspend fun getAllTemplatesWithAccountsSync(): List<TemplateWithAccounts> = templates.values.toList()

    override suspend fun insertTemplate(template: TemplateHeader): Long {
        val id = if (template.id == 0L) nextId++ else template.id
        template.id = id
        templates[id] = TemplateWithAccounts().apply {
            header = template
            accounts = emptyList()
        }
        return id
    }

    override suspend fun insertTemplateWithAccounts(templateWithAccounts: TemplateWithAccounts) {
        val id = if (templateWithAccounts.header.id == 0L) nextId++ else templateWithAccounts.header.id
        templateWithAccounts.header.id = id
        templates[id] = templateWithAccounts
    }

    override suspend fun updateTemplate(template: TemplateHeader) {
        templates[template.id]?.let { existing ->
            val updated = TemplateWithAccounts.from(existing)
            updated.header = template
            templates[template.id] = updated
        }
    }

    override suspend fun deleteTemplate(template: TemplateHeader) {
        templates.remove(template.id)
    }

    override suspend fun duplicateTemplate(id: Long): TemplateWithAccounts? {
        val source = templates[id] ?: return null
        val duplicate = source.createDuplicate()
        val newId = nextId++
        duplicate.header.id = newId
        duplicate.header.name = "${source.header.name} (copy)"
        duplicate.accounts.forEach { it.templateId = newId }
        templates[newId] = duplicate
        return duplicate
    }

    override suspend fun deleteAllTemplates() {
        templates.clear()
    }

    override suspend fun saveTemplateWithAccounts(header: TemplateHeader, accounts: List<TemplateAccount>): Long {
        val id = if (header.id == 0L) nextId++ else header.id
        header.id = id
        accounts.forEach { it.templateId = id }
        templates[id] = TemplateWithAccounts().apply {
            this.header = header
            this.accounts = accounts
        }
        return id
    }

    fun reset() {
        templates.clear()
        nextId = 1L
    }
}
