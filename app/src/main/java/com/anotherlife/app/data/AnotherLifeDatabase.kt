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

@Entity(tableName = "al_worlds")
data class SimWorldEntity(@PrimaryKey val id: String, val virtualMinute: Long, val autonomyMode: String)

@Entity(tableName = "al_characters", indices = [Index(value = ["worldId"])])
data class CharacterEntity(
    @PrimaryKey val id: String, val worldId: String, val name: String, val role: String,
    val personality: String, val background: String, val rest: Int, val food: Int,
    val social: Int, val closeness: Int, val achievement: Int, val security: Int,
    val curiosity: Int, val autonomyMode: String
)
typealias SimCharacterEntity = CharacterEntity

@Entity(tableName = "al_character_moods", indices = [Index(value = ["worldId"])])
data class CharacterMoodEntity(
    @PrimaryKey val characterId: String, val worldId: String, val joy: Int, val anxiety: Int,
    val anger: Int, val sadness: Int, val relief: Int, val updatedAt: Long
)

@Entity(tableName = "al_relationships", primaryKeys = ["worldId", "fromCharacterId", "toCharacterId"], indices = [Index(value = ["worldId"])])
data class CharacterRelationshipEntity(
    val worldId: String, val fromCharacterId: String, val toCharacterId: String,
    val trust: Int, val affinity: Int, val blocked: Boolean
)

@Entity(tableName = "al_npc_schedules", indices = [Index(value = ["worldId", "nextDueMinute"])])
data class NpcScheduleEntity(
    @PrimaryKey val id: String, val worldId: String, val characterId: String,
    val minuteOfDay: Int, val intervalDays: Int, val action: String, val need: String?,
    val needDelta: Int, val nextDueMinute: Long
)

@Entity(tableName = "al_simulation_events", indices = [Index(value = ["worldId", "virtualMinute"])])
data class SimulationEventEntity(
    @PrimaryKey val id: String, val worldId: String, val characterId: String,
    val virtualMinute: Long, val type: String, val description: String
)

@Entity(tableName = "al_social_dialogue_jobs", indices = [Index(value = ["worldId", "status", "createdAt"])])
data class SocialDialogueJobEntity(
    @PrimaryKey val id: String, val worldId: String, val senderId: String,
    val recipientId: String, val topic: String, val status: String, val createdAt: Long,
    val conversationId: String?, val error: String?
)

@Entity(tableName = "al_dialogue_lines", indices = [Index(value = ["conversationId", "sequence"], unique = true)])
data class DialogueLineEntity(
    @PrimaryKey val id: String, val worldId: String, val conversationId: String,
    val sequence: Int, val speakerId: String, val text: String, val createdAt: Long
)

@Entity(tableName = "al_character_memories", indices = [Index(value = ["worldId", "characterId", "createdAt"])])
data class CharacterMemoryEntity(
    @PrimaryKey val id: String, val worldId: String, val characterId: String,
    val text: String, val provenance: String, val importance: Int, val createdAt: Long
)

@Entity(tableName = "npc_utterances", indices = [Index(value = ["worldId", "conversationId", "createdAt"])])
data class NpcUtteranceEntity(
    @PrimaryKey val id: String, val worldId: String, val conversationId: String,
    val speakerId: String, val listenerId: String, val text: String, val createdAt: Long
)

