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

private const val ACCOUNT_DELIMITER = ':'

/**
 * Extract the parent account name from a hierarchical account name.
 *
 * Account names in hledger use colon (:) as a delimiter to denote hierarchy.
 * For example: "Assets:Bank:Checking" has parent "Assets:Bank".
 *
 * @return The parent name, or null if no parent exists (top-level account)
 */
fun String.extractParentAccountName(): String? {
    val colonPos = lastIndexOf(ACCOUNT_DELIMITER)
    return if (colonPos < 0) null else substring(0, colonPos)
}

/**
 * Determine the level of an account in the hierarchy.
 *
 * Level is defined as the number of colons in the name:
 * - "Assets" -> level 0
 * - "Assets:Bank" -> level 1
 * - "Assets:Bank:Checking" -> level 2
 *
 * @return The hierarchical level (0-based)
 */
fun String.accountLevel(): Int {
    var level = 0
    var delimiterPosition = indexOf(ACCOUNT_DELIMITER)
    while (delimiterPosition >= 0) {
        level++
        delimiterPosition = indexOf(ACCOUNT_DELIMITER, delimiterPosition + 1)
    }
    return level
}

/**
 * Check if this account is a parent of another account.
 *
 * @param child The potential child account name
 * @return true if this account is a direct or indirect parent of child
 */
fun String.isParentAccountOf(child: String): Boolean = child.startsWith("$this:")

// Legacy object for backward compatibility - will be removed in future
@Deprecated("Use String extension functions instead")
object AccountNameUtils {
    @JvmStatic
    @Deprecated("Use String.extractParentAccountName() instead", ReplaceWith("accountName.extractParentAccountName()"))
    fun extractParentName(accountName: String): String? = accountName.extractParentAccountName()

    @JvmStatic
    @Deprecated("Use String.accountLevel() instead", ReplaceWith("accountName.accountLevel()"))
    fun determineLevel(accountName: String): Int = accountName.accountLevel()

    @JvmStatic
    @Deprecated("Use String.isParentAccountOf() instead", ReplaceWith("possibleParent.isParentAccountOf(accountName)"))
    fun isParentOf(possibleParent: String, accountName: String): Boolean = possibleParent.isParentAccountOf(accountName)
}
