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

package net.ktnx.mobileledger.json.common

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [ObjectMapperProvider].
 *
 * Tests verify:
 * - Singleton behavior
 * - ObjectMapper configuration
 */
class ObjectMapperProviderTest {

    @Test
    fun `objectMapper is not null`() {
        val mapper = ObjectMapperProvider.objectMapper
        assertNotNull(mapper)
    }

    @Test
    fun `objectMapper returns same instance on repeated access`() {
        val first = ObjectMapperProvider.objectMapper
        val second = ObjectMapperProvider.objectMapper
        assertSame(first, second)
    }

    @Test
    fun `objectMapper can serialize simple data class`() {
        val mapper = ObjectMapperProvider.objectMapper
        data class TestData(val name: String, val value: Int)

        val data = TestData("test", 42)
        val json = mapper.writeValueAsString(data)

        assertTrue(json.contains("\"name\""))
        assertTrue(json.contains("\"test\""))
        assertTrue(json.contains("42"))
    }

    @Test
    fun `objectMapper can deserialize simple data class`() {
        val mapper = ObjectMapperProvider.objectMapper
        data class TestData(val name: String, val value: Int)

        val json = """{"name":"hello","value":123}"""
        val data = mapper.readValue(json, TestData::class.java)

        assertTrue(data.name == "hello")
        assertTrue(data.value == 123)
    }

    @Test
    fun `objectMapper handles kotlin default parameters`() {
        val mapper = ObjectMapperProvider.objectMapper
        data class TestData(val name: String = "default", val value: Int = 0)

        val json = """{"name":"custom"}"""
        val data = mapper.readValue(json, TestData::class.java)

        assertTrue(data.name == "custom")
        assertTrue(data.value == 0)
    }
}
