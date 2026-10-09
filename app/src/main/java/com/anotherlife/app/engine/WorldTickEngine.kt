package com.anotherlife.app.engine

import com.anotherlife.app.data.NpcMemoryDao
import com.anotherlife.app.data.NpcMemoryRecord
import java.util.UUID

/** Minimal persistent world clock and bounded NPC tick, independent of model inference. */
class WorldTickEngine(private val dao: NpcMemoryDao) {
 suspend fun tick(worldId: String, characterId: String, minute: Long, event: String) {
  require(worldId.isNotBlank() && characterId.isNotBlank())
  require(minute >= 0)
  require(event.isNotBlank() && event.length <= 500)
  dao.save(NpcMemoryRecord(
   UUID.randomUUID().toString(), worldId, characterId,
   "세계 시간 ${minute}분: ${event}", "WORLD_EVENT", 40, System.currentTimeMillis()
  ))
 }
}
