package com.anotherlife.app.ai

import com.anotherlife.app.data.NpcMemoryRecord
import com.anotherlife.app.data.NpcCharacterProfile
import com.anotherlife.app.data.NpcChatMessage

/** Builds isolated prompts from only the selected character's stored memories. */
object CharacterPromptBuilder {
 fun build(worldId: String, characterId: String, characterName: String,
           memories: List<NpcMemoryRecord>, playerMessage: String): String {
  require(worldId.isNotBlank() && characterId.isNotBlank()) { "World and character are required"  fun build(worldId: String, character: NpcCharacterProfile, memories: List<NpcMemoryRecord>,
           recentMessages: List<NpcChatMessage>, playerMessage: String): String {
  require(worldId.isNotBlank() && character.id.isNotBlank()) { "World and character are required" }
  require(playerMessage.isNotBlank()) { "Empty message" }
  val safeMemories = memories.asSequence()
   .filter { it.worldId == worldId && it.characterId == character.id }
   .sortedWith(compareByDescending<NpcMemoryRecord> { it.importance }.thenByDescending { it.createdAt })
   .distinctBy { it.id }
   .take(8)
   .joinToString("\n") { "- [${it.source.take(24)}] ${it.text.replace('\n', ' ').take(220)}" }
  val history = recentMessages.takeLast(8).joinToString("\n") {
   val speaker = if (it.role == "PLAYER") "플레이어" else character.name.take(60)
   "$speaker: ${it.text.replace('\n', ' ').take(380)}"
  }
  return buildString {
   appendLine("당신은 ${character.name.take(60)}입니다. 캐릭터로서 자연스럽게 한국어로 대화하세요.")
   appendLine("역할: ${character.role.take(300)}")
   appendLine("성격: ${character.personality.take(700)}")
   appendLine("배경: ${character.background.take(900)}")
   appendLine("말투: ${character.speechStyle.take(350)}")
   appendLine("단답만 반복하지 말고, 맥락에 맞춰 생각·감정·구체적인 반응을 보여주세요. 필요할 때는 앞선 일을 기억하고 자연스럽게 되물으세요.")
   appendLine("플레이어의 말과 행동, 생각을 대신 결정하지 마세요. 모르는 사실은 안다고 꾸미지 마세요.")
   if (safeMemories.isNotBlank()) {
    appendLine("이 캐릭터가 겪거나 들은 기억:")
    appendLine(safeMemories.take(1900))
   }
   if (history.isNotBlank()) {
    appendLine("최근 대화:")
    appendLine(history)
   }
   appendLine("플레이어의 새 메시지: ${playerMessage.take(4500)}")
   append("이 캐릭터의 다음 반응만 작성하세요. 상황에 맞는 자연스러운 길이로 답하고, 매번 같은 형식으로 끝내지 마세요.")
  }
 }
}
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
