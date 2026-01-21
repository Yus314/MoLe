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

import java.io.ByteArrayInputStream
import java.util.UUID
import kotlinx.coroutines.test.runTest
import net.ktnx.mobileledger.domain.model.Currency
import net.ktnx.mobileledger.domain.model.CurrencyPosition
import net.ktnx.mobileledger.domain.model.FutureDates
import net.ktnx.mobileledger.domain.model.Profile
import net.ktnx.mobileledger.domain.model.Template
import net.ktnx.mobileledger.domain.model.TemplateLine
import net.ktnx.mobileledger.fake.FakeCurrencyRepository
import net.ktnx.mobileledger.fake.FakePreferencesRepository
import net.ktnx.mobileledger.fake.FakeProfileRepository
import net.ktnx.mobileledger.fake.FakeTemplateRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for [RawConfigReader].
 *
 * Tests verify:
 * - Parsing commodities from JSON
 * - Parsing profiles from JSON
 * - Parsing templates with accounts from JSON
 * - Restoring data to repositories (deduplication by UUID/name)
 * - Current profile restoration
 * - Error handling
 */
@RunWith(RobolectricTestRunner::class)
class RawConfigReaderTest {

    private lateinit var fakeProfileRepository: FakeProfileRepository
    private lateinit var fakeTemplateRepository: FakeTemplateRepository
    private lateinit var fakeCurrencyRepository: FakeCurrencyRepository
    private lateinit var fakePreferencesRepository: FakePreferencesRepository

    @Before
    fun setup() {
        fakeProfileRepository = FakeProfileRepository()
        fakeTemplateRepository = FakeTemplateRepository()
        fakeCurrencyRepository = FakeCurrencyRepository()
        fakePreferencesRepository = FakePreferencesRepository()
    }

    // ========================================
    // Parse commodities tests
    // ========================================

    @Test
    fun `readConfig parses commodities`() {
        // Given
        val json = """
            {
                "commodities": [
                    {"name": "USD", "position": "before", "hasGap": true},
                    {"name": "EUR", "position": "after", "hasGap": false}
                ]
            }
        """.trimIndent()

        // When
        val reader = createReader(json)
        reader.readConfig()

        // Then
        assertNotNull(reader.commodities)
        assertEquals(2, reader.commodities?.size)
        assertEquals("USD", reader.commodities?.get(0)?.name)
        assertEquals("EUR", reader.commodities?.get(1)?.name)
    }

    @Test
    fun `readConfig handles null commodities`() {
        // Given
        val json = """{"commodities": null}"""

        // When
        val reader = createReader(json)
        reader.readConfig()

        // Then
        assertNull(reader.commodities)
    }

    // ========================================
    // Parse profiles tests
    // ========================================

    @Test
    fun `readConfig parses profiles`() {
        // Given
        val uuid = UUID.randomUUID().toString()
        val json = """
            {
                "profiles": [
                    {
                        "uuid": "$uuid",
                        "name": "Test Profile",
                        "url": "https://example.com/ledger",
                        "useAuth": false,
                        "permitPosting": true,
                        "colour": 0
                    }
                ]
            }
        """.trimIndent()

        // When
        val reader = createReader(json)
        reader.readConfig()

        // Then
        assertNotNull(reader.profiles)
        assertEquals(1, reader.profiles?.size)
        assertEquals("Test Profile", reader.profiles?.get(0)?.name)
        assertEquals(uuid, reader.profiles?.get(0)?.uuid)
    }

    @Test
    fun `readConfig parses profiles with auth fields`() {
        // Given
        val uuid = UUID.randomUUID().toString()
        val json = """
            {
                "profiles": [
                    {
                        "uuid": "$uuid",
                        "name": "Auth Profile",
                        "url": "https://example.com/ledger",
                        "useAuth": true,
                        "authUser": "testuser",
                        "authPass": "testpass",
                        "permitPosting": true,
                        "colour": 0
                    }
                ]
            }
        """.trimIndent()

        // When
        val reader = createReader(json)
        reader.readConfig()

        // Then
        assertEquals(true, reader.profiles?.get(0)?.useAuthentication)
        assertEquals("testuser", reader.profiles?.get(0)?.authUser)
        assertEquals("testpass", reader.profiles?.get(0)?.authPassword)
    }

