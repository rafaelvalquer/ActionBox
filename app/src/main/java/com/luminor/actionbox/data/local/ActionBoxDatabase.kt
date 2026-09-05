package com.luminor.actionbox.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ActionEntity::class,
        ProjectEntity::class,
        ActionListEntity::class,
        ListItemEntity::class,
        ActionCompletionEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class ActionBoxDatabase : RoomDatabase() {
    abstract fun actionDao(): ActionDao

    companion object {
        @Volatile private var INSTANCE: ActionBoxDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE actions ADD COLUMN description TEXT")
                db.execSQL("ALTER TABLE actions ADD COLUMN endAt INTEGER")
                db.execSQL("ALTER TABLE actions ADD COLUMN priority TEXT")
                db.execSQL("ALTER TABLE actions ADD COLUMN recurrenceType TEXT")
                db.execSQL("ALTER TABLE actions ADD COLUMN recurrenceDays TEXT")
                db.execSQL("ALTER TABLE actions ADD COLUMN reminderMinutes INTEGER")
                db.execSQL("ALTER TABLE actions ADD COLUMN projectId INTEGER")
                db.execSQL("CREATE TABLE IF NOT EXISTS projects (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL, createdAt INTEGER NOT NULL, archived INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS action_lists (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, createdAt INTEGER NOT NULL, archived INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS list_items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, listId INTEGER NOT NULL, title TEXT NOT NULL, position INTEGER NOT NULL, completedAt INTEGER)")
                db.execSQL("CREATE TABLE IF NOT EXISTS action_completions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, actionId INTEGER NOT NULL, occurrenceDate TEXT NOT NULL, completedAt INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_action_completions_actionId_occurrenceDate ON action_completions(actionId, occurrenceDate)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE projects ADD COLUMN completedAt INTEGER")
                db.execSQL("ALTER TABLE action_lists ADD COLUMN completedAt INTEGER")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE actions ADD COLUMN noteCategory TEXT")
                db.execSQL("ALTER TABLE actions ADD COLUMN noteColor TEXT")
                db.execSQL("ALTER TABLE actions ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE actions ADD COLUMN updatedAt INTEGER")
            }
        }

        fun getInstance(context: Context): ActionBoxDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ActionBoxDatabase::class.java,
                    "actionbox.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
