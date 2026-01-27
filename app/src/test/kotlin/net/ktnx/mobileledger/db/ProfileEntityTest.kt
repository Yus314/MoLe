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

package net.ktnx.mobileledger.db

import net.ktnx.mobileledger.core.database.entity.Profile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [Profile] Room entity.
 */
class ProfileEntityTest {

    @Test
    fun `default values are correct`() {
        val profile = Profile()

        assertEquals(0L, profile.id)
        assertEquals("", profile.name)
        assertNotNull(profile.uuid) // UUID is auto-generated
        assertEquals("", profile.url)
        assertFalse(profile.useAuthentication)
        assertNull(profile.authUser)
        assertNull(profile.authPassword)
        assertEquals(0, profile.orderNo)
        assertFalse(profile.permitPosting)
        assertEquals(-1, profile.theme)
        assertNull(profile.preferredAccountsFilter)
        assertEquals(0, profile.futureDates)
        assertEquals(0, profile.apiVersion)
        assertFalse(profile.showCommodityByDefault)
        assertNull(profile.defaultCommodity)
        assertTrue(profile.showCommentsByDefault)
        assertFalse(profile.detectedVersionPre_1_19)
        assertEquals(0, profile.detectedVersionMajor)
        assertEquals(0, profile.detectedVersionMinor)
    }

    @Test
    fun `uuid is generated on creation`() {
        val profile1 = Profile()
        val profile2 = Profile()

        assertNotNull(profile1.uuid)
        assertNotNull(profile2.uuid)
        assertNotEquals(profile1.uuid, profile2.uuid)
    }

    @Test
    fun `getDefaultCommodityOrEmpty returns empty string when null`() {
        val profile = Profile()

        assertEquals("", profile.getDefaultCommodityOrEmpty())
    }

    @Test
    fun `getDefaultCommodityOrEmpty returns commodity when set`() {
        val profile = Profile()
        profile.setDefaultCommodity("USD")

        assertEquals("USD", profile.getDefaultCommodityOrEmpty())
    }

    @Test
    fun `setDefaultCommodity with null sets to empty string`() {
        val profile = Profile()
        profile.setDefaultCommodity("USD")
        profile.setDefaultCommodity(null)

        // nullIsEmpty converts null to empty string
        assertEquals("", profile.defaultCommodity)
    }

    @Test
    fun `setDefaultCommodity with empty string keeps empty string`() {
        val profile = Profile()
        profile.setDefaultCommodity("")

        // nullIsEmpty keeps empty string as is
        assertEquals("", profile.defaultCommodity)
    }

    @Test
    fun `setDefaultCommodity with value sets correctly`() {
        val profile = Profile()
        profile.setDefaultCommodity("EUR")

        assertEquals("EUR", profile.defaultCommodity)
    }

    @Test
    fun `isAuthEnabled returns useAuthentication value`() {
        val profile = Profile()

        assertFalse(profile.isAuthEnabled())

        profile.useAuthentication = true
        assertTrue(profile.isAuthEnabled())
    }

    @Test
    fun `canPost returns permitPosting value`() {
        val profile = Profile()

        assertFalse(profile.canPost())

        profile.permitPosting = true
        assertTrue(profile.canPost())
    }

    @Test
    fun `isVersionPre_1_19 returns detectedVersionPre_1_19 value`() {
        val profile = Profile()

        assertFalse(profile.isVersionPre_1_19())

        profile.detectedVersionPre_1_19 = true
        assertTrue(profile.isVersionPre_1_19())
    }

    @Test
    fun `toString returns name`() {
        val profile = Profile()
        profile.name = "Test Profile"

        assertEquals("Test Profile", profile.toString())
    }

    @Test
    fun `toString returns empty string when name is empty`() {
        val profile = Profile()

        assertEquals("", profile.toString())
    }

    @Test
    fun `equals returns true for same values`() {
        val profile1 = Profile()
        profile1.id = 1L
        profile1.name = "Test"
        profile1.uuid = "test-uuid"
        profile1.url = "http://test.com"

        val profile2 = Profile()
        profile2.id = 1L
        profile2.name = "Test"
        profile2.uuid = "test-uuid"
        profile2.url = "http://test.com"

        assertEquals(profile1, profile2)
    }