@Dao
interface SimulationDao {
    @Query("SELECT * FROM al_worlds WHERE id = :worldId")
    suspend fun world(worldId: String): SimWorldEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveWorld(world: SimWorldEntity)
    @Query("SELECT * FROM al_characters WHERE worldId = :worldId ORDER BY id")
    suspend fun characters(worldId: String): List<SimCharacterEntity>
    @Query("SELECT * FROM al_characters WHERE worldId = :worldId AND id = :characterId LIMIT 1")
    suspend fun character(worldId: String, characterId: String): SimCharacterEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCharacter(character: SimCharacterEntity)
    @Query("UPDATE al_worlds SET virtualMinute = :newMinute WHERE id = :worldId AND virtualMinute = :expectedMinute")
    suspend fun setClock(worldId: String, expectedMinute: Long, newMinute: Long): Int
    @Query("""
        UPDATE al_characters SET
          rest = CASE WHEN :need = 'REST' THEN MIN(100, MAX(0, rest + :delta)) ELSE rest END,
          food = CASE WHEN :need = 'FOOD' THEN MIN(100, MAX(0, food + :delta)) ELSE food END,
          social = CASE WHEN :need = 'SOCIAL' THEN MIN(100, MAX(0, social + :delta)) ELSE social END,
          closeness = CASE WHEN :need = 'CLOSENESS' THEN MIN(100, MAX(0, closeness + :delta)) ELSE closeness END,
          achievement = CASE WHEN :need = 'ACHIEVEMENT' THEN MIN(100, MAX(0, achievement + :delta)) ELSE achievement END,
          security = CASE WHEN :need = 'SECURITY' THEN MIN(100, MAX(0, security + :delta)) ELSE security END,
          curiosity = CASE WHEN :need = 'CURIOSITY' THEN MIN(100, MAX(0, curiosity + :delta)) ELSE curiosity END
        WHERE id = :characterId
    """)
    suspend fun changeNeed(characterId: String, need: String, delta: Int): Int
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: NpcScheduleEntity)
    @Query("SELECT * FROM al_npc_schedules WHERE worldId = :worldId AND nextDueMinute <= :endMinute ORDER BY nextDueMinute, id LIMIT :limit")
    suspend fun dueSchedules(worldId: String, endMinute: Long, limit: Int): List<NpcScheduleEntity>
    @Query("UPDATE al_npc_schedules SET nextDueMinute = :newMinute WHERE id = :id AND nextDueMinute = :expectedMinute")
    suspend fun reschedule(id: String, expectedMinute: Long, newMinute: Long): Int
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEvent(event: SimulationEventEntity): Long
    @Query("SELECT * FROM al_character_moods WHERE worldId = :worldId ORDER BY characterId")
    suspend fun moods(worldId: String): List<CharacterMoodEntity>
    @Query("SELECT * FROM al_character_moods WHERE worldId = :worldId AND characterId = :characterId LIMIT 1")
    suspend fun mood(worldId: String, characterId: String): CharacterMoodEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMood(mood: CharacterMoodEntity)
    @Query("SELECT * FROM al_character_memories WHERE worldId = :worldId AND characterId = :characterId ORDER BY importance DESC, createdAt DESC LIMIT :limit")
    suspend fun memories(worldId: String, characterId: String, limit: Int): List<CharacterMemoryEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMemory(memory: CharacterMemoryEntity)
    @Query("DELETE FROM al_character_memories WHERE worldId = :worldId AND characterId = :characterId AND id NOT IN (SELECT id FROM al_character_memories WHERE worldId = :worldId AND characterId = :characterId ORDER BY importance DESC, createdAt DESC LIMIT :keep)")
    suspend fun pruneMemories(worldId: String, characterId: String, keep: Int): Int
}

@Dao
interface SocialDialogueDao {
    @Query("SELECT * FROM al_social_dialogue_jobs WHERE worldId = :worldId AND status = 'PENDING' ORDER BY createdAt, id LIMIT 1")
    suspend fun next(worldId: String): SocialDialogueJobEntity?
    @Query("UPDATE al_social_dialogue_jobs SET status = 'RUNNING', error = NULL WHERE worldId = :worldId AND id = :id AND status = 'PENDING'")
    suspend fun claim(worldId: String, id: String): Int
    @Query("UPDATE al_social_dialogue_jobs SET status = 'COMPLETED', conversationId = :conversationId, error = NULL WHERE worldId = :worldId AND id = :id AND status = 'RUNNING'")
    suspend fun complete(worldId: String, id: String, conversationId: String): Int
    @Query("UPDATE al_social_dialogue_jobs SET status = 'FAILED', error = :reason WHERE worldId = :worldId AND id = :id AND status = 'RUNNING'")
    suspend fun fail(worldId: String, id: String, reason: String): Int
    @Query("UPDATE al_social_dialogue_jobs SET status = 'SKIPPED' WHERE worldId = :worldId AND id = :id AND status = 'PENDING'")
    suspend fun skip(worldId: String, id: String): Int
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun enqueue(job: SocialDialogueJobEntity): Long
}

