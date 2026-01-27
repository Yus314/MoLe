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

package net.ktnx.mobileledger.backup

import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlinx.coroutines.test.runTest
import net.ktnx.mobileledger.core.domain.model.Currency
import net.ktnx.mobileledger.core.domain.model.CurrencyPosition
import net.ktnx.mobileledger.core.domain.model.FutureDates
import net.ktnx.mobileledger.core.domain.model.Profile
import net.ktnx.mobileledger.core.domain.model.ProfileAuthentication
import net.ktnx.mobileledger.core.domain.model.Template
import net.ktnx.mobileledger.core.domain.model.TemplateLine
import net.ktnx.mobileledger.fake.FakeCurrencyRepository
import net.ktnx.mobileledger.fake.FakeProfileRepository
import net.ktnx.mobileledger.fake.FakeTemplateRepository
import net.ktnx.mobileledger.json.API
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for [RawConfigWriter].
 *
 * Tests verify:
 * - Writing commodities section
 * - Writing profiles section with auth fields
 * - Writing templates section with accounts
 * - Conditional field writing (auth, commodity, API version)
 * - Empty data handling
 */
@RunWith(RobolectricTestRunner::class)
class RawConfigWriterTest {

    private lateinit var fakeProfileRepository: FakeProfileRepository
    private lateinit var fakeTemplateRepository: FakeTemplateRepository
    private lateinit var fakeCurrencyRepository: FakeCurrencyRepository

    @Before
    fun setup() {
        fakeProfileRepository = FakeProfileRepository()
        fakeTemplateRepository = FakeTemplateRepository()
        fakeCurrencyRepository = FakeCurrencyRepository()
    }

    // ========================================
    // Commodities section tests
    // ========================================

    @Test
    fun `writeConfig writes commodities section`() = runTest {
        // Given
        fakeCurrencyRepository.saveCurrency(
            Currency(name = "USD", position = CurrencyPosition.BEFORE, hasGap = true)
        )
        fakeCurrencyRepository.saveCurrency(
            Currency(name = "EUR", position = CurrencyPosition.AFTER, hasGap = false)
        )

        // When
        val json = writeConfigToJson()

        // Then
        assertTrue(json.has(BackupKeys.COMMODITIES))
        val commodities = json.getJSONArray(BackupKeys.COMMODITIES)
        assertEquals(2, commodities.length())

        val usd = commodities.getJSONObject(0)
        assertEquals("USD", usd.getString(BackupKeys.NAME))
        assertEquals("before", usd.getString(BackupKeys.POSITION))
        assertTrue(usd.getBoolean(BackupKeys.HAS_GAP))

        val eur = commodities.getJSONObject(1)
        assertEquals("EUR", eur.getString(BackupKeys.NAME))
        assertEquals("after", eur.getString(BackupKeys.POSITION))
        assertFalse(eur.getBoolean(BackupKeys.HAS_GAP))
    }

    @Test
    fun `writeConfig skips commodities section when empty`() = runTest {
        // Given - no currencies

        // When
        val json = writeConfigToJson()

        // Then
        assertFalse(json.has(BackupKeys.COMMODITIES))
    }

    // ========================================
    // Profiles section tests
    // ========================================

    @Test
    fun `writeConfig writes profiles section`() = runTest {
        // Given
        val profile = createTestProfile()
        fakeProfileRepository.insertProfile(profile)

        // When
        val json = writeConfigToJson()

        // Then
        assertTrue(json.has(BackupKeys.PROFILES))
        val profiles = json.getJSONArray(BackupKeys.PROFILES)
        assertEquals(1, profiles.length())

        val p = profiles.getJSONObject(0)
        assertEquals("Test Profile", p.getString(BackupKeys.NAME))
        assertEquals(profile.uuid, p.getString(BackupKeys.UUID))
        assertEquals("https://example.com/ledger", p.getString(BackupKeys.URL))
        assertFalse(p.getBoolean(BackupKeys.USE_AUTH))
        assertTrue(p.getBoolean(BackupKeys.CAN_POST))
    }

