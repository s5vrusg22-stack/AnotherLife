package com.anotherlife.app.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity(tableName = "sim_worlds")
data class SimWorldRecord(@PrimaryKey val id: String, val minute: Long = 0L)

@Entity(tableName = "sim_actors", primaryKeys = ["worldId","actorId"])
data class SimActorRecord(val worldId: String, val actorId: String,
 val energy: Int = 70, val hunger: Int = 20, val social: Int = 70)

@Dao
interface WorldStateDao {
 @Query("SELECT * FROM sim_worlds WHERE id = :id")
 suspend fun world(id: String): SimWorldRecord?
 @Query("SELECT * FROM sim_actors WHERE worldId = :worldId ORDER BY actorId")
 suspend fun actors(worldId: String): List<SimActorRecord>
 @Insert(onConflict = OnConflictStrategy.REPLACE)
 suspend fun saveWorld(world: SimWorldRecord)
 @Insert(onConflict = OnConflictStrategy.REPLACE)
 suspend fun saveActors(actors: List<SimActorRecord>)
}

@Database(entities = [SimWorldRecord::class,SimActorRecord::class],version = 1,exportSchema = false)
abstract class WorldStateStore : RoomDatabase() {
 abstract fun worlds(): WorldStateDao
}