    @Test
    fun `readConfig parses profiles with optional fields`() {
        // Given
        val uuid = UUID.randomUUID().toString()
        val json = """
            {
                "profiles": [
                    {
                        "uuid": "$uuid",
                        "name": "Full Profile",
                        "url": "https://example.com/ledger",
                        "useAuth": false,
                        "permitPosting": true,
                        "apiVersion": 3,
                        "defaultCommodity": "USD",
                        "showCommodityByDefault": true,
                        "showCommentsByDefault": false,
                        "futureDates": 1,
                        "preferredAccountsFilter": "Assets",
                        "colour": 5
                    }
                ]
            }
        """.trimIndent()

        // When
        val reader = createReader(json)
        reader.readConfig()

        // Then
        val profile = reader.profiles?.get(0)
        assertNotNull(profile)
        assertEquals(3, profile?.apiVersion)
        assertEquals(5, profile?.theme)
    }

    // ========================================
    // Parse templates tests
    // ========================================

    @Test
    fun `readConfig parses templates`() {
        // Given
        val uuid = UUID.randomUUID().toString()
        val json = """
            {
                "templates": [
                    {
                        "uuid": "$uuid",
                        "name": "Test Template",
                        "regex": ".*test.*",
                        "isFallback": false
                    }
                ]
            }
        """.trimIndent()

        // When
        val reader = createReader(json)
        reader.readConfig()

        // Then
        assertNotNull(reader.templates)
        assertEquals(1, reader.templates?.size)
        assertEquals("Test Template", reader.templates?.get(0)?.header?.name)
        assertEquals(".*test.*", reader.templates?.get(0)?.header?.regularExpression)
    }

    @Test
    fun `readConfig parses templates with accounts`() {
        // Given
        val uuid = UUID.randomUUID().toString()
        val json = """
            {
                "templates": [
                    {
                        "uuid": "$uuid",
                        "name": "Template With Accounts",
                        "regex": ".*",
                        "isFallback": false,
                        "accounts": [
                            {"name": "Expenses:Food", "amount": 100.0},
                            {"name": "Assets:Bank", "amount": -100.0, "negateAmount": true}
                        ]
                    }
                ]
            }
        """.trimIndent()

        // When
        val reader = createReader(json)
        reader.readConfig()

        // Then
        val template = reader.templates?.get(0)
        assertNotNull(template)
        assertEquals(2, template?.accounts?.size)
        assertEquals("Expenses:Food", template?.accounts?.get(0)?.accountName)
        assertEquals(100.0f, template?.accounts?.get(0)?.amount)
    }

    // ========================================
    // Parse current profile tests
    // ========================================

    @Test
    fun `readConfig parses current profile UUID`() {
        // Given
        val uuid = UUID.randomUUID().toString()
        val json = """{"currentProfile": "$uuid"}"""

        // When
        val reader = createReader(json)
        reader.readConfig()

        // Then
        assertEquals(uuid, reader.currentProfile)
    }

    // ========================================
    // Restore tests
    // ========================================

    @Test
    fun `restoreAll inserts new currencies`() = runTest {
        // Given
        val json = """
            {
                "commodities": [
                    {"name": "USD", "position": "before", "hasGap": true}
                ]
            }
        """.trimIndent()
        val reader = createReader(json)
        reader.readConfig()

        // When
        reader.restoreAll(
            fakeProfileRepository,
            fakeTemplateRepository,
            fakeCurrencyRepository,
            fakePreferencesRepository
        )

        // Then
        val currencies = fakeCurrencyRepository.getAllCurrenciesAsDomain().getOrThrow()
        assertEquals(1, currencies.size)
        assertEquals("USD", currencies[0].name)
    }

