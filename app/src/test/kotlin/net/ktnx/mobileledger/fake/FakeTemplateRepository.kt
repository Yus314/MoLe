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
import net.ktnx.mobileledger.core.database.entity.TemplateWithAccounts
import net.ktnx.mobileledger.core.domain.model.Template
import net.ktnx.mobileledger.data.repository.mapper.TemplateMapper.toDomain
import net.ktnx.mobileledger.data.repository.mapper.TemplateMapper.toEntity
import net.ktnx.mobileledger.domain.repository.TemplateRepository

/**
 * Fake implementation of [TemplateRepository] for testing.
 */
class FakeTemplateRepository : TemplateRepository {

    private val templates = mutableMapOf<Long, TemplateWithAccounts>()
    private val templatesFlow = MutableStateFlow<List<TemplateWithAccounts>>(emptyList())
    private var nextId = 1L

    // Error simulation properties
    var shouldFailGetTemplate: Boolean = false
    var shouldFailDuplicate: Boolean = false
    var shouldFailSave: Boolean = false
    var shouldFailGetAll: Boolean = false
    var shouldFailDelete: Boolean = false
    var errorToThrow: Exception = RuntimeException("Fake error for testing")

    // Convenience property for setting all failure modes
    var shouldFail: Boolean
        get() = shouldFailGetTemplate && shouldFailSave && shouldFailDelete
        set(value) {
            shouldFailGetTemplate = value
            shouldFailSave = value
            shouldFailDelete = value
            shouldFailDuplicate = value
            shouldFailGetAll = value
        }

    // Optional template to return (for null testing)
    var templateToReturn: Template? = null
    var useCustomTemplate: Boolean = false

    // ========================================
    // Domain Model Query Operations
    // ========================================

    override fun observeAllTemplatesAsDomain(): Flow<List<Template>> = templatesFlow.map { list ->
        list.sortedWith(compareBy({ it.header.isFallback }, { it.header.name }))
            .map { it.toDomain() }
    }

    override fun observeTemplateAsDomain(id: Long): Flow<Template?> =
        templatesFlow.map { list -> list.find { it.header.id == id }?.toDomain() }

    override suspend fun getTemplateAsDomain(id: Long): Result<Template?> = when {
        shouldFailGetTemplate -> Result.failure(errorToThrow)
        useCustomTemplate -> Result.success(templateToReturn)
        else -> Result.success(templates[id]?.toDomain())
    }

    override suspend fun getAllTemplatesAsDomain(): Result<List<Template>> = if (shouldFailGetAll) {
        Result.failure(errorToThrow)
    } else {
        Result.success(templates.values.map { it.toDomain() })
    }

    // ========================================
    // Database Entity Query Operations
    // ========================================

    @Deprecated("Internal use for backup/restore only")
    override suspend fun getTemplateWithAccountsByUuid(uuid: String): Result<TemplateWithAccounts?> =
        Result.success(templates.values.find { it.header.uuid == uuid })

    override suspend fun getAllTemplatesWithAccounts(): Result<List<TemplateWithAccounts>> =
        Result.success(templates.values.toList())

    override suspend fun insertTemplateWithAccounts(templateWithAccounts: TemplateWithAccounts): Result<Unit> {
        val id = if (templateWithAccounts.header.id == 0L) nextId++ else templateWithAccounts.header.id
        templateWithAccounts.header.id = id
        templates[id] = templateWithAccounts
        emitFlow()
        return Result.success(Unit)
    }

    override suspend fun deleteTemplateById(id: Long): Result<Boolean> {
        if (shouldFailDelete) return Result.failure(errorToThrow)
        val existed = templates.containsKey(id)
        templates.remove(id)
        emitFlow()
        return Result.success(existed)
    }

    @Deprecated("Use domain model operations instead")
    override suspend fun duplicateTemplate(id: Long): Result<TemplateWithAccounts?> {
        if (shouldFailDuplicate) return Result.failure(errorToThrow)
        val source = templates[id] ?: return Result.success(null)
        val duplicate = source.createDuplicate()
        val newId = nextId++
        duplicate.header.id = newId
        duplicate.header.name = "${source.header.name} (copy)"
        duplicate.accounts.forEach { it.templateId = newId }
        templates[newId] = duplicate
        emitFlow()
        return Result.success(duplicate)
    }

    override suspend fun deleteAllTemplates(): Result<Unit> {
        templates.clear()
        emitFlow()
        return Result.success(Unit)
    }

    override suspend fun saveTemplate(template: Template): Result<Long> {
        if (shouldFailSave) return Result.failure(errorToThrow)
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
        return Result.success(id)
    }

    private fun emitFlow() {
        templatesFlow.value = templates.values.toList()
    }

    fun reset() {
        templates.clear()
        nextId = 1L
        shouldFailGetTemplate = false
        shouldFailDuplicate = false
        shouldFailSave = false
        shouldFailGetAll = false
        shouldFailDelete = false
        errorToThrow = RuntimeException("Fake error for testing")
        templateToReturn = null
        useCustomTemplate = false
        emitFlow()
    }
}
