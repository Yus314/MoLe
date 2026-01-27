/*
 * Copyright © 2018 Damyan Ivanov.
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

package net.ktnx.mobileledger

import junit.framework.TestCase.assertEquals
import net.ktnx.mobileledger.core.common.utils.Digest
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class DigestUnitTest {
    @Test
    fun digestToHexString_isCorrect() {
        assertEquals('0', Digest.hexDigitFor(0))
        assertEquals('1', Digest.hexDigitFor(1))
        assertEquals('2', Digest.hexDigitFor(2))
        assertEquals('3', Digest.hexDigitFor(3))
        assertEquals('4', Digest.hexDigitFor(4))
        assertEquals('5', Digest.hexDigitFor(5))
        assertEquals('6', Digest.hexDigitFor(6))
        assertEquals('7', Digest.hexDigitFor(7))
        assertEquals('8', Digest.hexDigitFor(8))
        assertEquals('9', Digest.hexDigitFor(9))
        assertEquals('a', Digest.hexDigitFor(10))
        assertEquals('b', Digest.hexDigitFor(11))
        assertEquals('c', Digest.hexDigitFor(12))
        assertEquals('d', Digest.hexDigitFor(13))
        assertEquals('e', Digest.hexDigitFor(14))
        assertEquals('f', Digest.hexDigitFor(15))
    }

    @Test
    fun hexDigitsFor_isCorrect() {
        assertEquals("00", String(Digest.hexDigitsFor(0)))
        assertEquals("10", String(Digest.hexDigitsFor(16)))
        assertEquals("ff", String(Digest.hexDigitsFor(255)))
        assertEquals("a0", String(Digest.hexDigitsFor(160)))
        assertEquals("10", String(Digest.hexDigitsFor(16.toByte())))
        assertEquals("ff", String(Digest.hexDigitsFor((-1).toByte())))
    }

    @Test(expected = ArithmeticException::class)
    fun digestToHexString_throwsOnNegative() {
        Digest.hexDigitFor(-1)
    }

    @Test(expected = ArithmeticException::class)
    fun digestToHexString_throwsOnGreaterThan15() {
        Digest.hexDigitFor(16)
    }

    @Test(expected = ArithmeticException::class)
    fun hexDigitsFor_throwsOnNegative() {
        Digest.hexDigitsFor(-5)
    }

    @Test(expected = ArithmeticException::class)
    fun hexDigitsFor_throwsOnGreaterThan255() {
        Digest.hexDigitsFor(256)
    }
}
