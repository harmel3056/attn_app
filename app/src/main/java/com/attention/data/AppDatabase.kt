package com.attention.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.attention.data.dao.ArticleDao
import com.attention.data.dao.DailyBriefingDao
import com.attention.data.dao.SourceDao
import com.attention.data.dao.SummaryDao
import com.attention.data.entity.ArticleEntity
import com.attention.data.entity.DailyBriefingEntity
import com.attention.data.entity.SourceEntity
import com.attention.data.entity.SummaryEntity

@Database(
    // Defines Room database and the tables that exist
    entities = [
        SourceEntity::class,
        ArticleEntity::class,
        SummaryEntity::class,
        DailyBriefingEntity::class
    ],
    version = 2,
    // Tells Room if a version mismatch should trigger migration or other action
    exportSchema = false
    // May change to write migration scripts later as needed
)
abstract class AppDatabase : RoomDatabase() {
    /* AppDatabase is our DB, extending RoomDB which is the
    base class that Room generates implementations for.
    It's abstract because Room will generate the actual
    implementation at compile time */
    abstract fun sourceDao(): SourceDao
    abstract fun articleDao(): ArticleDao
    abstract fun summaryDao(): SummaryDao
    abstract fun dailyBriefingDao(): DailyBriefingDao
    // These are DAO accessors, exposing the DAOs to the db

    companion object {
        // Migration from version 1 to 2
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_briefings ADD COLUMN status TEXT NOT NULL DEFAULT 'FULL'")
            }
        }

        // Holds the single instance of the database.
        @Volatile
        // @Volatile ensures visibility across threads.
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // Returns the database instance, creating it if necessary.
            return INSTANCE ?: synchronized(this) {
                // If INSTANCE is not null, return it
                // Otherwise, enter the synchronized block

                val instance = Room.databaseBuilder(
                    // Create the database using Room.
                    context.applicationContext, // avoid leaking Activity
                    AppDatabase::class.java,    // tell Room which DB to build
                    "attention_database"        // DB filename
                )
                .addMigrations(MIGRATION_1_2)
                .build()

                INSTANCE = instance
                // Save the instance for future calls

                instance
                // Return the newly created instance
            }
        }
    }
}

