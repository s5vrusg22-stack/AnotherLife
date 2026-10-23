#!/usr/bin/env bash
# Deterministic build entry point. Requires Android SDK, Gradle wrapper, and optional llama.cpp checkout.
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ ! -x ./gradlew ]]; then
  echo "BLOCKED: Gradle wrapper missing. On a configured build machine run: gradle wrapper --gradle-version 8.11.1" >&2
  exit 2
fi
if [[ -z "${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}" ]]; then
  echo "BLOCKED: ANDROID_HOME or ANDROID_SDK_ROOT is not set" >&2
  exit 2
fi
if [[ "${1:-}" == "native" ]]; then
  [[ -f app/src/main/cpp/third_party/llama.cpp/CMakeLists.txt ]] || { echo 'BLOCKED: pinned llama.cpp checkout required' >&2; exit 2; }
  ./gradlew --no-daemon :app:assembleDebug -PenableLlamaNative=true
else
  ./gradlew --no-daemon :app:assembleDebug
fi
