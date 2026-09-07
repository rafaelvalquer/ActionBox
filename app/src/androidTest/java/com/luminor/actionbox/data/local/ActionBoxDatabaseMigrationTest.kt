package com.luminor.actionbox.data.local

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActionBoxDatabaseMigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ActionBoxDatabase::class.java
    )

    @Test fun migrate1To6() = migrateFrom(1)
    @Test fun migrate2To6() = migrateFrom(2)
    @Test fun migrate3To6() = migrateFrom(3)
    @Test fun migrate4To6() = migrateFrom(4)

    @Test
    fun migrate5To6PreservesActionsAndAddsEmoji() {
        context.deleteDatabase(TEST_DB)
        helper.createDatabase(TEST_DB, 5).use { db ->
            db.execSQL("INSERT INTO actions (type,title,content,sourceText,createdAt,status,isPinned,sortOrder) VALUES ('TASK','Legacy','Legacy','Legacy',1,'PENDING',0,0)")
        }
        helper.runMigrationsAndValidate(TEST_DB, 6, true, *ActionBoxDatabase.ALL_MIGRATIONS).use { db ->
            val columns = db.query("PRAGMA table_info(actions)")
            var hasEmoji = false
            while (columns.moveToNext()) hasEmoji = hasEmoji || columns.getString(columns.getColumnIndexOrThrow("name")) == "iconEmoji"
            columns.close()
            assertTrue(hasEmoji)
            db.query("SELECT iconEmoji FROM actions WHERE title = 'Legacy'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertTrue(cursor.isNull(0))
            }
        }
    }

    private fun migrateFrom(version: Int) {
        context.deleteDatabase(TEST_DB)
        helper.createDatabase(TEST_DB, version).close()
        helper.runMigrationsAndValidate(TEST_DB, 6, true, *ActionBoxDatabase.ALL_MIGRATIONS).close()
    }

    private companion object { const val TEST_DB = "actionbox-migration-test.db" }
}
