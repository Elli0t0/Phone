package io.github.elli0t0.phone.data.favorites

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.elli0t0.phone.data.favorites.model.ContactSimEntity
import io.github.elli0t0.phone.data.favorites.model.FavoriteContactEntity
import io.github.elli0t0.phone.data.recordings.RecordingEntity
import io.github.elli0t0.phone.data.recordings.RecordingsDao

@Database(
    entities = [FavoriteContactEntity::class, ContactSimEntity::class, RecordingEntity::class],
    version = 5,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoritesDao(): FavoritesDao
    abstract fun contactSimDao(): ContactSimDao
    abstract fun recordingsDao(): RecordingsDao

    companion object {
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `contact_sims` " +
                        "(`contactLookupKey` TEXT NOT NULL PRIMARY KEY, " +
                        "`simId` TEXT, `updatedAt` INTEGER NOT NULL)"
                )
            }
        }

        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `recordings` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`uri` TEXT NOT NULL, `number` TEXT NOT NULL, " +
                        "`displayName` TEXT, `startedAt` INTEGER NOT NULL, " +
                        "`durationMs` INTEGER NOT NULL, `sizeBytes` INTEGER NOT NULL, " +
                        "`isTest` INTEGER NOT NULL)"
                )
            }
        }

        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `recordings` " +
                        "ADD COLUMN `isSilent` INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `recordings` " +
                        "ADD COLUMN `starred` INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE `recordings` ADD COLUMN `title` TEXT"
                )
                db.execSQL(
                    "ALTER TABLE `recordings` ADD COLUMN `note` TEXT"
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "phone_database"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