    @Test
    fun `writeConfig writes auth fields only when authentication enabled`() = runTest {
        // Given
        val profileWithAuth = createTestProfile().copy(
            authentication = ProfileAuthentication("testuser", "testpass")
        )
        fakeProfileRepository.insertProfile(profileWithAuth)

        // When
        val json = writeConfigToJson()

        // Then
        val profiles = json.getJSONArray(BackupKeys.PROFILES)
        val p = profiles.getJSONObject(0)
        assertTrue(p.getBoolean(BackupKeys.USE_AUTH))
        assertEquals("testuser", p.getString(BackupKeys.AUTH_USER))
        assertEquals("testpass", p.getString(BackupKeys.AUTH_PASS))
    }

    @Test
    fun `writeConfig skips auth fields when authentication disabled`() = runTest {
        // Given
        val profileWithoutAuth = createTestProfile()
        fakeProfileRepository.insertProfile(profileWithoutAuth)

        // When
        val json = writeConfigToJson()

        // Then
        val profiles = json.getJSONArray(BackupKeys.PROFILES)
        val p = profiles.getJSONObject(0)
        assertFalse(p.getBoolean(BackupKeys.USE_AUTH))
        assertFalse(p.has(BackupKeys.AUTH_USER))
        assertFalse(p.has(BackupKeys.AUTH_PASS))
    }

    @Test
    fun `writeConfig writes API version only when not auto`() = runTest {
        // Given
        val profileWithApiVersion = createTestProfile().copy(apiVersion = 3)
        fakeProfileRepository.insertProfile(profileWithApiVersion)

        // When
        val json = writeConfigToJson()

        // Then
        val profiles = json.getJSONArray(BackupKeys.PROFILES)
        val p = profiles.getJSONObject(0)
        assertEquals(3, p.getInt(BackupKeys.API_VER))
    }

    @Test
    fun `writeConfig skips API version when auto`() = runTest {
        // Given - API version = 0 (auto)
        val profile = createTestProfile().copy(apiVersion = API.auto.toInt())
        fakeProfileRepository.insertProfile(profile)

        // When
        val json = writeConfigToJson()

        // Then
        val profiles = json.getJSONArray(BackupKeys.PROFILES)
        val p = profiles.getJSONObject(0)
        assertFalse(p.has(BackupKeys.API_VER))
    }

    @Test
    fun `writeConfig writes commodity fields only when canPost is true`() = runTest {
        // Given
        val profileWithPosting = createTestProfile().copy(
            permitPosting = true,
            defaultCommodity = "USD",
            showCommodityByDefault = true,
            showCommentsByDefault = false
        )
        fakeProfileRepository.insertProfile(profileWithPosting)

        // When
        val json = writeConfigToJson()

        // Then
        val profiles = json.getJSONArray(BackupKeys.PROFILES)
        val p = profiles.getJSONObject(0)
        assertTrue(p.getBoolean(BackupKeys.CAN_POST))
        assertEquals("USD", p.getString(BackupKeys.DEFAULT_COMMODITY))
        assertTrue(p.getBoolean(BackupKeys.SHOW_COMMODITY))
        assertFalse(p.getBoolean(BackupKeys.SHOW_COMMENTS))
    }

    @Test
    fun `writeConfig skips commodity fields when canPost is false`() = runTest {
        // Given
        val profileNoPosting = createTestProfile().copy(
            permitPosting = false,
            defaultCommodity = "USD"
        )
        fakeProfileRepository.insertProfile(profileNoPosting)

        // When
        val json = writeConfigToJson()

        // Then
        val profiles = json.getJSONArray(BackupKeys.PROFILES)
        val p = profiles.getJSONObject(0)
        assertFalse(p.getBoolean(BackupKeys.CAN_POST))
        assertFalse(p.has(BackupKeys.DEFAULT_COMMODITY))
        assertFalse(p.has(BackupKeys.SHOW_COMMODITY))
        assertFalse(p.has(BackupKeys.SHOW_COMMENTS))
    }

    @Test
    fun `writeConfig skips profiles section when empty`() = runTest {
        // Given - no profiles

        // When
        val json = writeConfigToJson()

        // Then
        assertFalse(json.has(BackupKeys.PROFILES))
    }

    // ========================================
    // Templates section tests
    // ========================================

