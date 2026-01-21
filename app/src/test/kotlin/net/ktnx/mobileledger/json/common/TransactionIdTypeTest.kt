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

package net.ktnx.mobileledger.json.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [TransactionIdType].
 */
class TransactionIdTypeTest {

    // ========================================
    // IntType tests
    // ========================================

    @Test
    fun `IntType defaultValue is 0`() {
        // Then
        assertEquals(0, TransactionIdType.IntType.defaultValue)
    }

    @Test
    fun `IntType fromIndex returns Int`() {
        // When
        val result = TransactionIdType.IntType.fromIndex(42)

        // Then
        assertTrue(result is Int)
        assertEquals(42, result)
    }

    @Test
    fun `IntType fromIndex with zero returns 0`() {
        // When
        val result = TransactionIdType.IntType.fromIndex(0)

        // Then
        assertEquals(0, result)
    }

    @Test
    fun `IntType fromIndex with negative returns negative`() {
        // When
        val result = TransactionIdType.IntType.fromIndex(-5)

        // Then
        assertEquals(-5, result)
    }

    // ========================================
    // StringType tests
    // ========================================

    @Test
    fun `StringType defaultValue is 1 as string`() {
        // Then
        assertEquals("1", TransactionIdType.StringType.defaultValue)
    }

    @Test
    fun `StringType fromIndex returns String`() {
        // When
        val result = TransactionIdType.StringType.fromIndex(42)

        // Then
        assertTrue(result is String)
        assertEquals("42", result)
    }

    @Test
    fun `StringType fromIndex with zero returns 0 as string`() {
        // When
        val result = TransactionIdType.StringType.fromIndex(0)

        // Then
        assertEquals("0", result)
    }

    @Test
    fun `StringType fromIndex with negative returns negative string`() {
        // When
        val result = TransactionIdType.StringType.fromIndex(-10)

        // Then
        assertEquals("-10", result)
    }

    // ========================================
    // Type safety tests
    // ========================================

    @Test
    fun `IntType is a TransactionIdType`() {
        // Given
        val type: TransactionIdType = TransactionIdType.IntType

        // Then - should compile and work
        assertEquals(0, type.defaultValue)
    }

    @Test
    fun `StringType is a TransactionIdType`() {
        // Given
        val type: TransactionIdType = TransactionIdType.StringType

        // Then - should compile and work
        assertEquals("1", type.defaultValue)
    }
}
