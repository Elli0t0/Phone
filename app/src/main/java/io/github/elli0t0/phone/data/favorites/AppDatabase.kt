package io.github.elli0t0.phone.data.favorites

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.elli0t0.phone.data.favorites.model.ContactSimEntity
import io.github.elli0t0.phone.data.favorites.model.FavoriteContactEntity

@Database(
    entities = [FavoriteContactEntity::class, ContactSimEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoritesDao(): FavoritesDao
    abstract fun contactSimDao(): ContactSimDao

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

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "phone_database"
                ).addMigrations(MIGRATION_1_2).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
