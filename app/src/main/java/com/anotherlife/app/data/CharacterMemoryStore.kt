package com.anotherlife.app.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Entity(tableName = "npc_memory", indices = [Index(value = ["worldId", "characterId", "createdAt"])])
data class NpcMemoryRecord(
 @PrimaryKey val id: String,
 val worldId: String,
 val characterId: String,
 val text: String,
 val source: String,
 val importance: Int,
 val createdAt: Long
)

@Dao
interface NpcMemoryDao {
 @Insert(onConflict = OnConflictStrategy.REPLACE)
 suspend fun save(memory: NpcMemoryRecord)

 @Query("SELECT * FROM npc_memory WHERE worldId = :worldId AND characterId = :characterId ORDER BY importance DESC, createdAt DESC LIMIT :limit")
 suspend fun forCharacter(worldId: String, characterId: String, limit: Int = 24): List<NpcMemoryRecord>

 @Query("SELECT COUNT(*) FROM npc_memory WHERE worldId = :worldId AND characterId = :characterId")
 suspend fun countForCharacter(worldId: String, characterId: String): Int

 @Query("DELETE FROM npc_memory WHERE worldId = :worldId AND characterId = :characterId")
 suspend fun forgetCharacter(worldId: String, characterId: String)
}

@Entity(tableName = "npc_profiles", indices = [Index(value = ["createdAt"])])
data class NpcCharacterProfile(
 @PrimaryKey val id: String,
 val name: String,
 val role: String,
 val personality: String,
 val background: String,
 val speechStyle: String,
 val createdAt: Long
)

@Entity(tableName = "npc_chat_messages", indices = [Index(value = ["worldId", "characterId", "createdAt"])])
data class NpcChatMessage(
 @PrimaryKey val id: String,
 val worldId: String,
 val characterId: String,
 val role: String,
 val text: String,
 val createdAt: Long
)

@Dao
interface NpcCharacterDao {
 @Query("SELECT * FROM npc_profiles ORDER BY createdAt, id")
 suspend fun all(): List<NpcCharacterProfile>
 @Insert(onConflict = OnConflictStrategy.REPLACE)
 suspend fun save(profile: NpcCharacterProfile)
}

@Dao
interface NpcChatMessageDao {
 @Query("SELECT * FROM npc_chat_messages WHERE worldId = :worldId AND characterId = :characterId ORDER BY createdAt DESC, id DESC LIMIT :limit")
 suspend fun forCharacter(worldId: String, characterId: String, limit: Int = 100): List<NpcChatMessage>
 @Insert(onConflict = OnConflictStrategy.REPLACE)
 suspend fun save(message: NpcChatMessage)
 @Query("DELETE FROM npc_chat_messages WHERE worldId = :worldId AND characterId = :characterId")
 suspend fun clear(worldId: String, characterId: String)
}

@Database(entities = [NpcMemoryRecord::class, NpcCharacterProfile::class, NpcChatMessage::class], version = 2, exportSchema = false)
abstract class CharacterMemoryStore : RoomDatabase() {
 abstract fun memories(): NpcMemoryDao
 abstract fun characters(): NpcCharacterDao
 abstract fun messages(): NpcChatMessageDao

 companion object {
  val MIGRATION_1_2 = object : Migration(1, 2) {
   override fun migrate(db: SupportSQLiteDatabase) {
    db.execSQL("CREATE TABLE IF NOT EXISTS npc_profiles (id TEXT NOT NULL, name TEXT NOT NULL, role TEXT NOT NULL, personality TEXT NOT NULL, background TEXT NOT NULL, speechStyle TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))")
    db.execSQL("CREATE INDEX IF NOT EXISTS index_npc_profiles_createdAt ON npc_profiles(createdAt)")
    db.execSQL("CREATE TABLE IF NOT EXISTS npc_chat_messages (id TEXT NOT NULL, worldId TEXT NOT NULL, characterId TEXT NOT NULL, role TEXT NOT NULL, text TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))")
    db.execSQL("CREATE INDEX IF NOT EXISTS index_npc_chat_messages_worldId_characterId_createdAt ON npc_chat_messages(worldId, characterId, createdAt)")
   }
  }
 }
}
