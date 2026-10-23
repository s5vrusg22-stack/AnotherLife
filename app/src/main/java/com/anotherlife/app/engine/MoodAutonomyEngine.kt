package com.anotherlife.app.engine

import com.anotherlife.app.data.AnotherLifeDatabase
import com.anotherlife.app.data.SimCharacterEntity
import com.anotherlife.app.data.SimulationEventEntity

/** Bounded, deterministic offline actions; a clock tick never launches surprise model inference. */
class MoodAutonomyEngine(private val db: AnotherLifeDatabase) {
    suspend fun apply(worldId: String, minute: Long, actors: List<SimCharacterEntity>): Int {
        val dao = db.simulationDao()
        var applied = 0
        for (actor in actors.take(200)) {
            val mood = dao.mood(worldId, actor.id) ?: MoodEngine.initial(worldId, actor.id, minute)
            val decision = MoodDecisionPolicy.decide(
                MoodSnapshot(mood.joy, mood.anxiety, mood.anger, mood.sadness, mood.relief),
                rest = actor.rest, security = actor.security, social = actor.social,
                autonomy = actor.autonomyMode
            )
            val description = when (decision.action) {
                MoodAction.REST -> "잠시 쉬며 기운을 회복했다."
                MoodAction.SEEK_SAFETY -> "안심할 수 있는 장소를 찾아 마음을 가라앉혔다."
                MoodAction.TAKE_SPACE -> "혼자 생각을 정리할 시간을 가졌다."
                MoodAction.SELF_CARE -> "기분을 돌보기 위해 좋아하는 일을 했다."
                MoodAction.SOCIALIZE -> "누군가와 이야기를 나누고 싶어 했다."
                MoodAction.PURSUE_GOAL -> "자신의 목표를 향해 한 걸음 나아갔다."
                MoodAction.NONE -> continue
            }
            val id = "autonomy:" + worldId + ":" + actor.id + ":" + minute
            if (dao.insertEvent(SimulationEventEntity(id, worldId, actor.id, minute, "AUTONOMY", description)) == -1L) continue
            applied++
            val updated = when (decision.action) {
                MoodAction.REST -> mood.copy(anxiety = (mood.anxiety - 2).coerceAtLeast(0))
                MoodAction.SEEK_SAFETY -> mood.copy(anxiety = (mood.anxiety - 3).coerceAtLeast(0), relief = (mood.relief + 2).coerceAtMost(100))
                MoodAction.TAKE_SPACE -> mood.copy(anger = (mood.anger - 4).coerceAtLeast(0))
                MoodAction.SELF_CARE -> mood.copy(sadness = (mood.sadness - 3).coerceAtLeast(0), joy = (mood.joy + 2).coerceAtMost(100))
                MoodAction.SOCIALIZE -> mood.copy(joy = (mood.joy + 1).coerceAtMost(100))
                MoodAction.PURSUE_GOAL, MoodAction.NONE -> mood
            }.copy(updatedAt = minute)
            dao.saveMood(updated)
            when (decision.action) {
                MoodAction.REST -> dao.changeNeed(actor.id, "REST", 8)
                MoodAction.SEEK_SAFETY -> dao.changeNeed(actor.id, "SECURITY", 5)
                MoodAction.SOCIALIZE -> dao.changeNeed(actor.id, "SOCIAL", 5)
                MoodAction.PURSUE_GOAL -> dao.changeNeed(actor.id, "ACHIEVEMENT", 4)
                else -> Unit
            }
        }
        return applied
    }
}
