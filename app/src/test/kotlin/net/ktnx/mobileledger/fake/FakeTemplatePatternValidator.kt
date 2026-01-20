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

package net.ktnx.mobileledger.fake

import androidx.compose.ui.text.AnnotatedString
import net.ktnx.mobileledger.domain.usecase.PatternValidationResult
import net.ktnx.mobileledger.domain.usecase.TemplatePatternValidator

/**
 * Fake implementation of TemplatePatternValidator for testing.
 */
class FakeTemplatePatternValidator : TemplatePatternValidator {

    var shouldReturnError: Boolean = false
    var errorMessage: String = "Invalid pattern"
    var groupCount: Int = 0
    var matchResultText: String? = null

    private var _validateCallCount = 0
    val validateCallCount: Int get() = _validateCallCount

    private var _lastPattern: String? = null
    val lastPattern: String? get() = _lastPattern

    private var _lastTestText: String? = null
    val lastTestText: String? get() = _lastTestText

    override fun validate(pattern: String, testText: String): PatternValidationResult {
        _validateCallCount++
        _lastPattern = pattern
        _lastTestText = testText

        return if (shouldReturnError) {
            PatternValidationResult(
                error = errorMessage,
                matchResult = null,
                groupCount = 0
            )
        } else {
            PatternValidationResult(
                error = null,
                matchResult = matchResultText?.let { AnnotatedString(it) },
                groupCount = groupCount
            )
        }
    }

    fun reset() {
        shouldReturnError = false
        errorMessage = "Invalid pattern"
        groupCount = 0
        matchResultText = null
        _validateCallCount = 0
        _lastPattern = null
        _lastTestText = null
    }
}