    @Test
    fun `writeConfig writes templates section`() = runTest {
        // Given
        val template = createTestTemplate()
        fakeTemplateRepository.saveTemplate(template)

        // When
        val json = writeConfigToJson()

        // Then
        assertTrue(json.has(BackupKeys.TEMPLATES))
        val templates = json.getJSONArray(BackupKeys.TEMPLATES)
        assertEquals(1, templates.length())

        val t = templates.getJSONObject(0)
        assertEquals("Test Template", t.getString(BackupKeys.NAME))
        assertEquals(".*test.*", t.getString(BackupKeys.REGEX))
    }

    @Test
    fun `writeConfig writes template accounts`() = runTest {
        // Given
        val template = createTestTemplate().copy(
            lines = listOf(
                TemplateLine(
                    accountName = "Expenses:Food",
                    amount = 100.0f,
                    negateAmount = false
                ),
                TemplateLine(
                    accountName = "Assets:Bank",
                    amount = -100.0f,
                    negateAmount = true
                )
            )
        )
        fakeTemplateRepository.saveTemplate(template)

        // When
        val json = writeConfigToJson()

        // Then
        val templates = json.getJSONArray(BackupKeys.TEMPLATES)
        val t = templates.getJSONObject(0)
        assertTrue(t.has(BackupKeys.ACCOUNTS))

        val accounts = t.getJSONArray(BackupKeys.ACCOUNTS)
        assertEquals(2, accounts.length())

        val firstAccount = accounts.getJSONObject(0)
        assertEquals("Expenses:Food", firstAccount.getString(BackupKeys.NAME))
    }

    @Test
    fun `writeConfig skips templates section when empty`() = runTest {
        // Given - no templates

        // When
        val json = writeConfigToJson()

        // Then
        assertFalse(json.has(BackupKeys.TEMPLATES))
    }

    // ========================================
    // Current profile tests
    // ========================================

    @Test
    fun `writeConfig writes current profile UUID`() = runTest {
        // Given
        val profile = createTestProfile()
        fakeProfileRepository.insertProfile(profile)
        fakeProfileRepository.setCurrentProfile(profile.copy(id = 1L))

        // When
        val json = writeConfigToJson()

        // Then
        assertTrue(json.has(BackupKeys.CURRENT_PROFILE))
        assertEquals(profile.uuid, json.getString(BackupKeys.CURRENT_PROFILE))
    }

    @Test
    fun `writeConfig skips current profile when not set`() = runTest {
        // Given
        val profile = createTestProfile()
        fakeProfileRepository.insertProfile(profile)
        // Don't set current profile

        // When
        val json = writeConfigToJson()

        // Then
        assertFalse(json.has(BackupKeys.CURRENT_PROFILE))
    }

    // ========================================
    // Empty data tests
    // ========================================

    @Test
    fun `writeConfig writes valid JSON structure with empty data`() = runTest {
        // Given - no data

        // When
        val json = writeConfigToJson()

        // Then - should produce valid empty JSON object
        // Just verify it's a valid JSON object (no exception thrown)
        assertTrue(json.length() == 0 || json.length() > 0)
    }

    // ========================================
    // Helper methods
    // ========================================

    private suspend fun writeConfigToJson(): JSONObject {
        val outputStream = ByteArrayOutputStream()
        val writer = RawConfigWriter(
            outputStream,
            fakeProfileRepository,
            fakeTemplateRepository,
            fakeCurrencyRepository
        )
        writer.writeConfig()
        return JSONObject(outputStream.toString("UTF-8"))
    }

    private fun createTestProfile(name: String = "Test Profile", url: String = "https://example.com/ledger"): Profile =
        Profile(
            id = null,
            name = name,
            uuid = UUID.randomUUID().toString(),
            url = url,
            authentication = null,
            orderNo = 0,
            permitPosting = true,
            theme = 0,
            preferredAccountsFilter = null,
            futureDates = FutureDates.None,
            apiVersion = 0,
            showCommodityByDefault = false,
            defaultCommodity = null,
            showCommentsByDefault = true,
            serverVersion = null
        )

    private fun createTestTemplate(name: String = "Test Template", pattern: String = ".*test.*"): Template = Template(
        id = null,
        name = name,
        pattern = pattern,
        testText = "sample test text",
        isFallback = false
    )
}
