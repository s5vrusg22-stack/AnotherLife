package com.anotherlife.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/** Auditable, conservative evidence. Scores are heuristics, not psychological facts. */
@Entity(tableName = "conversation_emotion_events", indices = [Index("worldId", "conversationId")])
data class ConversationEmotionEventEntity(
    @PrimaryKey val id: String,
    val worldId: String,
    val conversationId: String,
    val speakerId: String,
    val listenerId: String,
    val category: String,
    val evidence: String,
    val trustDelta: Int,
    val closenessDelta: Int,
    val tensionDelta: Int,
    val virtualMinute: Long
)

@Dao
interface EmotionDao {
    @Query("SELECT * FROM npc_utterances WHERE worldId=:worldId AND conversationId=:conversationId ORDER BY createdAt, id")
    suspend fun utterances(worldId: String, conversationId: String): List<NpcUtteranceEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(value: ConversationEmotionEventEntity)

    @Query("SELECT * FROM conversation_emotion_events WHERE worldId=:worldId ORDER BY virtualMinute DESC LIMIT 100")
    fun observe(worldId: String): Flow<List<ConversationEmotionEventEntity>>
}
