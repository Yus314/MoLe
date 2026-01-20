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

package net.ktnx.mobileledger.domain.usecase

import javax.inject.Inject
import net.ktnx.mobileledger.domain.model.Template
import net.ktnx.mobileledger.domain.model.TemplateLine
import net.ktnx.mobileledger.ui.templates.MatchableValue
import net.ktnx.mobileledger.ui.templates.TemplateAccountRow
import net.ktnx.mobileledger.ui.templates.TemplateDetailUiState
import net.ktnx.mobileledger.utils.Misc

/**
 * Implementation of TemplateDataMapper.
 *
 * Provides pure functions for converting between Template domain models and UI state.
 */
class TemplateDataMapperImpl @Inject constructor() : TemplateDataMapper {

    companion object {
        private const val MIN_ACCOUNT_ROWS = 2
    }

    override fun toTemplate(state: TemplateDetailUiState): Template {
        val lines = state.accounts
            .filterIndexed { index, row -> !row.isEmpty() || index < MIN_ACCOUNT_ROWS }
            .map { row -> buildTemplateLine(row) }

        return Template(
            id = state.templateId,
            name = Misc.trim(state.name) ?: "",
            pattern = state.pattern,
            testText = state.testText.ifEmpty { null },
            transactionDescription = state.transactionDescription
                .takeIf { it.isLiteral() }?.getLiteralValue()?.ifEmpty { null },
            transactionDescriptionMatchGroup = state.transactionDescription
                .takeIf { it.isMatchGroup() }?.getMatchGroup(),
            transactionComment = state.transactionComment
                .takeIf { it.isLiteral() }?.getLiteralValue()?.ifEmpty { null },
            transactionCommentMatchGroup = state.transactionComment
                .takeIf { it.isMatchGroup() }?.getMatchGroup(),
            dateYear = state.dateYear.takeIf { it.isLiteral() }?.getLiteralValue()?.toIntOrNull(),
            dateYearMatchGroup = state.dateYear.takeIf { it.isMatchGroup() }?.getMatchGroup(),
            dateMonth = state.dateMonth.takeIf { it.isLiteral() }?.getLiteralValue()?.toIntOrNull(),
            dateMonthMatchGroup = state.dateMonth.takeIf { it.isMatchGroup() }?.getMatchGroup(),
            dateDay = state.dateDay.takeIf { it.isLiteral() }?.getLiteralValue()?.toIntOrNull(),
            dateDayMatchGroup = state.dateDay.takeIf { it.isMatchGroup() }?.getMatchGroup(),
            isFallback = state.isFallback,
            lines = lines
        )
    }

    private fun buildTemplateLine(row: TemplateAccountRow): TemplateLine = TemplateLine(
        id = if (row.id > 0) row.id else null,
        accountName = row.accountName.takeIf { it.isLiteral() }?.getLiteralValue()?.ifEmpty { null },
        accountNameGroup = row.accountName.takeIf { it.isMatchGroup() }?.getMatchGroup(),
        amount = row.amount.takeIf { it.isLiteral() }?.getLiteralValue()?.toFloatOrNull(),
        amountGroup = row.amount.takeIf { it.isMatchGroup() }?.getMatchGroup(),
        currencyId = row.currency.takeIf { it.isLiteral() }?.getLiteralValue()?.toLongOrNull(),
        currencyGroup = row.currency.takeIf { it.isMatchGroup() }?.getMatchGroup(),
        comment = row.accountComment.takeIf { it.isLiteral() }?.getLiteralValue()?.ifEmpty { null },
        commentGroup = row.accountComment.takeIf { it.isMatchGroup() }?.getMatchGroup(),
        negateAmount = row.negateAmount
    )

    override fun toAccountRows(template: Template, idGenerator: () -> Long): List<TemplateAccountRow> =
        template.lines.mapIndexed { index, line ->
            TemplateAccountRow(
                id = line.id ?: idGenerator(),
                position = index,
                accountName = extractMatchableValue(line.accountName, line.accountNameGroup),
                accountComment = extractMatchableValue(line.comment, line.commentGroup),
                amount = extractMatchableValueFloat(line.amount, line.amountGroup),
                currency = extractMatchableValueCurrency(line.currencyId, line.currencyGroup),
                negateAmount = line.negateAmount
            )
        }.ifEmpty {
            listOf(
                TemplateAccountRow(id = idGenerator()),
                TemplateAccountRow(id = idGenerator())
            )
        }

    override fun extractMatchableValue(literal: String?, matchGroup: Int?): MatchableValue =
        if (matchGroup != null && matchGroup > 0) {
            MatchableValue.MatchGroup(matchGroup)
        } else {
            MatchableValue.Literal(literal ?: "")
        }

    override fun extractMatchableValueInt(literal: Int?, matchGroup: Int?): MatchableValue =
        if (matchGroup != null && matchGroup > 0) {
            MatchableValue.MatchGroup(matchGroup)
        } else {
            MatchableValue.Literal(literal?.toString() ?: "")
        }

    override fun extractMatchableValueFloat(literal: Float?, matchGroup: Int?): MatchableValue =
        if (matchGroup != null && matchGroup > 0) {
            MatchableValue.MatchGroup(matchGroup)
        } else {
            MatchableValue.Literal(literal?.toString() ?: "")
        }

    override fun extractMatchableValueCurrency(currencyId: Long?, matchGroup: Int?): MatchableValue =
        if (matchGroup != null && matchGroup > 0) {
            MatchableValue.MatchGroup(matchGroup)
        } else {
            MatchableValue.Literal(currencyId?.toString() ?: "")
        }
}