@Dao
interface DialogueDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun addLine(line: DialogueLineEntity)
    @Query("SELECT * FROM al_dialogue_lines WHERE conversationId = :conversationId ORDER BY sequence")
    suspend fun lines(conversationId: String): List<DialogueLineEntity>
}

@Database(entities = [
    SimWorldEntity::class, SimCharacterEntity::class, CharacterMoodEntity::class,
    CharacterRelationshipEntity::class, NpcScheduleEntity::class, SimulationEventEntity::class,
    SocialDialogueJobEntity::class, DialogueLineEntity::class, CharacterMemoryEntity::class,
    NpcUtteranceEntity::class, DirectorApprovalEntity::class, ConversationEmotionEventEntity::class,
    SocialPreferenceEntity::class, TimelineBranchEntity::class
], version = 12, exportSchema = false)
abstract class AnotherLifeDatabase : RoomDatabase() {
    abstract fun simulationDao(): SimulationDao
    abstract fun socialDialogueDao(): SocialDialogueDao
    abstract fun dialogueDao(): DialogueDao
    abstract fun directorDao(): DirectorDao
    abstract fun emotionDao(): EmotionDao
    abstract fun socialPreferenceDao(): SocialPreferenceDao
    abstract fun timelineDao(): TimelineDao

    companion object {
        val MIGRATION_1_2 = createMigration(1, 2)
        val MIGRATION_2_3 = createMigration(2, 3)
        val MIGRATION_3_4 = createMigration(3, 4)
        val MIGRATION_4_5 = createMigration(4, 5)
        val MIGRATION_5_6 = createMigration(5, 6)
        val MIGRATION_6_7 = createMigration(6, 7)
        val MIGRATION_7_8 = createMigration(7, 8)
        val MIGRATION_8_9 = createMigration(8, 9)
        val MIGRATION_9_10 = createMigration(9, 10)
        val MIGRATION_10_11 = createMigration(10, 11)
        val MIGRATION_11_12 = createMigration(11, 12)

        private fun createMigration(from: Int, to: Int) = object : Migration(from, to) {
            override fun migrate(db: SupportSQLiteDatabase) = createSchema(db)
        }

        private fun createSchema(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS al_worlds (id TEXT NOT NULL, virtualMinute INTEGER NOT NULL, autonomyMode TEXT NOT NULL, PRIMARY KEY(id))")
            db.execSQL("CREATE TABLE IF NOT EXISTS al_characters (id TEXT NOT NULL, worldId TEXT NOT NULL, name TEXT NOT NULL, role TEXT NOT NULL, personality TEXT NOT NULL, background TEXT NOT NULL, rest INTEGER NOT NULL, food INTEGER NOT NULL, social INTEGER NOT NULL, closeness INTEGER NOT NULL, achievement INTEGER NOT NULL, security INTEGER NOT NULL, curiosity INTEGER NOT NULL, autonomyMode TEXT NOT NULL, PRIMARY KEY(id))")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_al_characters_worldId ON al_characters(worldId)")
            db.execSQL("CREATE TABLE IF NOT EXISTS al_character_moods (characterId TEXT NOT NULL, worldId TEXT NOT NULL, joy INTEGER NOT NULL, anxiety INTEGER NOT NULL, anger INTEGER NOT NULL, sadness INTEGER NOT NULL, relief INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(characterId))")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_al_character_moods_worldId ON al_character_moods(worldId)")
            db.execSQL("CREATE TABLE IF NOT EXISTS al_relationships (worldId TEXT NOT NULL, fromCharacterId TEXT NOT NULL, toCharacterId TEXT NOT NULL, trust INTEGER NOT NULL, affinity INTEGER NOT NULL, blocked INTEGER NOT NULL, PRIMARY KEY(worldId, fromCharacterId, toCharacterId))")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_al_relationships_worldId ON al_relationships(worldId)")
            db.execSQL("CREATE TABLE IF NOT EXISTS al_npc_schedules (id TEXT NOT NULL, worldId TEXT NOT NULL, characterId TEXT NOT NULL, minuteOfDay INTEGER NOT NULL, intervalDays INTEGER NOT NULL, action TEXT NOT NULL, need TEXT, needDelta INTEGER NOT NULL, nextDueMinute INTEGER NOT NULL, PRIMARY KEY(id))")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_al_npc_schedules_worldId_nextDueMinute ON al_npc_schedules(worldId, nextDueMinute)")
            db.execSQL("CREATE TABLE IF NOT EXISTS al_simulation_events (id TEXT NOT NULL, worldId TEXT NOT NULL, characterId TEXT NOT NULL, virtualMinute INTEGER NOT NULL, type TEXT NOT NULL, description TEXT NOT NULL, PRIMARY KEY(id))")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_al_simulation_events_worldId_virtualMinute ON al_simulation_events(worldId, virtualMinute)")
            db.execSQL("CREATE TABLE IF NOT EXISTS al_social_dialogue_jobs (id TEXT NOT NULL, worldId TEXT NOT NULL, senderId TEXT NOT NULL, recipientId TEXT NOT NULL, topic TEXT NOT NULL, status TEXT NOT NULL, createdAt INTEGER NOT NULL, conversationId TEXT, error TEXT, PRIMARY KEY(id))")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_al_social_dialogue_jobs_worldId_status_createdAt ON al_social_dialogue_jobs(worldId, status, createdAt)")
            db.execSQL("CREATE TABLE IF NOT EXISTS al_dialogue_lines (id TEXT NOT NULL, worldId TEXT NOT NULL, conversationId TEXT NOT NULL, sequence INTEGER NOT NULL, speakerId TEXT NOT NULL, text TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_al_dialogue_lines_conversationId_sequence ON al_dialogue_lines(conversationId, sequence)")
            db.execSQL("CREATE TABLE IF NOT EXISTS al_character_memories (id TEXT NOT NULL, worldId TEXT NOT NULL, characterId TEXT NOT NULL, text TEXT NOT NULL, provenance TEXT NOT NULL, importance INTEGER NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_al_character_memories_worldId_characterId_createdAt ON al_character_memories(worldId, characterId, createdAt)")
            db.execSQL("CREATE TABLE IF NOT EXISTS npc_utterances (id TEXT NOT NULL, worldId TEXT NOT NULL, conversationId TEXT NOT NULL, speakerId TEXT NOT NULL, listenerId TEXT NOT NULL, text TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_npc_utterances_worldId_conversationId_createdAt ON npc_utterances(worldId, conversationId, createdAt)")
            db.execSQL("CREATE TABLE IF NOT EXISTS director_approvals (id TEXT NOT NULL, worldId TEXT NOT NULL, characterId TEXT NOT NULL, description TEXT NOT NULL, expectedVirtualMinute INTEGER NOT NULL, status TEXT NOT NULL, createdAt INTEGER NOT NULL, resolvedAt INTEGER, PRIMARY KEY(id), FOREIGN KEY(characterId) REFERENCES al_characters(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_director_approvals_worldId ON director_approvals(worldId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_director_approvals_characterId ON director_approvals(characterId)")
            db.execSQL("CREATE TABLE IF NOT EXISTS conversation_emotion_events (id TEXT NOT NULL, worldId TEXT NOT NULL, conversationId TEXT NOT NULL, speakerId TEXT NOT NULL, listenerId TEXT NOT NULL, category TEXT NOT NULL, evidence TEXT NOT NULL, trustDelta INTEGER NOT NULL, closenessDelta INTEGER NOT NULL, tensionDelta INTEGER NOT NULL, virtualMinute INTEGER NOT NULL, PRIMARY KEY(id))")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_conversation_emotion_events_worldId_conversationId ON conversation_emotion_events(worldId, conversationId)")
            db.execSQL("CREATE TABLE IF NOT EXISTS social_preferences (worldId TEXT NOT NULL, characterId TEXT NOT NULL, targetId TEXT NOT NULL, affinity INTEGER NOT NULL, blocked INTEGER NOT NULL, PRIMARY KEY(worldId, characterId, targetId))")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_social_preferences_worldId_characterId ON social_preferences(worldId, characterId)")
            db.execSQL("CREATE TABLE IF NOT EXISTS timeline_branches (worldId TEXT NOT NULL, parentWorldId TEXT, forkMinute INTEGER NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(worldId))")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_timeline_branches_parentWorldId ON timeline_branches(parentWorldId)")
        }
    }
}
