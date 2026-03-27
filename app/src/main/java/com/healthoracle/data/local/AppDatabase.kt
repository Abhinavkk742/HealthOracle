package com.healthoracle.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.healthoracle.data.local.dao.AppointmentDao
import com.healthoracle.data.local.dao.TodoDao
import com.healthoracle.data.local.dao.WalkSessionDao
import com.healthoracle.data.local.entity.AppointmentEntity
import com.healthoracle.data.local.entity.TodoEntity
import com.healthoracle.data.local.entity.WalkSession

@Database(
    entities = [
        AppointmentEntity::class,
        WalkSession::class,
        TodoEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appointmentDao(): AppointmentDao
    abstract fun walkSessionDao(): WalkSessionDao
    abstract fun todoDao(): TodoDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `todos` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `appointmentId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `time` TEXT NOT NULL,
                        `date` TEXT NOT NULL,
                        `category` TEXT NOT NULL DEFAULT 'Checkup',
                        `isDone` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }
    }
}