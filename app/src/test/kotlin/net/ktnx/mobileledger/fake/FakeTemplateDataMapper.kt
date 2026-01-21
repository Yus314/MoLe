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

package net.ktnx.mobileledger.fake

import net.ktnx.mobileledger.domain.model.Template
import net.ktnx.mobileledger.domain.usecase.TemplateDataMapper
import net.ktnx.mobileledger.ui.templates.MatchableValue
import net.ktnx.mobileledger.ui.templates.TemplateAccountRow
import net.ktnx.mobileledger.ui.templates.TemplateDetailUiState

/**
 * Fake implementation of [TemplateDataMapper] for testing.
 */
class FakeTemplateDataMapper : TemplateDataMapper {

    var toTemplateCallCount = 0
        private set

    var toAccountRowsCallCount = 0
        private set

    override fun toTemplate(state: TemplateDetailUiState): Template {
        toTemplateCallCount++
        return Template(
            id = state.templateId,
            name = state.name,
            pattern = state.pattern,
            testText = state.testText.ifBlank { null },
            isFallback = state.isFallback,
            transactionDescription = extractLiteral(state.transactionDescription),
            transactionDescriptionMatchGroup = extractMatchGroup(state.transactionDescription),
            transactionComment = extractLiteral(state.transactionComment),
            transactionCommentMatchGroup = extractMatchGroup(state.transactionComment),
            dateYear = extractLiteralInt(state.dateYear),
            dateYearMatchGroup = extractMatchGroup(state.dateYear),
            dateMonth = extractLiteralInt(state.dateMonth),
            dateMonthMatchGroup = extractMatchGroup(state.dateMonth),
            dateDay = extractLiteralInt(state.dateDay),
            dateDayMatchGroup = extractMatchGroup(state.dateDay),
            lines = emptyList()
        )
    }

    override fun toAccountRows(template: Template, idGenerator: () -> Long): List<TemplateAccountRow> {
        toAccountRowsCallCount++
        return if (template.lines.isEmpty()) {
            listOf(
                TemplateAccountRow(id = idGenerator(), position = 0),
                TemplateAccountRow(id = idGenerator(), position = 1)
            )
        } else {
            template.lines.mapIndexed { index, line ->
                TemplateAccountRow(
                    id = line.id ?: idGenerator(),
                    position = index,
                    accountName = extractMatchableValue(line.accountName, line.accountNameGroup),
                    accountComment = extractMatchableValue(line.comment, line.commentGroup),
                    amount = extractMatchableValueFloat(line.amount, line.amountGroup),
                    negateAmount = line.negateAmount
                )
            }
        }
    }

    override fun extractMatchableValue(literal: String?, matchGroup: Int?): MatchableValue = when {
        matchGroup != null && matchGroup > 0 -> MatchableValue.MatchGroup(matchGroup)
        else -> MatchableValue.Literal(literal ?: "")
    }

    override fun extractMatchableValueInt(literal: Int?, matchGroup: Int?): MatchableValue = when {
        matchGroup != null && matchGroup > 0 -> MatchableValue.MatchGroup(matchGroup)
        else -> MatchableValue.Literal(literal?.toString() ?: "")
    }

    override fun extractMatchableValueFloat(literal: Float?, matchGroup: Int?): MatchableValue = when {
        matchGroup != null && matchGroup > 0 -> MatchableValue.MatchGroup(matchGroup)
        else -> MatchableValue.Literal(literal?.toString() ?: "")
    }

    override fun extractMatchableValueCurrency(currencyId: Long?, matchGroup: Int?): MatchableValue = when {
        matchGroup != null && matchGroup > 0 -> MatchableValue.MatchGroup(matchGroup)
        else -> MatchableValue.Literal(currencyId?.toString() ?: "")
    }

    private fun extractLiteral(value: MatchableValue): String? = when (value) {
        is MatchableValue.Literal -> value.value.ifBlank { null }
        is MatchableValue.MatchGroup -> null
    }

    private fun extractLiteralInt(value: MatchableValue): Int? = when (value) {
        is MatchableValue.Literal -> value.value.toIntOrNull()
        is MatchableValue.MatchGroup -> null
    }

    private fun extractMatchGroup(value: MatchableValue): Int? = when (value) {
        is MatchableValue.Literal -> null
        is MatchableValue.MatchGroup -> value.group
    }

    fun reset() {
        toTemplateCallCount = 0
        toAccountRowsCallCount = 0
    }
}
