package com.luminor.actionbox.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActionBoxDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ActionBoxDatabase::class.java
    )

    @Test
    fun migrate1To6() = migrateFrom(1)

    @Test
    fun migrate2To6() = migrateFrom(2)

    @Test
    fun migrate3To6() = migrateFrom(3)

    @Test
    fun migrate4To6() = migrateFrom(4)

    @Test
    fun migrate5To6AddsIconEmoji() {
        val databaseName = "$TEST_DB-5"
        helper.createDatabase(databaseName, 5).close()
        val db = helper.runMigrationsAndValidate(
            databaseName,
            6,
            true,
            *ActionBoxDatabase.ALL_MIGRATIONS
        )

        val cursor = db.query("PRAGMA table_info(actions)")
        var found = false
        val nameIndex = cursor.getColumnIndex("name")
        while (cursor.moveToNext()) {
            if (nameIndex >= 0 && cursor.getString(nameIndex) == "iconEmoji") {
                found = true
                break
            }
        }
        cursor.close()
        db.close()
        assertTrue("iconEmoji deve existir após MIGRATION_5_6", found)
    }

    private fun migrateFrom(version: Int) {
        val databaseName = "$TEST_DB-$version"
        helper.createDatabase(databaseName, version).close()
        helper.runMigrationsAndValidate(
            databaseName,
            6,
            true,
            *ActionBoxDatabase.ALL_MIGRATIONS
        ).close()
    }

    private companion object {
        const val TEST_DB = "actionbox-migration-test"
    }
}
