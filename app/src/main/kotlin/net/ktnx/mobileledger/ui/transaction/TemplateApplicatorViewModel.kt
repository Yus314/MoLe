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
import net.ktnx.mobileledger.core.domain.model.MatchedTemplate
import net.ktnx.mobileledger.core.domain.model.Template
import net.ktnx.mobileledger.domain.usecase.GetAllTemplatesUseCase
import net.ktnx.mobileledger.domain.usecase.GetTemplateUseCase
import net.ktnx.mobileledger.domain.usecase.ObserveCurrentProfileUseCase
import net.ktnx.mobileledger.feature.templates.usecase.TemplateMatcher
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
    observeCurrentProfileUseCase: ObserveCurrentProfileUseCase,
    private val getAllTemplatesUseCase: GetAllTemplatesUseCase,
    private val getTemplateUseCase: GetTemplateUseCase,
    private val templateMatcher: TemplateMatcher,
    private val currencyFormatter: CurrencyFormatter,
    private val rowIdGenerator: RowIdGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow(TemplateApplicatorUiState())
    val uiState: StateFlow<TemplateApplicatorUiState> = _uiState.asStateFlow()

    private val _effects = Channel<TemplateApplicatorEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val currentProfile = observeCurrentProfileUseCase()

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
            getAllTemplatesUseCase()
                .onSuccess { templates ->
                    _uiState.update {
                        it.copy(
                            showTemplateSelector = true,
                            availableTemplates = templates.map { t -> t.toTemplateItem() },
                            isSearching = false
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isSearching = false) }
                }
        }
    }

    private fun dismissTemplateSelector() {
        _uiState.update { it.copy(showTemplateSelector = false, selectedTemplateId = null) }
    }

    private fun applyTemplate(templateId: Long) {
        viewModelScope.launch {
            val template = getTemplateUseCase(templateId).getOrNull()
            if (template != null) {
                val effect = buildApplyTemplateEffect(template)
                _effects.send(effect)
            }
            _uiState.update { it.copy(showTemplateSelector = false, selectedTemplateId = null) }
        }
    }

    private fun buildApplyTemplateEffect(template: Template): TemplateApplicatorEffect.ApplyTemplate {
        val defaultCurrency = currentProfile.value?.defaultCommodityOrEmpty ?: ""

        val newAccounts = template.lines.map { line ->
            val currencyName = line.currencyName ?: defaultCurrency

            TransactionAccountRow(
                id = rowIdGenerator.nextId(),
                accountName = line.accountName ?: "",
                amountText = line.amount?.let { currencyFormatter.formatNumber(it) } ?: "",
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

    /**
     * Apply template from QR code text.
     *
     * Delegates template matching and data extraction to [TemplateMatcher].
     */
    private fun applyTemplateFromQr(qrText: String) {
        viewModelScope.launch {
            getAllTemplatesUseCase()
                .onSuccess { templates ->
                    val matched = templateMatcher.findMatch(qrText, templates)
                    if (matched != null) {
                        applyMatchedTemplate(matched)
                    }
                }
        }
    }

    /**
     * Apply a matched template by extracting data and sending the effect.
     *
     * Delegates extraction to [TemplateMatcher] and converts to UI format.
     */
    private fun applyMatchedTemplate(matched: MatchedTemplate) {
        viewModelScope.launch {
            val defaultCurrency = currentProfile.value?.defaultCommodityOrEmpty ?: ""
            val extracted = templateMatcher.extractTransaction(matched, defaultCurrency)

            val newAccounts = extracted.lines.map { line ->
                TransactionAccountRow(
                    id = rowIdGenerator.nextId(),
                    accountName = line.accountName,
                    amountText = line.amount?.let { currencyFormatter.formatNumber(it) } ?: "",
                    currency = line.currency,
                    comment = line.comment,
                    isAmountValid = true
                )
            }

            val effect = TemplateApplicatorEffect.ApplyTemplate(
                description = extracted.description,
                transactionComment = extracted.comment,
                date = extracted.date,
                accounts = newAccounts
            )
            _effects.send(effect)
        }
    }

    private fun searchTemplates(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            getAllTemplatesUseCase()
                .onSuccess { allTemplates ->
                    val templates = if (query.isBlank()) {
                        allTemplates
                    } else {
                        allTemplates.filter {
                            it.name.contains(query, ignoreCase = true) ||
                                it.transactionDescription?.contains(query, ignoreCase = true) == true
                        }
                    }
                    _uiState.update {
                        it.copy(
                            availableTemplates = templates.map { t -> t.toTemplateItem() },
                            isSearching = false
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isSearching = false) }
                }
        }
    }

    private fun clearSelection() {
        _uiState.update { it.copy(selectedTemplateId = null) }
    }
}
