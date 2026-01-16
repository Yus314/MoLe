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
     * Get all templates as domain models ordered by fallback status and name.
     *
     * @return Flow that emits the template domain model list whenever it changes
     */
    fun getAllTemplatesAsDomain(): Flow<List<Template>>

    /**
     * Get a template as domain model by its ID.
     *
     * @param id The template ID
     * @return Flow that emits the template domain model when it changes
     */
    fun getTemplateAsDomain(id: Long): Flow<Template?>

    /**
     * Get a template as domain model by its ID synchronously.
     *
     * @param id The template ID
     * @return The template domain model or null if not found
     */
    suspend fun getTemplateAsDomainSync(id: Long): Template?

    /**
     * Get all templates as domain models synchronously.
     *
     * @return List of all template domain models
     */
    suspend fun getAllTemplatesAsDomainSync(): List<Template>

    // ========================================
    // Database Entity Query Operations (for internal use)
    // ========================================

    /**
     * Get all templates ordered by fallback status and name.
     *
     * @return Flow that emits the template list whenever it changes
     */
    fun getAllTemplates(): Flow<List<TemplateHeader>>

    /**
     * Get a template by its ID.
     *
     * @param id The template ID
     * @return Flow that emits the template when it changes
     */
    fun getTemplateById(id: Long): Flow<TemplateHeader?>

    /**
     * Get a template by its ID synchronously.
     *
     * @param id The template ID
     * @return The template or null if not found
     */
    suspend fun getTemplateByIdSync(id: Long): TemplateHeader?

    /**
     * Get a template with its accounts.
     *
     * @param id The template ID
     * @return Flow that emits the template with accounts when it changes
     */
    fun getTemplateWithAccounts(id: Long): Flow<TemplateWithAccounts?>

    /**
     * Get a template with its accounts synchronously.
     *
     * @param id The template ID
     * @return The template with accounts or null if not found
     */
    suspend fun getTemplateWithAccountsSync(id: Long): TemplateWithAccounts?

    /**
     * Get a template with its accounts by UUID synchronously.
     *
     * @param uuid The template UUID
     * @return The template with accounts or null if not found
     */
    suspend fun getTemplateWithAccountsByUuidSync(uuid: String): TemplateWithAccounts?

    /**
     * Get all templates with their accounts synchronously.
     *
     * @return List of all templates with accounts
     */
    suspend fun getAllTemplatesWithAccountsSync(): List<TemplateWithAccounts>

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
}
