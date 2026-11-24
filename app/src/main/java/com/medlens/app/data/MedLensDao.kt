package com.medlens.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface MedLensDao {
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun createAccount(account: UserAccount)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun updateAccount(account: UserAccount)
    @Query("SELECT * FROM user_accounts WHERE id = :id LIMIT 1") suspend fun account(id: String): UserAccount?
    @Query("SELECT * FROM user_accounts WHERE id = :email LIMIT 1") suspend fun accountByEmail(email: String): UserAccount?
    @Query("DELETE FROM user_accounts WHERE id = :accountId") suspend fun deleteAccount(accountId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveMedicine(item: SavedMedicine)
    @Query("SELECT * FROM saved_medicines WHERE accountId = :accountId ORDER BY genericName COLLATE NOCASE") suspend fun savedMedicines(accountId: String): List<SavedMedicine>
    @Query("SELECT EXISTS(SELECT 1 FROM saved_medicines WHERE accountId = :accountId AND genericName = :name)") suspend fun isSaved(accountId: String, name: String): Boolean
    @Query("DELETE FROM saved_medicines WHERE accountId = :accountId AND genericName = :name") suspend fun removeSavedMedicine(accountId: String, name: String)
    @Query("DELETE FROM saved_medicines WHERE accountId = :accountId") suspend fun clearSavedMedicines(accountId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun addSearch(search: RecentSearch)
    @Query("SELECT * FROM recent_searches WHERE accountId = :accountId ORDER BY searchedAt DESC LIMIT :limit") suspend fun recentSearches(accountId: String, limit: Int = 12): List<RecentSearch>
    @Query("DELETE FROM recent_searches WHERE accountId = :accountId AND query NOT IN (SELECT query FROM recent_searches WHERE accountId = :accountId ORDER BY searchedAt DESC LIMIT 12)") suspend fun pruneSearches(accountId: String)
    @Query("DELETE FROM recent_searches WHERE accountId = :accountId") suspend fun clearSearches(accountId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun addViewed(item: RecentlyViewed)
    @Query("SELECT * FROM recently_viewed WHERE accountId = :accountId ORDER BY viewedAt DESC LIMIT :limit") suspend fun recentlyViewed(accountId: String, limit: Int = 12): List<RecentlyViewed>
    @Query("DELETE FROM recently_viewed WHERE accountId = :accountId AND genericName NOT IN (SELECT genericName FROM recently_viewed WHERE accountId = :accountId ORDER BY viewedAt DESC LIMIT 12)") suspend fun pruneViewed(accountId: String)
    @Query("DELETE FROM recently_viewed WHERE accountId = :accountId") suspend fun clearRecentlyViewed(accountId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun addChatSession(session: ChatSession)
    @Query("SELECT * FROM chat_sessions WHERE accountId = :accountId ORDER BY createdAt DESC LIMIT 50") suspend fun chatSessions(accountId: String): List<ChatSession>
    @Query("SELECT * FROM chat_sessions WHERE id = :id AND accountId = :accountId LIMIT 1") suspend fun chatSession(id: String, accountId: String): ChatSession?
    @Query("DELETE FROM chat_sessions WHERE accountId = :accountId") suspend fun clearChatSessions(accountId: String)
    @Query("DELETE FROM chat_messages WHERE accountId = :accountId") suspend fun clearChatMessagesForAccount(accountId: String)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun addChatMessage(message: ChatMessage)
    @Query("SELECT * FROM (SELECT * FROM chat_messages WHERE sessionId = :sessionId AND accountId = :accountId ORDER BY sentAt DESC LIMIT 200) ORDER BY sentAt ASC") suspend fun chatMessages(sessionId: String, accountId: String): List<ChatMessage>
    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId AND accountId = :accountId") suspend fun clearChatMessages(sessionId: String, accountId: String)

    @Transaction
    suspend fun clearAllAccountData(accountId: String) {
        clearChatMessagesForAccount(accountId)
        clearChatSessions(accountId)
        clearSavedMedicines(accountId)
        clearSearches(accountId)
        clearRecentlyViewed(accountId)
        deleteAccount(accountId)
    }
}
