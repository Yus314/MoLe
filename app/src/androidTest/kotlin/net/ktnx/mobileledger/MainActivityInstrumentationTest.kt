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

package net.ktnx.mobileledger

import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import net.ktnx.mobileledger.db.DB
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Instrumentation tests for MainActivity with Hilt dependency injection.
 *
 * These tests demonstrate:
 * - Hilt test infrastructure working correctly
 * - In-memory database being injected via TestDatabaseModule
 * - DAO injection working in test context
 *
 * The TestDatabaseModule automatically replaces DatabaseModule,
 * providing an in-memory database for test isolation.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MainActivityInstrumentationTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var db: DB

    @Before
    fun setup() {
        hiltRule.inject()
    }

    /**
     * Verifies that Hilt injection is working in instrumentation tests.
     * The DB instance should be injected by TestDatabaseModule.
     */
    @Test
    fun testHiltInjectionWorks() {
        assertNotNull("DB should be injected", db)
    }

    /**
     * Verifies that the injected database is the in-memory test database.
     * The in-memory database should be empty initially.
     */
    @Test
    fun testInMemoryDatabaseIsUsed() {
        // In-memory database should start empty
        val profiles = db.getProfileDAO().getAllOrderedSync()
        assertTrue("In-memory database should be empty initially", profiles.isEmpty())
    }

    /**
     * Verifies that all DAOs can be obtained from the injected database.
     */
    @Test
    fun testDaoAccess() {
        assertNotNull("ProfileDAO should be accessible", db.getProfileDAO())
        assertNotNull("TransactionDAO should be accessible", db.getTransactionDAO())
        assertNotNull("AccountDAO should be accessible", db.getAccountDAO())
        assertNotNull("AccountValueDAO should be accessible", db.getAccountValueDAO())
        assertNotNull("TemplateDAO should be accessible", db.getTemplateDAO())
        assertNotNull("TemplateAccountDAO should be accessible", db.getTemplateAccountDAO())
        assertNotNull("CurrencyDAO should be accessible", db.getCurrencyDAO())
        assertNotNull("OptionDAO should be accessible", db.getOptionDAO())
    }
}
