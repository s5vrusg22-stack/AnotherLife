package com.anotherlife.app.ai

import com.anotherlife.app.data.NpcMemoryRecord
import org.junit.Assert.*
import org.junit.Test

class CharacterPromptBuilderTest {
 @Test fun excludesOtherWorldAndCharacterMemories() {
  fun memory(id: String, world: String, actor: String, text: String) =
   NpcMemoryRecord(id, world, actor, text, "DIRECT", 70, 1)
  val prompt = CharacterPromptBuilder.build("w1", "alice", "앨리스", listOf(
   memory("1", "w1", "alice", "앨리스의 비밀"),
   memory("2", "w1", "bob", "밥의 비밀"),
   memory("3", "w2", "alice", "다른 세계의 비밀")
  ), "안녕")
  assertTrue(prompt.contains("앨리스의 비밀"))
  assertFalse(prompt.contains("밥의 비밀"))
  assertFalse(prompt.contains("다른 세계의 비밀"))
 }
 @Test fun rejectsBlankMessages() {
  try {
   CharacterPromptBuilder.build("w", "a", "A", emptyList(), " ")
   fail("Expected rejection")
  } catch (_: IllegalArgumentException) {}
 }
}
