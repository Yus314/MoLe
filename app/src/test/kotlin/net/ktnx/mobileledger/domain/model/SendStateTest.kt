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

import net.ktnx.mobileledger.core.common.utils.SimpleDate
import net.ktnx.mobileledger.core.domain.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [SendState].
 *
 * Tests verify:
 * - Sealed class instantiation
 * - Data object singleton behavior
 * - Default message values
 */
class SendStateTest {

    // ========================================
    // Idle tests
    // ========================================

    @Test
    fun `Idle is singleton`() {
        assertSame(SendState.Idle, SendState.Idle)
    }

    @Test
    fun `Idle is SendState`() {
        val state: SendState = SendState.Idle
        assertTrue(state is SendState.Idle)
    }

    // ========================================
    // Sending tests
    // ========================================

    @Test
    fun `Sending with default message`() {
        val state = SendState.Sending()
        assertEquals("送信中...", state.message)
    }

    @Test
    fun `Sending with custom message`() {
        val state = SendState.Sending("Uploading...")
        assertEquals("Uploading...", state.message)
    }

    @Test
    fun `Sending is SendState`() {
        val state: SendState = SendState.Sending()
        assertTrue(state is SendState.Sending)
    }

    // ========================================
    // Completed tests
    // ========================================

    @Test
    fun `Completed is singleton`() {
        assertSame(SendState.Completed, SendState.Completed)
    }

    @Test
    fun `Completed is SendState`() {
        val state: SendState = SendState.Completed
        assertTrue(state is SendState.Completed)
    }

    // ========================================
    // Failed tests
    // ========================================

    @Test
    fun `Failed stores error`() {
        val error = SyncError.NetworkError("Connection failed")
        val state = SendState.Failed(error)
        assertSame(error, state.error)
    }

    @Test
    fun `Failed is SendState`() {
        val error = SyncError.NetworkError("error")
        val state: SendState = SendState.Failed(error)
        assertTrue(state is SendState.Failed)
    }

    // ========================================
    // Sealed class behavior
    // ========================================

    @Test
    fun `when expression handles all variants`() {
        val state: SendState = SendState.Sending("test")
        val result = when (state) {
            is SendState.Idle -> "idle"
            is SendState.Sending -> "sending"
            is SendState.Completed -> "completed"
            is SendState.Failed -> "failed"
        }
        assertEquals("sending", result)
    }
}
