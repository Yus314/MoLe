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

package net.ktnx.mobileledger.core.network.json.unified

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Char 用のカスタムシリアライザ
 */
object CharSerializer : KSerializer<Char> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Char", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Char) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Char {
        val str = decoder.decodeString()
        return if (str.isNotEmpty()) str[0] else '\u0000'
    }
}

/**
 * asprecision 用のカスタムシリアライザ（Int/Number どちらも受け入れる）
 */
object AsprecisionSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Asprecision", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Int) {
        encoder.encodeInt(value)
    }

    override fun deserialize(decoder: Decoder): Int = when (decoder) {
        is JsonDecoder -> {
            val element = decoder.decodeJsonElement().jsonPrimitive
            element.intOrNull ?: 0
        }

        else -> decoder.decodeInt()
    }
}

/**
 * 統合 ParsedStyle - hledger API v1_32+ 用
 *
 * v1_32+ で使用されるフィールド:
 * - asdecimalmark (String): 小数点記号
 * - asrounding: 丸めモード
 */
@Serializable
data class UnifiedParsedStyle(
    /** 通貨記号の位置（'L'=左, 'R'=右） */
    @Serializable(with = CharSerializer::class)
    val ascommodityside: Char = '\u0000',

    /** 通貨記号と数値の間にスペースを入れるか */
    @SerialName("ascommodityspaced")
    val isAscommodityspaced: Boolean = false,

    /** 桁グループ */
    val digitgroups: Int = 0,

    /** 小数点記号（v1_32+） */
    val asdecimalmark: String = ".",

    /** 小数点精度 */
    @Serializable(with = AsprecisionSerializer::class)
    val asprecision: Int = 0,

    /** 丸めモード（v1_32+） */
    val asrounding: String? = null
)
