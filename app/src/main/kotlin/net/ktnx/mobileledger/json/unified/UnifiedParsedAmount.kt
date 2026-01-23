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

package net.ktnx.mobileledger.json.unified

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * サロゲートクラス - aprice/acost 両方のフィールド名をサポート
 */
@Serializable
private data class UnifiedParsedAmountSurrogate(
    val acommodity: String? = null,
    val aquantity: UnifiedParsedQuantity? = null,
    val aismultiplier: Boolean = false,
    val astyle: UnifiedParsedStyle? = null,
    val aprice: UnifiedParsedPrice? = null,
    val acost: UnifiedParsedPrice? = null
)

/**
 * UnifiedParsedAmount 用のカスタムシリアライザ
 *
 * aprice/acost のエイリアス対応（v1_50 では acost、それ以前は aprice）
 */
object UnifiedParsedAmountSerializer : KSerializer<UnifiedParsedAmount> {
    override val descriptor: SerialDescriptor =
        UnifiedParsedAmountSurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: UnifiedParsedAmount) {
        val surrogate = UnifiedParsedAmountSurrogate(
            acommodity = value.acommodity,
            aquantity = value.aquantity,
            aismultiplier = value.aismultiplier,
            astyle = value.astyle,
            aprice = value.aprice
        )
        encoder.encodeSerializableValue(UnifiedParsedAmountSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): UnifiedParsedAmount {
        val surrogate = decoder.decodeSerializableValue(UnifiedParsedAmountSurrogate.serializer())
        return UnifiedParsedAmount(
            acommodity = surrogate.acommodity,
            aquantity = surrogate.aquantity,
            aismultiplier = surrogate.aismultiplier,
            astyle = surrogate.astyle,
            aprice = surrogate.aprice ?: surrogate.acost
        )
    }
}

/**
 * 統合 ParsedAmount - 全 API バージョンの差分を吸収
 *
 * バージョン間の差分:
 * - v1_14-v1_40: aprice フィールド
 * - v1_50: acost フィールド（同じ意味）
 *
 * カスタムシリアライザで両方のフィールド名に対応する。
 */
@Serializable(with = UnifiedParsedAmountSerializer::class)
data class UnifiedParsedAmount(
    /** 通貨/商品コード */
    val acommodity: String? = null,
    /** 金額数値 */
    val aquantity: UnifiedParsedQuantity? = null,
    /** 乗数かどうか */
    val aismultiplier: Boolean = false,
    /** 金額スタイル */
    val astyle: UnifiedParsedStyle? = null,
    /**
     * 価格情報
     *
     * v1_14-v1_40: aprice
     * v1_50: acost（デシリアライズ時にマージ）
     */
    val aprice: UnifiedParsedPrice? = null
)
