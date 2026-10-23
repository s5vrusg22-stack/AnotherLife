package com.anotherlife.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/** Explicit, directed preferences: no implicit mutual friendship. */
@Entity(tableName = "social_preferences", primaryKeys = ["worldId", "characterId", "targetId"],
    indices = [Index(value = ["worldId", "characterId"])])
data class SocialPreferenceEntity(
    val worldId: String, val characterId: String, val targetId: String,
    val affinity: Int = 50, val blocked: Boolean = false
)

@Dao
interface SocialPreferenceDao {
    @Query("SELECT * FROM social_preferences WHERE worldId=:worldId AND characterId=:characterId")
    suspend fun forCharacter(worldId: String, characterId: String): List<SocialPreferenceEntity>

    @Query("SELECT * FROM social_preferences WHERE worldId=:worldId ORDER BY characterId,targetId")
    fun observe(worldId: String): Flow<List<SocialPreferenceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(value: SocialPreferenceEntity)

    @Query("SELECT * FROM social_preferences WHERE worldId=:worldId AND characterId=:characterId AND targetId=:targetId")
    suspend fun get(worldId: String, characterId: String, targetId: String): SocialPreferenceEntity?
}
