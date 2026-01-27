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

package net.ktnx.mobileledger.core.testing.fake

import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import net.ktnx.mobileledger.core.domain.model.Template
import net.ktnx.mobileledger.core.domain.repository.TemplateRepository

/**
 * Fake implementation of [TemplateRepository] for testing.
 *
 * Uses domain models directly without depending on core:data mappers.
 */
class FakeTemplateRepository : TemplateRepository {

    private val templates = mutableMapOf<Long, Template>()
    private val templatesFlow = MutableStateFlow<List<Template>>(emptyList())
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
        list.sortedWith(compareBy({ it.isFallback }, { it.name }))
    }

    override fun observeTemplateAsDomain(id: Long): Flow<Template?> =
        templatesFlow.map { list -> list.find { it.id == id } }

    override suspend fun getTemplateAsDomain(id: Long): Result<Template?> = when {
        shouldFailGetTemplate -> Result.failure(errorToThrow)
        useCustomTemplate -> Result.success(templateToReturn)
        else -> Result.success(templates[id])
    }

    override suspend fun getAllTemplatesAsDomain(): Result<List<Template>> = if (shouldFailGetAll) {
        Result.failure(errorToThrow)
    } else {
        Result.success(templates.values.toList())
    }

    override suspend fun getTemplateByUuid(uuid: String): Result<Template?> =
        Result.success(templates.values.find { it.uuid == uuid })

    // ========================================
    // Mutation Operations
    // ========================================

    override suspend fun deleteTemplateById(id: Long): Result<Boolean> {
        if (shouldFailDelete) return Result.failure(errorToThrow)
        val existed = templates.containsKey(id)
        templates.remove(id)
        emitFlow()
        return Result.success(existed)
    }

    override suspend fun duplicateTemplate(id: Long): Result<Template?> {
        if (shouldFailDuplicate) return Result.failure(errorToThrow)
        val source = templates[id] ?: return Result.success(null)
        val newId = nextId++
        val duplicate = source.copy(
            id = newId,
            uuid = UUID.randomUUID().toString(),
            name = "${source.name} (copy)",
            lines = source.lines.map { it.copy(id = null) }
        )
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
        val existingId = template.id
        val id: Long = if (existingId == null || existingId == 0L) nextId++ else existingId
        val templateWithId = template.copy(
            id = id,
            lines = template.lines.mapIndexed { index, line ->
                if (line.id == null) line.copy(id = (id * 1000) + index) else line
            }
        )
        templates[id] = templateWithId
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
