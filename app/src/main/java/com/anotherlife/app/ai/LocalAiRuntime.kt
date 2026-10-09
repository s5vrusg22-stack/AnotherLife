package com.anotherlife.app.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Offline inference bridge. No network requests or paid API calls. */
class LocalAiRuntime {
    private var loaded = false
    private val libraryAvailable = runCatching { System.loadLibrary("anotherlife_llama") }.isSuccess

    suspend fun load(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(path.isNotBlank()) { "GGUF 모델 파일을 선택하세요." }
            check(libraryAvailable) { "llama.cpp 네이티브 라이브러리가 APK에 없습니다." }
            val error = nativeLoad(path, 16384)
            check(error.isEmpty()) { error }
            loaded = true
        }
    }

    suspend fun respond(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            check(loaded) { "먼저 Qwen3 8B GGUF 모델을 불러오세요." }
            val text = StringBuilder()
            val error = nativeGenerate(prompt, object : TokenReceiver {
                override fun onToken(token: String) { text.append(token) }
            })
            check(error.isEmpty()) { error }
            text.toString()
        }
    }

    suspend fun unload() = withContext(Dispatchers.IO) {
        if (libraryAvailable) nativeUnload()
        loaded = false
    }

    fun cancel() { if (libraryAvailable) nativeCancel() }
    interface TokenReceiver { fun onToken(token: String) }
    private external fun nativeLoad(path: String, contextSize: Int): String
    private external fun nativeGenerate(prompt: String, receiver: TokenReceiver): String
    private external fun nativeCancel()
    private external fun nativeUnload()
}