    @Test
    fun `restoreAll skips existing currencies by name`() = runTest {
        // Given - existing currency
        fakeCurrencyRepository.saveCurrency(
            Currency(name = "USD", position = CurrencyPosition.AFTER, hasGap = false)
        )

        val json = """
            {
                "commodities": [
                    {"name": "USD", "position": "before", "hasGap": true}
                ]
            }
        """.trimIndent()
        val reader = createReader(json)
        reader.readConfig()

        // When
        reader.restoreAll(
            fakeProfileRepository,
            fakeTemplateRepository,
            fakeCurrencyRepository,
            fakePreferencesRepository
        )

        // Then - should still have only 1 currency (not duplicated)
        val currencies = fakeCurrencyRepository.getAllCurrenciesAsDomain().getOrThrow()
        assertEquals(1, currencies.size)
        // Original currency should be preserved (not overwritten)
        assertEquals(CurrencyPosition.AFTER, currencies[0].position)
    }

    @Test
    fun `restoreAll inserts new profiles`() = runTest {
        // Given
        val uuid = UUID.randomUUID().toString()
        val json = """
            {
                "profiles": [
                    {
                        "uuid": "$uuid",
                        "name": "New Profile",
                        "url": "https://example.com/ledger",
                        "useAuth": false,
                        "permitPosting": true,
                        "colour": 0
                    }
                ]
            }
        """.trimIndent()
        val reader = createReader(json)
        reader.readConfig()

        // When
        reader.restoreAll(
            fakeProfileRepository,
            fakeTemplateRepository,
            fakeCurrencyRepository,
            fakePreferencesRepository
        )

        // Then
        val profiles = fakeProfileRepository.getAllProfiles().getOrThrow()
        assertEquals(1, profiles.size)
        assertEquals("New Profile", profiles[0].name)
    }

