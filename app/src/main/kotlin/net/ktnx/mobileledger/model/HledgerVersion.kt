/*
 * Copyright © 2020, 2024 Damyan Ivanov.
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

package net.ktnx.mobileledger.model

import java.util.Locale
import net.ktnx.mobileledger.json.API

class HledgerVersion {
    val major: Int
    val minor: Int
    val patch: Int
    val isPre_1_20_1: Boolean
    private val hasPatch: Boolean

    constructor(major: Int, minor: Int) {
        this.major = major
        this.minor = minor
        this.patch = 0
        this.isPre_1_20_1 = false
        this.hasPatch = false
    }

    constructor(major: Int, minor: Int, patch: Int) {
        this.major = major
        this.minor = minor
        this.patch = patch
        this.isPre_1_20_1 = false
        this.hasPatch = true
    }

    constructor(pre_1_20_1: Boolean) {
        require(pre_1_20_1) { "pre_1_20_1 argument must be true" }
        this.major = 0
        this.minor = 0
        this.patch = 0
        this.isPre_1_20_1 = true
        this.hasPatch = false
    }

    constructor(origin: HledgerVersion) {
        this.major = origin.major
        this.minor = origin.minor
        this.isPre_1_20_1 = origin.isPre_1_20_1
        this.patch = origin.patch
        this.hasPatch = origin.hasPatch
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is HledgerVersion) return false

        return isPre_1_20_1 == other.isPre_1_20_1 &&
                major == other.major &&
                minor == other.minor &&
                patch == other.patch &&
                hasPatch == other.hasPatch
    }

    override fun hashCode(): Int {
        var result = major
        result = 31 * result + minor
        result = 31 * result + patch
        result = 31 * result + isPre_1_20_1.hashCode()
        result = 31 * result + hasPatch.hashCode()
        return result
    }

    override fun toString(): String = if (isPre_1_20_1) {
            "(before 1.20)"
        } else if (hasPatch) {
            String.format(Locale.ROOT, "%d.%d.%d", major, minor, patch)
        } else {
            String.format(Locale.ROOT, "%d.%d", major, minor)
        }

    fun atLeast(major: Int, minor: Int): Boolean = (this.major == major && this.minor >= minor) || this.major > major

    fun getSuitableApiVersion(): API? {
        if (isPre_1_20_1) return null

        return when {
            atLeast(1, 50) -> API.v1_50
            atLeast(1, 40) -> API.v1_40
            atLeast(1, 32) -> API.v1_32
            atLeast(1, 23) -> API.v1_23
            atLeast(1, 19) -> API.v1_19_1
            atLeast(1, 15) -> API.v1_15
            atLeast(1, 14) -> API.v1_14
            else -> null
        }
    }
}
