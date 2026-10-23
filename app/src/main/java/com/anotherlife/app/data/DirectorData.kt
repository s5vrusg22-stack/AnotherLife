package com.anotherlife.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/** Director-authored major events are proposals until explicitly approved. */
@Entity(tableName = "director_approvals", indices = [Index("worldId"), Index("characterId")],
    foreignKeys = [ForeignKey(entity = CharacterEntity::class, parentColumns = ["id"], childColumns = ["characterId"], onDelete = ForeignKey.CASCADE)])
data class DirectorApprovalEntity(
    @PrimaryKey val id: String,
    val worldId: String,
    val characterId: String,
    val description: String,
    val expectedVirtualMinute: Long,
    val status: String = "PENDING",
    val createdAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null
)

@Dao
interface DirectorDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertApproval(value: DirectorApprovalEntity)

    @Query("SELECT * FROM director_approvals WHERE worldId=:worldId ORDER BY createdAt DESC, id DESC")
    fun observeApprovals(worldId: String): Flow<List<DirectorApprovalEntity>>

    @Query("SELECT * FROM director_approvals WHERE id=:id")
    suspend fun approval(id: String): DirectorApprovalEntity?

    @Query("UPDATE director_approvals SET status=:status, resolvedAt=:at WHERE id=:id AND status='PENDING'")
    suspend fun resolve(id: String, status: String, at: Long): Int
}
