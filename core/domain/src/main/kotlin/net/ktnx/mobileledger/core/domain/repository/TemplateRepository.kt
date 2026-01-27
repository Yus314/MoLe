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

package net.ktnx.mobileledger.core.domain.repository

import kotlinx.coroutines.flow.Flow
import net.ktnx.mobileledger.core.domain.model.Template

/**
 * Repository interface for Template data access.
 *
 * This repository provides:
 * - Reactive access to templates via Flow
 * - CRUD operations for templates
 * - Template duplication functionality
 *
 * ## Error Handling
 *
 * All suspend functions return `Result<T>` to handle errors explicitly.
 * Use `result.getOrNull()`, `result.getOrElse {}`, or `result.onSuccess/onFailure` to handle results.
 *
 * ## Usage
 *
 * ```kotlin
 * @HiltViewModel
 * class TemplateViewModel @Inject constructor(
 *     private val templateRepository: TemplateRepository
 * ) : ViewModel() {
 *     val templates = templateRepository.observeAllTemplatesAsDomain()
 *         .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
 * }
 * ```
 */
interface TemplateRepository {

    // ========================================
    // Domain Model Query Operations
    // ========================================

    /**
     * Observe all templates as domain models ordered by fallback status and name.
     *
     * @return Flow that emits the template domain model list whenever it changes
     */
    fun observeAllTemplatesAsDomain(): Flow<List<Template>>

    /**
     * Observe a template as domain model by its ID.
     *
     * @param id The template ID
     * @return Flow that emits the template domain model when it changes
     */
    fun observeTemplateAsDomain(id: Long): Flow<Template?>

    /**
     * Get a template as domain model by its ID.
     *
     * @param id The template ID
     * @return Result containing the template domain model or null if not found
     */
    suspend fun getTemplateAsDomain(id: Long): Result<Template?>

    /**
     * Get all templates as domain models.
     *
     * @return Result containing list of all template domain models
     */
    suspend fun getAllTemplatesAsDomain(): Result<List<Template>>

    /**
     * Get a template by its UUID.
     *
     * @param uuid The template UUID
     * @return Result containing the template domain model or null if not found
     */
    suspend fun getTemplateByUuid(uuid: String): Result<Template?>

    // ========================================
    // Mutation Operations
    // ========================================

    /**
     * Delete a template by its ID.
     *
     * @param id The template ID to delete
     * @return Result containing true if deleted, false if not found
     */
    suspend fun deleteTemplateById(id: Long): Result<Boolean>

    /**
     * Duplicate a template with all its accounts.
     *
     * @param id The ID of the template to duplicate
     * @return Result containing the duplicated template domain model, or null if source not found
     */
    suspend fun duplicateTemplate(id: Long): Result<Template?>

    /**
     * Delete all templates.
     *
     * @return Result indicating success or failure
     */
    suspend fun deleteAllTemplates(): Result<Unit>

    /**
     * Save a template domain model with its lines.
     * Handles insert/update of header and all accounts atomically.
     *
     * @param template The template domain model to save
     * @return Result containing the saved template ID
     */
    suspend fun saveTemplate(template: Template): Result<Long>
}
