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

package net.ktnx.mobileledger.utils

/**
 * Utility functions for parsing and manipulating hledger account names.
 *
 * Account names in hledger use colon (:) as a delimiter to denote hierarchy.
 * For example: "Assets:Bank:Checking" has parent "Assets:Bank" and level 2.
 */
object AccountNameUtils {
    private const val ACCOUNT_DELIMITER = ':'

    /**
     * Extract the parent account name from a hierarchical account name.
     *
     * @param accountName The full account name (e.g., "Assets:Bank:Checking")
     * @return The parent name (e.g., "Assets:Bank"), or null if no parent exists
     */
    @JvmStatic
    fun extractParentName(accountName: String): String? {
        val colonPos = accountName.lastIndexOf(ACCOUNT_DELIMITER)
        return if (colonPos < 0) null else accountName.substring(0, colonPos)
    }

    /**
     * Determine the level of an account in the hierarchy.
     *
     * Level is defined as the number of colons in the name:
     * - "Assets" -> level 0
     * - "Assets:Bank" -> level 1
     * - "Assets:Bank:Checking" -> level 2
     *
     * @param accountName The account name to analyze
     * @return The hierarchical level (0-based)
     */
    @JvmStatic
    fun determineLevel(accountName: String): Int {
        var level = 0
        var delimiterPosition = accountName.indexOf(ACCOUNT_DELIMITER)
        while (delimiterPosition >= 0) {
            level++
            delimiterPosition = accountName.indexOf(ACCOUNT_DELIMITER, delimiterPosition + 1)
        }
        return level
    }

    /**
     * Check if one account is a parent of another.
     *
     * @param possibleParent The potential parent account name
     * @param accountName The potential child account name
     * @return true if possibleParent is a direct or indirect parent of accountName
     */
    @JvmStatic
    fun isParentOf(possibleParent: String, accountName: String): Boolean = accountName.startsWith("$possibleParent:")
}
