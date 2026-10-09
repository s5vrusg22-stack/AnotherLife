# AnotherLife AI integration status

Target: Galaxy S25 Ultra, offline Qwen3 8B GGUF, 16,384-token context, temperature 0.7, no automatic online fallback.

The Kotlin offline runtime and native CMake integration points are committed. **The JNI C++ implementation, model GGUF, and end-to-end integration have not yet been committed or validated in this repository.**

This repository currently contains a UI alpha; do not describe it as an AI-capable APK. The source ZIP developed previously includes additional components that must be merged and tested.

Next: commit compatible llama_jni.cpp and its UTF-8 streaming header; compile with llama.cpp pinned to a compatible revision; integrate model selection and conversation state; validate on ARM64 device.
