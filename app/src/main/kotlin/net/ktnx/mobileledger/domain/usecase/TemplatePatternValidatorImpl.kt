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

package net.ktnx.mobileledger.domain.usecase

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import javax.inject.Inject

/**
 * Implementation of TemplatePatternValidator that validates regex patterns
 * and generates annotated strings showing match results.
 */
class TemplatePatternValidatorImpl @Inject constructor() : TemplatePatternValidator {

    override fun validate(pattern: String, testText: String): PatternValidationResult {
        if (pattern.isEmpty()) {
            return PatternValidationResult(
                error = "パターンが空です",
                matchResult = null,
                groupCount = 0
            )
        }

        return try {
            val compiledPattern = Pattern.compile(pattern)
            val groupCount = compiledPattern.matcher("").groupCount()

            val matchResult = if (testText.isNotEmpty()) {
                buildMatchResultAnnotatedString(compiledPattern, testText)
            } else {
                null
            }

            PatternValidationResult(
                error = null,
                matchResult = matchResult,
                groupCount = groupCount
            )
        } catch (e: PatternSyntaxException) {
            PatternValidationResult(
                error = e.description,
                matchResult = null,
                groupCount = 0
            )
        }
    }

    private fun buildMatchResultAnnotatedString(compiledPattern: Pattern, testText: String): AnnotatedString {
        val matcher = compiledPattern.matcher(testText)

        return if (matcher.find()) {
            buildAnnotatedString {
                // Before match
                if (matcher.start() > 0) {
                    pushStyle(SpanStyle(color = Color.Gray))
                    append(testText.substring(0, matcher.start()))
                    pop()
                }

                // Matched portion
                pushStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                append(testText.substring(matcher.start(), matcher.end()))
                pop()

                // After match
                if (matcher.end() < testText.length) {
                    pushStyle(SpanStyle(color = Color.Gray))
                    append(testText.substring(matcher.end()))
                    pop()
                }
            }
        } else {
            buildAnnotatedString {
                pushStyle(SpanStyle(color = Color.Gray))
                append(testText)
                pop()
            }
        }
    }
}
