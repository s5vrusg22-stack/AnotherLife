package com.anotherlife.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "timeline_branches", indices = [Index("parentWorldId")])
data class TimelineBranchEntity(
    @PrimaryKey val worldId: String,
    val parentWorldId: String?,
    val forkMinute: Long,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface TimelineDao {
    @Query("SELECT * FROM timeline_branches ORDER BY createdAt DESC")
    fun observeBranches(): Flow<List<TimelineBranchEntity>>
}
