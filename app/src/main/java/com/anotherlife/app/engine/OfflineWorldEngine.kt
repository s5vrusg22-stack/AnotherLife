package com.anotherlife.app.engine

/** Bounded deterministic offline world tick, independent of AI and Android services. */
data class WorldActor(val id: String, val energy: Int = 70, val hunger: Int = 20, val social: Int = 70)
data class WorldTick(val minute: Long, val actors: List<WorldActor>, val events: List<String>)
object OfflineWorldEngine {
 fun advance(current: WorldTick, minutes: Int): WorldTick {
  require(minutes in 1..1440) { "Tick must be 1..1440 minutes" }
  require(current.actors.size <= 200) { "Too many actors" }
  val next = Math.addExact(current.minute, minutes.toLong())
  val hours = minutes / 60
  val actors = current.actors.map {
   it.copy(energy = (it.energy - hours).coerceIn(0,100),
    hunger = (it.hunger + hours).coerceIn(0,100),
    social = (it.social - hours / 2).coerceIn(0,100))
  }
  val events = actors.filter { it.hunger >= 80 }.map { "${it.id}:NEEDS_FOOD" }
  return WorldTick(next, actors, events.take(200))
 }
}
