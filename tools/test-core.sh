#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
mkdir -p "$ROOT/test-output"
kotlinc "$ROOT"/core/src/main/kotlin/com/dennis/lightroombridge/core/*.kt \
  "$ROOT/core/src/test/kotlin/com/dennis/lightroombridge/core/CoreSmokeTest.kt" \
  -include-runtime -d "$ROOT/test-output/core-smoke.jar"
java -cp "$ROOT/test-output/core-smoke.jar" com.dennis.lightroombridge.core.CoreSmokeTestKt
