/*
 * Copyright © 2024 Damyan Ivanov.
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

package net.ktnx.mobileledger.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.logcat
import net.ktnx.mobileledger.data.repository.ProfileRepository
import net.ktnx.mobileledger.data.repository.TemplateRepository
import net.ktnx.mobileledger.domain.model.Template
import net.ktnx.mobileledger.domain.model.TemplateLine
import net.ktnx.mobileledger.model.MatchedTemplate
import net.ktnx.mobileledger.service.CurrencyFormatter
import net.ktnx.mobileledger.service.RowIdGenerator

private fun Template.toTemplateItem() = TemplateItem(
    id = id ?: 0L,
    name = name,
    description = transactionDescription,
    regex = pattern
)

@HiltViewModel
class TemplateApplicatorViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val templateRepository: TemplateRepository,
    private val currencyFormatter: CurrencyFormatter,
    private val rowIdGenerator: RowIdGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow(TemplateApplicatorUiState())
    val uiState: StateFlow<TemplateApplicatorUiState> = _uiState.asStateFlow()

    private val _effects = Channel<TemplateApplicatorEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onEvent(event: TemplateApplicatorEvent) {
        when (event) {
            TemplateApplicatorEvent.ShowTemplateSelector -> showTemplateSelector()
            TemplateApplicatorEvent.DismissTemplateSelector -> dismissTemplateSelector()
            is TemplateApplicatorEvent.ApplyTemplate -> applyTemplate(event.templateId)
            is TemplateApplicatorEvent.ApplyTemplateFromQr -> applyTemplateFromQr(event.qrText)
            is TemplateApplicatorEvent.SearchTemplates -> searchTemplates(event.query)
            TemplateApplicatorEvent.ClearSelection -> clearSelection()
        }
    }

    private fun showTemplateSelector() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            val templates = templateRepository.getAllTemplatesAsDomain()
                .map { it.toTemplateItem() }
            _uiState.update {
                it.copy(
                    showTemplateSelector = true,
                    availableTemplates = templates,
                    isSearching = false
                )
            }
        }
    }

    private fun dismissTemplateSelector() {
        _uiState.update { it.copy(showTemplateSelector = false, selectedTemplateId = null) }
    }

    private fun applyTemplate(templateId: Long) {
        viewModelScope.launch {
            val template = templateRepository.getTemplateAsDomain(templateId)
            if (template != null) {
                val effect = buildApplyTemplateEffect(template)
                _effects.send(effect)
            }
            _uiState.update { it.copy(showTemplateSelector = false, selectedTemplateId = null) }
        }
    }

    private fun buildApplyTemplateEffect(template: Template): TemplateApplicatorEffect.ApplyTemplate {
        val defaultCurrency = profileRepository.currentProfile.value?.defaultCommodityOrEmpty ?: ""

        val newAccounts = template.lines.map { line ->
            val currencyName = line.currencyName ?: defaultCurrency

            TransactionAccountRow(
                id = rowIdGenerator.nextId(),
                accountName = line.accountName ?: "",
                amountText = if (line.amount != null) currencyFormatter.formatNumber(line.amount!!) else "",
                currency = currencyName,
                comment = line.comment ?: "",
                isAmountValid = true
            )
        }

        return TemplateApplicatorEffect.ApplyTemplate(
            description = template.transactionDescription ?: "",
            transactionComment = template.transactionComment,
            date = null,
            accounts = newAccounts
        )
    }

    private fun applyTemplateFromQr(qrText: String) {
        viewModelScope.launch {
            val matched = findMatchingTemplate(qrText)
            if (matched != null) {
                applyMatchedTemplate(matched)
            }
        }
    }

    private suspend fun findMatchingTemplate(text: String): MatchedTemplate? {
        val templates = templateRepository.getAllTemplatesAsDomain()
        for (template in templates) {
            val regex = template.pattern
            if (regex.isBlank()) continue
            try {
                val javaPattern = java.util.regex.Pattern.compile(regex)
                val javaMatcher = javaPattern.matcher(text)
                if (javaMatcher.find()) {
                    return MatchedTemplate(template, javaMatcher.toMatchResult())
                }
            } catch (e: Exception) {
                logcat { "Invalid regex in template: $regex - ${e.message}" }
            }
        }
        return null
    }

    private fun applyMatchedTemplate(matched: MatchedTemplate) {
        viewModelScope.launch {
            val template = matched.template
            val matchResult = matched.matchResult
            val defaultCurrency = profileRepository.currentProfile.value?.defaultCommodityOrEmpty ?: ""

            val description = extractFromMatchGroup(
                matchResult,
                template.transactionDescriptionMatchGroup,
                template.transactionDescription
            ) ?: ""

            val comment = extractFromMatchGroup(
                matchResult,
                template.transactionCommentMatchGroup,
                template.transactionComment
            )

            val date = TemplateMatchGroupExtractor.extractDate(matchResult, template)

            val newAccounts = template.lines.map { line ->
                extractAccountRow(matchResult, line, defaultCurrency)
            }

            val effect = TemplateApplicatorEffect.ApplyTemplate(
                description = description,
                transactionComment = comment,
                date = date,
                accounts = newAccounts
            )
            _effects.send(effect)
        }
    }

    private fun extractFromMatchGroup(
        matchResult: java.util.regex.MatchResult,
        groupNumber: Int?,
        fallback: String?
    ): String? = TemplateMatchGroupExtractor.extractFromMatchGroup(matchResult, groupNumber, fallback)

    private fun extractAccountRow(
        matchResult: java.util.regex.MatchResult,
        line: TemplateLine,
        defaultCurrency: String
    ): TransactionAccountRow {
        val accountName = extractFromMatchGroup(
            matchResult,
            line.accountNameGroup,
            line.accountName
        ) ?: ""

        val amountStr = extractFromMatchGroup(
            matchResult,
            line.amountGroup,
            line.amount?.toString()
        )
        val amount = TemplateMatchGroupExtractor.parseAmount(amountStr, line.negateAmount)
        val amountText = amount?.let { currencyFormatter.formatNumber(it) } ?: ""

        val currencyName = if (line.currencyGroup != null && line.currencyGroup!! > 0) {
            extractFromMatchGroup(matchResult, line.currencyGroup, null)
        } else {
            line.currencyName
        } ?: defaultCurrency

        val comment = extractFromMatchGroup(
            matchResult,
            line.commentGroup,
            line.comment
        ) ?: ""

        return TransactionAccountRow(
            id = rowIdGenerator.nextId(),
            accountName = accountName,
            amountText = amountText,
            currency = currencyName,
            comment = comment,
            isAmountValid = true
        )
    }

    private fun searchTemplates(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            val templates = if (query.isBlank()) {
                templateRepository.getAllTemplatesAsDomain()
            } else {
                templateRepository.getAllTemplatesAsDomain().filter {
                    it.name.contains(query, ignoreCase = true) ||
                        it.transactionDescription?.contains(query, ignoreCase = true) == true
                }
            }
            _uiState.update {
                it.copy(
                    availableTemplates = templates.map { it.toTemplateItem() },
                    isSearching = false
                )
            }
        }
    }

    private fun clearSelection() {
        _uiState.update { it.copy(selectedTemplateId = null) }
    }
}
