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
        ActionCompletionEntity::class,
        TagEntity::class,
        TagRefEntity::class,
        ContentLinkEntity::class,
        RoutineRuleEntity::class
    ],
    version = 5,
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

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE actions ADD COLUMN deletedAt INTEGER")
                db.execSQL("ALTER TABLE actions ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE projects ADD COLUMN updatedAt INTEGER")
                db.execSQL("ALTER TABLE projects ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE projects ADD COLUMN deletedAt INTEGER")
                db.execSQL("ALTER TABLE action_lists ADD COLUMN updatedAt INTEGER")
                db.execSQL("ALTER TABLE action_lists ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE action_lists ADD COLUMN deletedAt INTEGER")

                db.execSQL("CREATE TABLE IF NOT EXISTS tags (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, normalizedName TEXT NOT NULL, colorKey TEXT, createdAt INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS tag_refs (tagId INTEGER NOT NULL, ownerType TEXT NOT NULL, ownerId INTEGER NOT NULL, PRIMARY KEY(tagId, ownerType, ownerId))")
                db.execSQL("CREATE TABLE IF NOT EXISTS content_links (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sourceType TEXT NOT NULL, sourceId INTEGER NOT NULL, targetType TEXT NOT NULL, targetId INTEGER NOT NULL, relationType TEXT NOT NULL, createdAt INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS routine_rules (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, actionId INTEGER NOT NULL, effectiveFrom INTEGER NOT NULL, effectiveUntil INTEGER, recurrenceType TEXT NOT NULL, recurrenceDays TEXT, scheduledTimeMinutes INTEGER, reminderMinutes INTEGER)")

                db.execSQL("CREATE INDEX IF NOT EXISTS index_actions_projectId ON actions(projectId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_actions_deletedAt ON actions(deletedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_projects_deletedAt ON projects(deletedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_action_lists_deletedAt ON action_lists(deletedAt)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_tags_normalizedName ON tags(normalizedName)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tag_refs_ownerType_ownerId ON tag_refs(ownerType, ownerId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_content_links_sourceType_sourceId ON content_links(sourceType, sourceId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_content_links_targetType_targetId ON content_links(targetType, targetId)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_content_links_sourceType_sourceId_targetType_targetId_relationType ON content_links(sourceType, sourceId, targetType, targetId, relationType)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_routine_rules_actionId ON routine_rules(actionId)")
            }
        }

        fun getInstance(context: Context): ActionBoxDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ActionBoxDatabase::class.java,
                    "actionbox.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
