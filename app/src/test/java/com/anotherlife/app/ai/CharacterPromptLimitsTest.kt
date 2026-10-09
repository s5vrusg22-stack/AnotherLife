package com.anotherlife.app.ai

import com.anotherlife.app.data.NpcMemoryRecord
import org.junit.Assert.*
import org.junit.Test

class CharacterPromptLimitsTest {
 private fun memory(id: String, text: String, importance: Int = 50) =
  NpcMemoryRecord(id, "world", "npc", text, "DIRECT", importance, 1)
 @Test fun longInputAndMemoryAreBounded() {
  val prompt = CharacterPromptBuilder.build("world", "npc", "Name", listOf(memory("a", "m".repeat(100000))), "u".repeat(100000))
  assertTrue(prompt.length < 10000)
  assertTrue(prompt.contains("u".repeat(6000)))
  assertFalse(prompt.contains("u".repeat(6001)))
 }
 @Test fun duplicateMemoryIdsAreNotRepeated() {
  val prompt = CharacterPromptBuilder.build("world", "npc", "Name",
   listOf(memory("same", "unique-marker"), memory("same", "unique-marker")), "hello")
  assertEquals(1, Regex("unique-marker").findAll(prompt).count())
 }
 @Test fun blankActorAndWorldAreRejected() {
  for ((world, actor) in listOf("" to "npc", "world" to "")) {
   try { CharacterPromptBuilder.build(world, actor, "Name", emptyList(), "hi"); fail("Expected rejection") }
   catch (_: IllegalArgumentException) {}
  }
 }
}
