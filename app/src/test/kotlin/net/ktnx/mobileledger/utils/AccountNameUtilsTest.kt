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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for account name extension functions.
 *
 * Tests verify:
 * - Parent name extraction from hierarchical account names
 * - Level determination based on colon separators
 * - Parent-child relationship checks
 */
class AccountNameUtilsTest {

    // ========================================
    // extractParentAccountName tests (String extension)
    // ========================================

    @Test
    fun `extractParentAccountName returns parent for nested account`() {
        assertEquals("Assets:Bank", "Assets:Bank:Checking".extractParentAccountName())
    }

    @Test
    fun `extractParentAccountName returns parent for two level account`() {
        assertEquals("Assets", "Assets:Bank".extractParentAccountName())
    }

    @Test
    fun `extractParentAccountName returns null for top level account`() {
        assertNull("Assets".extractParentAccountName())
    }

    @Test
    fun `extractParentAccountName returns null for empty string`() {
        assertNull("".extractParentAccountName())
    }

    @Test
    fun `extractParentAccountName handles deeply nested account`() {
        assertEquals(
            "Assets:Bank:Savings:Emergency",
            "Assets:Bank:Savings:Emergency:Fund".extractParentAccountName()
        )
    }

    // ========================================
    // accountLevel tests (String extension)
    // ========================================

    @Test
    fun `accountLevel returns 0 for top level account`() {
        assertEquals(0, "Assets".accountLevel())
    }

    @Test
    fun `accountLevel returns 1 for two level account`() {
        assertEquals(1, "Assets:Bank".accountLevel())
    }

    @Test
    fun `accountLevel returns 2 for three level account`() {
        assertEquals(2, "Assets:Bank:Checking".accountLevel())
    }

    @Test
    fun `accountLevel returns 0 for empty string`() {
        assertEquals(0, "".accountLevel())
    }

    @Test
    fun `accountLevel handles deeply nested account`() {
        assertEquals(4, "Assets:Bank:Savings:Emergency:Fund".accountLevel())
    }

    // ========================================
    // isParentAccountOf tests (String extension)
    // ========================================

    @Test
    fun `isParentAccountOf returns true for direct parent`() {
        assertTrue("Assets:Bank".isParentAccountOf("Assets:Bank:Checking"))
    }

    @Test
    fun `isParentAccountOf returns true for indirect parent`() {
        assertTrue("Assets".isParentAccountOf("Assets:Bank:Checking"))
    }

    @Test
    fun `isParentAccountOf returns false for non parent`() {
        assertFalse("Expenses".isParentAccountOf("Assets:Bank:Checking"))
    }

    @Test
    fun `isParentAccountOf returns false for same account`() {
        assertFalse("Assets:Bank".isParentAccountOf("Assets:Bank"))
    }

    @Test
    fun `isParentAccountOf returns false for partial match`() {
        // "Assets:Ban" is not a parent of "Assets:Bank:Checking"
        assertFalse("Assets:Ban".isParentAccountOf("Assets:Bank:Checking"))
    }

    @Test
    fun `isParentAccountOf returns false for child checking parent`() {
        assertFalse("Assets:Bank:Checking".isParentAccountOf("Assets:Bank"))
    }

    @Test
    fun `isParentAccountOf handles empty strings`() {
        assertFalse("".isParentAccountOf("Assets"))
        // Empty string with colon prefix shouldn't match real accounts
        assertFalse("".isParentAccountOf(""))
    }
}
