package com.anotherlife.app.engine

import com.anotherlife.app.data.AnotherLifeDatabase
import com.anotherlife.app.data.CharacterMoodEntity

/** Gradually returns strong feelings toward neutral as offline world time passes. */
class MoodEngine(private val db: AnotherLifeDatabase) {
    suspend fun decayAt(worldId: String, minute: Long) {
        val dao = db.simulationDao()
        dao.moods(worldId).forEach { mood ->
            if (minute <= mood.updatedAt) return@forEach
            val hours = ((minute - mood.updatedAt) / 60L).coerceAtLeast(1L).coerceAtMost(240L).toInt()
            dao.saveMood(mood.copy(
                joy = towardNeutral(mood.joy, hours),
                anxiety = towardNeutral(mood.anxiety, hours),
                anger = towardNeutral(mood.anger, hours),
                sadness = towardNeutral(mood.sadness, hours),
                relief = towardNeutral(mood.relief, hours),
                updatedAt = minute
            ))
        }
    }

    private fun towardNeutral(value: Int, amount: Int): Int = when {
        value > 50 -> (value - amount).coerceAtLeast(50)
        value < 50 -> (value + amount).coerceAtMost(50)
        else -> 50
    }

    companion object {
        fun initial(worldId: String, characterId: String, minute: Long = 0L) =
            CharacterMoodEntity(characterId, worldId, 50, 50, 50, 50, 50, minute)
    }
}
