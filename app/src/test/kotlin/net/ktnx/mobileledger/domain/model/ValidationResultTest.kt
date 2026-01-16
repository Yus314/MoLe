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

package net.ktnx.mobileledger.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationResultTest {

    @Test
    fun `Success isSuccess returns true`() {
        val result = ValidationResult.Success

        assertTrue(result.isSuccess)
    }

    @Test
    fun `Success isError returns false`() {
        val result = ValidationResult.Success

        assertFalse(result.isError)
    }

    @Test
    fun `Error isSuccess returns false`() {
        val result = ValidationResult.Error(listOf("some error"))

        assertFalse(result.isSuccess)
    }

    @Test
    fun `Error isError returns true`() {
        val result = ValidationResult.Error(listOf("some error"))

        assertTrue(result.isError)
    }

    @Test
    fun `Error reasons contains provided errors`() {
        val errors = listOf("error1", "error2", "error3")
        val result = ValidationResult.Error(errors)

        assertEquals(errors, (result as ValidationResult.Error).reasons)
    }

    @Test
    fun `Error single reason constructor creates list with one item`() {
        val result = ValidationResult.Error("single error")

        assertEquals(listOf("single error"), result.reasons)
    }

    @Test
    fun `Error empty reasons list is valid`() {
        val result = ValidationResult.Error(emptyList())

        assertEquals(emptyList<String>(), result.reasons)
    }
}
