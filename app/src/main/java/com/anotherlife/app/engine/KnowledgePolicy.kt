package com.anotherlife.app.engine

/** Pure policy: memory is never a canonical world fact merely because an NPC said it. */
object KnowledgePolicy {
    data class Entry(val worldId: String, val ownerId: String, val text: String,
                     val provenance: String, val confidence: Double, val visibility: String)

    fun context(worldId: String, actorId: String, entries: List<Entry>, maxChars: Int = 3000): String {
        require(maxChars in 0..16000)
        return entries.asSequence()
            .filter { it.worldId == worldId && it.ownerId == actorId }
            .filter { it.confidence.isFinite() && it.confidence in 0.0..1.0 }
            .filter { it.provenance in setOf("DIRECT", "TOLD", "INFERRED", "RUMOR", "DIRECTOR", "SYSTEM") }
            .map {
                val status = if (it.provenance in setOf("RUMOR", "TOLD", "INFERRED")) "검증되지 않은 정보" else "개인 기억"
                "[$status; ${it.provenance}; 신뢰도=${(it.confidence * 100).toInt()}%] ${it.text.replace('\n', ' ').take(400)}"
            }.joinToString("\n").take(maxChars)
    }
    fun canDisclose(ownerId: String, actorId: String, visibility: String): Boolean =
        ownerId == actorId && visibility in setOf("PUBLIC", "SHARED")
}