    @Test
    fun `equals returns false for different ids`() {
        val profile1 = Profile()
        profile1.id = 1L
        profile1.uuid = "same-uuid"

        val profile2 = Profile()
        profile2.id = 2L
        profile2.uuid = "same-uuid"

        assertNotEquals(profile1, profile2)
    }

    @Test
    fun `equals returns false for different names`() {
        val profile1 = Profile()
        profile1.id = 1L
        profile1.name = "Name 1"
        profile1.uuid = "same-uuid"

        val profile2 = Profile()
        profile2.id = 1L
        profile2.name = "Name 2"
        profile2.uuid = "same-uuid"

        assertNotEquals(profile1, profile2)
    }

    @Test
    fun `equals returns false for different urls`() {
        val profile1 = Profile()
        profile1.id = 1L
        profile1.uuid = "same-uuid"
        profile1.url = "http://url1.com"

        val profile2 = Profile()
        profile2.id = 1L
        profile2.uuid = "same-uuid"
        profile2.url = "http://url2.com"

        assertNotEquals(profile1, profile2)
    }

    @Test
    fun `equals returns false for different useAuthentication`() {
        val profile1 = Profile()
        profile1.id = 1L
        profile1.uuid = "same-uuid"
        profile1.useAuthentication = true

        val profile2 = Profile()
        profile2.id = 1L
        profile2.uuid = "same-uuid"
        profile2.useAuthentication = false

        assertNotEquals(profile1, profile2)
    }

    @Test
    fun `equals returns false for non-Profile object`() {
        val profile = Profile()

        assertFalse(profile.equals("not a profile"))
        assertFalse(profile.equals(null))
    }

    @Test
    fun `hashCode is consistent for equal objects`() {
        val profile1 = Profile()
        profile1.id = 1L
        profile1.name = "Test"
        profile1.uuid = "test-uuid"

        val profile2 = Profile()
        profile2.id = 1L
        profile2.name = "Test"
        profile2.uuid = "test-uuid"

        assertEquals(profile1.hashCode(), profile2.hashCode())
    }

    @Test
    fun `hashCode differs for different objects`() {
        val profile1 = Profile()
        profile1.id = 1L
        profile1.name = "Test 1"
        profile1.uuid = "uuid-1"

        val profile2 = Profile()
        profile2.id = 2L
        profile2.name = "Test 2"
        profile2.uuid = "uuid-2"

        assertNotEquals(profile1.hashCode(), profile2.hashCode())
    }

    @Test
    fun `NO_PROFILE_ID constant is 0`() {
        assertEquals(0L, Profile.NO_PROFILE_ID)
    }

    @Test
    fun `all fields can be set and read`() {
        val profile = Profile()

        profile.id = 42L
        profile.name = "My Profile"
        profile.uuid = "custom-uuid"
        profile.url = "http://example.com"
        profile.useAuthentication = true
        profile.authUser = "user"
        profile.authPassword = "pass"
        profile.orderNo = 5
        profile.permitPosting = true
        profile.theme = 100
        profile.preferredAccountsFilter = "Assets"
        profile.futureDates = 2
        profile.apiVersion = 3
        profile.showCommodityByDefault = true
        profile.setDefaultCommodity("JPY")
        profile.showCommentsByDefault = false
        profile.detectedVersionPre_1_19 = true
        profile.detectedVersionMajor = 1
        profile.detectedVersionMinor = 20

        assertEquals(42L, profile.id)
        assertEquals("My Profile", profile.name)
        assertEquals("custom-uuid", profile.uuid)
        assertEquals("http://example.com", profile.url)
        assertTrue(profile.useAuthentication)
        assertEquals("user", profile.authUser)
        assertEquals("pass", profile.authPassword)
        assertEquals(5, profile.orderNo)
        assertTrue(profile.permitPosting)
        assertEquals(100, profile.theme)
        assertEquals("Assets", profile.preferredAccountsFilter)
        assertEquals(2, profile.futureDates)
        assertEquals(3, profile.apiVersion)
        assertTrue(profile.showCommodityByDefault)
        assertEquals("JPY", profile.defaultCommodity)
        assertFalse(profile.showCommentsByDefault)
        assertTrue(profile.detectedVersionPre_1_19)
        assertEquals(1, profile.detectedVersionMajor)
        assertEquals(20, profile.detectedVersionMinor)
    }
}
