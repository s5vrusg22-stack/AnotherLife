package com.anotherlife.app.ai

import java.io.File
import java.io.InputStream

/** Transactional GGUF import. The previous valid model is preserved on failure. */
object ModelImport {
 fun install(source: InputStream, target: File): Long {
  val partial = File(target.parentFile, target.name + ".partial")
  try {
   partial.outputStream().buffered(1024 * 1024).use { output ->
    source.use { input -> input.copyTo(output, 1024 * 1024) }
   }
   require(partial.length() > 1024 * 1024) { "GGUF file is too small" }
   partial.inputStream().use { input ->
    val header = ByteArray(4)
    require(input.read(header) == 4 && header.contentEquals(byteArrayOf(71, 71, 85, 70))) {
     "Invalid GGUF header"
    }
   }
   // A failed rename must not destroy the existing working model.
   val backup = File(target.parentFile, target.name + ".backup")
   require(!backup.exists()) { "Previous model backup exists; recover it before importing" }
   if (target.exists()) {
    require(target.renameTo(backup)) { "Cannot preserve previous model" }
   }
   if (!partial.renameTo(target)) {
    if (backup.exists()) check(backup.renameTo(target)) { "Model restore failed; backup remains at ${backup.absolutePath}" }
    error("Cannot install model")
   }
   backup.delete()
   return target.length()
  } finally { partial.delete() }
 }
}
