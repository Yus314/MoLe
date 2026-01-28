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

package net.ktnx.mobileledger.feature.templates.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import net.ktnx.mobileledger.core.domain.model.Template
import net.ktnx.mobileledger.core.domain.repository.TemplateRepository

interface ObserveTemplatesUseCase {
    operator fun invoke(): Flow<List<Template>>
}

interface GetTemplateUseCase {
    suspend operator fun invoke(id: Long): Result<Template?>
}

interface GetAllTemplatesUseCase {
    suspend operator fun invoke(): Result<List<Template>>
}

interface SaveTemplateUseCase {
    suspend operator fun invoke(template: Template): Result<Long>
}

interface DeleteTemplateUseCase {
    suspend operator fun invoke(id: Long): Result<Boolean>
}

interface DuplicateTemplateUseCase {
    suspend operator fun invoke(id: Long): Result<Unit>
}

class ObserveTemplatesUseCaseImpl @Inject constructor(
    private val templateRepository: TemplateRepository
) : ObserveTemplatesUseCase {
    override fun invoke(): Flow<List<Template>> = templateRepository.observeAllTemplatesAsDomain()
}

class GetTemplateUseCaseImpl @Inject constructor(
    private val templateRepository: TemplateRepository
) : GetTemplateUseCase {
    override suspend fun invoke(id: Long): Result<Template?> = templateRepository.getTemplateAsDomain(id)
}

class GetAllTemplatesUseCaseImpl @Inject constructor(
    private val templateRepository: TemplateRepository
) : GetAllTemplatesUseCase {
    override suspend fun invoke(): Result<List<Template>> = templateRepository.getAllTemplatesAsDomain()
}

class SaveTemplateUseCaseImpl @Inject constructor(
    private val templateRepository: TemplateRepository
) : SaveTemplateUseCase {
    override suspend fun invoke(template: Template): Result<Long> = templateRepository.saveTemplate(template)
}

class DeleteTemplateUseCaseImpl @Inject constructor(
    private val templateRepository: TemplateRepository
) : DeleteTemplateUseCase {
    override suspend fun invoke(id: Long): Result<Boolean> = templateRepository.deleteTemplateById(id)
}

class DuplicateTemplateUseCaseImpl @Inject constructor(
    private val templateRepository: TemplateRepository
) : DuplicateTemplateUseCase {
    @Suppress("DEPRECATION")
    override suspend fun invoke(id: Long): Result<Unit> = templateRepository.duplicateTemplate(id).map { }
}
