package com.anotherlife.app.ai

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/** Only a user-selected document is read; no broad storage permissions are needed. */
class ModelStore(private val context: Context) {
    private val directory get() = File(context.filesDir, "models").apply { mkdirs() }
    val modelFile get() = File(directory, "qwen3-8b.gguf")
    private val digestFile get() = File(directory, "qwen3-8b.sha256")

    suspend fun import(uri: Uri, onProgress: (Long) -> Unit = {}): String = withContext(Dispatchers.IO) {
        val staged = File(directory, "import.partial")
        val hash = MessageDigest.getInstance("SHA-256")
        var bytes = 0L
        try {
            val source = context.contentResolver.openInputStream(uri)
                ?: error("선택한 파일을 열 수 없습니다.")
            source.use { input ->
                staged.outputStream().buffered().use { output ->
                    val buffer = ByteArray(1024 * 1024)
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        hash.update(buffer, 0, n)
                        bytes += n
                        onProgress(bytes)
                    }
                    output.flush()
                }
            }
            require(bytes > 1_000_000) { "GGUF 파일이 비어 있거나 너무 작습니다." }
            staged.inputStream().use { stream ->
                val header = ByteArray(4)
                require(stream.read(header) == 4 && header.contentEquals("GGUF".toByteArray())) {
                    "GGUF 형식의 모델 파일이 아닙니다."
                }
            }
            val previous = File(directory, "model.previous")
            val previousDigest = File(directory, "checksum.previous")
            val stagedDigest = File(directory, "checksum.partial")
            val checksum = hash.digest().joinToString("") { "%02x".format(it) }
            stagedDigest.writeText(checksum)
            check(!previous.exists() && !previousDigest.exists()) {
                "이전 모델 교체 복구 파일이 남아 있습니다. 먼저 수동으로 확인하세요."
            }
            var movedModel = false
            var movedDigest = false
            try {
                if (modelFile.exists()) {
                    check(modelFile.renameTo(previous)) { "기존 모델 보존 실패" }
                    movedModel = true
                }
                if (digestFile.exists()) {
                    check(digestFile.renameTo(previousDigest)) { "기존 체크섬 보존 실패" }
                    movedDigest = true
                }
                check(staged.renameTo(modelFile)) { "모델 파일 확정 실패" }
                check(stagedDigest.renameTo(digestFile)) { "체크섬 확정 실패" }
                previous.delete()
                previousDigest.delete()
            } catch (failure: Throwable) {
                modelFile.delete()
                digestFile.delete()
                if (movedModel) check(previous.renameTo(modelFile)) { "모델 롤백 실패" }
                if (movedDigest) check(previousDigest.renameTo(digestFile)) { "체크섬 롤백 실패" }
                throw failure
            }
            checksum
        } finally {
            staged.delete()
            File(directory, "checksum.partial").delete()
        }
    }

    suspend fun verify(): Boolean = withContext(Dispatchers.IO) {
        if (!modelFile.isFile || !digestFile.isFile) return@withContext false
        val hash = MessageDigest.getInstance("SHA-256")
        modelFile.inputStream().buffered().use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                hash.update(buffer, 0, n)
            }
        }
        hash.digest().joinToString("") { "%02x".format(it) } == digestFile.readText().trim()
    }
}
