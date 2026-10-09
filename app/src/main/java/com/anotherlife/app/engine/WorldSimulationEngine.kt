package com.anotherlife.app.engine

import androidx.room.withTransaction
import com.anotherlife.app.data.*
import java.util.UUID

/** Deterministic bounded offline simulation. All changes commit atomically. */
class WorldSimulationEngine(private val db: AnotherLifeDatabase) {
 private val dao = db.simulationDao()
 private val needs = setOf("REST","FOOD","SOCIAL","CLOSENESS","ACHIEVEMENT","SECURITY","AUTONOMY","CURIOSITY")

 suspend fun addDailyRoutine(worldId:String, characterId:String, minuteOfDay:Int, action:String,
  need:String?=null, delta:Int=0):String = db.withTransaction {
  require(minuteOfDay in 0..1439 && action.isNotBlank() && (need==null || need in needs))
  require(dao.characters(worldId).any { it.id==characterId })
  val now=dao.world(worldId)?.virtualMinute ?: error("World not found")
  val start=now-now%1440
  val next=start+minuteOfDay+if(start+minuteOfDay<=now)1440 else 0
  val id=UUID.randomUUID().toString()
  dao.insertSchedule(NpcScheduleEntity(id,worldId,characterId,minuteOfDay,1,action.trim(),need,delta.coerceIn(-100,100),next))
  id
 }

 suspend fun advance(worldId:String,minutes:Int):Int = db.withTransaction {
  require(minutes in 1..1440)
  val world=dao.world(worldId) ?: error("World not found")
  val end=Math.addExact(world.virtualMinute,minutes.toLong())
  val actors=dao.characters(worldId)
  require(actors.size<=200)
  check(dao.setClock(worldId,world.virtualMinute,end)==1) { "World clock changed" }
  MoodEngine(db).decayAt(worldId,end)
  val decay=minutes/60
  if(decay>0) for(actor in actors) {
   dao.changeNeed(actor.id,"REST",-decay)
   dao.changeNeed(actor.id,"FOOD",-decay)
   dao.changeNeed(actor.id,"SOCIAL",-(decay/2))
  }
  var count=MoodAutonomyEngine(db).apply(worldId,end,actors)
  while(true) {
   val due=dao.dueSchedules(worldId,end,201)
   if(due.isEmpty())break
   check(count+due.size<=200) { "Tick exceeds event budget" }
   for(schedule in due) {
    if(dao.reschedule(schedule.id,schedule.nextDueMinute,schedule.nextDueMinute+1440L*schedule.intervalDays)!=1)continue
    val id="routine:${schedule.id}:${schedule.nextDueMinute}"
    if(dao.insertEvent(SimulationEventEntity(id,worldId,schedule.characterId,schedule.nextDueMinute,"ROUTINE",schedule.action))!=-1L) {
     schedule.need?.let { dao.changeNeed(schedule.characterId,it,schedule.needDelta) }
     count++
    }
   }
  }
  count
 }
}
