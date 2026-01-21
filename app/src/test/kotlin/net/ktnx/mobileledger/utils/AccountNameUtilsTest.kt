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
 * Unit tests for [AccountNameUtils].
 *
 * Tests verify:
 * - Parent name extraction from hierarchical account names
 * - Level determination based on colon separators
 * - Parent-child relationship checks
 */
class AccountNameUtilsTest {

    // ========================================
    // extractParentName tests
    // ========================================

    @Test
    fun `extractParentName returns parent for nested account`() {
        assertEquals("Assets:Bank", AccountNameUtils.extractParentName("Assets:Bank:Checking"))
    }

    @Test
    fun `extractParentName returns parent for two level account`() {
        assertEquals("Assets", AccountNameUtils.extractParentName("Assets:Bank"))
    }

    @Test
    fun `extractParentName returns null for top level account`() {
        assertNull(AccountNameUtils.extractParentName("Assets"))
    }

    @Test
    fun `extractParentName returns null for empty string`() {
        assertNull(AccountNameUtils.extractParentName(""))
    }

    @Test
    fun `extractParentName handles deeply nested account`() {
        assertEquals(
            "Assets:Bank:Savings:Emergency",
            AccountNameUtils.extractParentName("Assets:Bank:Savings:Emergency:Fund")
        )
    }

    // ========================================
    // determineLevel tests
    // ========================================

    @Test
    fun `determineLevel returns 0 for top level account`() {
        assertEquals(0, AccountNameUtils.determineLevel("Assets"))
    }

    @Test
    fun `determineLevel returns 1 for two level account`() {
        assertEquals(1, AccountNameUtils.determineLevel("Assets:Bank"))
    }

    @Test
    fun `determineLevel returns 2 for three level account`() {
        assertEquals(2, AccountNameUtils.determineLevel("Assets:Bank:Checking"))
    }

    @Test
    fun `determineLevel returns 0 for empty string`() {
        assertEquals(0, AccountNameUtils.determineLevel(""))
    }

    @Test
    fun `determineLevel handles deeply nested account`() {
        assertEquals(4, AccountNameUtils.determineLevel("Assets:Bank:Savings:Emergency:Fund"))
    }

    // ========================================
    // isParentOf tests
    // ========================================

    @Test
    fun `isParentOf returns true for direct parent`() {
        assertTrue(AccountNameUtils.isParentOf("Assets:Bank", "Assets:Bank:Checking"))
    }

    @Test
    fun `isParentOf returns true for indirect parent`() {
        assertTrue(AccountNameUtils.isParentOf("Assets", "Assets:Bank:Checking"))
    }

    @Test
    fun `isParentOf returns false for non parent`() {
        assertFalse(AccountNameUtils.isParentOf("Expenses", "Assets:Bank:Checking"))
    }

    @Test
    fun `isParentOf returns false for same account`() {
        assertFalse(AccountNameUtils.isParentOf("Assets:Bank", "Assets:Bank"))
    }

    @Test
    fun `isParentOf returns false for partial match`() {
        // "Assets:Ban" is not a parent of "Assets:Bank:Checking"
        assertFalse(AccountNameUtils.isParentOf("Assets:Ban", "Assets:Bank:Checking"))
    }

    @Test
    fun `isParentOf returns false for child checking parent`() {
        assertFalse(AccountNameUtils.isParentOf("Assets:Bank:Checking", "Assets:Bank"))
    }

    @Test
    fun `isParentOf handles empty strings`() {
        assertFalse(AccountNameUtils.isParentOf("", "Assets"))
        // Empty string with colon prefix shouldn't match real accounts
        assertFalse(AccountNameUtils.isParentOf("", ""))
    }
}
