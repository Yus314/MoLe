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

import java.nio.ByteBuffer
import java.security.NoSuchAlgorithmException
import net.ktnx.mobileledger.core.common.utils.Digest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [Digest].
 *
 * Tests verify:
 * - Digest creation
 * - Hex digit conversion
 * - SHA-256 hashing
 */
class DigestTest {

    // ========================================
    // Constructor tests
    // ========================================

    @Test
    fun `constructor creates SHA-256 digest`() {
        val digest = Digest("SHA-256")
        assertNotNull(digest)
        assertEquals(32, digest.digestLength)
    }

    @Test
    fun `constructor creates MD5 digest`() {
        val digest = Digest("MD5")
        assertNotNull(digest)
        assertEquals(16, digest.digestLength)
    }

    @Test(expected = NoSuchAlgorithmException::class)
    fun `constructor throws for invalid algorithm`() {
        Digest("INVALID-ALGORITHM")
    }

    // ========================================
    // hexDigitFor tests
    // ========================================

    @Test
    fun `hexDigitFor returns 0-9 for 0-9`() {
        for (i in 0..9) {
            assertEquals("Digit $i should be '${i.toChar()}'", '0' + i, Digest.hexDigitFor(i))
        }
    }

    @Test
    fun `hexDigitFor returns a-f for 10-15`() {
        assertEquals('a', Digest.hexDigitFor(10))
        assertEquals('b', Digest.hexDigitFor(11))
        assertEquals('c', Digest.hexDigitFor(12))
        assertEquals('d', Digest.hexDigitFor(13))
        assertEquals('e', Digest.hexDigitFor(14))
        assertEquals('f', Digest.hexDigitFor(15))
    }

    @Test(expected = ArithmeticException::class)
    fun `hexDigitFor throws for negative`() {
        Digest.hexDigitFor(-1)
    }

    @Test(expected = ArithmeticException::class)
    fun `hexDigitFor throws for 16`() {
        Digest.hexDigitFor(16)
    }

    // ========================================
    // hexDigitsFor(Int) tests
    // ========================================

    @Test
    fun `hexDigitsFor returns 00 for 0`() {
        val result = Digest.hexDigitsFor(0)
        assertArrayEquals(charArrayOf('0', '0'), result)
    }

    @Test
    fun `hexDigitsFor returns ff for 255`() {
        val result = Digest.hexDigitsFor(255)
        assertArrayEquals(charArrayOf('f', 'f'), result)
    }

    @Test
    fun `hexDigitsFor returns 0a for 10`() {
        val result = Digest.hexDigitsFor(10)
        assertArrayEquals(charArrayOf('0', 'a'), result)
    }

    @Test
    fun `hexDigitsFor returns 10 for 16`() {
        val result = Digest.hexDigitsFor(16)
        assertArrayEquals(charArrayOf('1', '0'), result)
    }

    @Test(expected = ArithmeticException::class)
    fun `hexDigitsFor throws for negative int`() {
        Digest.hexDigitsFor(-1)
    }

    @Test(expected = ArithmeticException::class)
    fun `hexDigitsFor throws for 256`() {
        Digest.hexDigitsFor(256)
    }

    // ========================================
    // hexDigitsFor(Byte) tests
    // ========================================

    @Test
    fun `hexDigitsFor byte handles positive byte`() {
        val result = Digest.hexDigitsFor(15.toByte())
        assertArrayEquals(charArrayOf('0', 'f'), result)
    }

    @Test
    fun `hexDigitsFor byte handles negative byte as unsigned`() {
        // -1 as byte = 0xFF = 255
        val result = Digest.hexDigitsFor((-1).toByte())
        assertArrayEquals(charArrayOf('f', 'f'), result)
    }

    @Test
    fun `hexDigitsFor byte handles -128 as unsigned`() {
        // -128 as byte = 0x80 = 128
        val result = Digest.hexDigitsFor((-128).toByte())
        assertArrayEquals(charArrayOf('8', '0'), result)
    }

    // ========================================
    // Update and digest tests
    // ========================================

    @Test
    fun `update with byte and digest works`() {
        val digest = Digest("SHA-256")
        digest.update(0x41.toByte()) // 'A'
        val result = digest.digest()
        assertEquals(32, result.size)
    }

    @Test
    fun `update with byte array and digest works`() {
        val digest = Digest("SHA-256")
        val input = "Hello".toByteArray()
        digest.update(input)
        val result = digest.digest()
        assertEquals(32, result.size)
    }

    @Test
    fun `update with byte array subset works`() {
        val digest = Digest("SHA-256")
        val input = "Hello World".toByteArray()
        digest.update(input, 0, 5) // Only "Hello"
        val result = digest.digest()
        assertEquals(32, result.size)
    }

    @Test
    fun `update with ByteBuffer works`() {
        val digest = Digest("SHA-256")
        val buffer = ByteBuffer.wrap("Test".toByteArray())
        digest.update(buffer)
        val result = digest.digest()
        assertEquals(32, result.size)
    }

    @Test
    fun `digest with input bytes works`() {
        val digest = Digest("SHA-256")
        val result = digest.digest("Hello".toByteArray())
        assertEquals(32, result.size)
    }

    // ========================================
    // digestToHexString tests
    // ========================================

    @Test
    fun `digestToHexString returns 64 char hex string for SHA-256`() {
        val digest = Digest("SHA-256")
        digest.update("test".toByteArray())
        val hex = digest.digestToHexString()
        assertEquals(64, hex.length)
        assertTrue(hex.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun `digestToHexString for empty input`() {
        val digest = Digest("SHA-256")
        val hex = digest.digestToHexString()
        assertEquals(64, hex.length)
        // SHA-256 of empty string
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", hex)
    }

    // ========================================
    // Reset test
    // ========================================

    @Test
    fun `reset clears previous updates`() {
        val digest = Digest("SHA-256")
        digest.update("Hello".toByteArray())
        digest.reset()
        val hex = digest.digestToHexString()
        // Should be SHA-256 of empty string after reset
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", hex)
    }

    // ========================================
    // digestLength test
    // ========================================

    @Test
    fun `digestLength returns correct length for SHA-256`() {
        val digest = Digest("SHA-256")
        assertEquals(32, digest.digestLength)
    }

    @Test
    fun `digestLength returns correct length for SHA-512`() {
        val digest = Digest("SHA-512")
        assertEquals(64, digest.digestLength)
    }
}
