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

import net.ktnx.mobileledger.core.common.utils.SimpleDate
import net.ktnx.mobileledger.core.domain.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileTest {

    @Test
    fun `isAuthEnabled returns true when authentication is set`() {
        val profile = createTestProfile(
            authentication = ProfileAuthentication(user = "testuser", password = "testpass")
        )

        assertTrue(profile.isAuthEnabled)
    }

    @Test
    fun `isAuthEnabled returns false when authentication is null`() {
        val profile = createTestProfile(authentication = null)

        assertFalse(profile.isAuthEnabled)
    }

    @Test
    fun `canPost returns true when permitPosting is true`() {
        val profile = createTestProfile(permitPosting = true)

        assertTrue(profile.canPost)
    }

    @Test
    fun `canPost returns false when permitPosting is false`() {
        val profile = createTestProfile(permitPosting = false)

        assertFalse(profile.canPost)
    }

    @Test
    fun `defaultCommodityOrEmpty returns defaultCommodity when set`() {
        val profile = createTestProfile(defaultCommodity = "USD")

        assertEquals("USD", profile.defaultCommodityOrEmpty)
    }

    @Test
    fun `defaultCommodityOrEmpty returns empty string when defaultCommodity is null`() {
        val profile = createTestProfile(defaultCommodity = null)

        assertEquals("", profile.defaultCommodityOrEmpty)
    }

    @Test
    fun `profile with same values are equal`() {
        val profile1 = createTestProfile(id = 1L)
        val profile2 = createTestProfile(id = 1L)

        assertEquals(profile1, profile2)
    }

    @Test
    fun `profile copy preserves values`() {
        val original = createTestProfile(
            id = 1L,
            name = "Test",
            authentication = ProfileAuthentication(user = "user", password = "pass")
        )

        val copied = original.copy(name = "Modified")

        assertEquals("Modified", copied.name)
        assertEquals(original.id, copied.id)
        assertEquals(original.authentication, copied.authentication)
    }

    @Test
    fun `new profile has null id`() {
        val profile = createTestProfile(id = null)

        assertEquals(null, profile.id)
    }

    @Test
    fun `existing profile has non-null id`() {
        val profile = createTestProfile(id = 123L)

        assertEquals(123L, profile.id)
    }

    // ========================================
    // Server version properties
    // ========================================

    @Test
    fun `isVersionPre_1_20_1 returns false when serverVersion is null`() {
        val profile = createTestProfile(serverVersion = null)

        assertFalse(profile.isVersionPre_1_20_1)
    }

    @Test
    fun `isVersionPre_1_20_1 returns true when flag is set`() {
        val profile = createTestProfile(serverVersion = ServerVersion.preLegacy())

        assertTrue(profile.isVersionPre_1_20_1)
    }

    @Test
    fun `isVersionPre_1_20_1 returns false for normal version`() {
        val profile = createTestProfile(serverVersion = ServerVersion(1, 21))

        assertFalse(profile.isVersionPre_1_20_1)
    }

    @Test
    fun `detectedVersionMajor returns 0 when serverVersion is null`() {
        val profile = createTestProfile(serverVersion = null)

        assertEquals(0, profile.detectedVersionMajor)
    }

    @Test
    fun `detectedVersionMajor returns major version`() {
        val profile = createTestProfile(serverVersion = ServerVersion(2, 5))

        assertEquals(2, profile.detectedVersionMajor)
    }

    @Test
    fun `detectedVersionMinor returns 0 when serverVersion is null`() {
        val profile = createTestProfile(serverVersion = null)

        assertEquals(0, profile.detectedVersionMinor)
    }

    @Test
    fun `detectedVersionMinor returns minor version`() {
        val profile = createTestProfile(serverVersion = ServerVersion(1, 25))

        assertEquals(25, profile.detectedVersionMinor)
    }

    // ========================================
    // Companion object
    // ========================================

    @Test
    fun `NO_PROFILE_ID is 0`() {
        assertEquals(0L, Profile.NO_PROFILE_ID)
    }

    // ========================================
    // Helper method
    // ========================================

    private fun createTestProfile(
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
    ): Profile = Profile(
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
