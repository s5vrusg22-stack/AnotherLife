package com.anotherlife.app.engine

import org.junit.Assert.*
import org.junit.Test

class OfflineWorldEngineTest {
 @Test fun timeAndNeedsAdvanceDeterministically() {
  val start=WorldTick(10,listOf(WorldActor("alice")))
  val a=OfflineWorldEngine.advance(start,120)
  val b=OfflineWorldEngine.advance(start,120)
  assertEquals(a,b)
  assertEquals(130L,a.minute)
  assertEquals(68,a.actors.first().energy)
  assertEquals(22,a.actors.first().hunger)
 }
 @Test fun invalidTicksRejected() {
  try { OfflineWorldEngine.advance(WorldTick(0,emptyList()),1441);fail("Expected rejection") }
  catch (_: IllegalArgumentException) {}
 }
}
