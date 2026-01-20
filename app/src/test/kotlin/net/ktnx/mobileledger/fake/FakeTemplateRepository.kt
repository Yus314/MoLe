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
import kotlinx.coroutines.flow.map
import net.ktnx.mobileledger.data.repository.TemplateRepository
import net.ktnx.mobileledger.data.repository.mapper.TemplateMapper.toDomain
import net.ktnx.mobileledger.data.repository.mapper.TemplateMapper.toEntity
import net.ktnx.mobileledger.db.TemplateHeader
import net.ktnx.mobileledger.db.TemplateWithAccounts
import net.ktnx.mobileledger.domain.model.Template

/**
 * Fake implementation of [TemplateRepository] for testing.
 */
class FakeTemplateRepository : TemplateRepository {

    private val templates = mutableMapOf<Long, TemplateWithAccounts>()
    private val templatesFlow = MutableStateFlow<List<TemplateWithAccounts>>(emptyList())
    private var nextId = 1L

    // ========================================
    // Domain Model Query Operations
    // ========================================

    override fun observeAllTemplatesAsDomain(): Flow<List<Template>> =
        templatesFlow.map { list -> list.map { it.toDomain() } }

    override fun observeTemplateAsDomain(id: Long): Flow<Template?> =
        templatesFlow.map { list -> list.find { it.header.id == id }?.toDomain() }

    override suspend fun getTemplateAsDomain(id: Long): Template? = templates[id]?.toDomain()

    override suspend fun getAllTemplatesAsDomain(): List<Template> = templates.values.map { it.toDomain() }

    // ========================================
    // Database Entity Query Operations
    // ========================================

    override fun observeAllTemplates(): Flow<List<TemplateHeader>> =
        MutableStateFlow(templates.values.map { it.header }.sortedBy { it.name })

    override fun observeTemplateById(id: Long): Flow<TemplateHeader?> = MutableStateFlow(templates[id]?.header)

    override suspend fun getTemplateById(id: Long): TemplateHeader? = templates[id]?.header

    override fun observeTemplateWithAccounts(id: Long): Flow<TemplateWithAccounts?> = MutableStateFlow(templates[id])

    override suspend fun getTemplateWithAccounts(id: Long): TemplateWithAccounts? = templates[id]

    override suspend fun getTemplateWithAccountsByUuid(uuid: String): TemplateWithAccounts? =
        templates.values.find { it.header.uuid == uuid }

    override suspend fun getAllTemplatesWithAccounts(): List<TemplateWithAccounts> = templates.values.toList()

    override suspend fun insertTemplateWithAccounts(templateWithAccounts: TemplateWithAccounts) {
        val id = if (templateWithAccounts.header.id == 0L) nextId++ else templateWithAccounts.header.id
        templateWithAccounts.header.id = id
        templates[id] = templateWithAccounts
        emitFlow()
    }

    override suspend fun deleteTemplateById(id: Long): Boolean {
        val existed = templates.containsKey(id)
        templates.remove(id)
        emitFlow()
        return existed
    }

    override suspend fun duplicateTemplate(id: Long): TemplateWithAccounts? {
        val source = templates[id] ?: return null
        val duplicate = source.createDuplicate()
        val newId = nextId++
        duplicate.header.id = newId
        duplicate.header.name = "${source.header.name} (copy)"
        duplicate.accounts.forEach { it.templateId = newId }
        templates[newId] = duplicate
        emitFlow()
        return duplicate
    }

    override suspend fun deleteAllTemplates() {
        templates.clear()
        emitFlow()
    }

    override suspend fun saveTemplate(template: Template): Long {
        val entity = template.toEntity()
        val header = entity.header
        val accounts = entity.accounts
        val id = if (header.id == 0L) nextId++ else header.id
        header.id = id
        accounts.forEach { it.templateId = id }
        templates[id] = TemplateWithAccounts().apply {
            this.header = header
            this.accounts = accounts
        }
        emitFlow()
        return id
    }

    private fun emitFlow() {
        templatesFlow.value = templates.values.toList()
    }

    fun reset() {
        templates.clear()
        nextId = 1L
        emitFlow()
    }
}
