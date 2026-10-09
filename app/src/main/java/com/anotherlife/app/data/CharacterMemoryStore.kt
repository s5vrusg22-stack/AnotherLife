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

@Database(entities = [NpcMemoryRecord::class], version = 1, exportSchema = false)
abstract class CharacterMemoryStore : RoomDatabase() {
 abstract fun memories(): NpcMemoryDao
}
