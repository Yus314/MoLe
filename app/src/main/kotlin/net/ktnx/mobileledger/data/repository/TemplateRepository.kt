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

import kotlinx.coroutines.flow.Flow
import net.ktnx.mobileledger.db.TemplateAccount
import net.ktnx.mobileledger.db.TemplateHeader
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
     * @return The template domain model or null if not found
     */
    suspend fun getTemplateAsDomain(id: Long): Template?

    /**
     * Get all templates as domain models.
     *
     * @return List of all template domain models
     */
    suspend fun getAllTemplatesAsDomain(): List<Template>

    // ========================================
    // Database Entity Query Operations (for internal use)
    // ========================================

    /**
     * Observe all templates ordered by fallback status and name.
     *
     * @return Flow that emits the template list whenever it changes
     */
    fun observeAllTemplates(): Flow<List<TemplateHeader>>

    /**
     * Observe a template by its ID.
     *
     * @param id The template ID
     * @return Flow that emits the template when it changes
     */
    fun observeTemplateById(id: Long): Flow<TemplateHeader?>

    /**
     * Get a template by its ID.
     *
     * @param id The template ID
     * @return The template or null if not found
     */
    suspend fun getTemplateById(id: Long): TemplateHeader?

    /**
     * Observe a template with its accounts.
     *
     * @param id The template ID
     * @return Flow that emits the template with accounts when it changes
     */
    fun observeTemplateWithAccounts(id: Long): Flow<TemplateWithAccounts?>

    /**
     * Get a template with its accounts.
     *
     * @param id The template ID
     * @return The template with accounts or null if not found
     */
    suspend fun getTemplateWithAccounts(id: Long): TemplateWithAccounts?

    /**
     * Get a template with its accounts by UUID.
     *
     * @param uuid The template UUID
     * @return The template with accounts or null if not found
     */
    suspend fun getTemplateWithAccountsByUuid(uuid: String): TemplateWithAccounts?

    /**
     * Get all templates with their accounts.
     *
     * @return List of all templates with accounts
     */
    suspend fun getAllTemplatesWithAccounts(): List<TemplateWithAccounts>

    // ========================================
    // Mutation Operations
    // ========================================

    /**
     * Insert a new template.
     *
     * @param template The template to insert
     * @return The ID of the inserted template
     */
    suspend fun insertTemplate(template: TemplateHeader): Long

    /**
     * Insert a template with its accounts.
     *
     * @param templateWithAccounts The template with accounts to insert
     */
    suspend fun insertTemplateWithAccounts(templateWithAccounts: TemplateWithAccounts)

    /**
     * Update an existing template.
     *
     * @param template The template to update
     */
    suspend fun updateTemplate(template: TemplateHeader)

    /**
     * Delete a template.
     *
     * @param template The template to delete
     */
    suspend fun deleteTemplate(template: TemplateHeader)

    /**
     * Delete a template by its ID.
     *
     * @param id The template ID to delete
     * @return true if deleted, false if not found
     */
    suspend fun deleteTemplateById(id: Long): Boolean

    /**
     * Duplicate a template with all its accounts.
     *
     * @param id The ID of the template to duplicate
     * @return The duplicated template with accounts, or null if source not found
     */
    suspend fun duplicateTemplate(id: Long): TemplateWithAccounts?

    /**
     * Delete all templates.
     */
    suspend fun deleteAllTemplates()

    /**
     * Save a template with its accounts.
     * Handles insert/update of header and all accounts atomically.
     *
     * @param header The template header to save
     * @param accounts The list of accounts to save
     * @return The saved template ID
     */
    suspend fun saveTemplateWithAccounts(header: TemplateHeader, accounts: List<TemplateAccount>): Long

    /**
     * Save a template domain model with its lines.
     * Handles insert/update of header and all accounts atomically.
     *
     * @param template The template domain model to save
     * @return The saved template ID
     */
    suspend fun saveTemplate(template: Template): Long
}
