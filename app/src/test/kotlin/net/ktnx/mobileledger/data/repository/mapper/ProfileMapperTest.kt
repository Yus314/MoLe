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

package net.ktnx.mobileledger.data.repository.mapper

import net.ktnx.mobileledger.core.database.entity.Profile as DbProfile
import net.ktnx.mobileledger.core.domain.model.FutureDates
import net.ktnx.mobileledger.core.domain.model.Profile as DomainProfile
import net.ktnx.mobileledger.core.domain.model.ProfileAuthentication
import net.ktnx.mobileledger.core.domain.model.ServerVersion
import net.ktnx.mobileledger.data.repository.mapper.ProfileMapper.toDomain
import net.ktnx.mobileledger.data.repository.mapper.ProfileMapper.toEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileMapperTest {

    @Test
    fun `toDomain maps basic fields correctly`() {
        val dbProfile = createDbProfile(
            id = 1L,
            name = "Test Profile",
            uuid = "test-uuid-123",
            url = "https://example.com/api"
        )

        val domainProfile = dbProfile.toDomain()

        assertEquals(1L, domainProfile.id)
        assertEquals("Test Profile", domainProfile.name)
        assertEquals("test-uuid-123", domainProfile.uuid)
        assertEquals("https://example.com/api", domainProfile.url)
    }

    @Test
    fun `toDomain maps authentication when enabled`() {
        val dbProfile = createDbProfile(
            useAuthentication = true,
            authUser = "testuser",
            authPassword = "testpass"
        )

        val domainProfile = dbProfile.toDomain()

        assertNotNull(domainProfile.authentication)
        assertEquals("testuser", domainProfile.authentication?.user)
        assertEquals("testpass", domainProfile.authentication?.password)
        assertTrue(domainProfile.isAuthEnabled)
    }

    @Test
    fun `toDomain returns null authentication when disabled`() {
        val dbProfile = createDbProfile(
            useAuthentication = false,
            authUser = "testuser",
            authPassword = "testpass"
        )

        val domainProfile = dbProfile.toDomain()

        assertNull(domainProfile.authentication)
        assertFalse(domainProfile.isAuthEnabled)
    }

    @Test
    fun `toDomain maps serverVersion correctly`() {
        val dbProfile = createDbProfile(
            detectedVersionMajor = 1,
            detectedVersionMinor = 19,
            detectedVersionPre_1_19 = true
        )

        val domainProfile = dbProfile.toDomain()

        assertNotNull(domainProfile.serverVersion)
        assertEquals(1, domainProfile.serverVersion?.major)
        assertEquals(19, domainProfile.serverVersion?.minor)
        assertTrue(domainProfile.serverVersion?.isPre_1_20_1 ?: false)
        // When isPre_1_20_1 is true, displayString returns "(before 1.20)"
        assertEquals("(before 1.20)", domainProfile.serverVersion?.displayString)
    }

    @Test
    fun `toDomain maps futureDates enum correctly`() {
        val dbProfile = createDbProfile(futureDates = 14) // TwoWeeks has value 14

        val domainProfile = dbProfile.toDomain()

        assertEquals(FutureDates.TwoWeeks, domainProfile.futureDates)
    }

    @Test
    fun `toDomain handles invalid futureDates value`() {
        val dbProfile = createDbProfile(futureDates = 99) // Invalid value

        val domainProfile = dbProfile.toDomain()

        assertEquals(FutureDates.None, domainProfile.futureDates) // Should default to None
    }

    @Test
    fun `toDomain maps all optional fields`() {
        val dbProfile = createDbProfile(
            orderNo = 5,
            permitPosting = true,
            theme = 3,
            preferredAccountsFilter = "Assets:",
            apiVersion = 1,
            showCommodityByDefault = true,
            defaultCommodity = "USD",
            showCommentsByDefault = false
        )

        val domainProfile = dbProfile.toDomain()

        assertEquals(5, domainProfile.orderNo)
        assertTrue(domainProfile.permitPosting)
        assertTrue(domainProfile.canPost)
        assertEquals(3, domainProfile.theme)
        assertEquals("Assets:", domainProfile.preferredAccountsFilter)
        assertEquals(1, domainProfile.apiVersion)
        assertTrue(domainProfile.showCommodityByDefault)
        assertEquals("USD", domainProfile.defaultCommodity)
        assertEquals("USD", domainProfile.defaultCommodityOrEmpty)
        assertFalse(domainProfile.showCommentsByDefault)
    }

    @Test
    fun `toEntity maps basic fields correctly`() {
        val domainProfile = createDomainProfile(
            id = 1L,
            name = "Test Profile",
            uuid = "test-uuid-123",
            url = "https://example.com/api"
        )

        val dbProfile = domainProfile.toEntity()

        assertEquals(1L, dbProfile.id)
        assertEquals("Test Profile", dbProfile.name)
        assertEquals("test-uuid-123", dbProfile.uuid)
        assertEquals("https://example.com/api", dbProfile.url)
    }

    @Test
    fun `toEntity maps new profile with null id to id 0`() {
        val domainProfile = createDomainProfile(id = null)

        val dbProfile = domainProfile.toEntity()

        assertEquals(0L, dbProfile.id)
    }

    @Test
    fun `toEntity maps authentication fields when present`() {
        val domainProfile = createDomainProfile(
            authentication = ProfileAuthentication(user = "testuser", password = "testpass")
        )

        val dbProfile = domainProfile.toEntity()

        assertTrue(dbProfile.useAuthentication)
        assertEquals("testuser", dbProfile.authUser)
        assertEquals("testpass", dbProfile.authPassword)
    }

    @Test
    fun `toEntity maps authentication fields when absent`() {
        val domainProfile = createDomainProfile(authentication = null)

        val dbProfile = domainProfile.toEntity()

        assertFalse(dbProfile.useAuthentication)
        assertNull(dbProfile.authUser)
        assertNull(dbProfile.authPassword)
    }

    @Test
    fun `toEntity maps serverVersion fields`() {
        val domainProfile = createDomainProfile(
            serverVersion = ServerVersion(major = 1, minor = 19, isPre_1_20_1 = true)
        )

        val dbProfile = domainProfile.toEntity()

        assertEquals(1, dbProfile.detectedVersionMajor)
        assertEquals(19, dbProfile.detectedVersionMinor)
        assertTrue(dbProfile.detectedVersionPre_1_19)
    }

    @Test
    fun `toEntity maps null serverVersion to defaults`() {
        val domainProfile = createDomainProfile(serverVersion = null)

        val dbProfile = domainProfile.toEntity()

        assertEquals(0, dbProfile.detectedVersionMajor)
        assertEquals(0, dbProfile.detectedVersionMinor)
        assertFalse(dbProfile.detectedVersionPre_1_19)
    }

    @Test
    fun `toEntity maps futureDates to int`() {
        val domainProfile = createDomainProfile(futureDates = FutureDates.OneMonth)

        val dbProfile = domainProfile.toEntity()

        assertEquals(30, dbProfile.futureDates) // OneMonth has value 30
    }

    @Test
    fun `roundTrip preserves all data`() {
        val original = createDomainProfile(
            id = 123L,
            name = "Round Trip Test",
            uuid = "uuid-roundtrip",
            url = "https://roundtrip.example.com",
            authentication = ProfileAuthentication(user = "rtuser", password = "rtpass"),
            orderNo = 7,
            permitPosting = true,
            theme = 5,
            preferredAccountsFilter = "Expenses:",
            futureDates = FutureDates.All,
            apiVersion = 2,
            showCommodityByDefault = true,
            defaultCommodity = "EUR",
            showCommentsByDefault = false,
            serverVersion = ServerVersion(major = 2, minor = 5, isPre_1_20_1 = false)
        )

        val dbProfile = original.toEntity()
        val restored = dbProfile.toDomain()

        assertEquals(original.id, restored.id)
        assertEquals(original.name, restored.name)
        assertEquals(original.uuid, restored.uuid)
        assertEquals(original.url, restored.url)
        assertEquals(original.authentication, restored.authentication)
        assertEquals(original.orderNo, restored.orderNo)
        assertEquals(original.permitPosting, restored.permitPosting)
        assertEquals(original.theme, restored.theme)
        assertEquals(original.preferredAccountsFilter, restored.preferredAccountsFilter)
        assertEquals(original.futureDates, restored.futureDates)
        assertEquals(original.apiVersion, restored.apiVersion)
        assertEquals(original.showCommodityByDefault, restored.showCommodityByDefault)
        assertEquals(original.defaultCommodity, restored.defaultCommodity)
        assertEquals(original.showCommentsByDefault, restored.showCommentsByDefault)
        assertEquals(original.serverVersion, restored.serverVersion)
    }

    @Test
    fun `roundTrip preserves data without authentication`() {
        val original = createDomainProfile(
            id = 456L,
            name = "No Auth Profile",
            authentication = null
        )

        val dbProfile = original.toEntity()
        val restored = dbProfile.toDomain()

        assertEquals(original.id, restored.id)
        assertEquals(original.name, restored.name)
        assertNull(restored.authentication)
    }

    // Helper functions to create test objects
    private fun createDbProfile(
        id: Long = 0L,
        name: String = "Test",
        uuid: String = "test-uuid",
        url: String = "https://example.com",
        useAuthentication: Boolean = false,
        authUser: String? = null,
        authPassword: String? = null,
        orderNo: Int = 0,
        permitPosting: Boolean = false,
        theme: Int = -1,
        preferredAccountsFilter: String? = null,
        futureDates: Int = 0,
        apiVersion: Int = 0,
        showCommodityByDefault: Boolean = false,
        defaultCommodity: String? = null,
        showCommentsByDefault: Boolean = true,
        detectedVersionPre_1_19: Boolean = false,
        detectedVersionMajor: Int = 0,
        detectedVersionMinor: Int = 0
    ): DbProfile = DbProfile().apply {
        this.id = id
        this.name = name
        this.uuid = uuid
        this.url = url
        this.useAuthentication = useAuthentication
        this.authUser = authUser
        this.authPassword = authPassword
        this.orderNo = orderNo
        this.permitPosting = permitPosting
        this.theme = theme
        this.preferredAccountsFilter = preferredAccountsFilter
        this.futureDates = futureDates
        this.apiVersion = apiVersion
        this.showCommodityByDefault = showCommodityByDefault
        this.setDefaultCommodity(defaultCommodity)
        this.showCommentsByDefault = showCommentsByDefault
        this.detectedVersionPre_1_19 = detectedVersionPre_1_19
        this.detectedVersionMajor = detectedVersionMajor
        this.detectedVersionMinor = detectedVersionMinor
    }

    private fun createDomainProfile(
        id: Long? = null,
        name: String = "Test Profile",
        uuid: String = "test-uuid",
        url: String = "https://example.com",
        authentication: ProfileAuthentication? = null,
        orderNo: Int = 0,
        permitPosting: Boolean = false,
        theme: Int = -1,
        preferredAccountsFilter: String? = null,
        futureDates: FutureDates = FutureDates.None,
        apiVersion: Int = 0,
        showCommodityByDefault: Boolean = false,
        defaultCommodity: String? = null,
        showCommentsByDefault: Boolean = true,
        serverVersion: ServerVersion? = null
    ): DomainProfile = DomainProfile(
        id = id,
        name = name,
        uuid = uuid,
        url = url,
        authentication = authentication,
        orderNo = orderNo,
        permitPosting = permitPosting,
        theme = theme,
        preferredAccountsFilter = preferredAccountsFilter,
        futureDates = futureDates,
        apiVersion = apiVersion,
        showCommodityByDefault = showCommodityByDefault,
        defaultCommodity = defaultCommodity,
        showCommentsByDefault = showCommentsByDefault,
        serverVersion = serverVersion
    )
}
