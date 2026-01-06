/*
 * Copyright © 2020 Damyan Ivanov.
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
import java.security.DigestException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class Digest
@Throws(NoSuchAlgorithmException::class)
constructor(
    type: String
) {
    private val digest: MessageDigest = MessageDigest.getInstance(type)

    fun update(input: Byte) {
        digest.update(input)
    }

    fun update(input: ByteArray, offset: Int, len: Int) {
        digest.update(input, offset, len)
    }

    fun update(input: ByteArray) {
        digest.update(input)
    }

    fun update(input: ByteBuffer) {
        digest.update(input)
    }

    fun digest(): ByteArray = digest.digest()

    @Throws(DigestException::class)
    fun digest(buf: ByteArray, offset: Int, len: Int): Int = digest.digest(buf, offset, len)

    fun digest(input: ByteArray): ByteArray = digest.digest(input)

    fun digestToHexString(): String {
        val digestBytes = digest()
        val result = StringBuilder()
        for (i in 0 until digestLength) {
            result.append(hexDigitsFor(digestBytes[i]))
        }
        return result.toString()
    }

    fun reset() {
        digest.reset()
    }

    val digestLength: Int
        get() = digest.digestLength

    companion object {
        @JvmStatic
        fun hexDigitsFor(x: Byte): CharArray {
            val value = if (x < 0) 256 + x else x.toInt()
            return hexDigitsFor(value)
        }

        @JvmStatic
        fun hexDigitsFor(x: Int): CharArray {
            if (x < 0 || x > 255) {
                throw ArithmeticException("Hex digits must be between 0 and 255 (argument: $x)")
            }
            return charArrayOf(hexDigitFor(x / 16), hexDigitFor(x % 16))
        }

        @JvmStatic
        fun hexDigitFor(x: Int): Char {
            if (x < 0) {
                throw ArithmeticException("Hex digits can't be negative (argument: $x)")
            }
            if (x < 10) return ('0'.code + x).toChar()
            if (x < 16) return ('a'.code + x - 10).toChar()
            throw ArithmeticException("Hex digits can't be greater than 15 (argument: $x)")
        }
    }
}
