package com.anotherlife.app.ai

import com.anotherlife.app.data.NpcMemoryRecord

/** Builds isolated prompts from only the selected character's stored memories. */
object CharacterPromptBuilder {
 fun build(worldId: String, characterId: String, characterName: String,
           memories: List<NpcMemoryRecord>, playerMessage: String): String {
  require(worldId.isNotBlank() && characterId.isNotBlank()) { "World and character are required" }
  require(playerMessage.isNotBlank()) { "Empty message" }
  val safeMemories = memories.asSequence()
   .filter { it.worldId == worldId && it.characterId == characterId }
   .sortedWith(compareByDescending<NpcMemoryRecord> { it.importance }.thenByDescending { it.createdAt })
   .distinctBy { it.id }
   .take(12)
   .joinToString("\n") { "- [${it.source.take(24)}] ${it.text.replace('\n', ' ').take(240)}" }
  return buildString {
   appendLine("당신은 ${characterName.take(60)}입니다. 캐릭터로서 자연스럽게 한국어로 답하세요.")
   appendLine("플레이어의 행동이나 생각을 대신 결정하지 마세요.")
   appendLine("아래 기억은 이 캐릭터에게만 알려진 내용이며, 다른 캐릭터의 지식은 사용할 수 없습니다.")
   appendLine(safeMemories.take(2800))
   appendLine("플레이어: ${playerMessage.take(6000)}")
  }
 }
}
