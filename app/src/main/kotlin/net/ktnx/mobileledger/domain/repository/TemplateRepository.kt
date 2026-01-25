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

package net.ktnx.mobileledger.domain.repository

import kotlinx.coroutines.flow.Flow
import net.ktnx.mobileledger.db.TemplateWithAccounts
import net.ktnx.mobileledger.domain.model.Template

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
 *     val templates = templateRepository.getAllTemplates()
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

    // ========================================
    // Database Entity Query Operations (for internal use)
    // Note: These methods are deprecated. Use domain model methods instead.
    // ========================================

    /**
     * Get a template with its accounts by UUID.
     *
     * @param uuid The template UUID
     * @return Result containing the template with accounts or null if not found
     */
    @Deprecated(message = "Internal use for backup/restore only")
    suspend fun getTemplateWithAccountsByUuid(uuid: String): Result<TemplateWithAccounts?>

    /**
     * Get all templates with their accounts.
     *
     * @return Result containing list of all templates with accounts
     */
    @Deprecated(
        message = "Use getAllTemplatesAsDomain() instead. Internal use for backup only.",
        replaceWith = ReplaceWith("getAllTemplatesAsDomain()")
    )
    suspend fun getAllTemplatesWithAccounts(): Result<List<TemplateWithAccounts>>

    // ========================================
    // Mutation Operations
    // ========================================

    /**
     * Insert a template with its accounts.
     *
     * @param templateWithAccounts The template with accounts to insert
     * @return Result indicating success or failure
     */
    @Deprecated(
        message = "Use saveTemplate() instead. Internal use for backup/restore only.",
        replaceWith = ReplaceWith("saveTemplate(templateWithAccounts.toDomain())")
    )
    suspend fun insertTemplateWithAccounts(templateWithAccounts: TemplateWithAccounts): Result<Unit>

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
     * @return Result containing the duplicated template with accounts, or null if source not found
     */
    @Deprecated(message = "Returns DB entity. Consider using domain model alternative in future.")
    suspend fun duplicateTemplate(id: Long): Result<TemplateWithAccounts?>

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
