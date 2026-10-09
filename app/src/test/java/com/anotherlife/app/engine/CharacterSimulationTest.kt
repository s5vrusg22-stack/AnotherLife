package com.anotherlife.app.engine
import org.junit.Assert.*
import org.junit.Test
class CharacterSimulationTest {
 private val simulation=CharacterSimulation()
 @Test fun moodIsBounded(){assertEquals(100,simulation.updateMood(CharacterState("a","A"),999).mood)}
 @Test fun relationshipIsBounded(){assertEquals(0,simulation.relate(CharacterState("a","A"),"b",-999).relationships["b"])}
 @Test fun autonomousRest(){assertEquals("휴식한다",simulation.decide(CharacterState("a","A",energy=10)).description)}
 @Test fun contextExcludesOtherNpcMemories(){
  val a=simulation.remember(CharacterState("a","A"),"A만 아는 비밀","DIRECT",70,1)
  val b=simulation.remember(CharacterState("b","B"),"B만 아는 비밀","DIRECT",70,1)
  assertFalse(simulation.privateContext(a).contains("B만 아는 비밀"))
  assertFalse(simulation.privateContext(b).contains("A만 아는 비밀"))
 }
}