    @Test
    fun `restoreAll skips existing profiles by UUID`() = runTest {
        // Given - existing profile
        val existingUuid = UUID.randomUUID().toString()
        fakeProfileRepository.insertProfile(
            Profile(
                id = null,
                name = "Existing Profile",
                uuid = existingUuid,
                url = "https://old.example.com",
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
        )

        val json = """
            {
                "profiles": [
                    {
                        "uuid": "$existingUuid",
                        "name": "Updated Profile",
                        "url": "https://new.example.com",
                        "useAuth": false,
                        "permitPosting": true,
                        "colour": 0
                    }
                ]
            }
        """.trimIndent()
        val reader = createReader(json)
        reader.readConfig()

        // When
        reader.restoreAll(
            fakeProfileRepository,
            fakeTemplateRepository,
            fakeCurrencyRepository,
            fakePreferencesRepository
        )

        // Then - should still have only 1 profile (not duplicated)
        val profiles = fakeProfileRepository.getAllProfiles().getOrThrow()
        assertEquals(1, profiles.size)
        // Original profile should be preserved (not overwritten)
        assertEquals("Existing Profile", profiles[0].name)
    }

    @Test
    fun `restoreAll inserts new templates`() = runTest {
        // Given
        val uuid = UUID.randomUUID().toString()
        val json = """
            {
                "templates": [
                    {
                        "uuid": "$uuid",
                        "name": "New Template",
                        "regex": ".*",
                        "isFallback": false
                    }
                ]
            }
        """.trimIndent()
        val reader = createReader(json)
        reader.readConfig()

        // When
        reader.restoreAll(
            fakeProfileRepository,
            fakeTemplateRepository,
            fakeCurrencyRepository,
            fakePreferencesRepository
        )

        // Then
        val templates = fakeTemplateRepository.getAllTemplatesAsDomain().getOrThrow()
        assertEquals(1, templates.size)
        assertEquals("New Template", templates[0].name)
    }

    @Test
    fun `restoreAll skips existing templates by UUID`() = runTest {
        // Given - existing template
        val existingUuid = UUID.randomUUID().toString()
        fakeTemplateRepository.saveTemplate(
            Template(
                id = null,
                name = "Existing Template",
                pattern = ".*existing.*"
            )
        )
        // Get the saved template and update UUID manually via direct access
        val savedTemplates = fakeTemplateRepository.getAllTemplatesWithAccounts().getOrThrow()
        savedTemplates[0].header.uuid = existingUuid

        val json = """
            {
                "templates": [
                    {
                        "uuid": "$existingUuid",
                        "name": "Updated Template",
                        "regex": ".*updated.*",
                        "isFallback": false
                    }
                ]
            }
        """.trimIndent()
        val reader = createReader(json)
        reader.readConfig()

        // When
        reader.restoreAll(
            fakeProfileRepository,
            fakeTemplateRepository,
            fakeCurrencyRepository,
            fakePreferencesRepository
        )

        // Then - should still have only 1 template (not duplicated)
        val templates = fakeTemplateRepository.getAllTemplatesAsDomain().getOrThrow()
        assertEquals(1, templates.size)
        // Original template should be preserved (not overwritten)
        assertEquals("Existing Template", templates[0].name)
    }

    @Test
    fun `restoreAll restores current profile preference`() = runTest {
        // Given
        val uuid = UUID.randomUUID().toString()
        val json = """
            {
                "profiles": [
                    {
                        "uuid": "$uuid",
                        "name": "Current Profile",
                        "url": "https://example.com/ledger",
                        "useAuth": false,
                        "permitPosting": true,
                        "colour": 5
                    }
                ],
                "currentProfile": "$uuid"
            }
        """.trimIndent()
        val reader = createReader(json)
        reader.readConfig()

        // When
        reader.restoreAll(
            fakeProfileRepository,
            fakeTemplateRepository,
            fakeCurrencyRepository,
            fakePreferencesRepository
        )

        // Then
        assertNotNull(fakeProfileRepository.currentProfile.value)
        assertEquals("Current Profile", fakeProfileRepository.currentProfile.value?.name)
        assertEquals(5, fakePreferencesRepository.getStartupTheme())
    }

    @Test
    fun `restoreAll handles missing current profile gracefully`() = runTest {
        // Given - current profile UUID doesn't match any profile
        val nonExistentUuid = UUID.randomUUID().toString()
        val profileUuid = UUID.randomUUID().toString()
        val json = """
            {
                "profiles": [
                    {
                        "uuid": "$profileUuid",
                        "name": "Some Profile",
                        "url": "https://example.com/ledger",
                        "useAuth": false,
                        "permitPosting": true,
                        "colour": 0
                    }
                ],
                "currentProfile": "$nonExistentUuid"
            }
        """.trimIndent()
        val reader = createReader(json)
        reader.readConfig()

        // When
        reader.restoreAll(
            fakeProfileRepository,
            fakeTemplateRepository,
            fakeCurrencyRepository,
            fakePreferencesRepository
        )

        // Then - current profile should not be set
        assertNull(fakeProfileRepository.currentProfile.value)
    }

    // ========================================
    // Error handling tests
    // ========================================

    @Test(expected = RuntimeException::class)
    fun `readConfig throws on unexpected top-level item`() {
        // Given
        val json = """{"unknownField": "value"}"""

        // When
        val reader = createReader(json)
        reader.readConfig()

        // Then - exception is thrown
    }

    @Test
    fun `readConfig handles empty JSON object`() {
        // Given
        val json = """{}"""

        // When
        val reader = createReader(json)
        reader.readConfig()

        // Then - no exception, all fields are null
        assertNull(reader.commodities)
        assertNull(reader.profiles)
        assertNull(reader.templates)
        assertNull(reader.currentProfile)
    }

    // ========================================
    // Helper methods
    // ========================================

    private fun createReader(json: String): RawConfigReader {
        val inputStream = ByteArrayInputStream(json.toByteArray(Charsets.UTF_8))
        return RawConfigReader(inputStream)
    }
}
