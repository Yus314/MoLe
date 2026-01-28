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

import androidx.compose.ui.text.AnnotatedString

/**
 * Result of pattern validation containing:
 * - error: Error message if pattern is invalid, null otherwise
 * - matchResult: Annotated string showing match highlights, null if no test text
 * - groupCount: Number of capturing groups in the pattern
 */
data class PatternValidationResult(
    val error: String?,
    val matchResult: AnnotatedString?,
    val groupCount: Int
)

/**
 * Use case for validating regex patterns and testing them against sample text.
 */
interface TemplatePatternValidator {
    /**
     * Validates a regex pattern and optionally tests it against sample text.
     *
     * @param pattern The regex pattern to validate
     * @param testText Optional text to test the pattern against
     * @return PatternValidationResult containing validation results
     */
    fun validate(pattern: String, testText: String): PatternValidationResult
}
