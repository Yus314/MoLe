/*
 * Repository Interface Contract: TemplateRepository
 * Feature: 008-data-layer-repository
 *
 * This file defines the contract for TemplateRepository.
 * Implementation will be in TemplateRepositoryImpl.
 */

package net.ktnx.mobileledger.data.repository

import kotlinx.coroutines.flow.Flow
import net.ktnx.mobileledger.db.TemplateHeader
import net.ktnx.mobileledger.db.TemplateWithAccounts

/**
 * Repository for managing Transaction Template data.
 *
 * This repository provides:
 * - CRUD operations for templates
 * - Template duplication
 * - Reactive data streams via Flow
 *
 * Templates are global (not scoped to a profile).
 */
interface TemplateRepository {

    // ========================================
    // Query Operations
    // ========================================

    /**
     * Get all template headers.
     *
     * @return Flow emitting the list of templates whenever data changes.
     */
    fun getAllTemplates(): Flow<List<TemplateHeader>>

    /**
     * Get a template by its ID.
     *
     * @param templateId The template ID.
     * @return Flow emitting the template with accounts, or null if not found.
     */
    fun getTemplateById(templateId: Long): Flow<TemplateWithAccounts?>

    /**
     * Get a template by its ID (synchronous version).
     *
     * @param templateId The template ID.
     * @return The template with accounts, or null if not found.
     */
    suspend fun getTemplateByIdSync(templateId: Long): TemplateWithAccounts?

    /**
     * Get a template by its UUID.
     *
     * @param uuid The template UUID.
     * @return The template with accounts, or null if not found.
     */
    suspend fun getTemplateByUuid(uuid: String): TemplateWithAccounts?

    /**
     * Get all templates with their accounts (synchronous version).
     *
     * @return List of all templates with their accounts.
     */
    suspend fun getAllTemplatesWithAccountsSync(): List<TemplateWithAccounts>

    // ========================================
    // Mutation Operations
    // ========================================

    /**
     * Insert a new template with its accounts.
     *
     * @param template The template to insert.
     */
    suspend fun insertTemplate(template: TemplateWithAccounts)

    /**
     * Update an existing template with its accounts.
     *
     * @param template The template with updated values.
     */
    suspend fun updateTemplate(template: TemplateWithAccounts)

    /**
     * Delete a template.
     * This will also delete all associated template accounts.
     *
     * @param header The template header to delete.
     */
    suspend fun deleteTemplate(header: TemplateHeader)

    /**
     * Duplicate a template.
     * Creates a new template with a new UUID and all accounts copied.
     *
     * @param templateId The ID of the template to duplicate.
     * @return The duplicated template with its new ID.
     */
    suspend fun duplicateTemplate(templateId: Long): TemplateWithAccounts
}
