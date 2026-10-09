package com.anotherlife.app.ai

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.file.Files

class ModelImportTest {
 private fun valid(): ByteArray = ByteArray(1024 * 1024 + 16).also {
  it[0] = 71; it[1] = 71; it[2] = 85; it[3] = 70
 }
 @Test fun importsValidGguf() {
  val dir = Files.createTempDirectory("gguf-test").toFile()
  try {
   val target = dir.resolve("model.gguf")
   assertEquals(valid().size.toLong(), ModelImport.install(ByteArrayInputStream(valid()), target))
   assertTrue(target.exists())
  } finally { dir.deleteRecursively() }
 }
 @Test fun invalidReplacementPreservesExistingModel() {
  val dir = Files.createTempDirectory("gguf-test").toFile()
  try {
   val target = dir.resolve("model.gguf")
   target.writeText("original")
   try {
    ModelImport.install(ByteArrayInputStream(ByteArray(1024 * 1024 + 16)), target)
    fail("Expected invalid GGUF")
   } catch (_: IllegalArgumentException) {}
   assertEquals("original", target.readText())
   assertFalse(dir.resolve("model.gguf.partial").exists())
  } finally { dir.deleteRecursively() }
 }
}
