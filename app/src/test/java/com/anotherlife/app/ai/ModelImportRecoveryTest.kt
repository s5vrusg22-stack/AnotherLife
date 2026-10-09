package com.anotherlife.app.ai

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.file.Files

class ModelImportRecoveryTest {
 @Test fun preexistingBackupIsNeverOverwritten() {
  val dir = Files.createTempDirectory("model-backup").toFile()
  try {
   val target = dir.resolve("model.gguf")
   val backup = dir.resolve("model.gguf.backup")
   target.writeText("working")
   backup.writeText("recoverable")
   val data = ByteArray(1024 * 1024 + 8)
   "GGUF".toByteArray().copyInto(data)
   try { ModelImport.install(ByteArrayInputStream(data), target); fail("Expected backup collision") }
   catch (_: IllegalArgumentException) {}
   assertEquals("working", target.readText())
   assertEquals("recoverable", backup.readText())
   assertFalse(dir.resolve("model.gguf.partial").exists())
  } finally { dir.deleteRecursively() }
 }
}
