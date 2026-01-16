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

/**
 * サーバーバージョン情報
 *
 * hledger-webサーバーの検出されたバージョンを保持する。
 * APIの互換性判断に使用される。
 */
data class ServerVersion(
    /** メジャーバージョン */
    val major: Int,

    /** マイナーバージョン */
    val minor: Int,

    /** 1.19より前のバージョンかどうか */
    val isPre_1_19: Boolean = false
) {
    /**
     * 表示用のバージョン文字列
     *
     * @return "major.minor" 形式の文字列
     */
    val displayString: String get() = "$major.$minor"
}
