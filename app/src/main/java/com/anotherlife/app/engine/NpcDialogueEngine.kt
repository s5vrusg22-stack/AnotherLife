package com.anotherlife.app.engine

import com.anotherlife.app.ai.LlamaRuntime
import com.anotherlife.app.data.AnotherLifeDatabase
import com.anotherlife.app.data.CharacterMemoryEntity
import com.anotherlife.app.data.DialogueLineEntity
import java.util.UUID

data class NpcDialogueResult(val conversationId: String, val lines: List<DialogueLineEntity>)

/** Runs short local-model conversations while keeping each character's memories private. */
class NpcDialogueEngine(private val db: AnotherLifeDatabase, private val runtime: LlamaRuntime) {
    suspend fun converse(
        worldId: String, senderId: String, recipientId: String, topic: String, turns: Int = 4
    ): NpcDialogueResult {
        require(worldId.isNotBlank() && senderId.isNotBlank() && recipientId.isNotBlank())
        require(senderId != recipientId) { "A character cannot hold a conversation with itself." }
        require(topic.isNotBlank()) { "Conversation topic is required." }
        require(turns in 1..8) { "Keep NPC conversations bounded to eight turns." }

        val dao = db.simulationDao()
        val first = dao.character(worldId, senderId) ?: error("First character not found.")
        val second = dao.character(worldId, recipientId) ?: error("Second character not found.")
        val conversationId = UUID.randomUUID().toString()
        val result = ArrayList<DialogueLineEntity>(turns)
        val transcript = ArrayList<String>(turns)

        repeat(turns) { index ->
            val speaker = if (index % 2 == 0) first else second
            val listener = if (index % 2 == 0) second else first
            val memories = dao.memories(worldId, speaker.id, 12)
            val prompt = buildString {
                appendLine("당신은 오프라인 생활 시뮬레이션 속 한 인물입니다.")
                appendLine("지금 말하는 인물: ${speaker.name}")
                appendLine("역할: ${speaker.role}")
                appendLine("성격: ${speaker.personality}")
                appendLine("배경: ${speaker.background.take(900)}")
                appendLine("대화 상대: ${listener.name} — ${listener.role}")
                appendLine("직접 겪거나 들은 기억만 사실처럼 말하고 상대의 속마음을 단정하지 마세요.")
                appendLine("상대의 행동이나 플레이어의 생각을 대신 결정하지 마세요.")
                appendLine("인물의 말투를 유지하고 감정과 구체적인 대화를 주제에 맞게 이어가세요.")
                if (memories.isNotEmpty()) {
                    appendLine("당신만의 최근 기억:")
                    memories.forEach { appendLine("- ${it.text.take(220)}") }
                }
                if (transcript.isNotEmpty()) {
                    appendLine("이번 대화의 앞부분:")
                    transcript.takeLast(8).forEach { appendLine(it.take(500)) }
                }
                appendLine("이번 대화의 주제: ${topic.take(500)}")
                append("이제 ${speaker.name}의 다음 대사만 자연스러운 한국어로 작성하세요.")
            }
            val generated = runtime.generate(prompt).getOrThrow().trim()
            val spoken = generated.lineSequence().map { it.trim() }
                .filter { it.isNotEmpty() }.joinToString(" ").take(1200)
            require(spoken.isNotBlank()) { "The local model returned an empty line." }

            val line = DialogueLineEntity(
                UUID.randomUUID().toString(), worldId, conversationId, index, speaker.id,
                spoken, System.currentTimeMillis()
            )
            db.dialogueDao().addLine(line)
            result += line
            transcript += "${speaker.name}: $spoken"

            val heard = "${speaker.name}: $spoken"
            listOf(first.id, second.id).forEach { characterId ->
                dao.saveMemory(CharacterMemoryEntity(
                    UUID.randomUUID().toString(), worldId, characterId, heard,
                    "DIRECT", 42, line.createdAt
                ))
            }
        }
        dao.pruneMemories(worldId, first.id, 120)
        dao.pruneMemories(worldId, second.id, 120)
        return NpcDialogueResult(conversationId, result)
    }
}
