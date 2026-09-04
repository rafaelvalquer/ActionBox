package com.luminor.actionbox.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ActionEntity::class], version = 1, exportSchema = true)
abstract class ActionBoxDatabase : RoomDatabase() {
    abstract fun actionDao(): ActionDao

    companion object {
        @Volatile private var INSTANCE: ActionBoxDatabase? = null

        fun getInstance(context: Context): ActionBoxDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ActionBoxDatabase::class.java,
                    "actionbox.db"
                ).build().also { INSTANCE = it }
            }
    }
}
