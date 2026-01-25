/*
 * Use cases for template CRUD and observation.
 */
package net.ktnx.mobileledger.domain.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import net.ktnx.mobileledger.domain.model.Template
import net.ktnx.mobileledger.domain.repository.TemplateRepository

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
