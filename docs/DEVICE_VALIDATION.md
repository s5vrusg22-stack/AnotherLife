# AnotherLife — Galaxy S25 Ultra offline validation

Do not mark a case as passed until it has been executed on the target device.

1. Install the ARM64 debug APK; verify it launches without a crash.
2. Turn on airplane mode before starting the app; verify no network is required.
3. Import a valid Qwen3 8B GGUF; verify the imported file is retained after restart.
4. Import a corrupt GGUF; verify the previous working file is still present.
5. Load the model; verify a Korean response is generated and is nonempty.
6. Start a long generation, tap Stop; verify generation stops and the app remains responsive.
7. Switch NPC IDs and verify one NPC does not recall the other's private memories.
8. Force-stop and relaunch; verify memories survive and can be used.
9. Run at least 10 consecutive prompts; record latency, RAM usage, and crashes.
10. Keep the app running for 30 minutes; record thermal throttling and battery impact.

Record device build, model SHA-256, quantization, APK commit, test timestamp, and each result.
