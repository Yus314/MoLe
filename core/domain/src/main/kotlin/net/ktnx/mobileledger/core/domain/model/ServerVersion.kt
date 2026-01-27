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

package net.ktnx.mobileledger.core.domain.model

/**
 * サーバーバージョン情報
 *
 * hledger-webサーバーの検出されたバージョンを保持する。
 * APIの互換性判断に使用される。
 */
data class ServerVersion(
    /** メジャーバージョン */
    val major: Int = 0,

    /** マイナーバージョン */
    val minor: Int = 0,

    /** パッチバージョン */
    val patch: Int? = null,

    /** 1.20.1より前のバージョンかどうか（バージョン検出不可の場合） */
    val isPre_1_20_1: Boolean = false
) {
    /**
     * 表示用のバージョン文字列
     *
     * @return "major.minor" または "major.minor.patch" 形式の文字列
     */
    val displayString: String
        get() = when {
            isPre_1_20_1 -> "(before 1.20)"
            patch != null -> "$major.$minor.$patch"
            else -> "$major.$minor"
        }

    /**
     * 指定されたバージョン以上かどうかを判定
     *
     * @param major メジャーバージョン
     * @param minor マイナーバージョン
     * @return 指定バージョン以上なら true
     */
    fun atLeast(major: Int, minor: Int): Boolean = (this.major == major && this.minor >= minor) || this.major > major

    companion object {
        /**
         * 1.20.1より前のバージョンを示すインスタンスを生成
         */
        fun preLegacy(): ServerVersion = ServerVersion(isPre_1_20_1 = true)

        /**
         * バージョン文字列をパースしてServerVersionを生成
         *
         * @param versionString "major.minor" または "major.minor.patch" 形式、または "pre-1.19"
         * @return パース結果、またはパース失敗時は null
         */
        fun parse(versionString: String): ServerVersion? {
            if (versionString == "pre-1.19") {
                return preLegacy()
            }

            val parts = versionString.split(".")
            if (parts.size >= 2) {
                val major = parts[0].toIntOrNull() ?: return null
                val minor = parts[1].toIntOrNull() ?: return null
                val patch = if (parts.size >= 3) parts[2].toIntOrNull() else null

                return ServerVersion(major, minor, patch)
            }

            return null
        }
    }
}
