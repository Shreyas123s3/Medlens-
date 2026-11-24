package com.medlens.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Medicine::class, SavedMedicine::class, UserAccount::class, RecentSearch::class, RecentlyViewed::class, ChatSession::class, ChatMessage::class], version = 5, exportSchema = false)
abstract class MedLensDatabase : RoomDatabase() {
    abstract fun medicineDao(): MedicineDao
    abstract fun medLensDao(): MedLensDao

    companion object {
        @Volatile private var instance: MedLensDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM medicines WHERE source LIKE '%WHO Model List of Essential Medicines%'")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS saved_medicines")
                db.execSQL("CREATE TABLE IF NOT EXISTS user_accounts (id TEXT NOT NULL, name TEXT NOT NULL, email TEXT NOT NULL, passwordSalt TEXT NOT NULL, passwordHash TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_user_accounts_email ON user_accounts(email)")
                db.execSQL("CREATE TABLE IF NOT EXISTS saved_medicines (accountId TEXT NOT NULL, genericName TEXT NOT NULL, savedAt INTEGER NOT NULL, PRIMARY KEY(accountId, genericName))")
                db.execSQL("CREATE TABLE IF NOT EXISTS recent_searches (accountId TEXT NOT NULL, query TEXT NOT NULL, searchedAt INTEGER NOT NULL, PRIMARY KEY(accountId, query))")
                db.execSQL("CREATE TABLE IF NOT EXISTS recently_viewed (accountId TEXT NOT NULL, genericName TEXT NOT NULL, viewedAt INTEGER NOT NULL, PRIMARY KEY(accountId, genericName))")
                db.execSQL("CREATE TABLE IF NOT EXISTS chat_sessions (id TEXT NOT NULL, accountId TEXT NOT NULL, title TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE TABLE IF NOT EXISTS chat_messages (id TEXT NOT NULL, sessionId TEXT NOT NULL, accountId TEXT NOT NULL, role TEXT NOT NULL, text TEXT NOT NULL, sentAt INTEGER NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chat_messages_sessionId ON chat_messages(sessionId)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medicines ADD COLUMN howSupplied TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE medicines ADD COLUMN warnings TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE medicines ADD COLUMN brandAvailability TEXT NOT NULL DEFAULT 'Information unavailable'")
                db.execSQL("ALTER TABLE medicines ADD COLUMN synonyms TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE medicines ADD COLUMN otcStatus TEXT NOT NULL DEFAULT 'UNKNOWN'")
                db.execSQL("ALTER TABLE medicines ADD COLUMN retrievedAt TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE medicines ADD COLUMN sourceRecordId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE medicines ADD COLUMN sourceEffectiveDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE medicines ADD COLUMN sourceUrl TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE medicines ADD COLUMN productType TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE medicines ADD COLUMN unavailableFields TEXT NOT NULL DEFAULT ''")
                db.execSQL("DELETE FROM medicines WHERE source != 'U.S. FDA OpenFDA Drug Product Labeling'")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chat_messages_sessionId_sentAt ON chat_messages(sessionId, sentAt)")
            }
        }

        fun get(context: Context): MedLensDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                MedLensDatabase::class.java,
                "medlens.db",
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build().also { instance = it }
        }
    }
}
