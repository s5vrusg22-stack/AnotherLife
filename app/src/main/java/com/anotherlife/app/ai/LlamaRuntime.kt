package com.anotherlife.app.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** One inference at a time. Cancellation remains callable during generation. */
class LlamaRuntime {
 private val available = runCatching { System.loadLibrary("anotherlife_llama") }.isSuccess
 private val gate = Mutex()
 @Volatile private var ready = false

 suspend fun loadModel(path: String): Result<Unit> = withContext(Dispatchers.IO) {
  gate.withLock {
   runCatching {
    check(available) { "Native llama.cpp library is missing" }
    ready = false
    val message = nativeLoad(path, 16384)
    check(message.isEmpty()) { message }
    ready = true
   }
  }
 }

 suspend fun generate(prompt: String): Result<String> = withContext(Dispatchers.IO) {
  gate.withLock {
   runCatching {
    check(ready) { "Load the model first" }
    val output = StringBuilder()
    val error = nativeGenerate(prompt, object : TokenReceiver {
     override fun onToken(token: String) { output.append(token) }
    })
    check(error.isEmpty()) { error }
    output.toString()
   }
  }
 }

 fun cancel() { if (available) nativeCancel() }

 suspend fun unload() = withContext(Dispatchers.IO) {
  gate.withLock {
   if (available) nativeUnload()
   ready = false
  }
 }

 interface TokenReceiver { fun onToken(token: String) }
 private external fun nativeLoad(path: String, contextSize: Int): String
 private external fun nativeGenerate(prompt: String, receiver: TokenReceiver): String
 private external fun nativeCancel()
 private external fun nativeUnload()
}
