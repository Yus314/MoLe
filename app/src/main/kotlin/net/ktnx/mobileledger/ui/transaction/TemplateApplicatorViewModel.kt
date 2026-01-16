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
import net.ktnx.mobileledger.data.repository.CurrencyRepository
import net.ktnx.mobileledger.data.repository.ProfileRepository
import net.ktnx.mobileledger.data.repository.TemplateRepository
import net.ktnx.mobileledger.db.TemplateAccount
import net.ktnx.mobileledger.db.TemplateWithAccounts
import net.ktnx.mobileledger.model.MatchedTemplate
import net.ktnx.mobileledger.service.CurrencyFormatter

@HiltViewModel
class TemplateApplicatorViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val templateRepository: TemplateRepository,
    private val currencyRepository: CurrencyRepository,
    private val currencyFormatter: CurrencyFormatter
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
            val templates = templateRepository.getAllTemplatesWithAccountsSync()
                .map {
                    TemplateItem(
                        it.header.id,
                        it.header.name,
                        it.header.transactionDescription,
                        it.header.regularExpression
                    )
                }
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
            val template = templateRepository.getTemplateWithAccountsSync(templateId)
            if (template != null) {
                val effect = buildApplyTemplateEffect(template)
                _effects.send(effect)
            }
            _uiState.update { it.copy(showTemplateSelector = false, selectedTemplateId = null) }
        }
    }

    private suspend fun buildApplyTemplateEffect(
        template: TemplateWithAccounts
    ): TemplateApplicatorEffect.ApplyTemplate {
        val defaultCurrency = profileRepository.currentProfile.value?.defaultCommodityOrEmpty ?: ""

        val newAccounts = template.accounts.map { acc ->
            val currencyName = acc.currency?.let { currencyId ->
                currencyRepository.getCurrencyByIdSync(currencyId)?.name
            } ?: defaultCurrency

            TransactionAccountRow(
                id = AccountRowsUiState.nextId(),
                accountName = acc.accountName ?: "",
                amountText = if (acc.amount != null) currencyFormatter.formatNumber(acc.amount!!) else "",
                currency = currencyName,
                comment = acc.accountComment ?: "",
                isAmountValid = true
            )
        }

        return TemplateApplicatorEffect.ApplyTemplate(
            description = template.header.transactionDescription ?: "",
            transactionComment = template.header.transactionComment,
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
        val templates = templateRepository.getAllTemplatesWithAccountsSync()
        for (twa in templates) {
            val header = twa.header
            val regex = header.regularExpression
            if (regex.isBlank()) continue
            try {
                val pattern = Regex(regex)
                val matchResult = pattern.find(text)
                if (matchResult != null) {
                    val javaPattern = java.util.regex.Pattern.compile(regex)
                    val javaMatcher = javaPattern.matcher(text)
                    if (javaMatcher.find()) {
                        return MatchedTemplate(header, javaMatcher.toMatchResult())
                    }
                }
            } catch (e: Exception) {
                logcat { "Invalid regex in template: $regex - ${e.message}" }
            }
        }
        return null
    }

    private fun applyMatchedTemplate(matched: MatchedTemplate) {
        viewModelScope.launch {
            val template = templateRepository.getTemplateWithAccountsSync(matched.templateHead.id)
                ?: return@launch

            val matchResult = matched.matchResult
            val header = template.header
            val defaultCurrency = profileRepository.currentProfile.value?.defaultCommodityOrEmpty ?: ""

            val description = extractFromMatchGroup(
                matchResult,
                header.transactionDescriptionMatchGroup,
                header.transactionDescription
            ) ?: ""

            val comment = extractFromMatchGroup(
                matchResult,
                header.transactionCommentMatchGroup,
                header.transactionComment
            )

            val date = TemplateMatchGroupExtractor.extractDate(matchResult, header)

            val newAccounts = template.accounts.map { acc ->
                extractAccountRow(matchResult, acc, defaultCurrency)
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

    private suspend fun extractAccountRow(
        matchResult: java.util.regex.MatchResult,
        acc: TemplateAccount,
        defaultCurrency: String
    ): TransactionAccountRow {
        val accountName = extractFromMatchGroup(
            matchResult,
            acc.accountNameMatchGroup,
            acc.accountName
        ) ?: ""

        val amountStr = extractFromMatchGroup(
            matchResult,
            acc.amountMatchGroup,
            acc.amount?.toString()
        )
        val amount = TemplateMatchGroupExtractor.parseAmount(amountStr, acc.negateAmount ?: false)
        val amountText = amount?.let { currencyFormatter.formatNumber(it) } ?: ""

        val currencyName = if (acc.currencyMatchGroup != null && acc.currencyMatchGroup!! > 0) {
            extractFromMatchGroup(matchResult, acc.currencyMatchGroup, null)
        } else {
            acc.currency?.let { currencyRepository.getCurrencyByIdSync(it)?.name }
        } ?: defaultCurrency

        val comment = extractFromMatchGroup(
            matchResult,
            acc.accountCommentMatchGroup,
            acc.accountComment
        ) ?: ""

        return TransactionAccountRow(
            id = AccountRowsUiState.nextId(),
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
                templateRepository.getAllTemplatesWithAccountsSync()
            } else {
                templateRepository.getAllTemplatesWithAccountsSync().filter {
                    it.header.name.contains(query, ignoreCase = true) ||
                        it.header.transactionDescription?.contains(query, ignoreCase = true) == true
                }
            }
            _uiState.update {
                it.copy(
                    availableTemplates = templates.map { t ->
                        TemplateItem(
                            t.header.id,
                            t.header.name,
                            t.header.transactionDescription,
                            t.header.regularExpression
                        )
                    },
                    isSearching = false
                )
            }
        }
    }

    private fun clearSelection() {
        _uiState.update { it.copy(selectedTemplateId = null) }
    }
}
